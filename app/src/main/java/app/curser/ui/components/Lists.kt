package app.curser.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.curser.ui.theme.CurserTheme
import app.curser.ui.theme.CurserType
import app.curser.ui.theme.Motion

@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CurserTheme.colors
    Column(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        if (header != null) {
            Text(
                header,
                style = CurserType.Footnote,
                color = colors.secondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp).semantics { heading() },
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.cell),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                style = CurserType.Footnote,
                color = colors.secondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            )
        }
    }
}

@Composable
fun CellDivider(startInset: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(0.5.dp)
            .background(CurserTheme.colors.separator),
    )
}

/** Grouped-list row. Press shows a flat highlight, like UITableViewCell. */
@Composable
fun Cell(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = CurserTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hl by animateFloatAsState(if (pressed) 1f else 0f, Motion.snappy(), label = "cellHl")
    var m: Modifier = modifier
        .fillMaxWidth()
        .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
        .heightIn(min = 44.dp)
        .drawBehind { if (hl > 0f) drawRect(colors.highlight, alpha = hl) }
    if (onClick != null) {
        m = m.clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
    }
    Row(
        m.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun SwitchCell(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = CurserTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = CurserType.Body, color = colors.label, modifier = Modifier.weight(1f))
        IosSwitch(checked)
    }
}

@Composable
fun SliderCell(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    val colors = CurserTheme.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = CurserType.Body, color = colors.label, modifier = Modifier.weight(1f))
            Text(valueText, style = CurserType.Body, color = colors.secondary)
        }
        IosSlider(value, onValueChange, valueRange, description = title)
    }
}

@Composable
fun IconTile(glyph: Glyph, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(30.dp).background(color, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, Color.White, size = 18.dp)
    }
}

/** Title + subtitle column used inside Cell. */
@Composable
fun RowScope.CellText(title: String, subtitle: String? = null) {
    val colors = CurserTheme.colors
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
        Text(title, style = CurserType.Body, color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(subtitle, style = CurserType.Footnote, color = colors.secondary, maxLines = 2)
        }
    }
}

/** Page with a large title that collapses into an inline title once scrolled. */
@Composable
fun LargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CurserTheme.colors
    val scroll = rememberScrollState()
    val threshold = with(LocalDensity.current) { 40.dp.toPx() }
    val collapsed by remember { derivedStateOf { scroll.value > threshold } }
    val barAlpha by animateFloatAsState(if (collapsed) 1f else 0f, Motion.snappy(), label = "navBar")

    Box(modifier.fillMaxSize().background(colors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.systemBars).only(WindowInsetsSides.Horizontal))
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Spacer(Modifier.height(44.dp))
            Text(
                title,
                style = CurserType.LargeTitle,
                color = colors.label,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp).semantics { heading() },
            )
            content()
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
          }
        }
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .graphicsLayer { alpha = barAlpha }
                .background(colors.background),
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                Text(title, style = CurserType.Headline, color = colors.label)
            }
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
        }
    }
}
