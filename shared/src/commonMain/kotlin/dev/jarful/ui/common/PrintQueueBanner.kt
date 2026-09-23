package dev.jarful.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.jarful.ui.AppState
import dev.jarful.ui.ds.ButtonKind
import dev.jarful.ui.ds.CardTone
import dev.jarful.ui.ds.DsButton
import dev.jarful.ui.ds.DsCard
import dev.jarful.ui.ds.DsProgress
import dev.jarful.ui.i18n.LocalStrings

/** Whether the banner has something to show: printing, retrying, or a paused queue. */
fun AppState.printBannerVisible(): Boolean = printProgress != null || printQueue.isNotEmpty()

/** Print progress while printing, or the paused queue with resume / discard actions (FR-9.12). Shown above every tab. */
@Composable
fun PrintQueueBanner(state: AppState, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    DsCard(modifier.fillMaxWidth().testTag("printQueueBanner"), tone = CardTone.Highlight) {
        Column(Modifier.padding(12.dp)) {
            val progress = state.printProgress
            if (progress != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.printProgress(progress.first, progress.second), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    DsButton(onClick = { state.stopPrinting() }) { Text(s.printStop) }
                }
                state.printRetry?.let { (n, max) -> Text(s.printRetrying(n, max), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp)) }
                DsProgress(progress = { if (progress.second == 0) 0f else progress.first.toFloat() / progress.second }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            } else {
                Text(s.printPaused(state.printQueue.size), style = MaterialTheme.typography.titleMedium)
                state.printLastError?.let { Text(s.printLastError(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 2.dp)) }
                Text(s.printPaperHint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DsButton(onClick = { state.resumePrintQueue() }, kind = ButtonKind.Accent, enabled = !state.printing, modifier = Modifier.testTag("printResume")) { Text(s.printResume) }
                    DsButton(onClick = { state.discardPrintQueue() }) { Text(s.printDiscard) }
                }
            }
        }
    }
}
