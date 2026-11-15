package app.curser.ui.keyboard

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.curser.core.HostOs
import app.curser.core.TrackpadController
import app.curser.core.keyboard.KeyMap
import app.curser.ui.components.Glyph
import app.curser.ui.components.GlyphIcon
import app.curser.ui.components.pressable
import app.curser.ui.theme.CurserType
import app.curser.ui.theme.Motion
import kotlinx.coroutines.delay

private val PanelBg = Color(0xFF0B0B0D)
private val KeyFill = Color(0xFF6B6B6B)
private val KeyFillDown = Color(0xFF9A9A9C)
private val SpecialFill = Color(0xFF3A3A3C)
private val SpecialFillDown = Color(0xFF636366)
private val ActiveFill = Color(0xFFF2F2F7)
private val KeyShape = RoundedCornerShape(5.dp)

private enum class Page { LETTERS, NUMBERS, SYMBOLS }

/** Shared by all keys: sticky state and the single "current key" rule (a newer key replaces the older). */
private class Ctx(
    val controller: TrackpadController,
    val sticky: StickyMods,
    val haptics: Boolean,
    val view: View,
    val keyHeight: Dp,
) {
    private var current: Any? = null

    private fun tick() {
        if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun down(token: Any, mods: Int, usage: Int, keepShift: Boolean = true) {
        tick()
        current = token
        val base = if (keepShift) sticky.mods else sticky.mods and StickyMods.SHIFT.inv()
        controller.setKeyboard(base or mods, usage)
    }

    fun up(token: Any) {
        if (current !== token) return
        current = null
        sticky.keyReleased()
        controller.setKeyboard(sticky.mods, 0)
    }

    fun toggle(mod: Int) {
        tick()
        sticky.tap(mod)
        controller.setKeyboard(sticky.mods, 0)
    }
}

@Composable
fun KeyboardPanel(
    controller: TrackpadController,
    os: HostOs,
    haptics: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val sticky = remember { StickyMods() }
    val ctx = remember(controller, haptics, compact) { Ctx(controller, sticky, haptics, view, if (compact) 34.dp else 42.dp) }
    var page by remember { mutableStateOf(Page.LETTERS) }
    var note by remember { mutableStateOf<String?>(null) }

    DisposableEffect(controller) { onDispose { controller.setKeyboard(0, 0) } }
    LaunchedEffect(note) {
        if (note != null) {
            delay(2000)
            note = null
        }
    }

    val voice = rememberVoiceInput(
        onText = { text ->
            val skipped = controller.typeText(text)
            if (skipped >= text.trim().length) note = "Only English letters can be typed"
        },
        onMessage = { note = it },
    )

    val gap = if (compact) 3.dp else 5.dp
    val mac = os != HostOs.WINDOWS
    val shift = sticky.state(StickyMods.SHIFT) != Sticky.OFF

    Box(modifier.background(PanelBg)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            val shortcuts = @Composable { Shortcuts(ctx, os, Modifier) }
            val functions = @Composable { Functions(ctx, Modifier) }
            if (compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { shortcuts() }
                    Box(Modifier.weight(1f)) { functions() }
                }
            } else {
                shortcuts()
                functions()
            }

            KeyRow(gap) {
                ModKey(ctx, StickyMods.CTRL, if (mac) "control" else "Ctrl", Modifier.weight(1.3f))
                ModKey(ctx, StickyMods.ALT, if (mac) "option" else "Alt", Modifier.weight(1.3f))
                ModKey(ctx, StickyMods.GUI, if (mac) "cmd" else "Win", Modifier.weight(1.3f))
                UsageKey(ctx, "Tab", "Tab", KeyMap.TAB, 1.2f, special = true)
                UsageKey(ctx, "←", "Left arrow", KeyMap.LEFT, 1f, special = true)
                UsageKey(ctx, "↑", "Up arrow", KeyMap.UP, 1f, special = true)
                UsageKey(ctx, "↓", "Down arrow", KeyMap.DOWN, 1f, special = true)
                UsageKey(ctx, "→", "Right arrow", KeyMap.RIGHT, 1f, special = true)
            }

            when (page) {
                Page.LETTERS -> {
                    KeyRow(gap) { "qwertyuiop".forEach { CharKey(ctx, it, shift) } }
                    KeyRow(gap) {
                        Spacer(Modifier.weight(0.5f))
                        "asdfghjkl".forEach { CharKey(ctx, it, shift) }
                        Spacer(Modifier.weight(0.5f))
                    }
                    KeyRow(gap) {
                        ModKey(ctx, StickyMods.SHIFT, "⇧", Modifier.weight(1.5f), "Shift")
                        "zxcvbnm".forEach { CharKey(ctx, it, shift) }
                        Backspace(ctx, 1.5f)
                    }
                }
                Page.NUMBERS -> {
                    KeyRow(gap) { "1234567890".forEach { CharKey(ctx, it, false) } }
                    KeyRow(gap) { "-/:;()$&@\"".forEach { CharKey(ctx, it, false) } }
                    KeyRow(gap) {
                        PageKey(ctx, "#+=", 1.5f) { page = Page.SYMBOLS }
                        ".,?!'".forEach { CharKey(ctx, it, false, 1.4f) }
                        Backspace(ctx, 1.5f)
                    }
                }
                Page.SYMBOLS -> {
                    KeyRow(gap) { "[]{}#%^*+=".forEach { CharKey(ctx, it, false) } }
                    KeyRow(gap) {
                        Spacer(Modifier.weight(1.5f))
                        "_\\|~<>`".forEach { CharKey(ctx, it, false) }
                        Spacer(Modifier.weight(1.5f))
                    }
                    KeyRow(gap) {
                        PageKey(ctx, "123", 1.5f) { page = Page.NUMBERS }
                        ".,?!'".forEach { CharKey(ctx, it, false, 1.4f) }
                        Backspace(ctx, 1.5f)
                    }
                }
            }

            KeyRow(gap) {
                if (page == Page.LETTERS) PageKey(ctx, "123", 1.4f) { page = Page.NUMBERS }
                else PageKey(ctx, "ABC", 1.4f) { page = Page.LETTERS }
                if (voice != null) {
                    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
                        1f, 0.45f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "micAlpha",
                    )
                    Key(
                        ctx, null, if (voice.listening) "Stop voice input" else "Voice input", Modifier.weight(1f),
                        special = true, glyph = Glyph.Mic, active = voice.listening,
                        contentAlpha = if (voice.listening) pulse else 1f,
                        onDown = { voice.toggle() },
                    )
                }
                CharKey(ctx, ' ', false, 4f, "space", "Space")
                CharKey(ctx, '\n', false, 1.8f, "return", "Return", special = true)
            }
        }

        note?.let {
            Text(
                it,
                style = CurserType.Footnote,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .semantics { stateDescription = it },
            )
        }
    }
}

@Composable
private fun KeyRow(gap: Dp, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap / 1.25f), content = content)
}

@Composable
private fun Shortcuts(ctx: Ctx, os: HostOs, modifier: Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (s in remember(os) { ctx.controller.shortcuts(os) }) {
            Box(
                Modifier
                    .height(ctx.keyHeight * 0.8f)
                    .pressable { ctx.controller.tapKey(s.mods, s.usage) }
                    .clip(RoundedCornerShape(50))
                    .background(SpecialFill)
                    .semantics { contentDescription = s.label }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(s.label, style = CurserType.Footnote, color = Color.White, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Functions(ctx: Ctx, modifier: Modifier) {
    val keys = buildList {
        add(Triple("esc", "Escape", KeyMap.ESC))
        for (i in 1..12) add(Triple("F$i", "F$i", KeyMap.F1 + i - 1))
        add(Triple("PrtSc", "Print screen", KeyMap.PRINT_SCREEN))
        add(Triple("Ins", "Insert", KeyMap.INSERT))
        add(Triple("Del", "Delete", KeyMap.DELETE))
        add(Triple("Home", "Home", KeyMap.HOME))
        add(Triple("End", "End", KeyMap.END))
        add(Triple("PgUp", "Page up", KeyMap.PAGE_UP))
        add(Triple("PgDn", "Page down", KeyMap.PAGE_DOWN))
    }
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((label, desc, usage) in keys) {
            Key(
                ctx, label, desc, Modifier.widthIn(min = 40.dp).height(ctx.keyHeight * 0.8f),
                special = true, scrolls = true,
                onDown = { ctx.down(it, 0, usage) }, onUp = { ctx.up(it) },
            )
        }
    }
}

@Composable
private fun RowScope.UsageKey(ctx: Ctx, label: String, desc: String, usage: Int, weight: Float, special: Boolean) {
    Key(
        ctx, label, desc, Modifier.weight(weight), special = special,
        onDown = { ctx.down(it, 0, usage) }, onUp = { ctx.up(it) },
    )
}

@Composable
private fun RowScope.Backspace(ctx: Ctx, weight: Float) {
    Key(
        ctx, "⌫", "Backspace", Modifier.weight(weight), special = true,
        onDown = { ctx.down(it, 0, KeyMap.BACKSPACE) }, onUp = { ctx.up(it) },
    )
}

@Composable
private fun RowScope.CharKey(
    ctx: Ctx,
    c: Char,
    shift: Boolean,
    weight: Float = 1f,
    label: String = c.toString(),
    desc: String = c.toString(),
    special: Boolean = false,
) {
    val stroke = KeyMap.stroke(c) ?: return
    val text = if (shift && c.isLetter()) label.uppercase() else label
    // caps lock must not turn the digit and symbol pages into "!@#"; Shift+Space and Shift+Return stay possible
    val keepShift = c.isLetter() || c == ' ' || c == '\n'
    Key(
        ctx, text, if (shift && c.isLetter()) text else desc, Modifier.weight(weight), special = special,
        onDown = { ctx.down(it, stroke.mods, stroke.usage, keepShift) }, onUp = { ctx.up(it) },
    )
}

@Composable
private fun RowScope.ModKey(ctx: Ctx, mod: Int, label: String, modifier: Modifier, desc: String = label) {
    val state = ctx.sticky.state(mod)
    Key(
        ctx, label, desc, modifier, special = true, active = state != Sticky.OFF, locked = state == Sticky.LOCKED,
        stateText = when (state) {
            Sticky.OFF -> "off"
            Sticky.ARMED -> "armed for next key"
            Sticky.LOCKED -> "locked"
        },
        onDown = { ctx.toggle(mod) },
    )
}

@Composable
private fun RowScope.PageKey(ctx: Ctx, label: String, weight: Float, onSwitch: () -> Unit) {
    Key(ctx, label, "Switch to $label keys", Modifier.weight(weight), special = true, onDown = { onSwitch() })
}

/**
 * One key. Reacts on pointer down and reports up on release or cancel; never consumes, so several keys
 * track their own pointers and a scrolling strip can still scroll. A [scrolls] key sends its press on
 * release instead, and nothing at all once the finger has moved past touch slop.
 */
@Composable
private fun Key(
    ctx: Ctx,
    label: String?,
    desc: String,
    modifier: Modifier,
    special: Boolean = false,
    glyph: Glyph? = null,
    active: Boolean = false,
    locked: Boolean = false,
    scrolls: Boolean = false,
    contentAlpha: Float = 1f,
    stateText: String? = null,
    onDown: (Any) -> Unit,
    onUp: (Any) -> Unit = {},
) {
    val token = remember { Any() }
    val downCb by rememberUpdatedState(onDown)
    val upCb by rememberUpdatedState(onUp)
    var pressed by remember { mutableStateOf(false) }
    val slop = LocalViewConfiguration.current.touchSlop
    val fill by animateColorAsState(
        when {
            active -> ActiveFill
            special -> if (pressed) SpecialFillDown else SpecialFill
            else -> if (pressed) KeyFillDown else KeyFill
        },
        Motion.snappy(), label = "keyFill",
    )
    val textColor = if (active) Color.Black else Color.White
    Box(
        modifier
            .height(ctx.keyHeight)
            .clip(KeyShape)
            .background(fill)
            .pointerInput(token, scrolls) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        // a key in a scrolling strip fires on release: a scroll that starts on Del must not delete on the host
                        if (!scrolls) downCb(token)
                        try {
                            var tapped = false
                            while (true) {
                                val change = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    tapped = true
                                    break
                                }
                                if (scrolls && (change.position - down.position).getDistance() > slop) break
                            }
                            if (scrolls && tapped) downCb(token)
                        } finally {
                            pressed = false
                            upCb(token)
                        }
                    }
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = desc
                stateText?.let { stateDescription = it }
                // TalkBack activates by click action instead of raw touches
                onClick { downCb(token); upCb(token); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) GlyphIcon(glyph, textColor, Modifier.alpha(contentAlpha), size = 22.dp)
        if (label != null) Text(label, style = if (label.length > 2) CurserType.Footnote else CurserType.Body, color = textColor, maxLines = 1)
        if (locked) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(4.dp).clip(CircleShape).background(Color.Black))
    }
}
