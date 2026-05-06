# ReMEXA

ReMEXA is a modern Java reimplementation and host runtime for SoftBank MEXA S-Appli, Vodafone V-Appli, and J-SKY V-Appli software.

## Overview

ReMEXA focuses on preserving and running Japanese feature-phone appli on modern desktop systems.

Current areas of work include:

- A Swing-based launcher with drag-and-drop `.jad` loading and recent launch history.
- Direct command-line launch support for `.jad` files.
- Compatibility and runtime support for `javax.microedition`, `com.j_phone`, `com.vodafone`, `com.jblend`, `com.mexa`, and many more APIs.
- Display, input, phone profile, and frame-rate configuration for target device behavior.
- Bitmap and system font rendering support.
- SMAF/MMF audio detection and playback paths for MA-3, MA-5, MA-7(in the future), and host MIDI output.
- Optional virtual Bluetooth-over-IP support for multiplayer titles.
- Debug logging, FPS overlay, host details, RMS dumps, and frame capture helpers for compatibility work.

## Status

ReMEXA is an early work-in-progress project. While most appli launch and run, some may fail because of missing APIs, incomplete hardware behavior, media differences, timing differences, or appli-specific assumptions from original devices.
If you encounter issues, you are encourged to open a issue ticket. 

## Install / Build

Requirements:

- JDK 24 or newer.
- Apache Maven.
- A configured Maven JDK toolchain for JDK 24+ if your Maven setup requires one.
- VLC installed on the host system if you want to exercise VLCJ-backed media features.

Build the nightly jar:

```sh
mvn -DskipTests package
```

Build the release jar:

```sh
mvn -Prelease -DskipTests package
```

## Usage

Open the desktop launcher:

```sh
java -jar target/ReMEXA-Nightly.jar
```

From the launcher, open or drag in a `.jad` file.

Launch a `.jad` directly:

```sh
java -jar target/ReMEXA-Nightly.jar --run-jad path/to/app.jad
```

Useful launch options:

```sh
--show-host-details
--font bitmap|system
--jsky-phone JSKY-Generic|J-SH53
--vodafone-phone Vodafone-Generic|V604SH
--mexa-phone MEXA-Generic|930SH
--host-scale 1|2|3|4|5
--frame-rate uncapped|5|10|15|20|30|60
--bluetooth-backend off|virtual-ip
--bluetooth-role host|client
--bluetooth-local-name <name>
--bluetooth-host <host>
--bluetooth-port <port>
```

Example:

```sh
java -jar target/ReMEXA-Nightly.jar --host-scale 3 --frame-rate 30 --run-jad path/to/app.jad
```

## References / Licenses

ReMEXA is licensed under the GNU General Public License v3.0.
This project is informed by public research and open-source emulator/runtime projects in the Java ME and Japanese feature-phone preservation space. Check each upstream project's current license before copying or porting code.

- [SquirrelJME](https://github.com/squirreljme/squirreljme) - Java ME VM and preservation project.
- [openDoJa](https://github.com/GrenderG/openDoJa) - DoJa 5.1 runtime reimplementation.
- [FreeJ2ME-Plus](https://github.com/TASEmulators/freej2me-plus/tree/devel/src) - J2ME emulator source reference.
- [KEmulator nnmod](https://github.com/shinovon/KEmulator) - KEmulator-derived J2ME emulator reference.
- [vavi-sound](https://github.com/umjammer/vavi-sound) - Java sound, MFi, and SMAF reference work.
- [smaf825](https://github.com/but80/smaf825) - SMAF/MMF playback and dump reference.
