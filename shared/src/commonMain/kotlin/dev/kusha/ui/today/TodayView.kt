package dev.kusha.ui.today

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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kusha.ui.AppState
import dev.kusha.ui.PrintTarget
import dev.kusha.ui.i18n.LocalStrings
import dev.kusha.ui.jar.JarView

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
                    TextButton(onClick = { state.print(PrintTarget.Today) }, enabled = !state.printing && tickets.any { !it.isDone }) {
                        Icon(Icons.Default.Print, null); Spacer(Modifier.width(4.dp)); Text(s.printToday, maxLines = 1)
                    }
                }
            }
            if (old.isNotEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(s.carryOverTitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        old.forEach { k ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("${k.date}  ${k.title}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { state.store.carryOver(listOf(k.id)) }) { Text(s.carryOver) }
                                TextButton(onClick = { state.store.discardOld(listOf(k.id)) }) { Text(s.discard) }
                            }
                        }
                        Row {
                            TextButton(onClick = { state.store.carryOver(old.map { it.id }) }) { Text(s.carryOverAll) }
                            TextButton(onClick = { state.store.discardOld(old.map { it.id }) }) { Text(s.discardAll) }
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
        ExtendedFloatingActionButton(
            onClick = { state.refocusOpen = true },
            icon = { Icon(Icons.Default.Bolt, null) },
            text = { Text(s.refocus) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}
