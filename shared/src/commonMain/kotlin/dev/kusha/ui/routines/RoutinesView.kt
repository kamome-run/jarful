package dev.kusha.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kusha.model.Routine
import dev.kusha.ui.AppState
import dev.kusha.ui.PrintTarget
import dev.kusha.ui.common.IntField
import dev.kusha.ui.common.LabeledRow
import dev.kusha.ui.i18n.LocalStrings
import kotlinx.datetime.DayOfWeek

/** Weekday routines (FR-6). */
@Composable
fun RoutinesView(state: AppState, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val routines = state.data.routines.sortedBy { it.order }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(s.routines, style = MaterialTheme.typography.titleLarge)
            Text(s.routineOrderHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { state.editingRoutine = state.newRoutine() }) { Icon(Icons.Default.Add, null); Text(s.routineNew) }
                OutlinedButton(onClick = { state.print(PrintTarget.Routines) }, enabled = !state.printing) { Icon(Icons.Default.Print, null); Text(s.routinePrintToday) }
                TextButton(onClick = { state.store.regenerateToday() }) { Text(s.routineRegenerate) }
            }
        }
        if (routines.isEmpty()) {
            item {
                Text(s.routineEmpty, modifier = Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { state.addSampleRoutines() }) { Text(s.sampleRoutinesAdd) }
            }
        }
        items(routines, key = { it.id }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val days = DayOfWeek.entries.filter { it in r.weekdays }.joinToString(" ") { s.dayShort(it) }
                        val meta = buildList {
                            if (r.category.isNotBlank()) add(r.category)
                            add(days)
                            r.estimateMin?.let { add("~$it${s.minutes}") }
                            r.timeboxMin?.let { add("⏱ $it${s.minutes}") }
                            r.quotaTarget?.let { add("x$it") }
                        }.joinToString(" · ")
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = r.enabled, onCheckedChange = { state.store.upsertRoutine(r.copy(enabled = it)) })
                    IconButton(onClick = { state.store.moveRoutine(r.id, -1) }) { Icon(Icons.Default.KeyboardArrowUp, s.moveUp) }
                    IconButton(onClick = { state.store.moveRoutine(r.id, +1) }) { Icon(Icons.Default.KeyboardArrowDown, s.moveDown) }
                    TextButton(onClick = { state.editingRoutine = r }) { Text(s.rename) }
                }
            }
        }
    }
    state.editingRoutine?.let { r -> RoutineDialog(state, r) }
}

@Composable
private fun RoutineDialog(state: AppState, initial: Routine) {
    val s = LocalStrings.current
    var r by remember(initial.id) { mutableStateOf(initial) }
    val isNew = state.data.routines.none { it.id == initial.id }
    AlertDialog(
        onDismissRequest = { state.editingRoutine = null },
        title = { Text(if (isNew) s.routineNew else r.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = r.title, onValueChange = { r = r.copy(title = it) }, label = { Text(s.routineTitle) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = r.category, onValueChange = { r = r.copy(category = it) }, label = { Text(s.routineCategory) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(s.routineDays, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        FilterChip(selected = d in r.weekdays, onClick = { r = r.copy(weekdays = if (d in r.weekdays) r.weekdays - d else r.weekdays + d) }, label = { Text(s.dayShort(d)) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntField(s.estimate, r.estimateMin, { r = r.copy(estimateMin = it) }, suffix = s.minutes)
                    IntField(s.timebox, r.timeboxMin, { r = r.copy(timeboxMin = it) }, suffix = s.minutes)
                }
                IntField(s.routineQuota, r.quotaTarget, { r = r.copy(quotaTarget = it?.takeIf { v -> v > 0 }) })
                Text(s.routineQuotaHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LabeledRow(s.routineEnabled) { Switch(checked = r.enabled, onCheckedChange = { r = r.copy(enabled = it) }) }
            }
        },
        confirmButton = {
            TextButton(enabled = r.title.isNotBlank() && r.weekdays.isNotEmpty(), onClick = {
                state.store.upsertRoutine(r.copy(title = r.title.trim(), category = r.category.trim()))
                state.store.regenerateToday()
                state.editingRoutine = null
            }) { Text(s.save) }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = { state.store.deleteRoutine(r.id); state.store.regenerateToday(); state.editingRoutine = null }) { Text(s.delete, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { state.editingRoutine = null }) { Text(s.cancel) }
            }
        },
    )
}
