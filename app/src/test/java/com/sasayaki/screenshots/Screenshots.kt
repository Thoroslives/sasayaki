package com.sasayaki.screenshots

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.sasayaki.ui.theme.SasayakiTheme
import java.io.File

const val PHONE = "w411dp-h891dp-xxhdpi"

// TALL_PHONE fits a whole scrolling page in one capture.
const val TALL_PHONE = "w411dp-h1900dp-xxhdpi"

fun ComposeContentTestRule.screenshot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
    setContent { SasayakiTheme(darkTheme = dark) { content() } }
    waitForIdle()
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    val directory = File("build/screenshots").apply { mkdirs() }
    File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
