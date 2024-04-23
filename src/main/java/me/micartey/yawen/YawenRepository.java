package me.micartey.yawen;

import com.google.gson.Gson;
import io.vavr.control.Try;
import me.micartey.yawen.json.Asset;
import me.micartey.yawen.json.Release;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.stream.Collectors;

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
        this.apiUrl = String.format("https://api.github.com/repos/%s/releases", repository);
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
     * Get the {@link Release latest release} in case a release is present
     *
     * @return Optional of {@link Release LatestRelease}
     * @see YawenRepository#getWebsiteContent(String)
     */
    public Optional<Release> getLatestRelease() {
        return this.getWebsiteContent(this.apiUrl + "/latest")
                .mapTry(content -> Optional.of(this.gson.fromJson(content, Release.class)))
                .getOrElse(Optional.empty());
    }

    /**
     * Get all {@link Release releases} to filter through
     *
     * @return Set of releases
     * @see YawenRepository#getWebsiteContent(String)
     */
    public Set<Release> getReleases() {
        return this.getWebsiteContent(this.apiUrl)
                .mapTry(content -> Arrays.stream(this.gson.fromJson(content, Release[].class)).collect(Collectors.toSet()))
                .getOrElse(Collections.emptySet());
    }

    /**
     * Collect the names of all jar assets
     *
     * @return Set of {@link String}
     */
    public Set<String> getAssetNames(Release release) {
        return Arrays.stream(release.assets).filter(info -> info.name.endsWith(".jar"))
                .map(Asset::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Load an asset
     *
     * @param asset Asset
     * @return Optional of {@link ClassLoader}
     */
    public Optional<ClassLoader> load(Asset asset) {
        return this.loadDependency(asset.browserDownloadUrl).toJavaOptional();
    }

    /**
     * Load an asset and cache it
     *
     * @param asset Asset
     * @return Optional of {@link ClassLoader}
     */
    public Optional<ClassLoader> loadCached(Asset asset) {
        boolean update = Try.ofCallable(() -> {
            File parent = new File(".yawen");
            parent.mkdir();

            if (new File(parent, asset.id + ".jar").exists())
                return false;

            URL website = new URL(asset.browserDownloadUrl);
            ReadableByteChannel byteChannel = Channels.newChannel(website.openStream());

            try (FileOutputStream stream = new FileOutputStream(".yawen/" + asset.id + ".jar")) {
                stream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
            }

            return true;
        }).get();

        // Logging might be unwanted - but will be added for debugging purposes
        if (update) {
            System.out.println("[yawen] Updated cache!");
        }

        return this.loadDependency(new File(".yawen/" + asset.id + ".jar")).toJavaOptional();
    }

    /**
     * Load an asset from the latest release
     *
     * @return Optional of {@link ClassLoader}
     * @see YawenRepository#load(Asset)
     */
    public Optional<ClassLoader> load() {
        Release latest = this.getLatestRelease()
                .orElseThrow(() -> new RuntimeException("No release found"));

        Asset asset = Arrays.stream(latest.assets).findFirst()
                .orElseThrow(() -> new RuntimeException("Latest release has no assets"));

        return this.load(asset);
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
