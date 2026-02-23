<h3 align="center"><img src="assets/icon/gs_round_256.png" alt="Gaia Sky" width="130px"><br>Gaia Sky - <i>Open source 3D Universe platform</i></h3>

<p align="center">
<a href="https://codeberg.org/gaiasky/gaiasky/releases"><img src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fcodeberg.org%2Fapi%2Fv1%2Frepos%2Fgaiasky%2Fgaiasky%2Freleases%2Flatest&query=%24.tag_name&label=release" alt="Latest release" /></a>
<a href="https://codeberg.org/gaiasky/gaiasky/issues"><img src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fcodeberg.org%2Fapi%2Fv1%2Frepos%2Fgaiasky%2Fgaiasky%2Fissues&query=%24.length&label=open%20issues" alt="Open issues" /></a>
<a href="https://opensource.org/licenses/MPL-2.0"><img src="https://img.shields.io/badge/license-MPL%202.0-brightgreen.svg" alt="License: MPL2.0" /></a>
<a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs"><img src="https://img.shields.io/badge/docs-latest-3245a9" alt="Docs" /></a>
<a href="https://gaia.ari.uni-heidelberg.de/gaiasky/stats"><img src="https://img.shields.io/badge/stats-gaiasky-%234d7" alt="Stats" /></a>
</p>

[**Gaia Sky**](https://gaiasky.space) is a real-time 3D Universe application that runs on Linux, Windows and macOS. It is developed within the framework of [ESA](https://www.esa.int/ESA)'s [Gaia mission](https://www.esa.int/Science_Exploration/Space_Science/Gaia) to chart more than 1 billion stars.

A part of Gaia Sky is described in the paper [Gaia Sky: Navigating the Gaia Catalog](https://dx.doi.org/10.1109/TVCG.2018.2864508).


To get the latest up-to-date and most complete information,

*  Visit our [**home page**](https://gaiasky.space)
*  Read the [**official documentation**](http://docs.gaiasky.space) ([PDF version](https://gaia.ari.uni-heidelberg.de/gaiasky/docs-pdf))
*  Submit a [**bug** or a **feature request**](https://codeberg.org/gaiasky/gaiasky/issues)
*  Follow development news at [@gaiasky.bsky.social](https://bsky.app/profile/gaiasky.bsky.social) or [#GaiaSky@mastodon](https://mastodon.social/tags/GaiaSky)

---

## 1. Overview and Stack

Gaia Sky is a 3D universe platform built using the following technologies:

*   **Language:** Java
*   **Frameworks:** [libGDX](https://libgdx.com/) (core 3D engine), [Ashley](https://github.com/libgdx/ashley) (entity-component system), [LWJGL 3](https://www.lwjgl.org/) (desktop backend).
*   **Package Manager/Build Tool:** [Gradle](https://gradle.org/).
*   **Scripting Interface:** Python (via [Py4J](https://www.py4j.org/)).
*   **VR Support:** OpenXR.

## 2. Requirements

### 2.1 Hardware

| Component             | Minimum requirement                                           |
|-----------------------|---------------------------------------------------------------|
| **Operating system**  | Linux / Windows 10+ / macOS                                   |
| **Architecture**      | x86_64, ARM (only Apple silicon through compat layer)         |
| **CPU**               | Intel Core i5 3rd Generation. 4+ cores recommended            |
| **GPU**               | Support for OpenGL 3.3 (4.2 recommended), 1 GB VRAM           |
| **Memory**            | 4+ GB RAM (depends on loaded datasets)                        |
| **Hard drive**        | 1+ GB of free disk space (depends on downloaded datasets)     |

### 2.2 Software

- **Java Development Kit (JDK):** Gaia Sky is developed on the most recent JDK version. We recommend using at least the latest LTS version (JDK 25+).
- **Git:** To clone the repository.
- **Python (Optional):** For external scripting.

## 3. Setup and Run

### 3.1 Installation from Source

Clone the [Gaia Sky repository](https://codeberg.org/gaiasky/gaiasky):

```console
git clone https://codeberg.org/gaiasky/gaiasky.git
cd gaiasky
```

### 3.2 Running Gaia Sky

**On Linux and macOS:**
Using the provided wrapper script:
```console
./gaiasky
```
Or directly with Gradle:
```console
./gradlew core:run
```

**On Windows:**
Open PowerShell and run:
```powershell
.\gradlew.bat core:run
```

### 3.3 CLI Arguments

Run `./gaiasky -h` to find out about launch arguments. If running with Gradle, use the `--args` flag:
```console
./gradlew core:run --args='-h'
```

### 3.4 Gaia Sky VR

Gaia Sky VR works with [OpenXR](https://registry.khronos.org/OpenXR/)-enabled runtimes.
To run in VR:
```console
# Windows
gradlew.bat core:run --args='-vr'
# Linux
./gradlew core:run --args='-vr'
```

## 4. Scripts and Project Structure

### 4.1 Key Scripts
- `gaiasky`: A bash script to run Gaia Sky with Gradle.
- `gradlew` / `gradlew.bat`: The Gradle wrapper for building and running.
- `makefile`: Primarily for Linux/Debian packaging and installation.
  - `make install`: Installs Gaia Sky to `/opt/gaiasky`.
- `justfile`: Contains commands for Monado-service (OpenXR) management on Linux.

### 4.2 Project Structure
- `core/`: Contains the main Java source code and assets.
  - `src/gaiasky/`: Root of the Java package structure.
  - `src/gaiasky/desktop/GaiaSkyDesktop.java`: Main entry point for the desktop application.
  - `scripts/`: Scripts and utilities to create releases and manipulate datasets.
- `assets/`: Textures, shaders, data, scripts, and internationalization files.
- `gradle/`: Gradle wrapper and configuration.

## 5. Environment Variables

- `JAVA_HOME`: Should point to your JDK installation.
- `XDG_DATA_HOME`, `LOCALAPPDATA`: Used to determine where to store data.
- `GS`: Points to the `gaiasky` project directory.

## 6. Tests

Run the test suite using Gradle:
```console
./gradlew core:test
```

## 7. Documentation and Help

- [**User Manual**](http://docs.gaiasky.space)
- [**Gaia Sky VR Docs**](https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Gaia-sky-vr.html)
- [**DeepWiki Technical Docs**](https://deepwiki.com/langurmonkey/gaiasky)

## 8. License

This software is published and distributed under the **Mozilla Public License 2.0 (MPL 2.0)**. See the [LICENSE.md](LICENSE.md) file for details.

---

### Contributing
Find information about contributing in [CONTRIBUTING.md](CONTRIBUTING.md).

### Acknowledgements
See [ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md) and [AUTHORS.md](AUTHORS.md).

