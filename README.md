# Curser

Curser turns your Android phone into a Bluetooth trackpad and keyboard for a laptop or tablet. It uses the Android Bluetooth HID Device profile, so the computer sees the phone as an ordinary Bluetooth mouse and keyboard. You install nothing on the computer. Curser detects whether the host is Windows, macOS or iPadOS and maps your gestures to that system. The look is iOS style, and it works in portrait and landscape.

Current version: 1.0.

## Features

- Trackpad: pointer, tap to click, two-finger right click, two-finger scroll, two-finger zoom, tap-and-drag and optional three-finger drag.
- Multi-finger gestures for three and four fingers (and four or five for pinch and spread). They send the host's own shortcuts, for example Mission Control on macOS or Task View on Windows.
- On-screen keyboard: QWERTY, digits and symbols pages, sticky Ctrl, Alt, Win (Cmd) and Shift, Esc, F1-F12, PrtSc, Ins, Del, Home, End, PgUp, PgDn, Tab and arrow keys.
- Shortcut chips (Copy, Paste, Undo and more) that change with the host system.
- Voice input that types what you say.
- Host system auto-detection, with a manual override per device.
- Settings for speed, scrolling, tapping, haptics and screen orientation.

## Requirements

- Phone: Android 9 (API 28) or newer, with the Bluetooth HID Device profile. Some phones do not include this profile. Curser cannot work on them.
- Host: a Windows PC, a Mac or an iPad with Bluetooth.

## Install

1. Open the [Releases page](https://github.com/harshvardhankulkarni/Curser/releases) on your phone.
2. Download `Curser-1.0.apk`.
3. Open the file. Allow "install unknown apps" for your browser or file manager when Android asks.
4. If you installed a debug build before, uninstall it first. A release build cannot be installed over a debug build.

## Quick start

1. Open Curser and allow the Bluetooth permission.
2. Tap **Pair New Device**. The phone is visible to other devices for 5 minutes.
3. On the computer, open the Bluetooth settings and pick the phone. The [user guide](docs/USER_GUIDE.md) lists the steps for each system.
4. Back in Curser, tap the computer under **My Devices**. The trackpad opens when the connection is made.
5. Move one finger to move the pointer. Tap to click. Use the keyboard button next to the gear to open the keyboard.

## Gestures at a glance

| Gesture | What it does |
|---|---|
| One finger | Move the pointer |
| Tap | Left click |
| Two-finger tap | Right click |
| Two-finger drag | Scroll |
| Two-finger pinch | Zoom |
| Tap, then drag | Drag |
| Three or four-finger swipe or tap | The host's own shortcut (Mission Control, Task View, Home and so on) |

The full table for each system is in the [user guide](docs/USER_GUIDE.md#gestures).

## Keyboard

The keyboard opens with the keyboard button next to the gear on the trackpad screen. In portrait the trackpad is on top and the keyboard is below. In landscape the keyboard is on the left and the trackpad is on the right. Tap Ctrl, Alt, Win (Cmd) or Shift once to apply it to the next key only. Tap it a second time to lock it. Tap it a third time to turn it off. See the [user guide](docs/USER_GUIDE.md#keyboard) for the details.

## Limits

- The host must use a US keyboard layout. The phone sends key positions, so other layouts print other characters.
- Voice input types English letters, digits and common symbols only.
- The scrolling function-key strip keys do not auto-repeat when held.
- Some phones do not ship the Bluetooth HID Device profile. Curser cannot work on them.
- Not every phone and host combination has been tested.

## Build from source

You need JDK 17 and the Android SDK with platform 35.

```
git clone https://github.com/harshvardhankulkarni/Curser.git
cd Curser
gradlew assembleDebug
```

The APK is at `app/build/outputs/apk/debug/app-debug.apk`. See [docs/BUILDING.md](docs/BUILDING.md) for tests, release signing and `adb install`.

## Project layout

```
app/src/main/java/app/curser/
  MainActivity.kt        single activity
  core/
    TrackpadController.kt   the API the UI uses
    Model.kt                state and settings types
    gesture/                touch frames to gesture events
    mapping/                gesture events to HID actions, per host system
    hid/                    HID report descriptor and Bluetooth HID wrapper
    detect/                 host system detection
    keyboard/               key map and shortcut lists
  ui/
    screens/                devices, trackpad, pairing sheet, settings sheet
    keyboard/               keyboard panel, sticky modifiers, voice input
    components/             iOS-style controls
    theme/                  colours, type, motion
```

## More documentation

- [User guide](docs/USER_GUIDE.md)
- [Building from source](docs/BUILDING.md)
- [Architecture](ARCHITECTURE.md)
