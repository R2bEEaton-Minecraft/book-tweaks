# Book Tweaks

Book Tweaks is a client-side Fabric mod that improves Minecraft's book screens.

## Features

- Jump-to-start and jump-to-end buttons for written and writable books
- Per-book page memory so books reopen on the last page you used
- Writable book navigation fixes across supported Minecraft versions

## Supported Versions

This repository currently builds for:

- `1.21`
- `1.21.1`
- `1.21.2`
- `1.21.3`
- `1.21.4`
- `1.21.5`
- `1.21.6`
- `1.21.7`
- `1.21.8`
- `1.21.9`
- `1.21.10`
- `1.21.11`
- `26.1`
- `26.1.1`
- `26.1.2`
- `26.2`
- `26.3`

Each Minecraft version is built as its own jar. There is no single universal jar for the full range.

## Requirements

- A locally installed JDK 25. Gradle selects it automatically via `gradle/gradle-daemon-jvm.properties`, so `JAVA_HOME` does not need to point at it.
- Fabric Loader and Fabric API versions are selected automatically from `versionProperties/`

## Building

Build a specific Minecraft target with:

```powershell
.\gradlew.bat clean build '-Pmc_ver=1.21.11'
```

Replace `1.21.11` with any supported target version.

Run a client for a specific target with:

```powershell
.\gradlew.bat runClient '-Pmc_ver=26.3'
```

## Release Files

Built jars are written to `build/libs/`.

Upload the normal jar to Modrinth, for example:

- `book-tweaks-1.2.0-1.21.11.jar`
- `book-tweaks-1.2.0-26.3.jar`

Do not upload the `-sources.jar` files.
