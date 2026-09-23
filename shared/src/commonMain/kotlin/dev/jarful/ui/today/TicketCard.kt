package dev.jarful.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.jarful.model.Ticket
import dev.jarful.ui.AppState
import dev.jarful.ui.PrintTarget
import dev.jarful.ui.ds.ButtonKind
import dev.jarful.ui.ds.CardTone
import dev.jarful.ui.ds.DsButton
import dev.jarful.ui.ds.DsCard
import dev.jarful.ui.ds.DsIconButton
import dev.jarful.ui.ds.DsMenu
import dev.jarful.ui.ds.DsMenuItem
import dev.jarful.ui.ds.DsProgress
import dev.jarful.ui.i18n.LocalStrings
import kotlinx.coroutines.coroutineScope
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
    val scope = rememberCoroutineScope()

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

    val tone = if (ticket.isDone) CardTone.Muted else CardTone.Default

    // Touch: swipe the card to the right (past 40% of its width) to complete it (NFR-CB touch).
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(1f) }
    val swipe = remember { Animatable(0f) }
    val swipeModifier = if (ticket.isDone) Modifier else Modifier.pointerInput(ticket.id) {
        detectHorizontalDragGestures(
            onDragEnd = {
                if (swipe.value > widthPx * 0.4f) { if (!crumpling) crumpling = true }
                scope.launch { swipe.animateTo(0f, tween(200)) }
            },
            onDragCancel = { scope.launch { swipe.animateTo(0f, tween(200)) } },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                scope.launch { swipe.snapTo((swipe.value + dragAmount).coerceIn(0f, widthPx)) }
            },
        )
    }

    DsCard(
        modifier = modifier.fillMaxWidth()
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .offset { IntOffset(swipe.value.roundToInt(), 0) }
            .then(swipeModifier)
            .scale(scale.value).rotate(rotation.value).alpha(alpha.value),
        tone = tone,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ticket.category.ifBlank { "—" }.uppercase(),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = LocalContentColor.current.copy(alpha = 0.75f), modifier = Modifier.weight(1f),
                )
                val meta = buildList {
                    ticket.estimateMin?.let { add("~$it${s.minutes}") }
                    ticket.timeboxMin?.let { add("⏱ $it${s.minutes}") }
                    if (ticket.printedAt != null) add("🖨")
                }.joinToString("  ")
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.75f))
                if (!ticket.isDone) {
                    Box {
                        DsIconButton(onClick = { menu = true }, icon = Icons.Default.MoreVert, contentDescription = null, size = 32.dp)
                        DsMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (showReorder) {
                                DsMenuItem(s.moveUp, icon = Icons.Default.KeyboardArrowUp, onClick = { menu = false; state.store.moveTicket(ticket.id, -1) })
                                DsMenuItem(s.moveDown, icon = Icons.Default.KeyboardArrowDown, onClick = { menu = false; state.store.moveTicket(ticket.id, +1) })
                            }
                            DsMenuItem(s.print, icon = Icons.Default.Print, onClick = { menu = false; state.print(PrintTarget.Tickets(listOf(ticket))) })
                            ticket.taskId?.let { tid -> DsMenuItem(s.breakDown, onClick = { menu = false; state.breakDownTaskId = tid }) }
                            DsMenuItem(s.delete, icon = Icons.Default.Close, danger = true, onClick = { menu = false; state.store.removeTicket(ticket.id) })
                        }
                    }
                }
            }
            Text(
                ticket.title,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (ticket.isDone) TextDecoration.LineThrough else null,
                modifier = Modifier.padding(top = 2.dp),
            )

            if (ticket.isQuota && !ticket.isDone) QuotaRow(state, ticket)

            Row(Modifier.padding(top = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ticket.isDone) {
                    DsButton(onClick = { state.undoComplete(ticket.id) }, kind = ButtonKind.Subtle) { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(s.undoDone) }
                } else {
                    DsButton(onClick = { if (!crumpling) crumpling = true }, enabled = !crumpling, kind = ButtonKind.Accent, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(s.done, maxLines = 1)
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
        DsButton(onClick = { state.store.setQuotaCount(ticket.id, ticket.quotaCount - 1) }) { Text("−1") }
        Spacer(Modifier.width(8.dp))
        Text(s.quotaProgress(ticket.quotaCount, target), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        DsButton(onClick = {
            val next = ticket.quotaCount + 1
            if (next >= target) state.feedback()
            state.store.setQuotaCount(ticket.id, next)
            if (next >= target) { state.lastCompletedTicketId = ticket.id; state.jarDropSignal++ }
        }) { Text("+1") }
    }
    DsProgress(progress = { ticket.quotaCount.toFloat() / target }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
}
