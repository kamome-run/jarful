package dev.kusha.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kusha.domain.Dates
import dev.kusha.domain.Stats
import dev.kusha.ui.AppState
import dev.kusha.ui.common.SectionTitle
import dev.kusha.ui.i18n.LocalStrings

/** Loops per day, streak, routine completion (FR-11). */
@Composable
fun StatsView(state: AppState, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val today = Dates.today()
    val perDay = Stats.loopsPerDay(state.data, today, 90)
    val streak = Stats.streak(state.data, today)
    val rates = Stats.routineRates(state.data, today, 30)
    val total = perDay.sumOf { it.second }
    val best = perDay.maxOfOrNull { it.second } ?: 0
    val bar = MaterialTheme.colorScheme.primary
    val barToday = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outline

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text(s.stats, style = MaterialTheme.typography.titleLarge)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(s.streak(streak), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Column { Text(s.totalLoops(total)); Text(s.bestDay(best)) }
            }
        }
        item {
            SectionTitle(s.last90)
            if (total == 0) Text(s.noData, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Canvas(Modifier.fillMaxWidth().height(140.dp).padding(top = 4.dp)) {
                val n = perDay.size
                val gap = 2f
                val bw = (size.width - gap * (n - 1)) / n
                val max = (best.coerceAtLeast(1)).toFloat()
                drawLine(grid, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
                perDay.forEachIndexed { i, (d, v) ->
                    val h = size.height * (v / max)
                    val x = i * (bw + gap)
                    drawRect(if (d == today) barToday else bar, Offset(x, size.height - h), Size(bw, h))
                }
            }
        }
        item { SectionTitle(s.routineRates) }
        if (rates.isEmpty()) item { Text(s.noData, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        rates.forEach { r ->
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row { Text(r.routine.title, Modifier.weight(1f)); Text("${r.done}/${r.scheduled}  ${(r.ratio * 100).toInt()}%") }
                    LinearProgressIndicator(progress = { r.ratio }, modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 2.dp))
                }
            }
        }
    }
}
