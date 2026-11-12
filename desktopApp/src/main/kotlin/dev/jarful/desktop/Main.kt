package dev.jarful.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.jarful.data.DATA_FILE
import dev.jarful.data.Store
import dev.jarful.model.Language
import dev.jarful.platform.FileStore
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.i18n.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val store = Store(FileStore(DATA_FILE), scope, stringsFor(Language.SYSTEM).inbox)
    store.load()
    Runtime.getRuntime().addShutdownHook(Thread { store.flush() })
    application {
        val windowState = rememberWindowState(size = DpSize(1280.dp, 800.dp), placement = WindowPlacement.Floating)
        Window(
            onCloseRequest = { store.flush(); exitApplication() },
            title = "Jarful",
            state = windowState,
        ) {
            JarfulApp(store)
        }
    }
}
