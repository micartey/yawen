# yawen

<div align="center">
  <a href="https://www.oracle.com/java/">
    <img
      src="https://img.shields.io/badge/Written%20in-java-%23EF4041?style=for-the-badge"
      height="30"
    />
  </a>
  <a href="https://jitpack.io/#micartey/yawen/master-SNAPSHOT">
    <img
      src="https://img.shields.io/badge/jitpack-master-%2321f21?style=for-the-badge"
      height="30"
    />
  </a>
  <a href="https://micartey.github.io/yawen/docs" target="_blank">
    <img
      src="https://img.shields.io/badge/javadoc-reference-5272B4.svg?style=for-the-badge"
      height="30"
    />
  </a>
</div>

## :books: Introduction

yawen loads GitHub releases into the Java runtime. With the use of yawen you can keep your application always up-to-date **without any changes** to the end user.

### How does it work?

yawen uses the GitHub-Api to get the releases and all of its assets.
An asset can be loaded through an URLClassLoader and thus contains all classes from that jar.
Only assets that are jar files can be loaded.

## :ballot_box: Usage

First of all, you need to create a new `YawenRepository` object by instanciating it with the GitHub user and repository name: **Username/Repository**

```java
YawenRepository repository = new YawenRepository("Username/Repository");
```

### Load an Asset

```java
Optional<Release> optRelease = repository.getLatestRelease();

optRelease.ifPresent(release -> {
    Asset asset = Arrays.stream(release.getAssets()).filter(asset -> asset.name.equals("dependency.jar"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No asset found"));
    
    repository.load(asset).ifPresent(classLoader -> {
        Class fromRelease = classLoader.loadClass("my.example.project.Class");
        // ...
    });
});

```