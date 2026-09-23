package dev.jarful.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.jarful.domain.Dates
import dev.jarful.model.Routine
import dev.jarful.ui.AppState
import dev.jarful.ui.PrintTarget
import dev.jarful.ui.common.DuplicateDialog
import dev.jarful.ui.common.IntField
import dev.jarful.ui.common.LabeledRow
import dev.jarful.ui.common.rememberReorderState
import dev.jarful.ui.common.reorderHandle
import dev.jarful.ui.common.reorderItem
import dev.jarful.ui.ds.ButtonKind
import dev.jarful.ui.ds.DsButton
import dev.jarful.ui.ds.DsCard
import dev.jarful.ui.ds.DsCheckbox
import dev.jarful.ui.ds.DsChip
import dev.jarful.ui.ds.DsDialog
import dev.jarful.ui.ds.DsListItem
import dev.jarful.ui.ds.DsSectionTitle
import dev.jarful.ui.ds.DsSwitch
import dev.jarful.ui.ds.DsTextField
import dev.jarful.ui.i18n.LocalStrings
import kotlinx.datetime.DayOfWeek

/** Weekday routines (FR-6), grouped under category headings (FR-6.11). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutinesView(state: AppState, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val routines = state.data.routines.sortedBy { it.order }
    // Groups appear in the order their first routine appears; the routine order inside a group is the global order.
    val groups: List<Pair<String, List<Routine>>> = routines.groupBy { it.category.trim() }.entries.map { it.key to it.value }
    val display = groups.flatMap { it.second }
    val selectMode = state.routineSelectMode
    val listState = rememberLazyListState()
    val reorder = rememberReorderState(listState, keys = { display.map { it.id } }) { from, to ->
        val moving = display.getOrNull(from) ?: return@rememberReorderState
        val target = display.getOrNull(to) ?: return@rememberReorderState
        // Dragging into another heading moves the routine to that category (FR-6.11).
        if (moving.category.trim() != target.category.trim()) state.store.upsertRoutine(moving.copy(category = target.category))
        state.store.moveRoutineTo(moving.id, routines.indexOfFirst { it.id == target.id })
    }

    LazyColumn(modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "header") {
            Text(s.routines, style = MaterialTheme.typography.titleLarge)
            Text(s.routineOrderHint + " " + s.dragToReorder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DsButton(onClick = { state.editingRoutine = state.newRoutine() }, kind = ButtonKind.Accent) { Icon(Icons.Default.Add, null, modifier = Modifier.width(18.dp)); Spacer(Modifier.width(4.dp)); Text(s.routineNew) }
                DsButton(onClick = { state.print(PrintTarget.Routines) }, enabled = !state.printing) { Icon(Icons.Default.Print, null, modifier = Modifier.width(18.dp)); Spacer(Modifier.width(4.dp)); Text(s.routinePrintToday) }
                DsButton(onClick = { state.printDateDialog = true }, enabled = !state.printing, modifier = Modifier.testTag("routinePrintDate")) { Icon(Icons.Default.DateRange, null, modifier = Modifier.width(18.dp)); Spacer(Modifier.width(4.dp)); Text(s.routinePrintDate) }
                DsButton(onClick = { if (selectMode) state.exitRoutineSelect() else state.routineSelectMode = true }, kind = if (selectMode) ButtonKind.Accent else ButtonKind.Standard, modifier = Modifier.testTag("routineSelectMode")) {
                    Icon(Icons.Default.Checklist, null, modifier = Modifier.width(18.dp)); Spacer(Modifier.width(4.dp)); Text(if (selectMode) s.close else s.selectMode)
                }
                DsButton(onClick = { state.store.regenerateToday() }, kind = ButtonKind.Subtle) { Text(s.routineRegenerate) }
            }
            if (selectMode) SelectionBar(state, routines)
        }
        if (routines.isEmpty()) {
            item(key = "empty") {
                Text(s.routineEmpty, modifier = Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                DsButton(onClick = { state.addSampleRoutines() }, kind = ButtonKind.Subtle) { Text(s.sampleRoutinesAdd) }
            }
        }
        groups.forEach { (category, members) ->
            item(key = "cat:$category") {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selectMode) {
                        val all = members.all { it.id in state.selectedRoutineIds }
                        DsCheckbox(checked = all, onCheckedChange = { on -> members.forEach { r -> if (on && r.id !in state.selectedRoutineIds) state.selectedRoutineIds.add(r.id); if (!on) state.selectedRoutineIds.remove(r.id) } })
                    }
                    DsSectionTitle(category.ifBlank { s.uncategorized }, Modifier.weight(1f))
                    Text("${members.count { it.enabled }}/${members.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            members.forEach { r ->
                item(key = r.id) {
                    RoutineRow(state, r, selectMode, Modifier.reorderItem(reorder, r.id), Modifier.reorderHandle(reorder, r.id))
                }
            }
        }
    }
    state.editingRoutine?.let { r -> RoutineDialog(state, r) }
    state.duplicateRoutineId?.let { id ->
        val r = state.data.routines.firstOrNull { it.id == id }
        if (r == null) state.duplicateRoutineId = null
        else DuplicateDialog(title = r.title, onDismiss = { state.duplicateRoutineId = null }) { n -> state.store.copyRoutine(id, n); state.store.regenerateToday(); state.duplicateRoutineId = null }
    }
    if (state.printDateDialog) PrintDateDialog(state)
}

@Composable
private fun SelectionBar(state: AppState, routines: List<Routine>) {
    val s = LocalStrings.current
    val n = state.selectedRoutineIds.size
    DsCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(Modifier.padding(8.dp)) {
            Text(s.selectedCount(n), style = MaterialTheme.typography.labelLarge)
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DsButton(onClick = { state.selectedRoutineIds.clear(); state.selectedRoutineIds.addAll(routines.map { it.id }) }, kind = ButtonKind.Subtle) { Text(s.selectAll) }
                DsButton(onClick = { state.selectedRoutineIds.clear() }, kind = ButtonKind.Subtle) { Text(s.selectNone) }
                DsButton(onClick = { state.setSelectedRoutinesEnabled(true) }, enabled = n > 0, kind = ButtonKind.Accent, modifier = Modifier.testTag("enableSelected")) { Text(s.enableSelected) }
                DsButton(onClick = { state.setSelectedRoutinesEnabled(false) }, enabled = n > 0, modifier = Modifier.testTag("disableSelected")) { Text(s.disableSelected) }
            }
        }
    }
}

@Composable
private fun RoutineRow(state: AppState, r: Routine, selectMode: Boolean, modifier: Modifier, handle: Modifier) {
    val s = LocalStrings.current
    val selected = r.id in state.selectedRoutineIds
    DsCard(modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectMode) {
                DsCheckbox(checked = selected, onCheckedChange = { state.toggleRoutineSelected(r.id) })
            } else {
                Icon(Icons.Default.DragHandle, s.dragToReorder, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = handle.padding(horizontal = 6.dp).size(28.dp).testTag("drag:${r.id}"))
            }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium)
                val days = DayOfWeek.entries.filter { it in r.weekdays }.joinToString(" ") { s.dayShort(it) }
                val meta = buildList {
                    add(days)
                    r.estimateMin?.let { add("~$it${s.minutes}") }
                    r.timeboxMin?.let { add("⏱ $it${s.minutes}") }
                    r.quotaTarget?.let { add("x$it") }
                }.joinToString(" · ")
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DsSwitch(checked = r.enabled, onCheckedChange = { state.store.setRoutinesEnabled(listOf(r.id), it) })
            Spacer(Modifier.width(4.dp))
            DsButton(onClick = { state.duplicateRoutineId = r.id }, kind = ButtonKind.Subtle) { Icon(Icons.Default.ContentCopy, s.duplicate, modifier = Modifier.size(16.dp)) }
            DsButton(onClick = { state.editingRoutine = r }, kind = ButtonKind.Subtle) { Text(s.rename) }
        }
    }
}

/** Pick a date (today .. +7 days) whose routines are regenerated and printed (FR-6.7). */
@Composable
private fun PrintDateDialog(state: AppState) {
    val s = LocalStrings.current
    val today = Dates.today()
    DsDialog(
        onDismissRequest = { state.printDateDialog = false },
        title = { Text(s.printDateTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(s.printDateHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                for (i in 0..7) {
                    val d = Dates.plusDays(today, i); val iso = Dates.iso(d)
                    val count = state.data.routines.count { it.enabled && d.dayOfWeek in it.weekdays }
                    val label = when (i) { 0 -> s.today; 1 -> s.tomorrow; else -> "" }
                    DsListItem(
                        modifier = Modifier.fillMaxWidth().clickable { state.printDateDialog = false; state.printRoutinesOn(iso) }.testTag("printDate:$i"),
                        headline = { Text(state.dateLabel(iso) + if (label.isNotEmpty()) "  ·  $label" else "") },
                        supporting = { Text("$count") },
                    )
                }
            }
        },
        confirmButton = { DsButton(onClick = { state.printDateDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(s.cancel) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineDialog(state: AppState, initial: Routine) {
    val s = LocalStrings.current
    var r by remember(initial.id) { mutableStateOf(initial) }
    val isNew = state.data.routines.none { it.id == initial.id }
    val categories = state.data.routines.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
    DsDialog(
        onDismissRequest = { state.editingRoutine = null },
        title = { Text(if (isNew) s.routineNew else r.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DsTextField(value = r.title, onValueChange = { r = r.copy(title = it) }, label = s.routineTitle, singleLine = true, modifier = Modifier.fillMaxWidth())
                DsTextField(value = r.category, onValueChange = { r = r.copy(category = it) }, label = s.routineCategory, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (categories.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { c -> DsChip(selected = r.category.trim() == c, onClick = { r = r.copy(category = c) }, label = c) }
                    }
                }
                Text(s.routineDays, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        DsChip(selected = d in r.weekdays, onClick = { r = r.copy(weekdays = if (d in r.weekdays) r.weekdays - d else r.weekdays + d) }, label = s.dayShort(d))
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField(s.estimate, r.estimateMin, { r = r.copy(estimateMin = it) }, suffix = s.minutes)
                    IntField(s.timebox, r.timeboxMin, { r = r.copy(timeboxMin = it) }, suffix = s.minutes)
                }
                IntField(s.routineQuota, r.quotaTarget, { r = r.copy(quotaTarget = it?.takeIf { v -> v > 0 }) })
                Text(s.routineQuotaHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LabeledRow(s.routineEnabled) { DsSwitch(checked = r.enabled, onCheckedChange = { r = r.copy(enabled = it) }) }
            }
        },
        confirmButton = {
            DsButton(enabled = r.title.isNotBlank() && r.weekdays.isNotEmpty(), kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth(), onClick = {
                state.store.upsertRoutine(r.copy(title = r.title.trim(), category = r.category.trim()))
                state.store.regenerateToday()
                state.editingRoutine = null
            }) { Text(s.save) }
        },
        dismissButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isNew) DsButton(onClick = { state.store.deleteRoutine(r.id); state.store.regenerateToday(); state.editingRoutine = null }, modifier = Modifier.weight(1f)) { Text(s.delete, color = MaterialTheme.colorScheme.error) }
                DsButton(onClick = { state.editingRoutine = null }, modifier = Modifier.weight(1f)) { Text(s.cancel) }
            }
        },
    )
}
