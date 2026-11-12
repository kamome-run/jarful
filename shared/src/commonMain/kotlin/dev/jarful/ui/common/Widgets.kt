package dev.jarful.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jarful.ui.ds.ButtonKind
import dev.jarful.ui.ds.DsButton
import dev.jarful.ui.ds.DsDialog
import dev.jarful.ui.ds.DsSectionTitle
import dev.jarful.ui.ds.DsTextField
import dev.jarful.ui.i18n.LocalStrings

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) = DsSectionTitle(text, modifier)

/** Small numeric text field that maps blank to null. */
@Composable
fun IntField(label: String, value: Int?, onChange: (Int?) -> Unit, modifier: Modifier = Modifier, suffix: String? = null) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    DsTextField(
        value = text,
        onValueChange = { t ->
            val clean = t.filter { it.isDigit() }.take(5)
            text = clean
            onChange(clean.toIntOrNull())
        },
        label = label, suffix = suffix, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = modifier.width(140.dp),
    )
}

@Composable
fun ConfirmDialog(title: String, body: String? = null, confirmText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    DsDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = body?.let { { Text(it) } },
        confirmButton = { DsButton(onClick = onConfirm, kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth()) { Text(confirmText) } },
        dismissButton = { DsButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(s.cancel) } },
    )
}

/** Multiline paste dialog used for import and "break down from lines". */
@Composable
fun TextAreaDialog(title: String, hint: String, confirmText: String, initial: String = "", onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    var text by rememberSaveable { mutableStateOf(initial) }
    DsDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                DsTextField(value = text, onValueChange = { text = it }, minLines = 5, maxLines = 12, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { DsButton(onClick = { onConfirm(text) }, kind = ButtonKind.Accent, modifier = Modifier.fillMaxWidth()) { Text(confirmText) } },
        dismissButton = { DsButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(s.cancel) } },
    )
}

@Composable
fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        content()
    }
}
