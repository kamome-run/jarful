package dev.jarful.ui.ds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class ShellNavItem(val id: String, val label: String, val icon: ImageVector)

/**
 * Application frame (§7). Material: Scaffold + NavigationRail/NavigationBar + FAB.
 * Fluent: WinUI NavigationView (left pane on Mica, selection pill, content in a rounded Layer),
 * a command bar in the header, and an InfoBar for messages.
 */
@Composable
fun DsAppShell(
    wide: Boolean,
    items: List<ShellNavItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    title: String,
    actions: @Composable () -> Unit,
    primaryAction: Pair<String, ImageVector>?,
    onPrimaryAction: () -> Unit,
    snackbar: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    if (isFluent()) FluentShell(wide, items, selectedId, onSelect, title, actions, primaryAction, onPrimaryAction, snackbar, content)
    else MaterialShell(wide, items, selectedId, onSelect, title, actions, primaryAction, onPrimaryAction, snackbar, content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialShell(
    wide: Boolean, items: List<ShellNavItem>, selectedId: String, onSelect: (String) -> Unit, title: String,
    actions: @Composable () -> Unit, primaryAction: Pair<String, ImageVector>?, onPrimaryAction: () -> Unit,
    snackbar: SnackbarHostState, content: @Composable () -> Unit,
) {
    if (wide) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                header = {
                    if (primaryAction != null) FloatingActionButton(onClick = onPrimaryAction, modifier = Modifier.padding(vertical = 8.dp)) { Icon(primaryAction.second, primaryAction.first) }
                },
            ) {
                items.forEach { e -> NavigationRailItem(selected = e.id == selectedId, onClick = { onSelect(e.id) }, icon = { Icon(e.icon, e.label) }, label = { Text(e.label) }) }
            }
            Scaffold(
                topBar = { TopAppBar(title = { Text(title) }, actions = { actions() }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
                snackbarHost = { SnackbarHost(snackbar) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { content() } }
        }
    } else {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text(title) }, actions = { actions() }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
            bottomBar = {
                NavigationBar {
                    items.forEach { e -> NavigationBarItem(selected = e.id == selectedId, onClick = { onSelect(e.id) }, icon = { Icon(e.icon, e.label) }, label = { Text(e.label) }) }
                }
            },
            floatingActionButton = {
                if (primaryAction != null) ExtendedFloatingActionButton(onClick = onPrimaryAction, icon = { Icon(primaryAction.second, null) }, text = { Text(primaryAction.first) })
            },
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { content() } }
    }
}

@Composable
private fun FluentShell(
    wide: Boolean, items: List<ShellNavItem>, selectedId: String, onSelect: (String) -> Unit, title: String,
    actions: @Composable () -> Unit, primaryAction: Pair<String, ImageVector>?, onPrimaryAction: () -> Unit,
    snackbar: SnackbarHostState, content: @Composable () -> Unit,
) {
    val t = LocalFluent.current
    Box(Modifier.fillMaxSize().background(t.micaBase)) {
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                // NavigationView pane (open mode): 200dp, brand header, items with the 3x16 selection pill.
                Column(Modifier.width(200.dp).fillMaxHeight().padding(top = 8.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)) {
                    Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(t.accent))
                        Spacer(Modifier.width(10.dp))
                        Text("Jarful", style = FluentType.bodyStrong, color = t.textPrimary)
                    }
                    items.forEach { e -> FluentNavItem(e, e.id == selectedId, compact = false) { onSelect(e.id) } }
                    Spacer(Modifier.weight(1f))
                    if (primaryAction != null) {
                        DsButton(onClick = onPrimaryAction, kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                            Icon(primaryAction.second, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(primaryAction.first)
                        }
                    }
                }
                FluentContentFrame(title, actions, snackbar, content)
            }
        } else {
            // Top navigation mode for narrow windows.
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    items.forEach { e -> FluentNavItem(e, e.id == selectedId, compact = true) { onSelect(e.id) } }
                    Spacer(Modifier.weight(1f))
                    if (primaryAction != null) DsIconButton(onClick = onPrimaryAction, icon = primaryAction.second, contentDescription = primaryAction.first, tint = t.accent)
                }
                FluentContentFrame(title, actions, snackbar, content)
            }
        }
    }
}

@Composable
private fun FluentNavItem(e: ShellNavItem, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val t = LocalFluent.current
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val shape = RoundedCornerShape(t.controlRadius)
    val bg = when { selected -> t.subtleFill; hovered -> t.subtleFill.copy(alpha = t.subtleFill.alpha * 0.7f); else -> Color.Transparent }
    Row(
        Modifier.then(if (compact) Modifier else Modifier.fillMaxWidth()).padding(vertical = 1.dp).height(36.dp).clip(shape).background(bg, shape)
            .hoverable(src).clickable(interactionSource = src, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // selection indicator pill (WinUI NavigationViewItem)
        Box(Modifier.width(3.dp).height(16.dp).clip(CircleShape).background(if (selected) t.accent else Color.Transparent))
        Spacer(Modifier.width(9.dp))
        Icon(e.icon, e.label, tint = if (selected) t.textPrimary else t.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(e.label, style = if (selected) FluentType.bodyStrong else FluentType.body, color = t.textPrimary, maxLines = 1)
        Spacer(Modifier.width(if (compact) 12.dp else 8.dp))
    }
}

/** Header (title + command bar) and the rounded content Layer, like Windows 11 Settings. */
@Composable
private fun FluentContentFrame(title: String, actions: @Composable () -> Unit, snackbar: SnackbarHostState, content: @Composable () -> Unit) {
    val t = LocalFluent.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = FluentType.title, color = t.textPrimary, modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { actions() }
        }
        Box(
            Modifier.fillMaxSize().padding(start = 0.dp)
                .clip(RoundedCornerShape(topStart = t.overlayRadius))
                .background(t.layerFill)
                .border(1.dp, t.cardStroke, RoundedCornerShape(topStart = t.overlayRadius)),
        ) {
            content()
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp)) { data -> FluentInfoBar(data.visuals.message) }
        }
    }
}

/** WinUI InfoBar (informational). */
@Composable
private fun FluentInfoBar(message: String) {
    val t = LocalFluent.current
    val shape = RoundedCornerShape(t.controlRadius)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (t.dark) Color(0xFF2B2B2B) else Color(0xFFF7F7F7), shape).border(1.dp, t.cardStroke, shape).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(t.accent))
        Spacer(Modifier.width(12.dp))
        Text(message, style = FluentType.body, color = t.textPrimary)
    }
}

@Suppress("unused")
private fun Snackbar() = Unit
