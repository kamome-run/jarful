package dev.jarful.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.jarful.data.DATA_FILE
import dev.jarful.data.Store
import dev.jarful.model.Language
import dev.jarful.platform.AndroidPlatform
import dev.jarful.platform.FileStore
import dev.jarful.ui.JarfulApp
import dev.jarful.ui.i18n.stringsFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var store: Store
    private var pendingPermission: ((Boolean) -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        pendingPermission?.invoke(result.values.all { it })
        pendingPermission = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AndroidPlatform.init(this)
        AndroidPlatform.permissionRequester = { perms, cb -> pendingPermission = cb; permissionLauncher.launch(perms) }
        store = Store(FileStore(DATA_FILE), scope, stringsFor(Language.SYSTEM).inbox)
        store.load()
        setContent { JarfulApp(store) }
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) store.prepareRoutines()
    }

    override fun onPause() {
        super.onPause()
        if (::store.isInitialized) store.flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        AndroidPlatform.permissionRequester = null
        scope.cancel()
    }
}
