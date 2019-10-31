# Gaia Sky VR

*This file is only concerned with Gaia Sky VR. If you are looking for the regular desktop Gaia Sky, [check this out](README.md).*

[**Gaia Sky VR**](https://zah.uni-heidelberg.de/gaia/outreach/gaiasky) is the VR version of Gaia Sky. It runs on multiple headsets and operating systems thanks to Valve's [OpenVR](https://github.com/ValveSoftware/openvr), also implemented by [OpenOVR](https://gitlab.com/znixian/OpenOVR). It is developed in the framework of [ESA](http://www.esa.int/ESA)'s [Gaia mission](http://sci.esa.int/gaia) to chart about 1 billion stars of our Galaxy.

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
2. Follow the provided vendor instructions and install the Oculus app with the runtime. If using a SteamVR headset (HTC Vive/Pro, Valve Index, etc.), just get Steam and download SteamVR.
3. For the Oculus, you need a translation layer from OpenVR to LibOVR. You can either use SteamVR (slower) or OpenOVR (faster). We recommend using OpenOVR, as it is much simpler and faster.
    1. **OpenOVR** - Download [OpenOVR's OpenComposite Launcher](https://gitlab.com/znixian/OpenOVR), launch it and select 'Switch to OpenComposite'. That's it.
    2. **SteamVR** - Download and install [Steam](http://store.steampowered.com/) and then install [SteamVR](http://store.steampowered.com/steamvr) and launch it. The SteamVR runtime must be running alongside the Oculus Runtime for it to work.
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

You can use the provided bypass script instead of gradle. Use the `-vr` command line argument to launch Gaia Sky in VR mode.

```
$  gaiasky -vr
```

Run `gaiasky -h` or `man gaiasky` to find out about how to launch Gaia Sky and what arguments are accepted.

### 2.3 Getting the data

As of version `2.1.0`, Gaia Sky offers an automated way to download all data packs and catalogs from within the application. When Gaia Sky starts, if no base data or catalogs are found, the downloader window will prompt automatically. Otherwise, you can force the download window at startup with the `-d` argument (`gradlew core:rund` with the gradle wrapper). Just select the data packs and catalogs that you want to download, press `Download now` and wait for the process to finish.

You can also download the **data packs manually** [here](http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload/).


### 2.6 Common problems

- If you are using an Optimus-powered laptop, make sure that the `java.exe` you are using to run Gaia Sky VR is [set up properly in the Nvidia Control Panel](https://www.pcgamer.com/nvidia-control-panel-a-beginners-guide/) to use the discrete GPU.
- Make sure you are using Java 11+.

##  3. More info

The project's main README file is [here](README.md).
