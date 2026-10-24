package com.sasayaki.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build

/**
 * Copies [text] and reports whether the caller should confirm it. Android 13+ shows its
 * own clipboard confirmation, so a second one would be noise.
 */
fun Context.copyText(text: String): Boolean {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("dictation", text))
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
}
