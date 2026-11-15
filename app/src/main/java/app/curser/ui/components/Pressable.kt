package app.curser.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.curser.ui.theme.Motion

/** iOS press feedback: scale and dim on a spring. No ripple, indication is always null. */
fun Modifier.pressable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, Motion.snappy(), label = "pressScale")
    val dim by animateFloatAsState(if (pressed) 0.65f else 1f, Motion.snappy(), label = "pressDim")
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) dim else 0.4f
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** Icon-only round button. 44 dp touch target, spoken description required. */
@Composable
fun GlyphButton(
    glyph: Glyph,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color,
    background: Color,
) {
    Box(
        modifier
            .size(44.dp)
            .pressable(onClick = onClick)
            .semantics { this.contentDescription = description }
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, tint, size = 22.dp)
    }
}
