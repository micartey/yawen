package me.micartey.yawen;

import com.google.gson.Gson;
import io.vavr.control.Try;
import me.micartey.yawen.json.LatestRelease;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class YawenRepository {

    private final String apiUrl;
    private final Gson   gson;

    /**
     * Initialise YawenRepository with a github repository.
     * <strong>Username/Repository</strong>
     *
     * @param repository GitHub repository
     */
    public YawenRepository(String repository) {
        this.apiUrl = String.format("https://api.github.com/repos/%s/releases/latest", repository);
        this.gson = new Gson();
    }

    /**
     * Parse the visible website context to String
     *
     * @param url Website URL
     * @return Try of {@link String}
     */
    private Try<String> getWebsiteContent(String url) {
        return Try.ofCallable(() -> {
            Scanner sc = new Scanner(new URL(url).openStream());

            StringBuilder buffer = new StringBuilder();
            while(sc.hasNext())
                buffer.append(sc.next());

            return buffer.toString();
        });
    }

    /**
     * Get the {@link LatestRelease latest release} in case a release is present
     *
     * @return Optional of {@link LatestRelease LatestRelease}
     * @see YawenRepository#getWebsiteContent(String)
     */
    public Optional<LatestRelease> getLatestRelease() {
        return this.getWebsiteContent(this.apiUrl)
                .mapTry(content -> Optional.of(this.gson.fromJson(content, LatestRelease.class)))
                .getOrElse(Optional.empty());
    }

    /**
     * Collect the names of all jar assets
     *
     * @return Set of {@link String}
     */
    public Set<String> getAssetNames() {
        List<String> names = new ArrayList<>();
        this.getLatestRelease().ifPresent(release -> Arrays.stream(release.assetInfos)
                .filter(info -> info.name.endsWith(".jar"))
                .forEach(info -> {
                    names.add(info.name);
                }));
        return new HashSet<>(names);
    }

    /**
     * Load an asset by name from the latest release
     *
     * @param name Name of jar file
     * @return Optional of {@link ClassLoader}
     */
    public Optional<ClassLoader> load(String name) {
        AtomicReference<Optional<ClassLoader>> reference = new AtomicReference<>(Optional.empty());

        this.getLatestRelease().flatMap(release -> Arrays.stream(release.assetInfos)
                .filter(info -> info.name.equals(name))
                .filter(info -> info.state.equals("uploaded"))
                .filter(info -> info.name.endsWith(".jar")).findFirst()).ifPresent(info -> {
            this.loadDependency(info.browserDownloadUrl).onSuccess(classLoader -> {
                reference.set(Optional.of(classLoader));
            });
        });

        return reference.get();
    }

    /**
     * Load an asset from the latest release
     *
     * @return Optional of {@link ClassLoader}
     * @see YawenRepository#load(String)
     */
    public Optional<ClassLoader> load() {
        for(String asset : this.getAssetNames()) {
            Optional<ClassLoader> classLoader = this.load(asset);
            if(classLoader.isPresent()) return classLoader;
        }
        return Optional.empty();
    }
    
    private Try<ClassLoader> loadDependency(String url) {
        return Try.ofCallable(() -> new URLClassLoader(new URL[]{
                new URL(url)
        }, YawenRepository.class.getClassLoader()));
    }
}
