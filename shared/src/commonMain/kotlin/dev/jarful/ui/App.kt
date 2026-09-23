package dev.jarful.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.jarful.platform.hasHardwareKeyboard
import dev.jarful.ui.columns.ColumnsView
import dev.jarful.ui.ds.DesignSystem
import dev.jarful.ui.ds.DsAppShell
import dev.jarful.ui.ds.DsIconButton
import dev.jarful.ui.ds.JarfulDesignTheme
import dev.jarful.ui.ds.ShellNavItem
import dev.jarful.ui.ds.platformDesignSystem
import dev.jarful.ui.i18n.LocalStrings
import dev.jarful.ui.i18n.resolveLanguage
import dev.jarful.ui.i18n.stringsFor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dev.jarful.ui.jar.JarView
import dev.jarful.ui.routines.RoutinesView
import dev.jarful.ui.settings.SettingsView
import dev.jarful.ui.stats.StatsView
import dev.jarful.ui.today.TodayView
import kotlinx.coroutines.delay

/** Root composable shared by Android and desktop (§7). Material 3 throughout (NFR-8). */
@Composable
fun JarfulApp(store: Store, darkTheme: Boolean? = null, designSystemOverride: DesignSystem? = null) {
    val data by store.data.collectAsState()
    val strings = stringsFor(data.settings.language)
    val scope = rememberCoroutineScope()
    val state = remember { AppState(store, scope, strings) }
    state.strings = strings
    val snackbar = remember { SnackbarHostState() }
    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!data.settings.onboardingDone && data.routines.isEmpty()) state.showOnboarding = true
        if (data.settings.sync.autoSync) state.syncNow(quiet = true)
        // Re-check routine preparation every minute (FR-6.3) — covers apps left open across midnight / prepare time.
        var tick = 0
        while (true) {
            delay(60_000); tick++
            store.prepareRoutines()
            if (tick % 5 == 0 && store.data.value.settings.sync.autoSync) state.syncNow(quiet = true) // FR-14.6: every 5 min
        }
    }
    LaunchedEffect(data.settings.sync.hostEnabled, data.settings.sync.port) { state.reconcileSyncHost() }
    DisposableEffect(Unit) { onDispose { state.stopSyncHost() } }
    LaunchedEffect(state.toast) {
        val t = state.toast ?: return@LaunchedEffect
        snackbar.showSnackbar(t)
        state.toast = null
    }
    // Return keyboard focus to the root whenever inline editing ends so plain-key shortcuts keep working.
    LaunchedEffect(state.addingIn, state.renamingId, state.refocusOpen) {
        if (state.addingIn == AppState.NONE && state.renamingId == null && !state.refocusOpen) runCatching { rootFocus.requestFocus() }
    }

    val designSystem = remember { platformDesignSystem() }
    val layoutDirection = if (resolveLanguage(data.settings.language).rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalStrings provides strings, LocalLayoutDirection provides layoutDirection) {
        JarfulDesignTheme(system = designSystemOverride ?: designSystem, dark = darkTheme ?: isSystemInDarkTheme()) {
            BoxWithConstraints(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .focusRequester(rootFocus).focusable()
                    .onPreviewKeyEvent { globalShortcut(state, it) }
                    .onKeyEvent { columnShortcut(state, it) },
            ) {
                val wide = maxWidth >= 840.dp
                val s = LocalStrings.current
                val items = listOf(
                    ShellNavItem(Tab.TODAY.name, s.tabToday, Icons.Default.Today),
                    ShellNavItem(Tab.COLUMNS.name, s.tabColumns, Icons.Default.ViewColumn),
                    ShellNavItem(Tab.ROUTINES.name, s.tabRoutines, Icons.Default.Repeat),
                    ShellNavItem(Tab.STATS.name, s.tabStats, Icons.Default.BarChart),
                    ShellNavItem(Tab.SETTINGS.name, s.tabSettings, Icons.Default.Settings),
                )
                // In the wide layout TODAY and COLUMNS share one screen.
                val selected = if (wide && state.tab == Tab.TODAY) Tab.COLUMNS.name else state.tab.name
                val title = when (state.tab) {
                    Tab.TODAY -> if (wide) s.tabColumns else s.tabToday
                    Tab.COLUMNS -> s.tabColumns
                    Tab.ROUTINES -> s.tabRoutines
                    Tab.STATS -> s.tabStats
                    Tab.SETTINGS -> s.tabSettings
                }
                DsAppShell(
                    wide = wide, items = items, selectedId = selected, onSelect = { state.tab = Tab.valueOf(it) },
                    title = title,
                    actions = { TopActions(state, wide) },
                    primaryAction = if (wide || state.tab == Tab.TODAY || state.tab == Tab.COLUMNS) (s.refocus to Icons.Default.Bolt) else null,
                    onPrimaryAction = { state.refocusOpen = true },
                    snackbar = snackbar,
                ) {
                    if (wide) WideContent(state) else CompactContent(state)
                }
                AppDialogs(state)
            }
        }
    }
}

/** Shared top-bar / command-bar actions: sync, print today, shortcuts (§8). */
@Composable
private fun TopActions(state: AppState, wide: Boolean) {
    val s = LocalStrings.current
    val hasPeer = state.data.settings.sync.peerHost.isNotBlank()
    if (hasPeer) DsIconButton(onClick = { state.syncNow() }, icon = Icons.Default.Sync, contentDescription = s.syncNow, enabled = !state.syncing)
    DsIconButton(onClick = { state.printFromTopBar(wide) }, icon = Icons.Default.Print, contentDescription = if (!wide && state.tab == Tab.COLUMNS) s.printColumn else s.printToday, enabled = !state.printing)
    if (hasHardwareKeyboard()) DsIconButton(onClick = { state.showShortcuts = true }, icon = Icons.Default.Keyboard, contentDescription = s.shortcuts)
}

@Composable
private fun WideContent(state: AppState) {
    Row(Modifier.fillMaxSize()) {
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
}

@Composable
private fun CompactContent(state: AppState) {
    Box(Modifier.fillMaxSize()) {
        when (state.tab) {
            Tab.TODAY -> TodayView(state, showJar = true)
            Tab.COLUMNS -> ColumnsView(state, compact = true)
            Tab.ROUTINES -> RoutinesView(state)
            Tab.STATS -> StatsView(state)
            Tab.SETTINGS -> SettingsView(state)
        }
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
