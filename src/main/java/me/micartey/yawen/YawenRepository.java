package me.micartey.yawen;

import com.google.gson.Gson;
import io.vavr.control.Try;
import lombok.Setter;
import me.micartey.yawen.json.LatestRelease;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class YawenRepository {

    private final String apiUrl;
    private final Gson   gson;

    /**
     * Initialise YawenRepository with a GitHub repository.
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
     * Load an asset by name from the latest release but cache it and update if needed
     *
     * @param name Name of jar file
     * @return Optional of {@link ClassLoader}
     */
    public Optional<ClassLoader> loadCached(String name) {
        AtomicReference<Optional<ClassLoader>> reference = new AtomicReference<>(Optional.empty());

        this.getLatestRelease().flatMap(release -> Arrays.stream(release.assetInfos)
                .filter(info -> info.name.equals(name))
                .filter(info -> info.state.equals("uploaded"))
                .filter(info -> info.name.endsWith(".jar")).findFirst()).ifPresent(info -> {

            boolean update = Try.ofCallable(() -> {
                File parent = new File(".yawen");
                parent.mkdir();

                if(new File(parent, info.id + ".jar").exists())
                    return false;

                // Remove untracked files - we don't need them anymore I guess?
                Arrays.stream(parent.listFiles()).forEach(File::delete);

                URL website = new URL(info.browserDownloadUrl);
                ReadableByteChannel byteChannel = Channels.newChannel(website.openStream());

                try (FileOutputStream stream = new FileOutputStream(".yawen/" + info.id + ".jar")) {
                    stream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
                }

                return true;
            }).get();

            // Logging might be unwanted - but will be added for debugging purposes
            if (update) {
                System.out.println("[yawen] Updated cache!");
            }

            this.loadDependency(new File(".yawen/" + info.id + ".jar")).onSuccess(classLoader -> {
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

    private Try<ClassLoader> loadDependency(File file) {
        return Try.ofCallable(() -> new URLClassLoader(new URL[]{
                file.toURI().toURL()
        }, YawenRepository.class.getClassLoader()));
    }
}
