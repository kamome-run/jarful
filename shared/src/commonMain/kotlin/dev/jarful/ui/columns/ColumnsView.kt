package dev.jarful.ui.columns

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jarful.domain.Dates
import dev.jarful.domain.TaskTree
import dev.jarful.model.INBOX_ID
import dev.jarful.model.Task
import dev.jarful.platform.nowMillis
import dev.jarful.ui.AppState
import dev.jarful.ui.PrintTarget
import dev.jarful.ui.ds.ButtonKind
import dev.jarful.ui.ds.DsButton
import dev.jarful.ui.ds.DsCheckbox
import dev.jarful.ui.ds.DsDivider
import dev.jarful.ui.ds.DsIconButton
import dev.jarful.ui.ds.DsListItem
import dev.jarful.ui.ds.DsMenu
import dev.jarful.ui.ds.DsMenuItem
import dev.jarful.ui.i18n.LocalStrings

/** Miller-column task browser (FR-1). Wide layout shows all columns; compact shows one with a breadcrumb. */
@Composable
fun ColumnsView(state: AppState, compact: Boolean, modifier: Modifier = Modifier) {
    if (compact) CompactColumns(state, modifier) else WideColumns(state, modifier)
}

@Composable
private fun WideColumns(state: AppState, modifier: Modifier) {
    val scroll = rememberScrollState()
    LaunchedEffect(state.columnCount) { scroll.animateScrollTo(scroll.maxValue) }
    Column(modifier.fillMaxSize()) {
        Breadcrumb(state)
        Row(Modifier.fillMaxSize().horizontalScroll(scroll)) {
            for (i in 0 until state.columnCount) {
                TaskColumn(state, i, Modifier.width(300.dp).fillMaxHeight())
                if (i < state.columnCount - 1) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                }
            }
        }
    }
}

@Composable
private fun CompactColumns(state: AppState, modifier: Modifier) {
    val i = state.compactColumnIndex.coerceIn(0, state.columnCount - 1)
    val s = LocalStrings.current
    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            DsIconButton(onClick = { if (i > 0) { state.compactColumnIndex = i - 1; state.focusedColumn = i - 1 } }, enabled = i > 0, icon = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.close)
            Text(state.columnTitle(i), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Breadcrumb(state)
        TaskColumn(state, i, Modifier.fillMaxSize(), compact = true)
    }
}

@Composable
private fun Breadcrumb(state: AppState) {
    val s = LocalStrings.current
    val scroll = rememberScrollState()
    Row(Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        DsButton(onClick = { state.select(0, state.selectedIdInColumn(0)); state.compactColumnIndex = 0 }, kind = ButtonKind.Subtle) { Text(s.root) }
        state.path.forEachIndexed { i, id ->
            val t = TaskTree.byId(state.data.tasks, id) ?: return@forEachIndexed
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            DsButton(onClick = { state.focusedColumn = i + 1; state.compactColumnIndex = i + 1 }, kind = ButtonKind.Subtle) {
                Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TaskColumn(state: AppState, index: Int, modifier: Modifier, compact: Boolean = false) {
    val s = LocalStrings.current
    val parentId = state.parentIdForColumn(index)
    val tasks = state.visibleChildren(parentId)
    val selectedId = state.selectedIdInColumn(index)
    val focused = state.focusedColumn == index
    val open = tasks.count { !it.done }
    var menu by remember { mutableStateOf(false) }

    Column(modifier.background(if (focused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background)) {
        if (!compact) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(state.columnTitle(index), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("$open", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ColumnMenu(state, parentId, menu, { menu = it })
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(end = 4.dp), horizontalArrangement = Arrangement.End) { ColumnMenu(state, parentId, menu, { menu = it }) }
        }
        DsDivider()
        LazyColumn(Modifier.weight(1f)) {
            items(tasks, key = { it.id }) { t ->
                TaskRow(state, t, index, selected = t.id == selectedId, focusedColumn = focused, compact = compact)
            }
            item {
                if (state.addingIn == parentId) {
                    NewTaskField(state, parentId)
                } else {
                    DsButton(onClick = { state.focusedColumn = index; state.startAdd(parentId) }, kind = ButtonKind.Subtle, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.width(18.dp)); Spacer(Modifier.width(4.dp)); Text(if (parentId == null) s.newTask else s.newSubtask)
                    }
                }
                if (tasks.isEmpty() && state.addingIn != parentId) {
                    Text(s.microtaskHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ColumnMenu(state: AppState, parentId: String?, open: Boolean, setOpen: (Boolean) -> Unit) {
    val s = LocalStrings.current
    Box {
        DsIconButton(onClick = { setOpen(true) }, icon = Icons.Default.MoreVert, contentDescription = null)
        DsMenu(expanded = open, onDismissRequest = { setOpen(false) }) {
            DsMenuItem(s.columnToToday, icon = Icons.Default.Today, onClick = { setOpen(false); state.ticketizeColumn(parentId) })
            DsMenuItem(s.printColumn, icon = Icons.Default.Print, onClick = { setOpen(false); state.print(PrintTarget.Column(parentId)) })
            if (parentId != null) {
                DsMenuItem(s.pasteLines, icon = Icons.Default.Add, onClick = { setOpen(false); state.breakDownTaskId = parentId })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(state: AppState, t: Task, column: Int, selected: Boolean, focusedColumn: Boolean, compact: Boolean) {
    val s = LocalStrings.current
    val (openKids, totalKids) = TaskTree.childCounts(state.data.tasks, t.id)
    val todayTicket = state.data.tickets.any { it.taskId == t.id && it.date == state.todayIso() }
    val staleTicket = !t.done && state.data.tickets.any { k -> k.taskId == t.id && !k.isDone && Dates.parse(k.date) <= Dates.minusDays(Dates.today(), 3) }
    var menu by remember { mutableStateOf(false) }
    val bg = when {
        selected && focusedColumn -> MaterialTheme.colorScheme.primaryContainer
        selected -> MaterialTheme.colorScheme.surfaceVariant
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val fg = when {
        t.done -> MaterialTheme.colorScheme.onSurfaceVariant
        selected && focusedColumn -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (compact) {
                        state.select(column, t.id)
                        state.compactColumnIndex = column + 1
                        state.focusedColumn = column + 1
                    } else state.select(column, t.id)
                },
                onDoubleClick = { state.select(column, t.id); state.renamingId = t.id },
                onLongClick = { menu = true },
            ),
    ) {
        DsListItem(
            containerColor = bg, contentColor = fg,
            leading = { DsCheckbox(checked = t.done, onCheckedChange = { state.toggleDone(t.id) }, enabled = t.id != INBOX_ID) },
            headline = {
                if (state.renamingId == t.id) {
                    Row { RenameField(state, t) }
                } else {
                    Text(
                        t.title,
                        textDecoration = if (t.done) TextDecoration.LineThrough else null,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            supporting = if (staleTicket) {
                {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.breakDownHint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                        DsButton(onClick = { state.select(column, t.id); state.startAddChildOfSelection() }, kind = ButtonKind.Subtle) { Text(s.breakDownAction) }
                    }
                }
            } else null,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (todayTicket) Icon(Icons.Default.Today, s.toToday, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 2.dp))
                    if (totalKids > 0) {
                        Text(s.childCount(openKids, totalKids), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        DsIconButton(onClick = { menu = true }, icon = Icons.Default.MoreVert, contentDescription = null)
                        TaskMenu(state, t, column, menu) { menu = false }
                    }
                }
            },
        )
    }
}

@Composable
private fun TaskMenu(state: AppState, t: Task, column: Int, open: Boolean, dismiss: () -> Unit) {
    val s = LocalStrings.current
    DsMenu(expanded = open, onDismissRequest = dismiss) {
        DsMenuItem(s.newSubtask, icon = Icons.Default.Add, onClick = { dismiss(); state.select(column, t.id); state.startAddChildOfSelection() })
        if (t.id != INBOX_ID) {
            DsMenuItem(s.toToday, icon = Icons.Default.Today, onClick = { dismiss(); state.ticketize(t.id) })
            DsMenuItem(s.printTask, icon = Icons.Default.Print, onClick = { dismiss(); state.print(PrintTarget.Task(t.id)) })
        }
        DsMenuItem(s.estimate + " / " + s.timebox, onClick = { dismiss(); state.detailTaskId = t.id })
        DsMenuItem(s.pasteLines, onClick = { dismiss(); state.breakDownTaskId = t.id })
        DsMenuItem(s.rename, onClick = { dismiss(); state.select(column, t.id); state.renamingId = t.id })
        DsMenuItem(s.moveUp, onClick = { dismiss(); state.store.moveTask(t.id, -1) })
        DsMenuItem(s.moveDown, onClick = { dismiss(); state.store.moveTask(t.id, +1) })
        if (t.id != INBOX_ID) {
            DsMenuItem(s.moveTo, onClick = { dismiss(); state.moveTaskId = t.id })
            DsMenuItem(s.delete, danger = true, onClick = { dismiss(); state.confirmDeleteId = t.id })
        }
    }
}

/** Inline "new task" field: Enter commits and re-opens, Tab commits and opens a child field, Esc cancels (FR-2.1, FR-2.2). */
@Composable
private fun NewTaskField(state: AppState, parentId: String?) {
    val s = LocalStrings.current
    var text by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    fun commit(child: Boolean) {
        if (text.isBlank()) { if (!child) state.cancelEdit(); return }
        state.commitAdd(parentId, text, child)
        text = ""
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text, onValueChange = { text = it.replace("\n", "") },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit(false) }),
            modifier = Modifier.weight(1f).focusRequester(fr).onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.Tab -> { commit(true); true }
                    Key.Enter, Key.NumPadEnter -> { commit(e.isShiftPressed); true }
                    Key.Escape -> { state.cancelEdit(); true }
                    else -> false
                }
            },
            decorationBox = { inner ->
                Box { if (text.isEmpty()) Text(if (parentId == null) s.addTaskHint else s.addSubtaskHint, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1); inner() }
            },
        )
        DsIconButton(onClick = { commit(false) }, icon = Icons.Default.Check, contentDescription = s.ok)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.RenameField(state: AppState, t: Task) {
    var text by remember(t.id) { mutableStateOf(t.title) }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    BasicTextField(
        value = text, onValueChange = { text = it.replace("\n", "") }, singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardActions = KeyboardActions(onDone = { state.commitRename(t.id, text) }),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.weight(1f).focusRequester(fr)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)).padding(6.dp)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.Enter, Key.NumPadEnter -> { state.commitRename(t.id, text); true }
                    Key.Escape -> { state.renamingId = null; true }
                    else -> false
                }
            },
    )
}

@Suppress("unused")
private fun now() = nowMillis()
