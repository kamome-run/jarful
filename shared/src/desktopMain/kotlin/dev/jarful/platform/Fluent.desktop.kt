package dev.jarful.platform

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle

private val cached: FontFamily? by lazy {
    listOf("Segoe UI Variable Text", "Segoe UI Variable", "Segoe UI", "Yu Gothic UI").firstNotNullOfOrNull { name ->
        runCatching {
            val tf = FontMgr.default.matchFamilyStyle(name, FontStyle.NORMAL) ?: return@runCatching null
            if (tf.familyName.equals(name, ignoreCase = true)) FontFamily(Typeface(tf)) else null
        }.getOrNull()
    }
}

actual fun fluentFontFamily(): FontFamily? = cached
