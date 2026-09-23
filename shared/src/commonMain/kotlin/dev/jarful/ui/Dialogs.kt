package dev.jarful.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import dev.jarful.ui.ds.ButtonKind
import dev.jarful.ui.ds.DsButton
import dev.jarful.ui.ds.DsDialog
import dev.jarful.ui.ds.DsTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.jarful.domain.TaskTree
import dev.jarful.model.INBOX_ID
import dev.jarful.ui.common.ConfirmDialog
import dev.jarful.ui.common.IntField
import dev.jarful.ui.common.TextAreaDialog
import dev.jarful.ui.common.DuplicateDialog
import dev.jarful.ui.i18n.LocalStrings

/** All modal dialogs, driven by [AppState] flags. */
@Composable
fun AppDialogs(state: AppState) {
    val s = LocalStrings.current

    if (state.refocusOpen) RefocusDialog(state)

    state.confirmDeleteId?.let { id ->
        val t = TaskTree.byId(state.data.tasks, id)
        if (t == null) state.confirmDeleteId = null
        else ConfirmDialog(title = s.delete, body = s.confirmDelete(t.title), confirmText = s.delete, onConfirm = { state.deleteTask(id) }, onDismiss = { state.confirmDeleteId = null })
    }

    state.breakDownTaskId?.let { id ->
        val t = TaskTree.byId(state.data.tasks, id)
        if (t == null) state.breakDownTaskId = null
        else TextAreaDialog(title = s.subtasksOf(t.title), hint = s.pasteLinesHint + " · " + s.microtaskHint, confirmText = s.breakDownAction,
            onConfirm = { text -> state.store.addTasksFromText(id, text); state.breakDownTaskId = null; state.tab = Tab.COLUMNS },
            onDismiss = { state.breakDownTaskId = null })
    }

    if (state.importOpen) {
        TextAreaDialog(title = s.importJson, hint = s.pasteJsonHint, confirmText = s.importJson,
            onConfirm = { text -> state.showToast(if (state.store.importJson(text)) s.importOk else s.importFailed); state.importOpen = false },
            onDismiss = { state.importOpen = false })
    }

    state.detailTaskId?.let { id -> TaskDetailDialog(state, id) }
    state.moveTaskId?.let { id -> MoveTaskDialog(state, id) }
    state.duplicateTaskId?.let { id ->
        val t = TaskTree.byId(state.data.tasks, id)
        if (t == null) state.duplicateTaskId = null
        else DuplicateDialog(title = t.title, onDismiss = { state.duplicateTaskId = null }) { n -> state.store.copyTask(id, n); state.duplicateTaskId = null }
    }

    if (state.showShortcuts) {
        DsDialog(
            onDismissRequest = { state.showShortcuts = false },
            title = { Text(s.shortcuts) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 420.dp)) {
                    s.shortcutsList.forEach { (k, v) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text(k, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth(0.4f))
                            Text(v, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { DsButton(onClick = { state.showShortcuts = false }, modifier = Modifier.fillMaxWidth()) { Text(s.close) } },
        )
    }

    if (state.showOnboarding) {
        DsDialog(
            onDismissRequest = { state.finishOnboarding(false) },
            title = { Text(s.sampleRoutinesTitle) },
            text = { Text(s.sampleRoutinesBody) },
            confirmButton = { DsButton(onClick = { state.finishOnboarding(true) }, kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth()) { Text(s.sampleRoutinesAdd) } },
            dismissButton = { DsButton(onClick = { state.finishOnboarding(false) }, modifier = Modifier.fillMaxWidth()) { Text(s.sampleRoutinesSkip) } },
        )
    }
}

/** "Next 3–5 tasks" quick entry (FR-7). Ctrl+Enter starts. */
@Composable
private fun RefocusDialog(state: AppState) {
    val s = LocalStrings.current
    var text by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    DsDialog(
        onDismissRequest = { state.refocusOpen = false },
        title = { Text("⚡ " + s.refocus) },
        text = {
            Column {
                Text(s.refocusHint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                DsTextField(
                    value = text, onValueChange = { text = it }, minLines = 5, maxLines = 8,
                    placeholder = s.refocusPlaceholder,
                    modifier = Modifier.fillMaxWidth().focusRequester(fr).onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.isCtrlPressed && (e.key == Key.Enter || e.key == Key.NumPadEnter)) { state.refocus(text); true } else false
                    },
                )
            }
        },
        confirmButton = { DsButton(onClick = { state.refocus(text) }, enabled = text.isNotBlank(), kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth()) { Text(s.refocusGo) } },
        dismissButton = { DsButton(onClick = { state.refocusOpen = false }, modifier = Modifier.fillMaxWidth()) { Text(s.cancel) } },
    )
}

/** Estimate / timebox editor (FR-2.3, FR-2.4). */
@Composable
private fun TaskDetailDialog(state: AppState, id: String) {
    val s = LocalStrings.current
    val t = TaskTree.byId(state.data.tasks, id) ?: run { state.detailTaskId = null; return }
    var est by remember(id) { mutableStateOf(t.estimateMin) }
    var box by remember(id) { mutableStateOf(t.timeboxMin) }
    DsDialog(
        onDismissRequest = { state.detailTaskId = null },
        title = { Text(t.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.microtaskHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField(s.estimate, est, { est = it }, suffix = s.minutes)
                    IntField(s.timebox, box, { box = it }, suffix = s.minutes)
                }
            }
        },
        confirmButton = { DsButton(onClick = { state.store.setTaskEstimate(id, est, box); state.detailTaskId = null }, kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth()) { Text(s.save) } },
        dismissButton = { DsButton(onClick = { state.detailTaskId = null }, modifier = Modifier.fillMaxWidth()) { Text(s.cancel) } },
    )
}

/** Re-parent a task (FR-2.5): pick any task that is not itself or a descendant. */
@Composable
private fun MoveTaskDialog(state: AppState, id: String) {
    val s = LocalStrings.current
    val tasks = state.data.tasks
    val t = TaskTree.byId(tasks, id) ?: run { state.moveTaskId = null; return }
    val excluded = (TaskTree.descendants(tasks, id).map { it.id } + id).toSet()
    val candidates = tasks.filter { it.id !in excluded && !it.done }.map { it to TaskTree.pathTo(tasks, it.id).joinToString(" › ") { p -> p.title } }.sortedBy { it.second }
    DsDialog(
        onDismissRequest = { state.moveTaskId = null },
        title = { Text("${s.moveTo}: ${t.title}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)) {
                DsButton(onClick = { state.store.reparentTask(id, null); state.moveTaskId = null }, kind = ButtonKind.Subtle) { Text(s.moveToRoot) }
                candidates.forEach { (c, label) ->
                    DsButton(onClick = { state.store.reparentTask(id, c.id); state.moveTaskId = null }, kind = ButtonKind.Subtle) { Text(label) }
                }
            }
        },
        confirmButton = { DsButton(onClick = { state.moveTaskId = null }, modifier = Modifier.fillMaxWidth()) { Text(s.cancel) } },
    )
}

@Suppress("unused")
private const val UNUSED_INBOX = INBOX_ID
