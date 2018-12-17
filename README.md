![Gaia Sky](https://zah.uni-heidelberg.de/fileadmin/user_upload/gaia/gaiasky/img/GaiaSkyBanner-vr.jpg)
--------------------------

[![Documentation Status](https://readthedocs.org/projects/gaia-sky/badge/?version=latest)](http://gaia-sky.readthedocs.io/en/latest/?badge=latest)
[![Build status](https://gitlab.com/langurmonkey/gaiasky/badges/master/pipeline.svg)](https://gitlab.com/langurmonkey/gaiasky/commits/master)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)
[![GitHub issues](https://img.shields.io/github/issues/langurmonkey/gaiasky.svg)](https://github.com/langurmonkey/gaiasky/issues)
[![GitHub forks](https://img.shields.io/github/forks/langurmonkey/gaiasky.svg)](https://github.com/langurmonkey/gaiasky/network)
[![GitHub tag](https://img.shields.io/github/tag/langurmonkey/gaiasky.svg)](https://github.com/langurmonkey/gaiasky/tags/)

[**Gaia Sky VR**](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky) is a real-time, 3D, astronomy VR software that
runs on multiple headsets and operating systems thanks to Valve's [OpenVR](https://github.com/ValveSoftware/openvr). It is developed in the framework of [ESA](http://www.esa.int/ESA)'s [Gaia mission](http://sci.esa.int/gaia) to chart about 1 billion stars of our Galaxy.
To get the latest up-to-date and most complete information,

*  Visit our [**home page**](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky)
*  Read the [**Documentation**](http://gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest) for the non-VR version
*  Submit a [**bug** or a **feature request**](https://gitlab.com/langurmonkey/gaiasky/issues)
*  Follow development news at [@GaiaSky_Dev](https://twitter.com/GaiaSky_Dev)

This file contains the following sections:

1. [Running Gaia Sky VR](#1-running-gaia-sky-vr)
2. [Documentation and help](#2-documentation-and-help)
3. [Copyright and licensing information](#3-copyright-and-licensing-information)
4. [Contact information](#4-contact-information)
5. [Credits and acknowledgements](#5-acknowledgements)


## 1. Running Gaia Sky VR

The Gaia Sky VR project is the Virtual Reality version of Gaia Sky. At the moment, only [OpenVR](https://github.com/ValveSoftware/openvr) is supported, but nothing prevents us from supporting other APIs (like the Kronos Group's [OpenXR](https://www.khronos.org/openxr)) in the future if it makes sense. Our tests have only been carried out with the Oculus Rift CV1 headset in direct mode under Windows. Supporting Linux is a top priority for us, and the HTC Vive should work well under Linux, even though the state of OpenVR in the platform is a bit rough. We have reports indicating that the HTC Vive VR controllers' mappings are not fully working.

Also, we want to point out that Linux support for the Oculus Rift was dropped for the CV1 and it is not expected to be continued any time soon, unfortunately.

Gaia Sky VR is heavily under development, and it is not guaranteed to work. Currently, no binaries are provided, but it can still be run by compiling the source. Just keep in mind that this is the developmen branch.

### 1.1. Pre-requisites

This guide is for running Gaia Sky VR with the Oculus Rift in Windows. You will need the following: 

1. Download and install [Git for Windows](http://gitforwindows.org/) and get used to the unix-like command line interface.
2. If you are using the Oculus Rift headset, follow the provided instructions and install the Oculus app with the runtime.
3. Download and install [Steam](http://store.steampowered.com/) and then install [SteamVR](http://store.steampowered.com/steamvr).
4. JDK 1.8

### 1.2. Cloning the repository

First, open the Git for Windows CLI and clone the [Gitlab](https://gitlab.com/langurmonkey/gaiasky) repository and checkout the `2.0.0-vr` tag. This should give you a working version:

```
$  git clone https://github.com/langurmonkey/gaiasky.git
$  cd gaiasky
$  git checkout tags/2.0.2-vr
```

You can also use the `vr` branch directly (`git checkout vr`), but since it is a development branch, it is not guaranteed to work.


### 1.4. Running

To run Gaia Sky VR, make sure that both the Oculus runtime and Steam VR are running. Then, run Gaia Sky through gradle. The first time it will pull lots of dependencies and compile the whole project, so it may take a while.

```
$  gradlew.bat core:run
```

**Tip**: Gaia Sky will check that you are using Java 1.8 when running the build. You can still use a newer JDK version (e.g. JDK 10) by setting the following environment variable to `false` in the context of gradle:

```
$  export GS_JAVA_VERSION_CHECK=false
```


### 1.5 CLI arguments

Gaia Sky accepts a few command-line arguments:

```
Usage: gaiasky [options]
 Options:
    -c, --cat-chooser
      Displays the catalog chooser dialog at startup
      Default: false
    -d, --ds-download
      Displays the download dialog at startup
      Default: false
    -h, --help
      Shows help
    -v, --version
      Lists version and build inforamtion
      Default: false
```

### 2.5 Getting the data

As of version `2.1.0`, Gaia Sky offers an automated way to download all data packs and catalogs from within the application. When Gaia Sky starts, if no base data or catalogs are found, the downloader window will prompt automatically. Otherwise, you can force the download window at startup with the `-d` argument (`gradlew core:rund` with the gradle wrapper). Just select the data packs and catalogs that you want to download, press `Download now` and wait for the process to finish.

You can also download the **data packs manually** [here](http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload/).


##  3. Documentation and help

The most up-to-date documentation of Gaia Sky is always in [gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest](http://gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest). For older versions and other formats, see [here](http://gaia.ari.uni-heidelberg.de/gaiasky/docs).

We also have a mirror at [gaia-sky.rtfd.org](https://gaia-sky.readthedocs.io).

### 3.1. Documentation submodule

In order to add the documentation submodule to the project, do:

```
$  git submodule init
$  git submodule update
```

The documentation project will be checked out in the `docs/` folder.


##  4. Copyright and licensing information

This software is published and distributed under the MPL 2.0
(Mozilla Public License 2.0). You can find the full license
text here https://gitlab.com/langurmonkey/gaiasky/blob/master/LICENSE.md
or visiting https://opensource.org/licenses/MPL-2.0

##  5. Contact information

The main webpage of the project is
**[https://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky](https://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky)**. There you can find
the latest versions and the latest information on Gaia Sky.

##  6. Acknowledgements

The latest acknowledgements are always in the [ACKNOWLEDGEMENTS.md](https://gitlab.com/langurmonkey/gaiasky/blob/master/ACKNOWLEDGEMENTS.md) file.

