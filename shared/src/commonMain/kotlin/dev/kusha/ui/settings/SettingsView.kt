package dev.kusha.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kusha.model.Language
import dev.kusha.model.PaperWidth
import dev.kusha.model.PrintProtocol
import dev.kusha.model.PrinterCharset
import dev.kusha.model.PrinterSettings
import dev.kusha.model.PrinterTransport
import dev.kusha.platform.PrinterEndpoint
import dev.kusha.platform.bluetoothSupported
import dev.kusha.platform.copyToClipboard
import dev.kusha.platform.listBluetoothDevices
import dev.kusha.platform.listSerialPorts
import dev.kusha.platform.serialSupported
import dev.kusha.ui.AppState
import dev.kusha.ui.common.IntField
import dev.kusha.ui.common.LabeledRow
import dev.kusha.ui.common.SectionTitle
import dev.kusha.ui.i18n.LocalStrings
import kotlinx.coroutines.launch

/** Settings (FR-13) including the printer configuration (FR-9). */
@Composable
fun SettingsView(state: AppState, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val settings = state.data.settings
    val p = settings.printer
    fun update(f: (PrinterSettings) -> PrinterSettings) = state.store.updateSettings { it.copy(printer = f(it.printer)) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
        item {
            Text(s.settings, style = MaterialTheme.typography.titleLarge)
            LabeledRow(s.sound) { Switch(checked = settings.soundEnabled, onCheckedChange = { v -> state.store.updateSettings { it.copy(soundEnabled = v) } }) }
            LabeledRow(s.haptics) { Switch(checked = settings.hapticsEnabled, onCheckedChange = { v -> state.store.updateSettings { it.copy(hapticsEnabled = v) } }) }
            LabeledRow(s.showCompleted) { Switch(checked = settings.showCompleted, onCheckedChange = { v -> state.store.updateSettings { it.copy(showCompleted = v) } }) }
            SectionTitle(s.prepareTime)
            Text(s.prepareTimeHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                IntField("HH", settings.prepareHour, { v -> if (v != null && v in 0..23) state.store.updateSettings { it.copy(prepareHour = v) } })
                Text(":")
                IntField("MM", settings.prepareMinute, { v -> if (v != null && v in 0..59) state.store.updateSettings { it.copy(prepareMinute = v) } })
            }
            SectionTitle(s.language)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Language.SYSTEM to s.langSystem, Language.JA to s.langJa, Language.EN to s.langEn).forEach { (l, label) ->
                    FilterChip(selected = settings.language == l, onClick = { state.store.updateSettings { it.copy(language = l) } }, label = { Text(label) })
                }
            }
        }

        item {
            SectionTitle(s.printer)
            PrinterSection(state, p, ::update)
        }

        item {
            SectionTitle(s.dataTitle)
            Text("${s.dataPath}: ${state.store.let { runCatching { dev.kusha.platform.FileStore(dev.kusha.data.DATA_FILE).path() }.getOrDefault("-") }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(onClick = { copyToClipboard(state.store.exportJson()); state.showToast(s.copied) }) { Text(s.exportJson) }
                OutlinedButton(onClick = { state.importOpen = true }) { Text(s.importJson) }
            }
            SectionTitle(s.shortcuts)
            TextButton(onClick = { state.showShortcuts = true }) { Text(s.shortcuts) }
            SectionTitle(s.about)
            Text(s.aboutBody, style = MaterialTheme.typography.bodyMedium)
            Text("https://github.com/kamome-run/kusha", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(24.dp))
        }
    }
}

@Composable
private fun PrinterSection(state: AppState, p: PrinterSettings, update: ((PrinterSettings) -> PrinterSettings) -> Unit) {
    val s = LocalStrings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val transports = buildList {
        if (bluetoothSupported()) add(PrinterTransport.BLUETOOTH)
        if (serialSupported()) add(PrinterTransport.SERIAL)
        add(PrinterTransport.TCP)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            transports.forEach { t ->
                FilterChip(selected = p.transport == t, onClick = { update { it.copy(transport = t) } }, label = { Text(t.label) })
            }
        }
        when (p.transport) {
            PrinterTransport.TCP -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = p.host, onValueChange = { v -> update { it.copy(host = v) } }, label = { Text(s.printerHost) }, singleLine = true, modifier = Modifier.weight(1f))
                IntField(s.printerPort, p.port, { v -> if (v != null) update { it.copy(port = v) } })
            }
            PrinterTransport.BLUETOOTH -> EndpointPicker(
                selectedId = p.bluetoothAddress, selectedName = p.bluetoothName,
                load = { listBluetoothDevices() },
                onSelect = { e -> update { it.copy(bluetoothAddress = e.id, bluetoothName = e.name) } },
            )
            PrinterTransport.SERIAL -> EndpointPicker(
                selectedId = p.serialPort, selectedName = p.serialPort,
                load = { listSerialPorts() },
                onSelect = { e -> update { it.copy(serialPort = e.id) } },
            )
        }
        EnumDropdown("Protocol", PrintProtocol.entries, p.protocol, { it.label }) { v -> update { it.copy(protocol = v) } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnumDropdown(s.paperWidth, PaperWidth.entries, p.paperWidth, { it.label }, Modifier.weight(1f)) { v -> update { it.copy(paperWidth = v) } }
            if (p.protocol == PrintProtocol.ESCPOS_TEXT) {
                EnumDropdown(s.charset, PrinterCharset.entries, p.charset, { it.label }, Modifier.weight(1f)) { v -> update { it.copy(charset = v) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IntField("Feed", p.feedLines, { v -> if (v != null) update { it.copy(feedLines = v.coerceIn(0, 20)) } })
            Text("Cut (GS V)"); Switch(checked = p.cutEnabled, onCheckedChange = { v -> update { it.copy(cutEnabled = v) } })
        }
        if (p.protocol == PrintProtocol.TSPL || p.protocol == PrintProtocol.CPCL) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Label H", p.labelHeightMm, { v -> if (v != null) update { it.copy(labelHeightMm = v) } }, suffix = "mm")
                IntField("Gap", p.labelGapMm, { v -> if (v != null) update { it.copy(labelGapMm = v) } }, suffix = "mm")
            }
        }
        Button(onClick = { scope.launch { state.testPrint() } }, enabled = !state.printing) { Text(s.testPrint) }
    }
}

@Composable
private fun EndpointPicker(selectedId: String, selectedName: String, load: suspend () -> List<PrinterEndpoint>, onSelect: (PrinterEndpoint) -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var list by remember { mutableStateOf<List<PrinterEndpoint>>(emptyList()) }
    var open by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { loading = true; list = runCatching { load() }.getOrDefault(emptyList()); loading = false }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedName.ifBlank { selectedId.ifBlank { if (loading) "…" else "—" } })
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                if (list.isEmpty()) DropdownMenuItem(text = { Text("—") }, onClick = { open = false })
                list.forEach { e -> DropdownMenuItem(text = { Text(e.name) }, onClick = { onSelect(e); open = false }) }
            }
        }
        OutlinedButton(onClick = { scope.launch { loading = true; list = runCatching { load() }.getOrDefault(emptyList()); loading = false } }) {
            androidx.compose.material3.Icon(Icons.Default.Refresh, null)
        }
    }
}

@Composable
private fun <T> EnumDropdown(label: String, values: List<T>, selected: T, labelOf: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${labelOf(selected)}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            values.forEach { v -> DropdownMenuItem(text = { Text(labelOf(v)) }, onClick = { onSelect(v); open = false }) }
        }
    }
}

@Suppress("unused")
private val spacerWidth = 8.dp
