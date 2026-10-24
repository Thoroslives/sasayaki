package com.sasayaki.screenshots

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sasayaki.service.bubble.BubbleActions
import com.sasayaki.service.bubble.BubbleContent
import com.sasayaki.service.bubble.BubblePhase
import com.sasayaki.service.bubble.BubbleUiState
import com.sasayaki.service.bubble.mode
import com.sasayaki.ui.theme.SasayakiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

// Filmstrips of bubble transitions, one frame per row, to check that nothing jumps.
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE, application = Application::class)
class BubbleMotionScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val actions = BubbleActions({}, {}, {}, {}, {}, {}, {})

    private fun filmstrip(name: String, from: BubbleUiState, to: BubbleUiState) {
        var state by mutableStateOf(from)
        var window by mutableStateOf(from.phase.mode)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SasayakiTheme(darkTheme = true) {
                Box(Modifier.background(Color(0xFFF0F0F0)).size(window.windowWidth, window.windowHeight)) {
                    BubbleContent(state, { List(18) { 0.3f } }, actions, {})
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        val frames = mutableListOf<Bitmap>()
        fun capture() = frames.add(compose.onRoot().captureToImage().asAndroidBitmap())
        capture()
        state = to
        if (to.phase.mode.windowWidth > window.windowWidth) window = to.phase.mode
        repeat(14) {
            compose.mainClock.advanceTimeBy(32)
            capture()
        }
        window = to.phase.mode
        repeat(3) {
            compose.mainClock.advanceTimeBy(32)
            capture()
        }
        val width = frames.maxOf { it.width }
        val strip = Bitmap.createBitmap(width, frames.sumOf { it.height }, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(strip)
        var y = 0f
        frames.forEach { canvas.drawBitmap(it, (width - it.width).toFloat(), y, null); y += it.height }
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { strip.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun cancel() = filmstrip(
        "motion_recording_to_idle",
        BubbleUiState(BubblePhase.Recording(false), elapsedSeconds = 4, profileName = "Chat"),
        BubbleUiState(BubblePhase.Idle)
    )

    @Test
    fun stop() = filmstrip(
        "motion_recording_to_processing",
        BubbleUiState(BubblePhase.Recording(false), elapsedSeconds = 4, profileName = "Chat"),
        BubbleUiState(BubblePhase.Processing(false))
    )
}
