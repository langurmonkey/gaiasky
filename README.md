<h3 align="center"><img src="assets/icon/gs_icon.png" alt="Gaia Sky" width="130px"><br>Gaia Sky - <i>Open source 3D Universe platform</i></h3>

<p align="center">
<a href="https://codeberg.org/gaiasky/gaiasky/releases"><img src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fcodeberg.org%2Fapi%2Fv1%2Frepos%2Fgaiasky%2Fgaiasky%2Freleases%2Flatest&query=%24.tag_name&label=release" alt="Latest release" /></a>
<a href="https://codeberg.org/gaiasky/gaiasky/issues"><img src="https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Fcodeberg.org%2Fapi%2Fv1%2Frepos%2Fgaiasky%2Fgaiasky%2Fissues&query=%24.length&label=open%20issues" alt="Open issues" /></a>
<a href="https://opensource.org/licenses/MPL-2.0"><img src="https://img.shields.io/badge/license-MPL%202.0-brightgreen.svg" alt="License: MPL2.0" /></a>
<a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs"><img src="https://img.shields.io/badge/docs-latest-3245a9" alt="Docs" /></a>
<a href="https://gaia.ari.uni-heidelberg.de/gaiasky/stats"><img src="https://img.shields.io/badge/stats-gaiasky-%234d7" alt="Stats" /></a>
</p>

[**Gaia Sky**](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky) is a real-time 3D Universe application that runs on Linux, Windows and macOS. It is developed within the framework of [ESA](https://www.esa.int/ESA)'s [Gaia mission](https://www.esa.int/Science_Exploration/Space_Science/Gaia) to chart more than 1 billion stars.

A part of Gaia Sky is described in the paper [Gaia Sky: Navigating the Gaia Catalog](https://dx.doi.org/10.1109/TVCG.2018.2864508).


To get the latest up-to-date and most complete information,

*  Visit our [**home page**](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky)
*  Read the [**official documentation**](https://gaia.ari.uni-heidelberg.de/gaiasky/docs) ([PDF version](https://gaia.ari.uni-heidelberg.de/gaiasky/docs-pdf))
*  Submit a [**bug** or a **feature request**](https://codeberg.org/gaiasky/gaiasky/issues)
*  Follow development news at [@gaiasky.bsky.social](https://bsky.app/profile/gaiasky.bsky.social) or [#GaiaSky@mastodon](https://mastodon.social/tags/GaiaSky)

This file contains the following sections:

1. [Installation instructions and requirements](#1-installation-instructions-and-requirements)
2. [Pre-built packages](#2-pre-built-packages)
3. [Running from source](#3-development-branch)
4. [Gaia Sky VR](#4-gaia-sky-vr)
5. [Documentation and help](#5-documentation-and-help)
6. [Copyright and licensing information](#6-copyright-and-licensing-information)
7. [Contact information](#7-contact-information)
8. [Contributing](#8-contributing)
9. [Credits and acknowledgements](#9-acknowledgements)

##  1. Installation instructions and requirements

### 1.1. Requirements

| Component             | Minimum requirement                                           |
|-----------------------|---------------------------------------------------------------|
| **Operating system**  | Linux / Windows 7+ / macOS, x86-64 (ARM CPUs are unsupported) |
| **CPU**               | Intel Core i5 3rd Generation. 4+ cores recommended            |
| **GPU**               | Support for OpenGL 3.3 (4.x recommended), 1 GB VRAM           |
| **Memory**            | 2-6 GB RAM (depends on loaded datasets)                       |
| **Hard drive**        | 1 GB of free disk space (depends on downloaded datasets)      |

### 2. Pre-built packages

This is the Gaia Sky source repository. We recommend using the [pre-built packages](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky/downloads) for the different Operating Systems in case you want a stable and hassle-free experience. We offer pre-built packages for Linux, macOS or Windows [here](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky/downloads/).


### 3. Development branch

In order to compile and run Gaia Sky using the `master` branch sources, you need the following installed in your system:

- `JDK`, latest LTS version recommended
- `git`

First, clone the [Gaia Sky repository](https://codeberg.org/gaiasky/gaiasky):

```console
git clone https://codeberg.org/gaiasky/gaiasky.git
```

Then, run Gaia Sky (Linux, macOS) with the provided script:

```console
cd gaiasky
./gaiasky
```

On Windows, open PowerShell, make sure your `$JAVA_HOME` environment variable points to a valid JDK installation, and run:

```batchfile
.\gradlew.bat core:run
```

Et voilà! The bleeding edge Gaia Sky is running in your machine.


### 3.1 CLI arguments

Run `gaiasky -h` or `man gaiasky` to find out about how to launch Gaia Sky and what arguments are accepted.

If running directly with gradle, you can add arguments using the gradle `--args` flag, like this: 

```
gradlew core:run --args='-h'
```

### 3.2 Getting the data

As of version `2.1.0`, Gaia Sky offers an integrated way to download and manage all datasets and catalogs from within the application. The dataset manager, accessible from the welcome screen, enables browsing and downloading available datasets, and enabling and disabling already installed/downloaded datasets.

You can also download the **datasets manually** [here](https://gaia.ari.uni-heidelberg.de/gaiasky/repository/). Once downloaded, the datasets, which usually come in `.tar.gz` packages, can be extracted directly in the [Gaia Sky data directory](https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Folders.html#dataset-location).

##  4. Gaia Sky VR

Gaia Sky VR works with [OpenXR](https://registry.khronos.org/OpenXR/)-enabled runtimes to interface with virtual reality sets. 

Run Gaia Sky in VR using the `-vr` flag from the CLI, or, on Windows, run the `gaiaskyvr.exe` file.

```console
gaiasky -vr
```

The most up-to-date information on Gaia Sky VR, as well as how to install and run it, is available in the official documentation:

- [Gaia Sky VR documentation](https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Gaia-sky-vr.html)


##  5. Documentation and help

The most up-to-date documentation of Gaia Sky is always hosted at [gaia.ari.uni-heidelberg.de/gaiasky/docs](https://gaia.ari.uni-heidelberg.de/gaiasky/docs). The documentation source repository is hosted [here](https://codeberg.org/gaiasky/gaiasky-docs).


##  6. Copyright and licensing information

This software is published and distributed under the MPL 2.0 (Mozilla Public License 2.0). You can find the [full license text here](LICENSE.md) or visiting [opensource.org/licenses/MPL-2.0](https://opensource.org/licenses/MPL-2.0).

##  7. Contact information

The main webpage of the project is [zah.uni-heidelberg.de/gaia/outreach/gaiasky](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky). There you can find the latest versions and the latest information on Gaia Sky.

##  8. Contributing

Find information about contributing translations, code or ideas in the [CONTRIBUTING.md](CONTRIBUTING.md) file.

##  9. Acknowledgements

The latest acknowledgements are always in the [ACKNOWLEDGEMENTS.md](ACKNOWLEDGEMENTS.md) file.

