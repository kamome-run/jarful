package dev.jarful.ui.ds

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/*
 * Design-system primitives (NFR-8). Every composable here has a Material 3 branch (Android) and a
 * Fluent / WinUI 3 branch (Windows). Screens use only these plus foundation layouts, so the two
 * platforms share one screen implementation while looking native.
 */

enum class ButtonKind { Accent, Standard, Subtle }

@Composable
fun DsButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, kind: ButtonKind = ButtonKind.Standard, content: @Composable RowScope.() -> Unit) {
    if (!isFluent()) {
        when (kind) {
            ButtonKind.Accent -> Button(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
            ButtonKind.Standard -> OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
            ButtonKind.Subtle -> TextButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)
        }
        return
    }
    val t = LocalFluent.current
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val pressed by src.collectIsPressedAsState()
    val bg = when (kind) {
        ButtonKind.Accent -> when { !enabled -> t.textDisabled.copy(alpha = 0.2f); pressed -> t.accentPressed; hovered -> t.accentHover; else -> t.accent }
        ButtonKind.Standard -> when { !enabled -> t.controlFillTertiary; pressed -> t.controlFillTertiary; hovered -> t.controlFillSecondary; else -> t.controlFill }
        ButtonKind.Subtle -> when { pressed -> t.subtleFill.copy(alpha = t.subtleFill.alpha * 0.6f); hovered -> t.subtleFill; else -> Color.Transparent }
    }
    val fg = when { !enabled -> t.textDisabled; kind == ButtonKind.Accent -> t.textOnAccent; else -> t.textPrimary }
    val shape = RoundedCornerShape(t.controlRadius)
    Row(
        modifier
            .defaultMinSize(minHeight = 32.dp)
            .clip(shape)
            .background(bg, shape)
            .then(if (kind == ButtonKind.Standard) Modifier.border(1.dp, t.controlStroke, shape) else Modifier)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg, LocalTextStyle provides FluentType.body.copy(color = fg)) { content() }
    }
}

@Composable
fun DsIconButton(onClick: () -> Unit, icon: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, enabled: Boolean = true, tint: Color? = null, size: androidx.compose.ui.unit.Dp = 40.dp) {
    if (!isFluent()) {
        IconButton(onClick = onClick, modifier = modifier.size(size), enabled = enabled) { Icon(icon, contentDescription, tint = tint ?: LocalContentColor.current) }
        return
    }
    val t = LocalFluent.current
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val pressed by src.collectIsPressedAsState()
    val bg = when { pressed -> t.subtleFill.copy(alpha = t.subtleFill.alpha * 0.6f); hovered -> t.subtleFill; else -> Color.Transparent }
    val shape = RoundedCornerShape(t.controlRadius)
    Box(
        modifier.size(size - 4.dp).clip(shape).background(bg, shape)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (!enabled) t.textDisabled else tint ?: t.textPrimary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun DsSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    if (!isFluent()) { Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled); return }
    val t = LocalFluent.current
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val track by animateColorAsState(if (checked) (if (hovered) t.accentHover else t.accent) else (if (hovered) t.controlFillSecondary else t.controlFill))
    val knobX by animateDpAsState(if (checked) 22.dp else 4.dp)
    val knobSize by animateDpAsState(if (checked || hovered) 14.dp else 12.dp)
    Box(
        Modifier.width(40.dp).height(20.dp).clip(CircleShape).background(track)
            .border(1.dp, if (checked) Color.Transparent else t.textSecondary, CircleShape)
            .clickable(interactionSource = src, indication = null, enabled = enabled) { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = knobX - (knobSize - 12.dp) / 2).size(knobSize).clip(CircleShape)
                .background(if (checked) t.textOnAccent else t.textSecondary),
        )
    }
}

@Composable
fun DsCheckbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    if (!isFluent()) { Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled); return }
    val t = LocalFluent.current
    val shape = RoundedCornerShape(t.controlRadius)
    val bg = when { !enabled -> t.controlFillTertiary; checked -> t.accent; else -> t.controlFill }
    Box(
        Modifier.padding(10.dp).size(20.dp).clip(shape).background(bg, shape)
            .border(1.dp, if (checked) Color.Transparent else t.textSecondary, shape)
            .clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Default.Check, null, tint = t.textOnAccent, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun DsTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: String? = null, placeholder: String? = null, singleLine: Boolean = false, minLines: Int = 1, maxLines: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default, suffix: String? = null,
) {
    if (!isFluent()) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = modifier,
            label = label?.let { { Text(it) } }, placeholder = placeholder?.let { { Text(it) } },
            singleLine = singleLine, minLines = minLines, maxLines = if (singleLine) 1 else maxLines,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, suffix = suffix?.let { { Text(it) } },
        )
        return
    }
    val t = LocalFluent.current
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val shape = RoundedCornerShape(t.controlRadius)
    Column(modifier) {
        if (label != null) Text(label, style = FluentType.body, color = t.textPrimary, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            Modifier.fillMaxWidth().background(t.controlFill, shape).border(1.dp, t.controlStroke, shape)
                .then(if (focused) Modifier.bottomStroke(t.accent, 2.dp, t.controlRadius) else Modifier.bottomStroke(t.textSecondary, 1.dp, t.controlRadius))
                .padding(horizontal = 10.dp, vertical = if (singleLine) 6.dp else 8.dp)
                .heightIn(min = if (singleLine) 20.dp else (20 * minLines).dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value, onValueChange = onValueChange, singleLine = singleLine, minLines = minLines, maxLines = if (singleLine) 1 else maxLines,
                    keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, interactionSource = src,
                    textStyle = FluentType.body.copy(color = t.textPrimary), cursorBrush = SolidColor(t.textPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner -> Box { if (value.isEmpty() && placeholder != null) Text(placeholder, style = FluentType.body, color = t.textSecondary); inner() } },
                )
                if (suffix != null) Text(suffix, style = FluentType.body, color = t.textSecondary, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

/** WinUI text boxes have a stronger 1px (2px accent when focused) bottom edge. */
private fun Modifier.bottomStroke(color: Color, width: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp): Modifier =
    drawWithContent {
        drawContent()
        val w = width.toPx(); val r = radius.toPx()
        drawLine(color, Offset(r, size.height - w / 2), Offset(size.width - r, size.height - w / 2), strokeWidth = w)
    }

enum class CardTone { Default, Highlight, Muted }

@Composable
fun DsCard(modifier: Modifier = Modifier, tone: CardTone = CardTone.Default, content: @Composable ColumnScope.() -> Unit) {
    if (!isFluent()) {
        when (tone) {
            CardTone.Default -> ElevatedCard(modifier = modifier, colors = CardDefaults.elevatedCardColors(), elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp), content = content)
            CardTone.Highlight -> Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer), content = content)
            CardTone.Muted -> Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), content = content)
        }
        return
    }
    val t = LocalFluent.current
    val shape = RoundedCornerShape(t.overlayRadius)
    val (bg, stroke, fg) = when (tone) {
        CardTone.Default -> Triple(t.cardFill, t.cardStroke, t.textPrimary)
        CardTone.Highlight -> Triple(t.accentSoft, t.accent, t.textPrimary)
        CardTone.Muted -> Triple(t.cardFillSecondary, t.cardStroke, t.textSecondary)
    }
    Column(modifier.clip(shape).background(bg, shape).border(1.dp, stroke, shape)) {
        CompositionLocalProvider(LocalContentColor provides fg) { content() }
    }
}

@Composable
fun DsListItem(
    modifier: Modifier = Modifier,
    headline: @Composable () -> Unit,
    supporting: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color? = null,
) {
    if (!isFluent()) {
        ListItem(
            modifier = modifier,
            colors = ListItemDefaults.colors(containerColor = containerColor, headlineColor = contentColor ?: MaterialTheme.colorScheme.onSurface),
            headlineContent = headline, supportingContent = supporting, leadingContent = leading, trailingContent = trailing,
        )
        return
    }
    val t = LocalFluent.current
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val shape = RoundedCornerShape(t.controlRadius)
    val bg = if (containerColor == Color.Transparent && hovered) t.subtleFill else containerColor
    Row(
        modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp).clip(shape).background(bg, shape)
            .hoverable(src)
            .defaultMinSize(minHeight = 40.dp).padding(start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides (contentColor ?: t.textPrimary), LocalTextStyle provides FluentType.body) {
            if (leading != null) leading()
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                headline()
                if (supporting != null) CompositionLocalProvider(LocalTextStyle provides FluentType.caption, LocalContentColor provides t.textSecondary) { supporting() }
            }
            if (trailing != null) trailing()
        }
    }
}

@Composable
fun DsDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    if (!isFluent()) {
        AlertDialog(onDismissRequest = onDismissRequest, title = title, text = text, confirmButton = confirmButton, dismissButton = dismissButton)
        return
    }
    val t = LocalFluent.current
    val shape = RoundedCornerShape(t.overlayRadius)
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // WinUI ContentDialog: layer background, title, content, then a footer strip with equal-width buttons.
        Column(
            Modifier.widthIn(min = 320.dp, max = 560.dp).clip(shape)
                .background(if (t.dark) Color(0xFF2B2B2B) else Color(0xFFFFFFFF), shape)
                .border(1.dp, t.cardStroke, shape),
        ) {
            Column(Modifier.padding(24.dp)) {
                CompositionLocalProvider(LocalTextStyle provides FluentType.subtitle.copy(color = t.textPrimary), LocalContentColor provides t.textPrimary) { title() }
                if (text != null) {
                    Spacer(Modifier.height(12.dp))
                    CompositionLocalProvider(LocalTextStyle provides FluentType.body.copy(color = t.textPrimary), LocalContentColor provides t.textPrimary) { text() }
                }
            }
            Row(
                Modifier.fillMaxWidth().background(if (t.dark) Color(0xFF202020) else Color(0xFFF3F3F3)).border(1.dp, t.dividerStroke).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) { confirmButton() }
                if (dismissButton != null) Box(Modifier.weight(1f)) { dismissButton() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (!isFluent()) { DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, content = content); return }
    val t = LocalFluent.current
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(t.overlayRadius),
        containerColor = if (t.dark) Color(0xFF2C2C2C) else Color(0xFFF9F9F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.cardStroke),
        content = content,
    )
}

@Composable
fun DsMenuItem(text: String, onClick: () -> Unit, icon: ImageVector? = null, danger: Boolean = false) {
    if (!isFluent()) {
        DropdownMenuItem(
            text = { Text(text, color = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified) },
            leadingIcon = icon?.let { { Icon(it, null) } }, onClick = onClick,
        )
        return
    }
    val t = LocalFluent.current
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val shape = RoundedCornerShape(t.controlRadius)
    val fg = if (danger) t.critical else t.textPrimary
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp).clip(shape)
            .background(if (hovered) t.subtleFill else Color.Transparent, shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp).widthIn(min = 160.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(12.dp)) }
        Text(text, style = FluentType.body, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Material: SegmentedButton row. Fluent: SelectorBar-style tabs with an accent underline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DsSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (!isFluent()) {
        SingleChoiceSegmentedButtonRow(modifier) {
            options.forEachIndexed { i, label ->
                SegmentedButton(selected = selected == i, onClick = { onSelect(i) }, shape = SegmentedButtonDefaults.itemShape(i, options.size)) { Text(label, maxLines = 1) }
            }
        }
        return
    }
    val t = LocalFluent.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val src = remember { MutableInteractionSource() }
            val hovered by src.collectIsHoveredAsState()
            val on = selected == i
            Column(
                Modifier.clip(RoundedCornerShape(t.controlRadius)).background(if (hovered) t.subtleFill else Color.Transparent)
                    .clickable(interactionSource = src, indication = null) { onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, style = if (on) FluentType.bodyStrong else FluentType.body, color = if (on) t.textPrimary else t.textSecondary, maxLines = 1)
                Box(Modifier.padding(top = 4.dp).width(16.dp).height(3.dp).clip(CircleShape).background(if (on) t.accent else Color.Transparent))
            }
        }
    }
}

@Composable
fun DsChip(selected: Boolean, onClick: () -> Unit, label: String) {
    if (!isFluent()) { FilterChip(selected = selected, onClick = onClick, label = { Text(label) }); return }
    DsButton(onClick = onClick, kind = if (selected) ButtonKind.Accent else ButtonKind.Standard) { Text(label) }
}

@Composable
fun DsProgress(progress: () -> Float, modifier: Modifier = Modifier, color: Color? = null) {
    if (!isFluent()) {
        LinearProgressIndicator(progress = progress, modifier = modifier, color = color ?: MaterialTheme.colorScheme.primary)
        return
    }
    val t = LocalFluent.current
    Box(modifier.height(4.dp).clip(CircleShape).background(t.controlStroke)) {
        Box(Modifier.fillMaxWidth(progress().coerceIn(0f, 1f)).height(4.dp).clip(CircleShape).background(color ?: t.accent))
    }
}

@Composable
fun DsDivider() {
    if (!isFluent()) { HorizontalDivider(); return }
    val t = LocalFluent.current
    Box(Modifier.fillMaxWidth().height(1.dp).background(t.dividerStroke))
}

/** Section heading used on settings-like screens. */
@Composable
fun DsSectionTitle(text: String, modifier: Modifier = Modifier) {
    if (!isFluent()) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = modifier.padding(top = 16.dp, bottom = 6.dp))
        return
    }
    val t = LocalFluent.current
    Text(text, style = FluentType.bodyStrong.copy(fontSize = 16.sp), color = t.textPrimary, modifier = modifier.padding(top = 20.dp, bottom = 8.dp))
}

private val Int.sp get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Suppress("unused")
private val unusedStyle: TextStyle? = null
