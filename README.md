# yawen

<div align="center">
  <a href="https://www.oracle.com/java/">
    <img
      src="https://img.shields.io/badge/Written%20in-java-%23EF4041?style=for-the-badge"
      height="30"
    />
  </a>
  <a href="https://clientastisch.github.io/yawen/docs" target="_blank">
    <img
      src="https://img.shields.io/badge/javadoc-reference-5272B4.svg?style=for-the-badge"
      height="30"
    />
  </a>
</div>

## :books: Introduction

yawen loads GitHub releases into the Java runtime. With the use of yawen you can keep your applcation always up-to-date **without any changes** on the users computer.

### How does it work?

yawen uses the GitHub-Api to identify the latest release and all of its assets.
**It is important that the release contains a compiled .jar file which also contains any dependencies it may uses**.
This jar file will be loaded into an `UrlClassLoader` and thereby loaded into the Java runtime.

## :ballot_box: Usage

```java
YawenRepository repository = new YawenRepository("Username/Repository");
```

### Load by asset name

```java
repository.load("my-release.jar").ifPresent(classLoader -> {
    Class fromRelease = classLoader.loadClass("my.example.project.Class");
    ...
});
```

### Load any asset

```java
repository.load().ifPresent(classLoader -> {
    Class fromRelease = classLoader.loadClass("my.example.project.Class");
    ...
});
```