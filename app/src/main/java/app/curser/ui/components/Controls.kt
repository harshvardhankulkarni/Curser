package app.curser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curser.ui.theme.CurserTheme
import app.curser.ui.theme.CurserType
import app.curser.ui.theme.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Visual only. The enclosing row owns the toggle semantics (see SwitchCell). */
@Composable
fun IosSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = CurserTheme.colors
    val track by animateColorAsState(if (checked) colors.green else colors.switchOff, Motion.snappy(), label = "swTrack")
    val thumbX by animateDpAsState(if (checked) 20.dp else 0.dp, Motion.snappy(), label = "swThumb")
    Box(
        modifier
            .size(51.dp, 31.dp)
            .background(track, CircleShape)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = thumbX)
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CurserTheme.colors
    // 44 dp tall hit area around a 32 dp track
    BoxWithConstraints(modifier.fillMaxWidth().height(44.dp).selectableGroup()) {
        val segW = (maxWidth - 4.dp) / options.size
        val thumbX by animateDpAsState(2.dp + segW * selectedIndex, Motion.snappy(), label = "segX")
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(colors.fill),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbX)
                .width(segW)
                .height(28.dp)
                .shadow(2.dp, RoundedCornerShape(7.dp))
                .background(colors.segmentThumb, RoundedCornerShape(7.dp)),
        )
        Row(Modifier.fillMaxSize().padding(horizontal = 2.dp)) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(i) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = CurserType.Footnote,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = colors.label,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun IosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = CurserTheme.colors
    val onChange by rememberUpdatedState(onValueChange)
    val thumb = 28.dp
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange)
                setProgress { target ->
                    onChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            }
            .pointerInput(valueRange) {
                awaitEachGesture {
                    val thumbPx = thumb.toPx()
                    fun emit(x: Float) {
                        val f = ((x - thumbPx / 2f) / (size.width - thumbPx)).coerceIn(0f, 1f)
                        onChange(valueRange.start + f * (valueRange.endInclusive - valueRange.start))
                    }
                    val down = awaitFirstDown()
                    emit(down.position.x)
                    drag(down.id) { change ->
                        emit(change.position.x)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val span = valueRange.endInclusive - valueRange.start
        val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
        val x = (maxWidth - thumb) * fraction
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.track))
        Box(Modifier.width(x + thumb / 2).height(4.dp).clip(CircleShape).background(colors.blue))
        Box(
            Modifier
                .offset(x = x)
                .size(thumb)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CurserTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .pressable(onClick = onClick)
            .background(colors.blue, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = CurserType.Headline, color = Color.White)
    }
}

/** 12-tick iOS style spinner. */
@Composable
fun ActivityIndicator(color: Color, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 20.dp) {
    val t by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "spinT",
    )
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val step = t.toInt() % 12
        for (i in 0 until 12) {
            val a = (i * 30f - 90f) * (PI / 180.0).toFloat()
            val age = (step - i + 12) % 12
            val alpha = (1f - age / 12f).coerceAtLeast(0.15f)
            drawLine(
                color.copy(alpha = alpha),
                Offset(center.x + cos(a) * r * 0.55f, center.y + sin(a) * r * 0.55f),
                Offset(center.x + cos(a) * r * 0.95f, center.y + sin(a) * r * 0.95f),
                strokeWidth = r * 0.2f,
                cap = StrokeCap.Round,
            )
        }
    }
}
