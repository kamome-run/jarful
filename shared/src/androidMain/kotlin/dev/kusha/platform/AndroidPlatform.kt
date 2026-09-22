package dev.kusha.platform

import android.content.Context

/** Holds the application context and the activity-provided permission requester. */
object AndroidPlatform {
    lateinit var appContext: Context
    /** Set by the Activity: asks for BLUETOOTH_CONNECT and reports the result. */
    var permissionRequester: ((permissions: Array<String>, onResult: (Boolean) -> Unit) -> Unit)? = null
    fun init(context: Context) { appContext = context.applicationContext }
}
