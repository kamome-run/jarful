package dev.jarful.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.jarful.data.Store
import dev.jarful.ui.columns.ColumnsView
import dev.jarful.ui.i18n.LocalStrings
import dev.jarful.ui.i18n.stringsFor
import dev.jarful.ui.jar.JarView
import dev.jarful.ui.routines.RoutinesView
import dev.jarful.ui.settings.SettingsView
import dev.jarful.ui.stats.StatsView
import dev.jarful.ui.theme.JarfulTheme
import dev.jarful.ui.today.TodayView
import kotlinx.coroutines.delay

/** Root composable shared by Android and desktop (§7). */
@Composable
fun JarfulApp(store: Store) {
    val data by store.data.collectAsState()
    val strings = stringsFor(data.settings.language)
    val scope = rememberCoroutineScope()
    val state = remember { AppState(store, scope, strings) }
    state.strings = strings
    val snackbar = remember { SnackbarHostState() }
    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!data.settings.onboardingDone && data.routines.isEmpty()) state.showOnboarding = true
        // Re-check routine preparation every minute (FR-6.3) — covers apps left open across midnight / prepare time.
        while (true) { delay(60_000); store.prepareRoutines() }
    }
    LaunchedEffect(state.toast) {
        val t = state.toast ?: return@LaunchedEffect
        snackbar.showSnackbar(t)
        state.toast = null
    }
    // Return keyboard focus to the root whenever inline editing ends so plain-key shortcuts keep working.
    LaunchedEffect(state.addingIn, state.renamingId, state.refocusOpen) {
        if (state.addingIn == AppState.NONE && state.renamingId == null && !state.refocusOpen) runCatching { rootFocus.requestFocus() }
    }

    CompositionLocalProvider(LocalStrings provides strings) {
        JarfulTheme {
            BoxWithConstraints(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .focusRequester(rootFocus).focusable()
                    .onPreviewKeyEvent { globalShortcut(state, it) }
                    .onKeyEvent { columnShortcut(state, it) },
            ) {
                val wide = maxWidth >= 840.dp
                if (wide) WideLayout(state) else CompactLayout(state)
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (wide) 16.dp else 88.dp)) { Snackbar(it) }
                AppDialogs(state)
            }
        }
    }
}

@Composable
private fun WideLayout(state: AppState) {
    val s = LocalStrings.current
    Row(Modifier.fillMaxSize()) {
        NavigationRail { RailItems(state) }
        when (state.tab) {
            Tab.COLUMNS, Tab.TODAY -> {
                ColumnsView(state, compact = false, modifier = Modifier.weight(1f).fillMaxHeight())
                VerticalDivider()
                Column(Modifier.width(380.dp).fillMaxHeight()) {
                    JarView(state.loopsToday(), state.jarDropSignal, Modifier.fillMaxWidth().padding(top = 8.dp), height = 200.dp)
                    TodayView(state, showJar = false, modifier = Modifier.weight(1f))
                }
            }
            Tab.ROUTINES -> RoutinesView(state, Modifier.weight(1f))
            Tab.STATS -> StatsView(state, Modifier.weight(1f))
            Tab.SETTINGS -> SettingsView(state, Modifier.weight(1f))
        }
    }
    @Suppress("UNUSED_VARIABLE") val unused = s
}

@Composable
private fun CompactLayout(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (state.tab) {
                Tab.TODAY -> TodayView(state, showJar = true)
                Tab.COLUMNS -> ColumnsView(state, compact = true)
                Tab.ROUTINES -> RoutinesView(state)
                Tab.STATS -> StatsView(state)
                Tab.SETTINGS -> SettingsView(state)
            }
        }
        NavigationBar { BarItems(state) }
    }
}

private data class NavEntry(val tab: Tab, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun navEntries(): List<NavEntry> {
    val s = LocalStrings.current
    return listOf(
        NavEntry(Tab.TODAY, s.tabToday, Icons.Default.Today),
        NavEntry(Tab.COLUMNS, s.tabColumns, Icons.Default.ViewColumn),
        NavEntry(Tab.ROUTINES, s.tabRoutines, Icons.Default.Repeat),
        NavEntry(Tab.STATS, s.tabStats, Icons.Default.BarChart),
        NavEntry(Tab.SETTINGS, s.tabSettings, Icons.Default.Settings),
    )
}

@Composable
private fun RailItems(state: AppState) {
    val s = LocalStrings.current
    navEntries().forEach { e ->
        // In the wide layout TODAY and COLUMNS share one screen.
        val selected = state.tab == e.tab || (e.tab == Tab.COLUMNS && state.tab == Tab.TODAY)
        NavigationRailItem(selected = selected, onClick = { state.tab = e.tab }, icon = { Icon(e.icon, e.label) }, label = { Text(e.label) })
    }
    NavigationRailItem(selected = false, onClick = { state.refocusOpen = true }, icon = { Icon(Icons.Default.Bolt, s.refocus) }, label = { Text(s.refocus) })
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BarItems(state: AppState) {
    navEntries().forEach { e ->
        NavigationBarItem(selected = state.tab == e.tab, onClick = { state.tab = e.tab }, icon = { Icon(e.icon, e.label) }, label = { Text(e.label) })
    }
}

/** Global shortcuts (§8) — evaluated before children so they work even while typing. */
private fun globalShortcut(state: AppState, e: KeyEvent): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    val ctrl = e.isCtrlPressed || e.isMetaPressed
    if (!ctrl) return false
    return when (e.key) {
        Key.K -> { state.refocusOpen = true; true }
        Key.P -> { state.print(PrintTarget.Today); true }
        Key.Z -> { state.undo(); true }
        Key.One -> { state.tab = Tab.TODAY; true }
        Key.Two -> { state.tab = Tab.COLUMNS; true }
        Key.Three -> { state.tab = Tab.ROUTINES; true }
        Key.Four -> { state.tab = Tab.STATS; true }
        Key.Five -> { state.tab = Tab.SETTINGS; true }
        else -> false
    }
}

/** Column shortcuts (§8) — only when no text field consumed the key (bubbling phase). */
private fun columnShortcut(state: AppState, e: KeyEvent): Boolean {
    if (e.type != KeyEventType.KeyDown) return false
    if (e.isCtrlPressed || e.isMetaPressed) return false
    if (e.key == Key.Escape) {
        state.cancelEdit(); state.refocusOpen = false; state.showShortcuts = false; return true
    }
    val onColumns = state.tab == Tab.COLUMNS || state.tab == Tab.TODAY
    if (!onColumns) return false
    if (state.addingIn != AppState.NONE || state.renamingId != null) return false
    val sel = state.selectedTaskId
    return when (e.key) {
        Key.DirectionUp -> { if (e.isAltPressed) sel?.let { state.store.moveTask(it, -1) } else state.moveSelection(-1); true }
        Key.DirectionDown -> { if (e.isAltPressed) sel?.let { state.store.moveTask(it, +1) } else state.moveSelection(+1); true }
        Key.DirectionLeft -> { state.focusLeft(); true }
        Key.DirectionRight -> { state.focusRight(); true }
        Key.N -> { state.startAdd(state.parentIdForColumn(state.focusedColumn)); true }
        Key.Enter, Key.NumPadEnter -> { if (e.isShiftPressed) state.startAddChildOfSelection() else state.startAdd(state.parentIdForColumn(state.focusedColumn)); true }
        Key.Tab -> { state.startAddChildOfSelection(); true }
        Key.Spacebar -> { sel?.let { state.toggleDone(it) }; true }
        Key.T -> { if (e.isShiftPressed) state.ticketizeColumn(state.parentIdForColumn(state.focusedColumn)) else state.ticketizeSelected(); true }
        Key.P -> { if (e.isShiftPressed) state.print(PrintTarget.Column(state.parentIdForColumn(state.focusedColumn))) else sel?.let { state.print(PrintTarget.Task(it)) }; true }
        Key.F2 -> { sel?.let { state.renamingId = it }; true }
        Key.Delete, Key.Backspace -> { sel?.let { state.confirmDeleteId = it }; true }
        else -> false
    }
}
