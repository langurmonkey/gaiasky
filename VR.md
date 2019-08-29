# Gaia Sky VR

[**Gaia Sky VR**](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky) is the VR version of Gaia Sky. It runs on multiple headsets and operating systems thanks to Valve's [OpenVR](https://github.com/ValveSoftware/openvr), also implemented by [OpenOVR](https://gitlab.com/znixian/OpenOVR). It is developed in the framework of [ESA](http://www.esa.int/ESA)'s [Gaia mission](http://sci.esa.int/gaia) to chart about 1 billion stars of our Galaxy.

A part of Gaia Sky is described in the paper [Gaia Sky: Navigating the Gaia Catalog](http://dx.doi.org/10.1109/TVCG.2018.2864508).

<img src="header.jpg" alt="Gaia Sky header" style="max-height: 20em;" />

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

Gaia Sky VR is heavily under development, and it is not guaranteed to work. Currently, no binaries are provided, but it can still be run by compiling the source. Just keep in mind that this is the development branch.

### 1.1. Pre-requisites

The minimum system requirements for running Gaia Sky VR are as following:

| | |
|-|-|
| **VR Headset**        | [OpenVR](https://en.wikipedia.org/wiki/OpenVR)-compatible (Oculus Rift, HTC Vive)     |
| **Operating system**  | Linux (untested) / Windows 10                                                         |
| **CPU**               | Intel Core i5 3rd Generation or similar. 4 core or higher recommended                 |
| **GPU**               | VR-capable GPU (GTX 970 or above)                                                     |
| **Memory**            | 8+ GB RAM                                                                             |
| **Hard drive**        | 1 GB of free disk space (depending on datasets)                                       |

From now on, this guide will assume you aim at running Gaia Sky VR with the Oculus Rift in Windows, as that's the only headset we currently have access to, which is unfortunately Windows-only. You can still run it on Linux if you have a compatible headset like the HTC Vive or the Valve Index.

You will need the following:

1. Download and install [Git for Windows](http://gitforwindows.org/) or [cygwin](https://www.cygwin.com/) and get used to the unix-like command line interface.
2. Follow the provided vendor instructions and install the Oculus app with the runtime. If using a SteamVR headset, just get Steam and download SteamVR.
3. OpenVR to LibOVR. GaiaSky uses the OpenVR API. It is implemented by SteamVR but also by OpenOVR. The latter is much much faster.
  3.1. OpenOVR is the faster option. Download [OpenOVR's OpenComposite Launcher](https://gitlab.com/znixian/OpenOVR), launch it and select 'Switch to OpenComposite'. That's it.
  3.2. Or, use SteamVR, which is the default option but much slower. Download and install [Steam](http://store.steampowered.com/) and then install [SteamVR](http://store.steampowered.com/steamvr).
4. [OpenJDK 11+](https://jdk.java.net/java-se-ri/11).
5. A setup [VR-ready rig](https://www.digitaltrends.com/virtual-reality/how-to-build-a-cheap-vr-ready-pc/).

### 1.2. Cloning the repository

First, open the Git or Cygwin CLI and clone the [Gitlab](https://gitlab.com/langurmonkey/gaiasky) repository. Right now only the master branch contains the version which shares the codebase with the desktop application, but starting with `2.2.1`, you'll need to check out a tag to get a guaranteed working version. This should give you a working version:

```
$  git clone https://github.com/langurmonkey/gaiasky.git
$  cd gaiasky
$  git checkout master
```

### 1.4. Running

To run Gaia Sky VR, make sure that both the Oculus runtime and Steam VR are running. Then, run Gaia Sky through gradle. The first time it will pull lots of dependencies and compile the whole project, so it may take a while.

```
$  gradlew.bat core:runvr
```


### 1.5 CLI arguments

Run `gaiasky -h` or `man gaiasky` to find out about how to launch Gaia Sky and what arguments are accepted.

### 2.3 Getting the data

As of version `2.1.0`, Gaia Sky offers an automated way to download all data packs and catalogs from within the application. When Gaia Sky starts, if no base data or catalogs are found, the downloader window will prompt automatically. Otherwise, you can force the download window at startup with the `-d` argument (`gradlew core:rund` with the gradle wrapper). Just select the data packs and catalogs that you want to download, press `Download now` and wait for the process to finish.

You can also download the **data packs manually** [here](http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload/).


### 2.6 Common problems

- If you are using an Optimus-powered laptop, make sure that the `java.exe` you are using to run Gaia Sky VR is [set up properly in the Nvidia Control Panel](https://www.pcgamer.com/nvidia-control-panel-a-beginners-guide/) to use the discrete GPU.
- Make sure you are using Java 11+.

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
(Mozilla Public License 2.0). You can find the [full license
text here](/LICENSE.md)
or visiting https://opensource.org/licenses/MPL-2.0.

##  5. Contact information

The main webpage of the project is
**[https://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky](https://www.zah.uni-heidelberg.de/gaia/outreach/gaiasky)**. There you can find
the latest versions and the latest information on Gaia Sky.

##  6. Acknowledgements

The latest acknowledgements are always in the [ACKNOWLEDGEMENTS.md](/ACKNOWLEDGEMENTS.md) file.

