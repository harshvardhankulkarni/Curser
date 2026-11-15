package app.curser.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.curser.core.Feedback
import app.curser.core.HostOs
import app.curser.core.Settings
import app.curser.core.TouchFrame
import app.curser.core.TouchPoint
import app.curser.core.TrackpadController
import app.curser.ui.components.Glyph
import app.curser.ui.components.GlyphButton
import app.curser.ui.components.GlyphIcon
import app.curser.ui.components.glyph
import app.curser.ui.keyboard.KeyboardPanel
import app.curser.ui.theme.CurserType
import app.curser.ui.theme.GlowBlue
import app.curser.ui.theme.Motion
import app.curser.ui.theme.TrackpadBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private class Glow(start: Offset) {
    var pos by mutableStateOf(start)
    val alpha = Animatable(0f)
    val scale = Animatable(0.55f)
}

@Composable
fun TrackpadScreen(
    controller: TrackpadController,
    settings: Settings,
    deviceName: String,
    os: HostOs,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val hapticsOn by rememberUpdatedState(settings.haptics)
    var pill by remember { mutableStateOf<String?>(null) }
    var kbOpen by remember { mutableStateOf(false) }

    LaunchedEffect(controller) {
        controller.feedback.collectLatest { fb ->
            if (hapticsOn) view.haptic(fb.kind)
            pill = fb.label
            delay(900)
            pill = null
        }
    }

    // A finger still down when we leave would otherwise leave a button held in core.
    DisposableEffect(controller) {
        onDispose { controller.onTouch(TouchFrame(SystemClock.uptimeMillis(), emptyList())) }
    }

    val glows = remember { mutableStateListOf<Glow>() }
    val scope = rememberCoroutineScope()

    Column(
        modifier
            .fillMaxSize()
            .background(TrackpadBackground)
            // keeps stray touches from reaching the screen underneath
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(horizontal = 12.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlyphButton(
                Glyph.ChevronLeft, "Disconnect and go back", onBack,
                tint = Color.White, background = Color.White.copy(alpha = 0.12f),
            )
            // the pill gets only the space between the buttons, so it cannot run under them on a narrow phone
            Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                StatusPill(deviceName, os, pill)
            }
            GlyphButton(
                Glyph.Keyboard, if (kbOpen) "Hide keyboard" else "Show keyboard", { kbOpen = !kbOpen },
                tint = Color.White, background = Color.White.copy(alpha = if (kbOpen) 0.28f else 0.12f),
            )
            Spacer(Modifier.width(8.dp))
            GlyphButton(
                Glyph.Gear, "Trackpad settings", onOpenSettings,
                tint = Color.White, background = Color.White.copy(alpha = 0.12f),
            )
        }

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val landscape = maxWidth > maxHeight
        val surface: @Composable (Modifier) -> Unit = { surfaceModifier ->
        Box(
            surfaceModifier
                // rotation can leave a finger mid-gesture; release everything
                .onSizeChanged { controller.onTouch(TouchFrame(SystemClock.uptimeMillis(), emptyList())) }
                .pointerInput(controller) {
                    val density = this.density
                    val active = HashMap<Long, Glow>()
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            controller.onTouch(
                                TouchFrame(
                                    event.changes.first().uptimeMillis,
                                    pressed.map { TouchPoint(it.id.value.toInt(), it.position.x / density, it.position.y / density) },
                                ),
                            )

                            val downIds = HashSet<Long>()
                            for (c in pressed) {
                                downIds.add(c.id.value)
                                val glow = active.getOrPut(c.id.value) {
                                    val g = Glow(c.position)
                                    glows.add(g)
                                    scope.launch {
                                        launch { g.alpha.animateTo(1f, Motion.soft()) }
                                        g.scale.animateTo(1f, Motion.bouncy())
                                    }
                                    g
                                }
                                glow.pos = c.position
                            }
                            val lifted = active.keys.filter { it !in downIds }
                            for (id in lifted) {
                                val g = active.remove(id) ?: continue
                                scope.launch {
                                    launch { g.alpha.animateTo(0f, Motion.snappy()) }
                                    g.scale.animateTo(1.7f, Motion.soft())
                                    glows.remove(g)
                                }
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val base = 96.dp.toPx()
                for (g in glows) {
                    val a = g.alpha.value.coerceIn(0f, 1f)
                    val r = base * g.scale.value.coerceAtLeast(0.1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                GlowBlue.copy(alpha = 0.55f * a),
                                GlowBlue.copy(alpha = 0.18f * a),
                                GlowBlue.copy(alpha = 0f),
                            ),
                            center = g.pos,
                            radius = r,
                        ),
                        radius = r,
                        center = g.pos,
                    )
                }
            }
        }
        }
        val panelInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars)
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    kbOpen,
                    Modifier.weight(1.7f).fillMaxHeight(),
                    enter = slideInHorizontally(Motion.soft()) { -it } + fadeIn(),
                    exit = slideOutHorizontally(Motion.soft()) { -it } + fadeOut(),
                ) {
                    KeyboardPanel(
                        controller, os, settings.haptics, compact = true,
                        modifier = Modifier.fillMaxSize().windowInsetsPadding(panelInsets),
                    )
                }
                surface(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                surface(Modifier.weight(1f).fillMaxWidth().heightIn(min = 160.dp))
                AnimatedVisibility(
                    kbOpen,
                    enter = expandVertically(Motion.soft()) + fadeIn(),
                    exit = shrinkVertically(Motion.soft()) + fadeOut(),
                ) {
                    KeyboardPanel(
                        controller, os, settings.haptics, compact = false,
                        modifier = Modifier.fillMaxWidth().windowInsetsPadding(panelInsets),
                    )
                }
            }
        }
        }
    }
}

/** Dynamic-Island-style pill: host name at rest, widens with the action label on feedback. */
@Composable
private fun StatusPill(deviceName: String, os: HostOs, label: String?, modifier: Modifier = Modifier) {
    val width by animateDpAsState(if (label != null) 220.dp else 160.dp, Motion.bouncy(), label = "pillW")
    val height by animateDpAsState(if (label != null) 42.dp else 36.dp, Motion.bouncy(), label = "pillH")
    Box(
        modifier
            .width(width)
            .height(height)
            .clip(CircleShape)
            .background(Color.Black)
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = {
                (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f)) togetherWith fadeOut(tween(120))
            },
            label = "pillContent",
        ) { text ->
            if (text != null) {
                Text(text, style = CurserType.Headline, color = Color.White, maxLines = 1)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphIcon(os.glyph(), Color.White.copy(alpha = 0.85f), size = 16.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        deviceName,
                        style = CurserType.Footnote,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Hides system bars and keeps the screen on while [enabled]; restores both after. */
@Composable
fun ImmersiveEffect(enabled: Boolean) {
    val view = LocalView.current
    val cfg = LocalConfiguration.current
    // the system may bring the bars back on rotation
    LaunchedEffect(enabled, cfg.orientation, cfg.screenWidthDp) {
        if (enabled) view.context.findActivity()?.window?.let {
            WindowCompat.getInsetsController(it, view).hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(enabled, view) {
        val window = view.context.findActivity()?.window
        if (!enabled || window == null) return@DisposableEffect onDispose { }
        val insets = WindowCompat.getInsetsController(window, view)
        insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insets.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            insets.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }
}

internal fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private fun View.haptic(kind: Feedback.Kind) {
    val constant = when (kind) {
        Feedback.Kind.CLICK, Feedback.Kind.DRAG -> HapticFeedbackConstants.CLOCK_TICK
        Feedback.Kind.GESTURE ->
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}
