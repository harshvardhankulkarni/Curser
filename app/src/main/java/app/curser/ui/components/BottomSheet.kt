package app.curser.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import app.curser.ui.theme.CurserTheme
import app.curser.ui.theme.CurserType
import app.curser.ui.theme.LocalColors
import app.curser.ui.theme.Motion
import app.curser.ui.theme.forSheet
import kotlinx.coroutines.launch

/**
 * In-composition sheet (no Dialog window, so immersive mode survives).
 * Rises on a spring, drags down from the header to dismiss.
 */
@Composable
fun BottomSheet(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val frac = remember { Animatable(0f) } // 0 hidden, 1 shown
    var heightPx by remember { mutableFloatStateOf(0f) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    var lastDy by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val currentDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(visible) { frac.animateTo(if (visible) 1f else 0f, Motion.sheet()) }

    fun close() {
        scope.launch {
            frac.animateTo(0f, Motion.sheet())
            currentDismiss()
        }
    }

    fun settle() {
        val dy = dragPx
        val flick = lastDy > 14f
        scope.launch {
            frac.snapTo((frac.value - dy / heightPx.coerceAtLeast(1f)).coerceIn(0f, 1f))
            dragPx = 0f
            if (flick || frac.value < 0.65f) {
                frac.animateTo(0f, Motion.sheet())
                currentDismiss()
            } else {
                frac.animateTo(1f, Motion.sheet())
            }
        }
    }

    BackHandler(enabled = visible) { close() }

    if (!visible && frac.value <= 0.001f) return

    val colors = CurserTheme.colors
    val sheetColors = remember(colors) { colors.forSheet() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxSheet = maxHeight * 0.92f
        val shown = (frac.value - dragPx / heightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * shown))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Close",
                    onClick = { close() },
                ),
        )

        CompositionLocalProvider(LocalColors provides sheetColors) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxSheet)
                    .onSizeChanged { heightPx = it.height.toFloat() }
                    .graphicsLayer {
                        translationY = ((1f - frac.value) * heightPx + dragPx).coerceAtLeast(0f)
                        alpha = if (heightPx == 0f) 0f else 1f
                    }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(sheetColors.background)
                    // swallow taps so they never reach the scrim
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
            ) {
                Column(
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { lastDy = 0f },
                            onDragEnd = { settle() },
                            onDragCancel = { settle() },
                            onVerticalDrag = { change, dy ->
                                change.consume()
                                lastDy = dy
                                dragPx = (dragPx + dy).coerceAtLeast(0f)
                            },
                        )
                    },
                ) {
                    Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(36.dp, 5.dp).background(sheetColors.grabber, CircleShape))
                    }
                    Box(Modifier.fillMaxWidth().height(44.dp)) {
                        Text(
                            title,
                            style = CurserType.Headline,
                            color = sheetColors.label,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                .pressable { close() }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Done", style = CurserType.Headline, color = sheetColors.blue)
                        }
                    }
                }
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                ) {
                    Spacer(Modifier.height(8.dp))
                    content()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
