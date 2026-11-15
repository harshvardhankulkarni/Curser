# Curser — architecture contract

Android phone acts as a Bluetooth trackpad for a Mac, Windows PC or iPad.
This file is the binding contract between the build, core and UI work. Do not change a signature here without updating this file.

## 1. Key decisions

| Decision | Choice | Reason |
|---|---|---|
| Transport | Bluetooth Classic HID Device profile (`android.bluetooth.BluetoothHidDevice`, API 28+) | Needs no software on the host. Only option that works with an iPad. |
| HID shape | Composite: relative mouse (report 1) + keyboard (report 2) + consumer control (report 3, added in v2) | Pointer, click, scroll go as mouse reports. Multi-finger gestures go as the host's own keyboard shortcuts. |
| Gesture recognition | On the phone, pure Kotlin state machine | Hosts do not accept third-party multi-touch over HID (macOS, iPadOS). Pure code is JVM-testable. |
| Host OS detection | Score Bluetooth signals (name, class, SDP UUIDs, NumLock LED report), per-device manual override | Bluetooth HID has no "what OS are you" query. Heuristic + override is the honest design. |
| UI | Jetpack Compose, custom iOS-style components, spring animations | "iPhone animation and theme" on Android. |
| Language / build | Kotlin 2.0.21, AGP 8.7.3, Gradle 8.11.1, JDK 17, compileSdk 35, targetSdk 35, minSdk 28 | minSdk 28 is the first API with `BluetoothHidDevice`. |
| Module layout | Single Gradle module `app`, applicationId and namespace `app.curser` | Smallest thing that works. |

Not in scope: typing through the phone's system keyboard (section 10 adds an in-app keyboard instead), media playback keys, on-screen mouse buttons, foreground service (trackpad only runs while the app is on screen), Wi-Fi transport, high-resolution wheel.

Not possible over HID keyboard/mouse, so not built: Mac smart zoom, Mac rotate, two-finger "swipe between pages", Mac Notification Centre edge swipe (macOS has no default shortcut for any of them, or the gesture is indistinguishable from scrolling).

## 2. File ownership

```
Curser/
  settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew*, gradle/   BUILD
  build.bat                                                                    BUILD
  app/build.gradle.kts                                                         BUILD
  app/src/main/AndroidManifest.xml                                             BUILD
  app/src/main/res/**                                                          BUILD
  app/src/main/java/app/curser/core/**                                         CORE
  app/src/test/java/app/curser/core/**                                         CORE
  app/src/main/java/app/curser/MainActivity.kt                                 UI
  app/src/main/java/app/curser/ui/**                                           UI
```

An owner writes only its own files. UI calls CORE only through section 4. CORE never imports `app.curser.ui`.

## 3. Build contract (BUILD)

- Toolchain lives outside the project in `%USERPROFILE%\.android-dev\` : `jdk17\`, `sdk\`, `gradle-8.11.1\`. No system PATH or environment changes.
- `local.properties` holds `sdk.dir`. `build.bat` sets `JAVA_HOME` and runs `gradlew.bat assembleDebug testDebugUnitTest`.
- Plugins: `com.android.application` 8.7.3, `org.jetbrains.kotlin.android` 2.0.21, `org.jetbrains.kotlin.plugin.compose` 2.0.21.
- Dependencies: Compose BOM `2024.12.01` (`ui`, `ui-graphics`, `foundation`, `animation`, `material3`, `ui-tooling-preview`), `androidx.activity:activity-compose:1.9.3`, `androidx.core:core-ktx:1.15.0`, `androidx.lifecycle:lifecycle-runtime-compose:2.8.7`, `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7`. Test: `junit:junit:4.13.2`. Nothing else.
- Manifest permissions:
  - `BLUETOOTH` and `BLUETOOTH_ADMIN` with `android:maxSdkVersion="30"`
  - `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` (API 31+)
  - `VIBRATE`
  - `<uses-feature android:name="android.hardware.bluetooth" android:required="true"/>`
- One activity: `app.curser.MainActivity`, exported launcher, `android:screenOrientation="fullUser"` (v2: was portrait), `android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden"`, theme `@style/Theme.Curser` (a `Theme.Material.NoActionBar` / `android:Theme.Material.Light.NoActionBar` descendant with transparent system bars; no AppCompat dependency).
- App label `Curser`. Adaptive launcher icon drawn as vector XML: white cursor arrow on an iOS-blue (#0A84FF to #5E5CE6) gradient.

## 4. Core API (CORE implements, UI consumes)

Package `app.curser.core`. All types below are public and must exist with exactly these names and members.

```kotlin
enum class HostOs(val label: String) { MAC("Mac"), WINDOWS("Windows"), IPAD("iPad") }

data class HostDevice(val address: String, val name: String, val os: HostOs)

enum class BtStatus {
    UNSUPPORTED,     // no Bluetooth adapter, or the phone has no HID Device profile
    NO_PERMISSION,   // BLUETOOTH_CONNECT not granted (API 31+)
    DISABLED,        // adapter is off
    REGISTERING,     // waiting for the HID app registration callback
    READY            // HID app registered; can connect
}

sealed interface Connection {
    data object Disconnected : Connection
    data class Connecting(val device: HostDevice) : Connection
    data class Connected(val device: HostDevice) : Connection
}

data class UiState(
    val bt: BtStatus = BtStatus.REGISTERING,
    val bonded: List<HostDevice> = emptyList(),
    val connection: Connection = Connection.Disconnected,
    val hostOs: HostOs = HostOs.WINDOWS,        // OS in effect for gesture mapping
    val hostOsOverridden: Boolean = false       // true when the user picked it by hand
)

data class Settings(
    val pointerSpeed: Float = 1.2f,   // 0.4 .. 3.0
    val scrollSpeed: Float = 1.0f,    // 0.4 .. 3.0
    val naturalScroll: Boolean = true,
    val tapToClick: Boolean = true,
    val tapAndDrag: Boolean = true,
    val haptics: Boolean = true,
    val threeFingerDrag: Boolean = false,                 // v2
    val orientation: Orientation = Orientation.AUTO       // v2
)

enum class Orientation { AUTO, PORTRAIT, LANDSCAPE }      // v2

data class TouchPoint(val id: Int, val x: Float, val y: Float)          // x, y in dp
data class TouchFrame(val timeMs: Long, val points: List<TouchPoint>)   // every pointer currently down; empty = all lifted

/** One-shot cue for the UI: show `label` in the status pill and fire a haptic. */
data class Feedback(val label: String, val kind: Kind) {
    enum class Kind { CLICK, DRAG, GESTURE }
}

data class GestureGuideRow(val gesture: String, val action: String)   // "Swipe up with three fingers" -> "Mission Control"

class TrackpadController(context: Context) {
    val state: StateFlow<UiState>
    val settings: StateFlow<Settings>
    val feedback: SharedFlow<Feedback>          // extraBufferCapacity >= 8, never suspends the sender

    fun start()                                  // Activity.onStart: read permission/adapter state, register HID app
    fun stop()                                   // Activity.onStop: release buttons and keys, unregister HID app
    fun refresh()                                // re-read permission, adapter state and bonded devices (call after a permission or enable result)
    fun connect(address: String)
    fun disconnect()
    fun setHostOs(os: HostOs?)                   // null = back to auto-detect; non-null is saved per device address
    fun updateSettings(transform: (Settings) -> Settings)   // persists to SharedPreferences
    fun onTouch(frame: TouchFrame)               // main thread
    fun gestureGuide(os: HostOs): List<GestureGuideRow>
}
```

Rules:
- Every public method is called on the main thread and returns fast. Bluetooth sends run on a single-thread executor inside CORE.
- `state` emits `Connection.Connected` when the host has connected, including when the host itself reconnects without a `connect()` call.
- UI creates exactly one `TrackpadController` (in an `AndroidViewModel` or remembered at activity scope) and passes `applicationContext`.
- CORE requests no permissions and starts no activities. UI does both (runtime permission, enable-Bluetooth intent, discoverable intent).

## 5. HID layer (`core/hid`)

### 5.1 Report descriptor — use these bytes exactly

```
// ---- Report ID 1: mouse. Payload 7 bytes: buttons, Xlo, Xhi, Ylo, Yhi, wheel, pan
0x05,0x01, 0x09,0x02, 0xA1,0x01,
  0x85,0x01,
  0x09,0x01, 0xA1,0x00,
    0x05,0x09, 0x19,0x01, 0x29,0x03, 0x15,0x00, 0x25,0x01, 0x95,0x03, 0x75,0x01, 0x81,0x02,   // 3 buttons
    0x95,0x01, 0x75,0x05, 0x81,0x03,                                                           // 5 bits padding
    0x05,0x01, 0x09,0x30, 0x09,0x31, 0x16,0x01,0x80, 0x26,0xFF,0x7F, 0x75,0x10, 0x95,0x02, 0x81,0x06, // X, Y int16 relative
    0x09,0x38, 0x15,0x81, 0x25,0x7F, 0x75,0x08, 0x95,0x01, 0x81,0x06,                          // wheel int8 relative
    0x05,0x0C, 0x0A,0x38,0x02, 0x15,0x81, 0x25,0x7F, 0x75,0x08, 0x95,0x01, 0x81,0x06,          // AC Pan int8 relative
  0xC0,
0xC0,
// ---- Report ID 2: keyboard. Input payload 8 bytes: modifiers, 0x00, key1..key6. Output payload 1 byte: LEDs
0x05,0x01, 0x09,0x06, 0xA1,0x01,
  0x85,0x02,
  0x05,0x07, 0x19,0xE0, 0x29,0xE7, 0x15,0x00, 0x25,0x01, 0x75,0x01, 0x95,0x08, 0x81,0x02,       // modifier bits
  0x95,0x01, 0x75,0x08, 0x81,0x01,                                                              // reserved byte
  0x95,0x05, 0x75,0x01, 0x05,0x08, 0x19,0x01, 0x29,0x05, 0x91,0x02,                             // 5 LED bits (output)
  0x95,0x01, 0x75,0x03, 0x91,0x01,                                                              // 3 bits LED padding
  0x95,0x06, 0x75,0x08, 0x15,0x00, 0x25,0x65, 0x05,0x07, 0x19,0x00, 0x29,0x65, 0x81,0x00,       // 6 key slots
0xC0,
// ---- Report ID 3 (v2): consumer control. Payload 2 bytes: one 16-bit usage, little-endian. 0 = released
0x05,0x0C, 0x09,0x01, 0xA1,0x01,
  0x85,0x03,
  0x15,0x00, 0x26,0xFF,0x03, 0x19,0x00, 0x2A,0xFF,0x03, 0x75,0x10, 0x95,0x01, 0x81,0x00,
0xC0
```

Total descriptor length is 171 bytes. Consumer usages used: Globe `0x029D` (AC Keyboard Layout Select; iPadOS treats it as the Globe modifier), Launchpad `0x02A0` (AC Desktop Show All Applications; macOS).
A host paired before v2 keeps its cached two-report descriptor: reports 1 and 2 keep working, report 3 is ignored until the pairing is removed and made again.

Mouse payload is 7 bytes, little-endian: `[buttons, x & 0xFF, x >> 8, y & 0xFF, y >> 8, wheel, pan]`.
Buttons: bit0 left, bit1 right, bit2 middle. Clamp X and Y to ±32767, wheel and pan to ±127.
Wheel: positive = scroll up. Pan: positive = scroll right. Y: positive = pointer down.

Modifier bits: LCtrl 0x01, LShift 0x02, LAlt 0x04, LGui 0x08 (Gui = Windows key = Command; Alt = Option).
Key usages needed: A 0x04, D 0x07, H 0x0B, N 0x11, S 0x16, Tab 0x2B, Space 0x2C, Minus 0x2D, Equal 0x2E, F11 0x44, Right 0x4F, Left 0x50, Down 0x51, Up 0x52.

Key and consumer reports go through one paced queue in `HidPeripheral` (v2): each queued report is sent in order, no sooner than 15 ms after the previous queued report. This replaces the fixed 20 ms release timer. `releaseAll()` clears the queue and sends all-zero mouse, keyboard and consumer reports. Never leave a key, consumer usage or button held across `stop()`, `disconnect()` or a lost connection.

### 5.2 `HidPeripheral` (Android wrapper)

- `BluetoothAdapter.getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)`. If it returns false or the proxy never arrives, report `UNSUPPORTED`.
- `registerApp(BluetoothHidDeviceAppSdpSettings("Curser Trackpad", "Trackpad and keyboard", "Curser", BluetoothHidDevice.SUBCLASS1_COMBO, descriptor), null, null, executor, callback)`.
- `onAppStatusChanged(pluggedDevice, registered)`: `registered == true` means `READY`.
- `onConnectionStateChanged`: map `STATE_CONNECTING/CONNECTED/DISCONNECTED` to `Connection`.
- `onGetReport`: `replyReport(device, type, id, zero-filled payload of the right size)`. `onSetReport`: `reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)`.
- LED output reports arrive in `onSetReport` (type output, id 2) or `onInterruptData(device, 2, data)`. If bit0 (Num Lock) is set, raise the `numLockSeen` detection signal for that device.
- `connect(device)`, `disconnect(device)`, `sendReport(device, id, payload)`.
- `stop()`: `unregisterApp()` then `closeProfileProxy`. Guard every Bluetooth call with a `SecurityException` catch and an API 31+ permission check.
- Coalesce pointer motion: accumulate deltas and send at most one mouse report per ~8 ms.
- `onGetReport` for report 3 replies with 2 zero bytes.

## 6. Gesture engine (`core/gesture`) — pure Kotlin, no Android imports

```kotlin
enum class Dir { UP, DOWN, LEFT, RIGHT }
sealed interface GestureEvent {
    data class Move(val dx: Float, val dy: Float) : GestureEvent          // dp, one finger
    data class Scroll(val dx: Float, val dy: Float) : GestureEvent        // dp, two-finger centroid delta
    data class Pinch(val zoomIn: Boolean) : GestureEvent                  // one step
    data class Tap(val fingers: Int) : GestureEvent                       // 2, 3 or 4 fingers
    data class Swipe(val fingers: Int, val dir: Dir) : GestureEvent       // 3 or 4 fingers, once per touch sequence
    data object LeftDown : GestureEvent
    data object LeftUp : GestureEvent
    data class SwipeRepeat(val fingers: Int, val dir: Dir) : GestureEvent   // v2: each further step after a Swipe, same axis, either direction
    data class SwipeEnd(val fingers: Int) : GestureEvent                    // v2: all fingers lifted after a Swipe
    data class MultiPinch(val spread: Boolean) : GestureEvent               // v2: four or five fingers, once per sequence
}
class GestureEngine(var tapToClick: Boolean = true, var tapAndDrag: Boolean = true, var threeFingerDrag: Boolean = false) {
    fun onFrame(frame: TouchFrame): List<GestureEvent>
    fun onTimeout(nowMs: Long): List<GestureEvent>     // caller invokes at `deadlineMs`
    val deadlineMs: Long?                               // non-null while a delayed LeftUp is pending
    fun reset(): List<GestureEvent>                     // returns LeftUp if the button is held
}
```

A touch sequence runs from the first finger down until all fingers are up. `maxFingers` is the largest simultaneous count in the sequence.

Constants: `TAP_MAX_MS = 250`, `TAP_SLOP_DP = 10`, `MOVE_SLOP_DP = 3`, `DRAG_WINDOW_MS = 180`, `SCROLL_SLOP_DP = 8`, `PINCH_SLOP_DP = 24`, `PINCH_STEP_DP = 40`, `SWIPE_DP = 56`, `SWIPE_STEP_DP = 44` (v2), `MULTI_PINCH_DP = 18` (v2).

Behaviour:
1. **One finger, moving.** After the finger travels `MOVE_SLOP_DP`, emit `Move` with each frame's delta. Do not emit the slop distance itself (no jump).
2. **One-finger tap** (`maxFingers == 1`, duration ≤ `TAP_MAX_MS`, travel ≤ `TAP_SLOP_DP`, `tapToClick`):
   - `tapAndDrag == false`: emit `LeftDown, LeftUp` at lift.
   - `tapAndDrag == true`: emit `LeftDown` at lift and set `deadlineMs = liftTime + DRAG_WINDOW_MS`.
     - Deadline passes with no new touch: `onTimeout` emits `LeftUp`.
     - A finger lands before the deadline: clear the deadline, keep the button held. If that finger moves past `MOVE_SLOP_DP`, it is a drag: emit `Move` events, and `LeftUp` when it lifts. If it lifts as a tap instead (a double tap): emit `LeftUp, LeftDown, LeftUp`.
3. **Two fingers.** Track the centroid and the distance between the fingers from the moment the second finger lands. Lock one mode for the rest of the sequence:
   - distance change ≥ `PINCH_SLOP_DP` and larger than centroid travel: PINCH. Emit one `Pinch` per further `PINCH_STEP_DP` of distance change (`zoomIn` when the distance grows). The crossing of the slop itself emits the first step.
   - else centroid travel ≥ `SCROLL_SLOP_DP`: SCROLL. Emit `Scroll(dx, dy)` with each frame's centroid delta.
4. **Three to five fingers (v2).** `fingers` in swipe events is `min(maxFingers, 4)`. When the centroid travels `SWIPE_DP` from where the last finger landed, emit one `Swipe(fingers, dominantAxisDirection)`. At most one `Swipe` per sequence.
   - After the `Swipe`, anchor at the centroid. Each time the centroid moves `SWIPE_STEP_DP` from the anchor along the axis of that first swipe (either way), emit `SwipeRepeat(fingers, directionOfThatStep)` and advance the anchor one step. Evaluate only on frames with three or more fingers down.
   - When the sequence ends after a `Swipe`, emit `SwipeEnd(fingers)`.
4a. **Four or five fingers, pinch (v2).** Track the mean distance of the fingers from their centroid, baselined whenever the finger count changes. Before the swipe check: if no `Swipe` was emitted, the change is ≥ `MULTI_PINCH_DP` and larger than the centroid travel, emit one `MultiPinch(spread = distance grew)` and emit no `Swipe` for the rest of the sequence.
4b. **Three-finger drag (v2, only when `threeFingerDrag`).** With exactly three fingers down and `maxFingers == 3`: once the centroid travels `MOVE_SLOP_DP`, emit `LeftDown` once, then `Move` with each frame's centroid delta. Emit `LeftUp` when the finger count leaves three or the sequence ends. No `Swipe` is emitted for three fingers in this mode. A three-finger tap still emits `Tap(3)`.
5. **Multi-finger tap.** All fingers up, `maxFingers` in 2..4, duration ≤ `TAP_MAX_MS + 50`, no finger travelled more than `TAP_SLOP_DP`, no mode locked, no swipe emitted: emit `Tap(maxFingers)`. Requires `tapToClick`.
6. **No pointer jumps.** When the finger count changes, re-baseline. Once `maxFingers ≥ 2`, the remaining single finger never emits `Move` for the rest of the sequence.
7. Six or more fingers: ignore the sequence (v2: five is allowed).

## 7. Mapping (`core/mapping`) — pure Kotlin

`GestureMapper` turns a `GestureEvent` + `HostOs` + `Settings` into HID actions and an optional `Feedback` label. Keep the tables as data so `gestureGuide(os)` reads from the same source.

```kotlin
sealed interface HidAction {                       // v2 shape
    data class MouseMove(val dx: Int, val dy: Int) : HidAction
    data class Wheel(val v: Int, val h: Int, val queued: Boolean = false) : HidAction   // queued = goes through the paced key queue, in order with Key/Consumer
    data class Buttons(val mask: Int) : HidAction
    data class Key(val mods: Int, val key: Int) : HidAction        // sets the keyboard state; Key(0, 0) releases everything
    data class Consumer(val usage: Int) : HidAction                // sets the consumer state; Consumer(0) releases
}
```

`Key`, `Consumer` and `Wheel(queued = true)` are sent through the paced queue of section 5.1. `MouseMove`, `Buttons` and plain `Wheel` are immediate.

Each table cell is one action kind:

| Kind | HID actions |
|---|---|
| `Combo(mods, key)` | `Key(mods, key)`, `Key(0, 0)` |
| `Media(usage)` | `Consumer(usage)`, `Consumer(0)` |
| `Globe(key)` | `Consumer(0x029D)`, `Key(0, key)`, `Key(0, 0)`, `Consumer(0)` |
| `Switcher(mod)` | On `Swipe`: `Key(mod, 0)`, then a Tab step. On each `SwipeRepeat`: a Tab step. On `SwipeEnd`: `Key(0, 0)`. Tab step = `Key(mod, Tab)`, `Key(mod, 0)` when the step direction is RIGHT, or `Key(mod + LShift, Tab)`, `Key(mod, 0)` when it is LEFT. Vertical steps are ignored. |
| `None` | nothing, and the row is left out of `gestureGuide` for that OS |

`SwipeRepeat` and `SwipeEnd` only act while a `Switcher` is open; other kinds ignore them. `GestureMapper.reset()` forgets an open switcher (the controller sends the zero reports).

Pointer: `out = delta_dp * pointerSpeed * gain(speed)` where gain rises smoothly from 1.0 (slow) to about 2.5 (fast flick). Keep the fractional remainder between reports.

Scroll: accumulate dp; one wheel unit per `18 / scrollSpeed` dp, same for pan. Sign for a direct host (Windows): `wheel = +dyUnits`, `pan = -dxUnits` (finger down the glass scrolls toward the top, like a touchscreen). macOS and iPadOS invert mouse wheels themselves (their "Natural scrolling" default), so negate both for `MAC` and `IPAD`. When `naturalScroll == false`, negate again.

Pinch (two fingers): Windows = `Key(LCtrl, 0)`, `Wheel(+1 or -1, 0, queued = true)`, `Key(0, 0)`. Mac and iPad = `Combo(Gui, Equal)` (zoom in) / `Combo(Gui, Minus)`.

`Tap(2)` = right click on every OS. `LeftDown` / `LeftUp` = left button.

| Gesture | MAC | WINDOWS | IPAD |
|---|---|---|---|
| Swipe 3 up | Mission Control `Ctrl+Up` | Task View `Gui+Tab` | Home `Gui+H` |
| Swipe 3 down | App Exposé `Ctrl+Down` | Show Desktop `Gui+D` | Search `Gui+Space` |
| Swipe 3 left | Next Space `Ctrl+Right` | Switch Apps `Switcher(LAlt)` | Switch Apps `Switcher(LGui)` |
| Swipe 3 right | Previous Space `Ctrl+Left` | Switch Apps `Switcher(LAlt)` | Switch Apps `Switcher(LGui)` |
| Swipe 4 up | Mission Control `Ctrl+Up` | Task View `Gui+Tab` | App Switcher `Globe(Up)` |
| Swipe 4 down | App Exposé `Ctrl+Down` | Show Desktop `Gui+D` | Control Centre `Globe(C)` |
| Swipe 4 left | Switch Apps `Switcher(LGui)` | Next Desktop `Gui+Ctrl+Right` | Next App `Globe(Right)` |
| Swipe 4 right | Switch Apps `Switcher(LGui)` | Previous Desktop `Gui+Ctrl+Left` | Previous App `Globe(Left)` |
| Tap 3 | Look Up `Ctrl+Gui+D` | Search `Gui+S` | Dock `Globe(A)` |
| Tap 4 | Spotlight `Gui+Space` | Notifications `Gui+N` | Notification Centre `Globe(N)` |
| Pinch in, four or five fingers | Launchpad `Media(0x02A0)` | None | Home `Gui+H` |
| Spread, four or five fingers | Show Desktop `F11` | None | None |
| Pinch out / in, two fingers | Zoom In / Out | Zoom In / Out | Zoom In / Out |
| Two-finger drag | Scroll | Scroll | Scroll |
| Two-finger tap | Right Click | Right Click | Right Click |
| Tap, then drag | Drag | Drag | Drag |
| Three-finger drag (when the setting is on) | Drag | Drag | Drag |

Extra key usage needed in v2: C 0x06.

Feedback labels are the action names in the table ("Mission Control", "Right Click", "Zoom In", "Switch Apps"). `Move`, `Scroll`, `SwipeRepeat` and `SwipeEnd` produce no feedback. `LeftDown` produces `Feedback("Click", CLICK)`.

## 8. Host detection (`core/detect`) — pure Kotlin

```kotlin
object HostDetector {
    data class Signals(
        val name: String?,
        val majorClass: Int?,      // BluetoothClass.Device.Major.*
        val deviceClass: Int?,     // BluetoothClass.Device.*
        val uuids: List<String>,   // SDP service UUID strings
        val numLockSeen: Boolean
    )
    fun detect(s: Signals): HostOs
}
```

First match wins:
1. name contains `ipad` → `IPAD`
2. name contains `macbook`, `imac`, `mac mini`, `mac studio`, `mac pro`, or the whole word `mac` → `MAC`
3. `numLockSeen` → `WINDOWS`
4. name starts with `desktop-` or `laptop-`, or contains `windows`, `surface` or the whole word `pc` → `WINDOWS`
5. any UUID contains `deca-fade` (Apple): `deviceClass` is `COMPUTER_LAPTOP` (0x010C) or `COMPUTER_DESKTOP` (0x0104) → `MAC`; `majorClass` is `PHONE` (0x0200) or `deviceClass` is a handheld/palm computer (0x0110, 0x0114) → `IPAD`; otherwise `MAC`
6. default → `WINDOWS`

Name matching is case-insensitive. Hard-code the class constants so the file has no Android imports.

`TrackpadController` runs detection on connect and again when `numLockSeen` first arrives. A saved per-address override always wins. `HostDevice.os` in the bonded list uses the same function (or the saved override).

## 9. UI (`ui/**`, `MainActivity.kt`)

iOS look and motion on Android. Follow system light/dark. No Material ripple, no Material top bars or switches.

- Tokens: iOS system colours (blue `#007AFF` light / `#0A84FF` dark, grouped background `#F2F2F7` / `#000000`, cell `#FFFFFF` / `#1C1C1E`, separator, secondary label). Type scale: Large Title 34 bold, Title 22 bold, Headline 17 semibold, Body 17, Footnote 13, Caption 12. Tight letter spacing on titles. System font.
- Motion: springs everywhere (`dampingRatio ≈ 0.8`, medium-low stiffness). Press state = scale 0.97 + dim. Push navigation = new screen slides in from the right, old screen shifts left 30 % and dims; back reverses. Sheets rise from the bottom with a grabber and drag-to-dismiss.
- Components to build: grouped inset list + cell, iOS switch, segmented control with sliding thumb, slider, bottom sheet, nav bar with large title, status pill.
- Icons: draw the few needed glyphs (chevron, gear, laptop, tablet, bluetooth, checkmark, plus) as `ImageVector` or Canvas paths. Do not add an icon dependency.

Screens:
1. **Devices** (home). Large title "Curser".
   - `BtStatus` banners: `NO_PERMISSION` → "Allow Bluetooth" button (requests `BLUETOOTH_CONNECT` + `BLUETOOTH_ADVERTISE` on API 31+). `DISABLED` → "Turn On Bluetooth" (`BluetoothAdapter.ACTION_REQUEST_ENABLE`). `UNSUPPORTED` → plain explanation that this phone cannot act as a Bluetooth input device. Call `controller.refresh()` after each result.
   - Section "MY DEVICES": bonded hosts, each with an OS glyph, name, OS label, and a spinner or "Connected". Tap → `connect(address)`.
   - Row "Pair New Device": fires `BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE` (300 s) and opens a sheet with pairing steps per OS (segmented Mac / Windows / iPad). The sheet must state: if the phone was paired with this computer before installing Curser, remove ("Forget") the pairing on both sides and pair again while this screen is open.
   - When `connection` becomes `Connected`, push the Trackpad screen automatically.
2. **Trackpad**. Immersive sticky full screen (hide status and navigation bars, so Android edge gestures do not steal swipes), keep screen on.
   - Whole screen is the touch surface, near-black with a soft glow under each finger that springs out on lift.
   - Convert pointer positions from px to dp and call `controller.onTouch(TouchFrame(uptimeMillis, pressedPointers))` on every pointer event, including the final empty frame.
   - Top centre: a pill in the style of Dynamic Island. Idle = host name + OS glyph. On each `Feedback` it springs wider, shows the label for about 900 ms, then springs back.
   - Top left: back chevron (calls `disconnect()` and pops). Top right: gear (opens Settings sheet). These two controls sit outside the gesture surface hit area.
   - Haptic on `Feedback` when `settings.haptics` (light tick for CLICK, firmer for GESTURE).
   - When `connection` becomes `Disconnected`, pop back to Devices.
3. **Settings sheet**. Host OS segmented control: Auto / Mac / Windows / iPad (`setHostOs`), with a footnote showing the detected OS. Sliders: tracking speed, scroll speed. Switches: natural scrolling, tap to click, tap and drag, haptics. Then the gesture guide for the OS in effect from `gestureGuide(os)`.
   - v2: segmented control "Orientation": Auto / Portrait / Landscape (`Settings.orientation`). Switch "Three-finger drag" (`Settings.threeFingerDrag`) with the footnote "Three-finger swipes are off while this is on."

Orientation (v2): the activity applies `requestedOrientation` from `Settings.orientation` whenever it changes: `AUTO` = `SCREEN_ORIENTATION_FULL_USER`, `PORTRAIT` = `SCREEN_ORIENTATION_PORTRAIT`, `LANDSCAPE` = `SCREEN_ORIENTATION_SENSOR_LANDSCAPE`.

Landscape layout (v2): every screen and sheet must be usable at 360 dp height. Devices content and both sheets are capped at 560 dp wide and centred; sheets are at most 92 % of the window height with their content scrolling inside. Devices and the trackpad top bar pad for `WindowInsets.displayCutout` and system bars. The trackpad touch surface still fills the whole window; touch coordinates are taken in the rotated view, so "up" is always up on the screen as held.

`MainActivity`: `enableEdgeToEdge()`, calls `controller.start()` in `onStart` and `controller.stop()` in `onStop`.

## 10. Keyboard (v3)

An on-screen keyboard on the Trackpad screen. It sends through the existing keyboard report (ID 2). **The HID descriptor does not change**, so no host needs a new pairing. All usages are in the range `0x04..0x52`, inside the descriptor's `0x00..0x65` key array.

Limit, by design: the phone sends key positions, not characters. The host must use a US layout to get the printed characters.

### 10.1 Core (`core/keyboard`, pure Kotlin, no Android imports)

```kotlin
package app.curser.core.keyboard

data class KeyStroke(val mods: Int, val usage: Int)     // mods = Hid.MOD_* bits
data class Shortcut(val label: String, val mods: Int, val usage: Int)

object KeyMap {
    // HID keyboard usages
    const val ENTER = 0x28; const val ESC = 0x29; const val BACKSPACE = 0x2A; const val TAB = 0x2B
    const val SPACE = 0x2C; const val CAPS_LOCK = 0x39
    const val F1 = 0x3A                                   // F1..F12 = 0x3A..0x45
    const val PRINT_SCREEN = 0x46; const val INSERT = 0x49; const val HOME = 0x4A; const val PAGE_UP = 0x4B
    const val DELETE = 0x4C; const val END = 0x4D; const val PAGE_DOWN = 0x4E
    const val RIGHT = 0x4F; const val LEFT = 0x50; const val DOWN = 0x51; const val UP = 0x52

    /** US layout. null = this character cannot be typed. */
    fun stroke(c: Char): KeyStroke?

    fun shortcuts(os: HostOs): List<Shortcut>
}
```

`stroke` table (US layout): `a..z` → `0x04..0x1D`; `A..Z` → same with `MOD_LSHIFT`; `1..9` → `0x1E..0x26`, `0` → `0x27`; `\n` → `0x28`; `\t` → `0x2B`; space → `0x2C`; unshifted `- = [ ] \ ; ' ` , . /` → `0x2D 0x2E 0x2F 0x30 0x31 0x33 0x34 0x35 0x36 0x37 0x38`; shifted on the same keys: `! @ # $ % ^ & * ( )` on `1..0`, `_ + { } | : " ~ < > ?`. Everything else → `null`.

`shortcuts(os)`, in this order. `P` = `MOD_LCTRL` on Windows, `MOD_LGUI` on Mac and iPad.

| Label | Windows | Mac | iPad |
|---|---|---|---|
| Copy | P+C | P+C | P+C |
| Paste | P+V | P+V | P+V |
| Cut | P+X | P+X | P+X |
| Undo | P+Z | P+Z | P+Z |
| Redo | Ctrl+Y | Shift+Cmd+Z | Shift+Cmd+Z |
| Select All | P+A | P+A | P+A |
| Save | P+S | P+S | (omit) |
| Find | P+F | P+F | P+F |
| New Tab | P+T | P+T | P+T |
| Close Tab | P+W | P+W | P+W |
| Screenshot | Shift+Win+S | Shift+Cmd+5 | Shift+Cmd+3 |
| Lock | Win+L | Ctrl+Cmd+Q | Ctrl+Cmd+Q |
| Close Window / Quit | "Close Window" Alt+F4 | "Quit" Cmd+Q | (omit) |
| Task Manager / Force Quit | "Task Manager" Ctrl+Shift+Esc | "Force Quit" Alt+Cmd+Esc | (omit) |

### 10.2 `TrackpadController` additions (all called on the main thread)

```kotlin
/** Absolute state of the on-screen keyboard: these modifiers and this key are down. usage 0 = no key. (0, 0) = all released. */
fun setKeyboard(mods: Int, usage: Int)
/** One press and release, e.g. a shortcut. Leaves the setKeyboard state as it was. */
fun tapKey(mods: Int, usage: Int)
/** Types the text (voice input). Returns the number of characters skipped because KeyMap.stroke gave null. */
fun typeText(text: String): Int
fun shortcuts(os: HostOs): List<Shortcut>     // = KeyMap.shortcuts(os)
```

Rules:
- All four do nothing unless `connection is Connected` (`typeText` then returns `text.length`).
- The controller keeps `kbMods` and `kbKey` (the last `setKeyboard` values). Every keyboard report goes through the existing paced queue `peripheral.key(mods, key)`, so order is kept.
- `setKeyboard` sends `key(mods, usage)`.
- `tapKey` sends `key(mods, usage)` then `key(kbMods, kbKey)`.
- `typeText` sends, per typable character, `key(stroke.mods, stroke.usage)` then `key(0, 0)`; after the last character one `key(kbMods, kbKey)` if that state is not `(0, 0)`. Sticky modifiers are not applied to typed text.
- Gesture and keyboard share the report. In `execute(HidAction.Key)`: an all-zero action `Key(0, 0)` is sent as `key(kbMods, kbKey)`. So a gesture shortcut returns to the keyboard's state instead of releasing a latched modifier. Mouse clicks are a different report: a latched Ctrl or Shift plus a tap gives Ctrl+click or Shift+click with no extra code.
- `resetInput` sets `kbMods = 0`, `kbKey = 0`.
- The host does key auto-repeat while a key is held. The app sends no repeats.

### 10.3 UI (`ui/keyboard/**`, plus edits in `TrackpadScreen.kt`, `Glyphs.kt`, `AndroidManifest.xml`)

Entry: a keyboard glyph button in the Trackpad top bar, left of the gear. It toggles the keyboard panel. The top bar is a `Row`: back, status pill in the weighted middle, keyboard, gear. The pill is centred between the buttons, not on the screen. The panel springs in and out (same `Motion` specs as the rest).

Layout:
- Portrait: trackpad surface on top (`weight(1f)`, at least 160 dp), keyboard panel below at its natural height.
- Landscape (`maxWidth > maxHeight`): `Row`, keyboard panel left `weight(1.7f)`, trackpad surface right `weight(1f)`. The panel must fit in 360 dp total window height including the top bar: reduce key height, and let the two strips share one line if needed.
- The trackpad surface keeps working unchanged in the space left. Its `onSizeChanged` already sends an empty frame.
- Pad for `displayCutout` and the bottom edge like the top bar does.

Panel, top to bottom. Look: iOS dark keyboard (key `#6B6B6B`-style grey on near-black, special keys darker, 5 dp corner radius, press = lighter fill, no ripple). Reuse `Pressable`/theme tokens where they fit.
1. **Shortcut strip**, horizontally scrollable chips from `controller.shortcuts(os)`. Tap → `tapKey(mods, usage)`.
2. **Function strip**, horizontally scrollable: `esc F1..F12 PrtSc Ins Del Home End PgUp PgDn`.
3. **Modifier row**: Ctrl, Alt, Win, Tab, `← ↑ ↓ →`. Labels on Mac and iPad: `control`, `option`, `cmd`. On Windows: `Ctrl`, `Alt`, `Win`.
4. **Letters**, iOS arrangement: `qwertyuiop` / `asdfghjkl` / shift + `zxcvbnm` + backspace / `123` + mic + space + return.
   - `123` page: `1234567890` / `-/:;()$&@"` / `#+=` + `.,?!'` + backspace. `#+=` page: `[]{}#%^*+=` / `_\|~<>` plus `` ` `` / `123` + `.,?!'` + backspace. ASCII only.

Key behaviour:
- Pointer down on a key → `setKeyboard(mods, usage)`; pointer up or cancel → `setKeyboard(stickyMods, 0)`. If a second key goes down before the first is up, the second replaces it, and the first key's up is ignored. Multi-touch on the panel must work (each key tracks its own pointer).
- Exception: keys in the function strip send press and release together when the finger lifts, and nothing if the finger moved past touch slop. A scroll that starts on `Del` must not delete on the host. These keys do not auto-repeat.
- A key defined by a character uses `KeyMap.stroke(c)`; `mods = stickyMods or stroke.mods`. Digit and symbol keys drop a sticky Shift first, so caps lock does not turn `123` into `!@#`. Letters, space and return keep it.
- Modifiers Ctrl, Alt, Win/Cmd, Shift are sticky, each with three states: off → armed (one tap; releases after the next non-modifier key goes up) → locked (second tap; stays until tapped again) → off. Armed = highlighted, locked = highlighted with an underline or dot. Each change calls `setKeyboard(stickyMods, 0)` so that modifier+click and modifier+scroll work.
- Shift armed or locked: letter keys show upper case. Shift locked = caps lock behaviour (no Caps Lock usage is sent).
- Haptic `KEYBOARD_TAP` on key down when `settings.haptics`.
- Panel leaves composition (toggle off, disconnect, back) → `setKeyboard(0, 0)`.
- The sticky-state logic is a small plain Kotlin class with no Compose or Android imports except state holders if needed; it has a unit test.

Voice input (mic key):
- In-app `android.speech.SpeechRecognizer`, not `RecognizerIntent`: a separate recognizer activity can stop `MainActivity`, and `onStop` disconnects the host.
- Manifest: `<uses-permission android:name="android.permission.RECORD_AUDIO" />` and `<queries><intent><action android:name="android.speech.RecognitionService" /></intent></queries>`.
- Hide the mic key when `SpeechRecognizer.isRecognitionAvailable(context)` is false.
- Tap: request `RECORD_AUDIO` if not granted, then `startListening` (free-form model, device default language). The key pulses while listening. Tap again → `stopListening`.
- `onResults`: first result + one trailing space → `typeText`. If every character was skipped, or on error, show a short text in the panel ("Only English letters can be typed", "Didn't catch that"). Create the recognizer on the main thread, `destroy()` it when the panel leaves composition.
