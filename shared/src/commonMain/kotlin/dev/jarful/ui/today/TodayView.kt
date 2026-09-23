package dev.jarful.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import dev.jarful.ui.ds.ButtonKind
import dev.jarful.ui.ds.CardTone
import dev.jarful.ui.ds.DsButton
import dev.jarful.ui.ds.DsCard
import dev.jarful.ui.ds.DsProgress
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.jarful.ui.AppState
import dev.jarful.ui.PrintTarget
import dev.jarful.ui.i18n.LocalStrings
import dev.jarful.ui.jar.JarView

/** Print progress while printing, or the paused queue with resume / discard actions (FR-9.12). */
@Composable
private fun PrintQueueBanner(state: AppState) {
    val s = LocalStrings.current
    DsCard(Modifier.fillMaxWidth(), tone = CardTone.Highlight) {
        Column(Modifier.padding(12.dp)) {
            val progress = state.printProgress
            if (progress != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.printProgress(progress.first, progress.second), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    DsButton(onClick = { state.stopPrinting() }) { Text(s.printStop) }
                }
                DsProgress(progress = { if (progress.second == 0) 0f else progress.first.toFloat() / progress.second }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            } else {
                Text(s.printPaused(state.printQueue.size), style = MaterialTheme.typography.titleMedium)
                Text(s.printPaperHint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DsButton(onClick = { state.resumePrintQueue() }, kind = ButtonKind.Accent, enabled = !state.printing) { Text(s.printResume) }
                    DsButton(onClick = { state.discardPrintQueue() }) { Text(s.printDiscard) }
                }
            }
        }
    }
}

/** Today's tickets (FR-3) with carry-over (FR-3.4). [showJar] embeds a compact jar on phones (FR-5.4). */
@Composable
fun TodayView(state: AppState, showJar: Boolean, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val tickets = state.todayTickets()
    val old = state.oldOpenTickets()
    Box(modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showJar) {
                item { JarView(state.loopsToday(), state.jarDropSignal, Modifier.fillMaxWidth(), height = 170.dp) }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.todayTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    AnimatedVisibility(state.combo >= 2) {
                        Text(s.combo(state.combo), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.width(8.dp))
                    DsButton(onClick = { state.print(PrintTarget.Today) }, enabled = !state.printing && tickets.any { !it.isDone }, kind = ButtonKind.Subtle) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.width(18.dp)); Spacer(Modifier.width(4.dp)); Text(s.printToday, maxLines = 1)
                    }
                }
            }
            if (state.printProgress != null || state.printQueue.isNotEmpty()) {
                item { PrintQueueBanner(state) }
            }
            if (old.isNotEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(s.carryOverTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        old.forEach { k ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("${k.date}  ${k.title}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                DsButton(onClick = { state.store.carryOver(listOf(k.id)) }, kind = ButtonKind.Subtle) { Text(s.carryOver) }
                                DsButton(onClick = { state.store.discardOld(listOf(k.id)) }, kind = ButtonKind.Subtle) { Text(s.discard) }
                            }
                        }
                        Row {
                            DsButton(onClick = { state.store.carryOver(old.map { it.id }) }, kind = ButtonKind.Subtle) { Text(s.carryOverAll) }
                            DsButton(onClick = { state.store.discardOld(old.map { it.id }) }, kind = ButtonKind.Subtle) { Text(s.discardAll) }
                        }
                    }
                }
            }
            if (tickets.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.todayEmpty, style = MaterialTheme.typography.titleMedium)
                        Text(s.todayEmptyHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            items(tickets, key = { it.id }) { k -> TicketCard(state, k) }
        }
    }
}
