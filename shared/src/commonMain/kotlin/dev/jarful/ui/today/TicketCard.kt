package dev.jarful.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.jarful.model.Ticket
import dev.jarful.model.TicketState
import dev.jarful.platform.nowMillis
import dev.jarful.ui.AppState
import dev.jarful.ui.PrintTarget
import dev.jarful.ui.i18n.LocalStrings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One receipt-like card (§7.3). Completing it plays the crumple animation (FR-4.1) then drops into the jar. */
@Composable
fun TicketCard(state: AppState, ticket: Ticket, modifier: Modifier = Modifier, showReorder: Boolean = true) {
    val s = LocalStrings.current
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    var crumpling by remember(ticket.id) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val isRunning = ticket.state == TicketState.RUNNING

    LaunchedEffect(crumpling) {
        if (crumpling) {
            state.feedback()
            // crumple: squash, twist, fade, all within ~600ms (FR-4.1)
            coroutineScope {
                launch { scale.animateTo(0.15f, tween(520, easing = FastOutSlowInEasing)) }
                launch { rotation.animateTo(38f, tween(520, easing = FastOutSlowInEasing)) }
                launch { alpha.animateTo(0.2f, tween(520)) }
            }
            state.completeTicket(ticket.id)
            crumpling = false
            scale.snapTo(1f); rotation.snapTo(0f); alpha.snapTo(1f)
        }
    }

    val colors = when {
        ticket.isDone -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        isRunning -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
        else -> CardDefaults.elevatedCardColors()
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth().scale(scale.value).rotate(rotation.value).alpha(alpha.value),
        colors = colors,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (ticket.isDone) 0.dp else 1.dp),
    ) {
      Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ticket.category.ifBlank { "—" }.uppercase(),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
            val meta = buildList {
                ticket.estimateMin?.let { add("~$it${s.minutes}") }
                ticket.timeboxMin?.let { add("⏱ $it${s.minutes}") }
                if (ticket.printedAt != null) add("🖨")
            }.joinToString("  ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!ticket.isDone) {
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (showReorder) {
                            DropdownMenuItem(text = { Text(s.moveUp) }, leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) }, onClick = { menu = false; state.store.moveTicket(ticket.id, -1) })
                            DropdownMenuItem(text = { Text(s.moveDown) }, leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) }, onClick = { menu = false; state.store.moveTicket(ticket.id, +1) })
                        }
                        DropdownMenuItem(text = { Text(s.print) }, leadingIcon = { Icon(Icons.Default.Print, null) }, onClick = { menu = false; state.print(PrintTarget.Tickets(listOf(ticket))) })
                        ticket.taskId?.let { tid -> DropdownMenuItem(text = { Text(s.breakDown) }, onClick = { menu = false; state.breakDownTaskId = tid }) }
                        DropdownMenuItem(text = { Text(s.delete) }, leadingIcon = { Icon(Icons.Default.Close, null) }, onClick = { menu = false; state.store.removeTicket(ticket.id) })
                    }
                }
            }
        }
        Text(
            ticket.title,
            style = MaterialTheme.typography.titleMedium,
            textDecoration = if (ticket.isDone) TextDecoration.LineThrough else null,
            color = when {
                ticket.isDone -> MaterialTheme.colorScheme.onSurfaceVariant
                isRunning -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 2.dp),
        )

        if (ticket.isQuota && !ticket.isDone) {
            QuotaRow(state, ticket)
        }
        if (isRunning) {
            TimerRow(state, ticket)
        }

        Row(Modifier.padding(top = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ticket.isDone) {
                TextButton(onClick = { state.undoComplete(ticket.id) }) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text(s.undoDone) }
            } else {
                if (isRunning) {
                    OutlinedButton(onClick = { state.store.stopTicket(ticket.id) }) { Text(s.stop) }
                } else {
                    OutlinedButton(onClick = { state.store.startTicket(ticket.id) }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(2.dp)); Text(s.start) }
                }
                Button(onClick = { if (!crumpling) crumpling = true }, enabled = !crumpling, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Check, null); Spacer(Modifier.width(4.dp)); Text(s.done, maxLines = 1)
                }
            }
        }
      }
    }
}

@Composable
private fun QuotaRow(state: AppState, ticket: Ticket) {
    val s = LocalStrings.current
    val target = ticket.quotaTarget ?: return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        OutlinedButton(onClick = { state.store.setQuotaCount(ticket.id, ticket.quotaCount - 1) }) { Text("−1") }
        Spacer(Modifier.width(8.dp))
        Text(s.quotaProgress(ticket.quotaCount, target), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        OutlinedButton(onClick = {
            val next = ticket.quotaCount + 1
            if (next >= target) state.feedback()
            state.store.setQuotaCount(ticket.id, next)
            if (next >= target) { state.lastCompletedTicketId = ticket.id; state.jarDropSignal++ }
        }) { Text("+1") }
    }
    LinearProgressIndicator(progress = { ticket.quotaCount.toFloat() / target }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp))
}

/** Elapsed / countdown timer for a running ticket (FR-8). */
@Composable
private fun TimerRow(state: AppState, ticket: Ticket) {
    val s = LocalStrings.current
    var now by remember { mutableLongStateOf(nowMillis()) }
    var notified by remember(ticket.id, ticket.timeboxMin, ticket.startedAt) { mutableStateOf(false) }
    LaunchedEffect(ticket.id) { while (true) { now = nowMillis(); delay(1000) } }
    val started = ticket.startedAt ?: now
    val elapsedSec = ((now - started) / 1000).coerceAtLeast(0)
    val box = ticket.timeboxMin
    val remainingSec = box?.let { it * 60L - elapsedSec }
    val timeUp = remainingSec != null && remainingSec <= 0
    if (timeUp && !notified) { notified = true; state.timeUp() }

    Column(Modifier.padding(top = 6.dp)) {
        if (box != null) {
            val ratio = (elapsedSec.toFloat() / (box * 60f)).coerceIn(0f, 1f)
            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (timeUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    if (timeUp) s.timeUp else "${s.remaining} ${fmt(remainingSec!!)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (timeUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (timeUp) {
                    TextButton(onClick = { state.store.extendTimebox(ticket.id, 5) }) { Text(s.extend5) }
                    ticket.taskId?.let { tid -> TextButton(onClick = { state.breakDownTaskId = tid }) { Text(s.breakDown) } }
                }
            }
        } else {
            Text("${s.elapsed} ${fmt(elapsedSec)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun fmt(sec: Long): String {
    val v = kotlin.math.abs(sec)
    val m = v / 60; val s = v % 60
    return (if (sec < 0) "-" else "") + m.toString() + ":" + s.toString().padStart(2, '0')
}

@Suppress("unused")
private val Transparent = Color.Transparent
