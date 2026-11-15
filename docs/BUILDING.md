# Building Curser

This guide shows you how to build, test and sign Curser from source.

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 17 |
| Android SDK | Platform 35 (`compileSdk` and `targetSdk` are 35, `minSdk` is 28) |
| Gradle | 8.11.1, from the Gradle wrapper in the repository. You do not install it yourself. |

The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21 and the Compose BOM 2024.12.01.

Tell Gradle where the SDK is. Create `local.properties` in the repository root:

```
sdk.dir=C\:\\path\\to\\Android\\sdk
```

On Linux or macOS, write the plain path, for example `sdk.dir=/home/you/Android/Sdk`.

Set `JAVA_HOME` to your JDK 17 folder before you run Gradle.

`build.bat` is a Windows helper. It sets `JAVA_HOME` to `%USERPROFILE%\.android-dev\jdk17`, then runs `gradlew.bat assembleDebug testDebugUnitTest`. If your JDK is somewhere else, set `JAVA_HOME` yourself and run `gradlew` directly. `local.properties` is git-ignored.

## Get the code

```
git clone https://github.com/harshvardhankulkarni/Curser.git
cd Curser
```

## Debug build

```
gradlew assembleDebug
```

On Linux and macOS use `./gradlew`. The APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Tests

```
gradlew testDebugUnitTest
```

This runs 46 JVM unit tests. They need no phone or emulator.

| Test file | Covers |
|---|---|
| `app/src/test/java/app/curser/core/GestureEngineTest.kt` | Touch frames to gesture events |
| `app/src/test/java/app/curser/core/GestureMapperTest.kt` | Gesture events to HID actions |
| `app/src/test/java/app/curser/core/HidReportTest.kt` | HID report bytes |
| `app/src/test/java/app/curser/core/HostDetectorTest.kt` | Host system detection |
| `app/src/test/java/app/curser/core/KeyMapTest.kt` | Characters to keys, shortcut lists |
| `app/src/test/java/app/curser/ui/keyboard/StickyModsTest.kt` | Sticky modifier states |

## Release build and signing

```
gradlew assembleRelease
```

A signed release build needs a `keystore.properties` file in the repository root. The file is git-ignored, so keep it out of the repository. Without it, the release build is unsigned and Android will not install it.

1. Create a keystore. Replace the placeholder values with your own:

   ```
   keytool -genkeypair -v -keystore curser-release.jks -alias curser-key -keyalg RSA -keysize 2048 -validity 10000
   ```

   `keytool` asks for a password and for your details. Keep the keystore file and the passwords safe. You need the same key for every update.

2. Create `keystore.properties` in the repository root:

   ```
   storeFile=C:/path/to/curser-release.jks
   storePassword=YOUR_STORE_PASSWORD
   keyAlias=curser-key
   keyPassword=YOUR_KEY_PASSWORD
   ```

3. Run `gradlew assembleRelease`.

A release build cannot be installed over a debug build, because the signing keys differ. Uninstall the old build from the phone first.

## Install with adb

Turn on USB debugging on the phone and connect it. Then run:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

To install a different APK file, replace the path. If `adb` reports a signature mismatch, run `adb uninstall app.curser` and install again.

## Project layout

Curser is one Gradle module, `app`. The package and application id are both `app.curser`.

```
build.gradle.kts, settings.gradle.kts, gradle.properties   root build files
gradlew, gradlew.bat, gradle/                              Gradle wrapper
build.bat                                                  Windows helper
ARCHITECTURE.md                                            design contract
app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/res/                                            icons, strings, theme
  src/main/java/app/curser/
    MainActivity.kt
    core/                    no UI code
      TrackpadController.kt  the API the UI uses
      Model.kt               state and settings types
      gesture/               GestureEngine
      mapping/               GestureMapper, GestureTable
      hid/                   HidDescriptor, HidPeripheral
      detect/                HostDetector
      keyboard/              KeyMap
    ui/
      CurserApp.kt, CurserViewModel.kt
      screens/               DevicesScreen, TrackpadScreen, PairingSheet, SettingsSheet
      keyboard/              KeyboardPanel, StickyMods, VoiceInput
      components/            iOS-style controls and glyphs
      theme/
  src/test/java/app/curser/  unit tests (listed above)
```

`core` never imports `ui`. The UI talks to `core` only through `TrackpadController`. [ARCHITECTURE.md](../ARCHITECTURE.md) describes the HID report descriptor, the gesture engine, host detection and the keyboard in detail.

## Permissions in the manifest

| Permission | Why |
|---|---|
| `BLUETOOTH`, `BLUETOOTH_ADMIN` (Android 11 and older only) | Bluetooth on older Android versions |
| `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` | Connect to the host and appear as a Bluetooth device |
| `VIBRATE` | Haptic feedback |
| `RECORD_AUDIO` | Voice input on the keyboard |

The manifest also declares the Bluetooth hardware feature as required.
