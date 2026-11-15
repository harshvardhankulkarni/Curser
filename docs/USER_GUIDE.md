# Curser user guide

This guide shows you how to pair your phone with a computer, use the trackpad and the keyboard, and fix common problems. For a short overview, see the [README](../README.md).

Contents: [Pair and connect](#pair-and-connect) | [Gestures](#gestures) | [Settings](#settings) | [Keyboard](#keyboard) | [Landscape](#landscape) | [Troubleshooting](#troubleshooting) | [Limits](#limits)

## Pair and connect

You pair once. After that you connect with one tap.

### Permissions

Curser asks for these permissions.

| Permission | Why |
|---|---|
| Bluetooth (Connect, Advertise) | To appear to your computer as a Bluetooth trackpad and keyboard. On Android 11 and older the older Bluetooth permissions are used instead. |
| Vibrate | For haptic feedback. You can turn it off in Settings. |
| Microphone | Only for voice input on the keyboard. |

If Bluetooth permission is missing, the Curser home screen shows **Allow Bluetooth**. If Bluetooth is off, it shows **Turn On Bluetooth**.

### Pair a new computer

1. Open Curser on the phone.
2. Tap **Pair New Device**. Android asks to make the phone visible. Accept. The phone stays visible for 5 minutes.
3. Keep the Curser screen open. Do the steps for your computer below.
4. When the computer shows the phone as connected or paired, the computer appears under **My Devices** in Curser.

On a Mac:

1. Open System Settings, then Bluetooth.
2. Find the phone under Nearby Devices.
3. Click Connect.
4. If the Keyboard Setup Assistant window appears, close it.

On Windows:

1. Open Settings, then Bluetooth & devices.
2. Click Add device, then Bluetooth.
3. Pick the phone from the list.

On an iPad:

1. Open Settings, then Bluetooth.
2. Pick the phone under Other Devices.

The same steps are in the **Pair New Device** sheet in the app. Use the Mac / Windows / iPad switch at the top of the sheet to see them.

If the phone was paired with the computer before you installed Curser, remove ("Forget") the pairing on both the phone and the computer. Then pair again while the Pair New Device sheet is open.

### Connect

1. Open Curser.
2. Tap the computer under **My Devices**. A spinner shows while it connects. The row then shows **Connected**.
3. The trackpad screen opens by itself when the connection is made. If the computer connects first, the trackpad opens as well.

The trackpad only works while Curser is on the screen. If you leave the app, the connection stops. Tap the back arrow at the top left of the trackpad to disconnect and return to the device list.

## Gestures

Curser detects the host system and uses its own shortcuts. You can change the detected system in [Settings](#settings). The same table also shows inside the app, in the settings sheet, for the system in use.

### Gestures that are the same on every system

| Gesture | Action |
|---|---|
| Move one finger | Move the pointer |
| Tap with one finger | Left click (needs **Tap to Click**) |
| Tap with two fingers | Right click |
| Drag with two fingers | Scroll |
| Pinch out / in with two fingers | Zoom in / out |
| Tap, then drag | Drag (needs **Tap and Drag**) |
| Three-finger drag | Drag (needs **Three-finger drag**) |

Zoom sends Ctrl and the mouse wheel on Windows. On macOS and iPadOS it sends Cmd and plus or minus.

### macOS

| Gesture | Action | Host shortcut |
|---|---|---|
| Swipe up with three fingers | Mission Control | Ctrl+Up |
| Swipe down with three fingers | App Exposé | Ctrl+Down |
| Swipe left with three fingers | Next Space | Ctrl+Right |
| Swipe right with three fingers | Previous Space | Ctrl+Left |
| Swipe up with four fingers | Mission Control | Ctrl+Up |
| Swipe down with four fingers | App Exposé | Ctrl+Down |
| Swipe left with four fingers | Switch Apps | Cmd+Tab (see below) |
| Swipe right with four fingers | Switch Apps | Cmd+Tab (see below) |
| Tap with three fingers | Look Up | Ctrl+Cmd+D |
| Tap with four fingers | Spotlight | Cmd+Space |
| Pinch in with four or five fingers | Launchpad | Launchpad key |
| Spread with four or five fingers | Show Desktop | F11 |

### Windows

| Gesture | Action | Host shortcut |
|---|---|---|
| Swipe up with three fingers | Task View | Win+Tab |
| Swipe down with three fingers | Show Desktop | Win+D |
| Swipe left with three fingers | Switch Apps | Alt+Tab (see below) |
| Swipe right with three fingers | Switch Apps | Alt+Tab (see below) |
| Swipe up with four fingers | Task View | Win+Tab |
| Swipe down with four fingers | Show Desktop | Win+D |
| Swipe left with four fingers | Next Desktop | Win+Ctrl+Right |
| Swipe right with four fingers | Previous Desktop | Win+Ctrl+Left |
| Tap with three fingers | Search | Win+S |
| Tap with four fingers | Notifications | Win+N |

Pinch and spread with four or five fingers do nothing on Windows.

### iPadOS

| Gesture | Action | Host shortcut |
|---|---|---|
| Swipe up with three fingers | Home | Cmd+H |
| Swipe down with three fingers | Search | Cmd+Space |
| Swipe left with three fingers | Switch Apps | Cmd+Tab (see below) |
| Swipe right with three fingers | Switch Apps | Cmd+Tab (see below) |
| Swipe up with four fingers | App Switcher | Globe+Up |
| Swipe down with four fingers | Control Centre | Globe+C |
| Swipe left with four fingers | Next App | Globe+Right |
| Swipe right with four fingers | Previous App | Globe+Left |
| Tap with three fingers | Dock | Globe+A |
| Tap with four fingers | Notification Centre | Globe+N |
| Pinch in with four or five fingers | Home | Cmd+H |

Spread with four or five fingers does nothing on iPadOS.

### Switch Apps

Switch Apps holds the switcher key (Alt on Windows, Cmd on macOS and iPadOS) and steps through Tab. Swipe once to open the switcher. Keep your fingers down and keep swiping left or right to move through the apps. A swipe to the right goes forward. A swipe to the left goes back. Lift your fingers to pick the app.

### Tap gestures

Tap gestures with two, three or four fingers need **Tap to Click** to be on. Keep the tap short and keep your fingers still.

## Settings

Tap the gear at the top right of the trackpad screen. The sheet is called **Trackpad**.

| Setting | What it does | Default |
|---|---|---|
| Computer: Auto / Mac / Windows / iPad | Chooses the system that gestures and shortcuts follow. **Auto** detects it from the Bluetooth device. Pick a system by hand if the detection is wrong. The choice is saved for that computer. The footnote shows the detected system. | Auto |
| Tracking Speed | Pointer speed, from 0.4x to 3.0x. | 1.2x |
| Scroll Speed | Scroll speed, from 0.4x to 3.0x. | 1.0x |
| Natural Scrolling | On: content follows your fingers. Off: the direction is reversed. | On |
| Tap to Click | A tap sends a click. Also needed for the two, three and four-finger taps. | On |
| Tap and Drag | Tap, then touch down again and drag to drag an item. | On |
| Haptics | Vibration when you click, drag, use a gesture or press a key. | On |
| Three-finger drag | Drag with three fingers. Three-finger swipes are off while this is on. A three-finger tap still works. | Off |
| Display: Auto / Portrait / Landscape | Locks or frees the screen orientation. | Auto |

Under the settings, the sheet lists the gestures for the system in use.

## Keyboard

Tap the keyboard button next to the gear on the trackpad screen to show or hide the keyboard.

The keyboard has these parts, from top to bottom:

1. A shortcut strip. Swipe sideways to see all the chips.
2. A function strip. Swipe sideways to see all the keys.
3. A modifier row: Ctrl, Alt, Win (Cmd), Tab and the four arrow keys.
4. The main keys, with a bottom row that has the page key, the microphone, space and return.

### Pages

- **Letters**: QWERTY with a Shift key and a backspace key.
- **123**: digits and the symbols `- / : ; ( ) $ & @ "` and `. , ? ! '`.
- **#+=**: more symbols, such as `[ ] { } # % ^ * + =` and `_ \ | ~ < >` plus the backtick.
- Use the page key at the bottom left to move between the pages. It reads **123** on the letters page and **ABC** on the other pages. The **#+=** key is on the 123 page, and **123** brings you back.

### Sticky modifiers

Ctrl, Alt, Win (Cmd) and Shift are sticky.

1. Tap once: the key is armed. It applies to the next key you press, then turns off.
2. Tap a second time: the key is locked. It stays on until you tap it again.
3. Tap a third time: the key is off.

An armed key is highlighted. A locked key is highlighted and shows a dot. Shift locked works like Caps Lock. Letters show in upper case while Shift is armed or locked. Digit and symbol keys ignore Shift, so a locked Shift does not turn `123` into `!@#`.

A held modifier also works with the trackpad. Arm Ctrl, then tap the trackpad to send Ctrl+click.

On a Mac or an iPad the keys read control, option and cmd. On Windows they read Ctrl, Alt and Win.

### Function strip

The function strip has Esc, F1 to F12, PrtSc, Ins, Del, Home, End, PgUp and PgDn. A key in this strip is sent when you lift your finger. If your finger moves to scroll the strip, no key is sent. These keys do not auto-repeat when held.

### Shortcut chips

Tap a chip to send the shortcut once. The list changes with the host system. Save is not shown on iPad.

| Chip | Windows | macOS | iPadOS |
|---|---|---|---|
| Copy | Ctrl+C | Cmd+C | Cmd+C |
| Paste | Ctrl+V | Cmd+V | Cmd+V |
| Cut | Ctrl+X | Cmd+X | Cmd+X |
| Undo | Ctrl+Z | Cmd+Z | Cmd+Z |
| Redo | Ctrl+Y | Shift+Cmd+Z | Shift+Cmd+Z |
| Select All | Ctrl+A | Cmd+A | Cmd+A |
| Save | Ctrl+S | Cmd+S | not shown |
| Find | Ctrl+F | Cmd+F | Cmd+F |
| New Tab | Ctrl+T | Cmd+T | Cmd+T |
| Close Tab | Ctrl+W | Cmd+W | Cmd+W |
| Screenshot | Shift+Win+S | Shift+Cmd+5 | Shift+Cmd+3 |
| Lock | Win+L | Ctrl+Cmd+Q | Ctrl+Cmd+Q |
| Close Window | Alt+F4 | not shown | not shown |
| Task Manager | Ctrl+Shift+Esc | not shown | not shown |
| Quit | not shown | Cmd+Q | not shown |
| Force Quit | not shown | Alt+Cmd+Esc | not shown |

### Voice input

The microphone key is on the bottom row of the keyboard. It is hidden if the phone has no speech recognition.

1. Tap the microphone key. Allow the microphone permission the first time.
2. Speak. The key pulses while it listens.
3. Tap the key again to stop. The first recognised result is typed on the computer, followed by a space.

Voice input types English letters, digits and common symbols only. If nothing can be typed, the keyboard shows "Only English letters can be typed". If nothing is heard, it shows "Didn't catch that". Sticky modifiers do not apply to voice text.

## Landscape

Curser works in portrait and in landscape. Settings has a **Display** control: Auto follows the phone, Portrait and Landscape lock the orientation.

- Portrait: the trackpad is on top and the keyboard is below.
- Landscape: the keyboard is on the left and the trackpad is on the right. The shortcut strip and the function strip share one line to save space.

## Troubleshooting

### The computer does not see the phone

- Check that Bluetooth is on, on both devices.
- Tap **Pair New Device** and keep that screen open. The phone is visible for 5 minutes.
- Check that Curser has Bluetooth permission. If it shows **Allow Bluetooth**, tap it.
- If the home screen shows **Not Supported**, the phone cannot act as a Bluetooth input device. Curser cannot work on it.
- If the phone was paired with the computer before you installed Curser, remove ("Forget") the pairing on both sides and pair again.

### It is connected, but the pointer does not move

- Keep Curser open on the trackpad screen. The connection stops when the app leaves the screen.
- Go back to the device list and tap the computer again.
- Remove ("Forget") the pairing on both the phone and the computer, then pair again.
- Try a higher **Tracking Speed** in Settings.

### The keyboard types the wrong characters

The host must use a US keyboard layout. The phone sends key positions, not characters. Change the keyboard layout on the computer to US.

### Three-finger swipes do nothing, or the phone takes a screenshot

Some phones have their own three-finger screenshot gesture, and it takes the swipe before Curser sees it. Turn that gesture off in the phone's own settings.

Also check that **Three-finger drag** is off. Three-finger swipes are off while it is on.

### Gestures do the wrong thing for my computer

Open Settings and look at the footnote under **Computer**. If the detected system is wrong, pick Mac, Windows or iPad by hand.

### Re-pair after an update changes the report layout

A computer keeps a copy of the Bluetooth report layout from the time it was paired. If an update changes that layout, the computer keeps using the old one. For example, a computer paired before the Globe and Launchpad reports were added ignores those reports. Remove ("Forget") the pairing on both sides and pair again to fix it.

## Limits

- The host must use a US keyboard layout to get the characters printed on the keys.
- Voice input types English letters, digits and common symbols only.
- The scrolling function-key strip keys do not auto-repeat when held.
- Some phones do not ship the Bluetooth HID Device profile. Curser cannot work on them.
- Not every phone and host combination has been tested.
- Curser only works while the app is on the screen.
