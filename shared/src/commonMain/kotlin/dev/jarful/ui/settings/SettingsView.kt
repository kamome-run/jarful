package dev.jarful.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
import dev.jarful.model.Language
import dev.jarful.model.PaperWidth
import dev.jarful.model.PrintProtocol
import dev.jarful.model.PrinterCharset
import dev.jarful.model.PrinterSettings
import dev.jarful.model.PrinterTransport
import dev.jarful.platform.PrinterEndpoint
import dev.jarful.platform.bleSupported
import dev.jarful.platform.bluetoothSupported
import dev.jarful.platform.listBleDevices
import dev.jarful.platform.copyToClipboard
import dev.jarful.platform.hasHardwareKeyboard
import dev.jarful.platform.listBluetoothDevices
import dev.jarful.platform.listSerialPorts
import dev.jarful.platform.localIpAddresses
import dev.jarful.platform.serialSupported
import dev.jarful.ui.AppState
import dev.jarful.ui.common.IntField
import dev.jarful.ui.ds.ButtonKind
import dev.jarful.ui.ds.DsButton
import dev.jarful.ui.ds.DsDialog
import dev.jarful.ui.ds.DsIconButton
import dev.jarful.ui.ds.DsMenu
import dev.jarful.ui.ds.DsMenuItem
import dev.jarful.ui.ds.DsSegmented
import dev.jarful.ui.ds.DsSwitch
import dev.jarful.ui.ds.DsTextField
import dev.jarful.ui.common.LabeledRow
import dev.jarful.ui.common.SectionTitle
import dev.jarful.ui.i18n.LocalStrings
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
            LabeledRow(s.sound) { DsSwitch(checked = settings.soundEnabled, onCheckedChange = { v -> state.store.updateSettings { it.copy(soundEnabled = v) } }) }
            LabeledRow(s.haptics) { DsSwitch(checked = settings.hapticsEnabled, onCheckedChange = { v -> state.store.updateSettings { it.copy(hapticsEnabled = v) } }) }
            LabeledRow(s.showCompleted) { DsSwitch(checked = settings.showCompleted, onCheckedChange = { v -> state.store.updateSettings { it.copy(showCompleted = v) } }) }
            SectionTitle(s.prepareTime)
            Text(s.prepareTimeHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                IntField("HH", settings.prepareHour, { v -> if (v != null && v in 0..23) state.store.updateSettings { it.copy(prepareHour = v) } })
                Text(":")
                IntField("MM", settings.prepareMinute, { v -> if (v != null && v in 0..59) state.store.updateSettings { it.copy(prepareMinute = v) } })
            }
            SectionTitle(s.language)
            EnumDropdown(s.language, Language.entries, settings.language, { if (it == Language.SYSTEM) s.langSystem else it.nativeName }) { v -> state.store.updateSettings { it.copy(language = v) } }
        }

        item {
            SectionTitle(s.printer)
            PrinterSection(state, p, ::update)
        }

        item {
            SectionTitle(s.sync)
            SyncSection(state)
        }

        item {
            SectionTitle(s.dataTitle)
            Text("${s.dataPath}: ${state.store.let { runCatching { dev.jarful.platform.FileStore(dev.jarful.data.DATA_FILE).path() }.getOrDefault("-") }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                DsButton(onClick = { copyToClipboard(state.store.exportJson()); state.showToast(s.copied) }) { Text(s.exportJson) }
                DsButton(onClick = { state.importOpen = true }) { Text(s.importJson) }
            }
            if (hasHardwareKeyboard()) {
                SectionTitle(s.shortcuts)
                DsButton(onClick = { state.showShortcuts = true }, kind = ButtonKind.Subtle) { Text(s.shortcuts) }
            }
            SectionTitle(s.about)
            Text("Jarful " + dev.jarful.AppVersion.NAME, style = MaterialTheme.typography.titleMedium)
            Text(s.aboutBody, style = MaterialTheme.typography.bodyMedium)
            Text(dev.jarful.platform.bluetoothCapabilityNote(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("https://github.com/kamome-run/jarful", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
        if (bleSupported()) add(PrinterTransport.BLUETOOTH_LE)
        if (serialSupported()) add(PrinterTransport.SERIAL)
        add(PrinterTransport.TCP)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DsSegmented(options = transports.map { it.label }, selected = transports.indexOf(p.transport).coerceAtLeast(0), onSelect = { i -> update { it.copy(transport = transports[i]) } }, modifier = Modifier.fillMaxWidth())
        when (p.transport) {
            PrinterTransport.TCP -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DsTextField(value = p.host, onValueChange = { v -> update { it.copy(host = v) } }, label = s.printerHost, singleLine = true, modifier = Modifier.weight(1f))
                IntField(s.printerPort, p.port, { v -> if (v != null) update { it.copy(port = v) } })
            }
            PrinterTransport.BLUETOOTH -> EndpointPicker(
                selectedId = p.bluetoothAddress, selectedName = p.bluetoothName,
                load = { listBluetoothDevices() },
                onSelect = { e -> update { it.copy(bluetoothAddress = e.id, bluetoothName = e.name) } },
            )
            PrinterTransport.BLUETOOTH_LE -> EndpointPicker(
                selectedId = p.bluetoothAddress, selectedName = p.bluetoothName,
                load = { listBleDevices() },
                onSelect = { e -> update { it.copy(bluetoothAddress = e.id, bluetoothName = e.name) } },
            )
            PrinterTransport.SERIAL -> EndpointPicker(
                selectedId = p.serialPort, selectedName = p.serialPort,
                load = { listSerialPorts() },
                onSelect = { e -> update { it.copy(serialPort = e.id) } },
            )
        }
        if (p.transport == PrinterTransport.BLUETOOTH || p.transport == PrinterTransport.BLUETOOTH_LE) {
            // Manual address entry for devices the OS does not list (e.g. Windows without pairing).
            DsTextField(
                value = p.bluetoothAddress,
                onValueChange = { v ->
                    val clean = v.uppercase().filter { c -> c.isLetterOrDigit() || c == ':' }.take(17)
                    update { it.copy(bluetoothAddress = clean, bluetoothName = if (clean == it.bluetoothAddress) it.bluetoothName else "") }
                },
                label = "Bluetooth MAC", placeholder = "00:11:22:33:44:55", singleLine = true, modifier = Modifier.fillMaxWidth(),
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
            Text("Cut (GS V)"); DsSwitch(checked = p.cutEnabled, onCheckedChange = { v -> update { it.copy(cutEnabled = v) } })
        }
        if (p.protocol == PrintProtocol.TSPL || p.protocol == PrintProtocol.CPCL) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IntField("Label H", p.labelHeightMm, { v -> if (v != null) update { it.copy(labelHeightMm = v) } }, suffix = "mm")
                IntField("Gap", p.labelGapMm, { v -> if (v != null) update { it.copy(labelGapMm = v) } }, suffix = "mm")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DsButton(onClick = { scope.launch { state.testPrint() } }, enabled = !state.printing, kind = ButtonKind.Accent) { Text(s.testPrint) }
            if (p.transport == PrinterTransport.BLUETOOTH || p.transport == PrinterTransport.BLUETOOTH_LE) {
                DsButton(onClick = { state.diagnosePrinter() }, enabled = !state.diagnosing && p.bluetoothAddress.isNotBlank()) { Text(if (state.diagnosing) "…" else s.printerDiagnose) }
            }
        }
    }
    state.diagnosis?.let { report ->
        DsDialog(
            onDismissRequest = { state.diagnosis = null },
            title = { Text(s.printerDiagnoseTitle) },
            text = {
                Column {
                    Text(s.printerDiagnoseHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    Text(report, style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()))
                }
            },
            confirmButton = { DsButton(onClick = { copyToClipboard(report); state.showToast(s.copied) }, kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth()) { Text(s.copy) } },
            dismissButton = { DsButton(onClick = { state.diagnosis = null }, modifier = Modifier.fillMaxWidth()) { Text(s.close) } },
        )
    }
}

@Composable
private fun SyncSection(state: AppState) {
    val s = LocalStrings.current
    val sy = state.data.settings.sync
    fun update(f: (dev.jarful.model.SyncSettings) -> dev.jarful.model.SyncSettings) = state.store.updateSettings { it.copy(sync = f(it.sync)) }
    val addresses = remember(sy.hostEnabled) { localIpAddresses() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(s.syncIntro, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LabeledRow(s.syncHost) { DsSwitch(checked = sy.hostEnabled, onCheckedChange = { v -> update { it.copy(hostEnabled = v) } }) }
        Text(s.syncHostHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (sy.hostEnabled) {
            val addr = if (addresses.isEmpty()) s.syncHostNoAddress else addresses.joinToString("  ")
            Text("${s.syncHostAddresses}: $addr", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${s.syncPort}: ${sy.port}")
                Text("${s.syncPin}: ${sy.pin}", style = MaterialTheme.typography.titleMedium)
                DsButton(onClick = { update { it.copy(pin = (100000 + kotlin.random.Random.nextInt(900000)).toString()) } }, kind = ButtonKind.Subtle) { Text(s.syncRegeneratePin) }
            }
            state.syncServerError?.let { Text(s.syncHostError(it), color = MaterialTheme.colorScheme.error) }
                ?: if (state.syncHostRunning) Text("● " + s.syncHostRunning, color = MaterialTheme.colorScheme.secondary) else Unit
        }
        Text(s.syncPeer, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DsTextField(value = sy.peerHost, onValueChange = { v -> update { it.copy(peerHost = v.trim()) } }, label = s.syncPeerHost, singleLine = true, modifier = Modifier.weight(1f))
            IntField(s.syncPort, sy.peerPort, { v -> if (v != null) update { it.copy(peerPort = v) } })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DsTextField(value = sy.peerPin, onValueChange = { v -> update { it.copy(peerPin = v.filter { c -> c.isDigit() }.take(6)) } }, label = s.syncPeerPin, singleLine = true, modifier = Modifier.width(140.dp))
            DsButton(onClick = { state.syncNow() }, enabled = !state.syncing && sy.peerHost.isNotBlank(), kind = ButtonKind.Accent) { Text(s.syncNow) }
        }
        LabeledRow(s.syncAuto) { DsSwitch(checked = sy.autoSync, onCheckedChange = { v -> update { it.copy(autoSync = v) } }) }
        val last = sy.lastSyncAt?.let { dev.jarful.domain.Dates.toLocalDateTime(it).toString().replace('T', ' ').take(16) } ?: s.syncNever
        val result = sy.lastSyncResult?.let { if (it == "OK") "" else "  (${s.syncError(it)})" } ?: ""
        Text("${s.syncLast}: $last$result", style = MaterialTheme.typography.bodySmall, color = if (result.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
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
            DsButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedName.ifBlank { selectedId.ifBlank { if (loading) "…" else "—" } })
            }
            DsMenu(expanded = open, onDismissRequest = { open = false }) {
                if (list.isEmpty()) DsMenuItem("—", onClick = { open = false })
                list.forEach { e -> DsMenuItem(e.name, onClick = { onSelect(e); open = false }) }
            }
        }
        DsIconButton(onClick = { scope.launch { loading = true; list = runCatching { load() }.getOrDefault(emptyList()); loading = false } }, icon = Icons.Default.Refresh, contentDescription = null)
    }
}

@Composable
private fun <T> EnumDropdown(label: String, values: List<T>, selected: T, labelOf: (T) -> String, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box(modifier) {
        DsButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${labelOf(selected)}") }
        DsMenu(expanded = open, onDismissRequest = { open = false }) {
            values.forEach { v -> DsMenuItem(labelOf(v), onClick = { onSelect(v); open = false }) }
        }
    }
}

@Suppress("unused")
private val spacerWidth = 8.dp
