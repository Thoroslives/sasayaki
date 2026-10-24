package com.sasayaki.screenshots

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sasayaki.domain.model.PostProcessingPrompt
import com.sasayaki.domain.model.Profile
import com.sasayaki.domain.model.RewriteMode
import com.sasayaki.domain.model.TextReplacementRule
import com.sasayaki.domain.provider.Provider
import com.sasayaki.ui.profiles.ProfileEditorNavigation
import com.sasayaki.ui.profiles.ProfileEditorScreen
import com.sasayaki.ui.profiles.ProfileEditorUiState
import com.sasayaki.ui.profiles.ProfilesScreen
import com.sasayaki.ui.profiles.ProfilesUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE, application = Application::class)
class ProfilesScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val providers = listOf(
        Provider(1, "Home server", "https://asr.home.lan", speechModel = "deepdml/faster-whisper-large-v3-turbo-ct2"),
        Provider(2, "llama-swap", "https://llm.home.lan", cleanupModel = "gpt-oss-120b")
    )

    private val chat = Profile(id = 1, name = "Chat", isActive = true, language = "en", llmEnabled = true, selectedRuleIds = setOf(1))
    private val email =
        Profile(
            id = 2,
            name = "Email",
            language = "es",
            llmEnabled = true,
            rewriteMode = RewriteMode.POLISH,
            speechProviderId = 1,
            asrModel = "Systran/faster-whisper-small"
        )
    private val notes = Profile(id = 3, name = "Raw notes")

    private val navigation = ProfileEditorNavigation({}, {}, {}, {}, {})

    private val editorState = ProfileEditorUiState(
        providers = providers,
        rules = listOf(TextReplacementRule(1, "", "sasa yaki", "Sasayaki"), TextReplacementRule(2, "", "kube", "Kubernetes")),
        customPrompts = listOf(PostProcessingPrompt(10, "Digits", "Write numbers as digits.")),
        profileCount = 3,
        active = true
    )

    @Test
    fun list() = compose.screenshot("profiles_list_light") {
        ProfilesScreen(ProfilesUiState(loaded = true, profiles = listOf(chat, email, notes), providers = providers), {}, {}, {})
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun editor() = compose.screenshot("profiles_editor_light") {
        ProfileEditorScreen(chat, editorState, navigation, {}, {}, {}, {}, {})
    }

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun editorDark() = compose.screenshot("profiles_editor_dark", dark = true) {
        ProfileEditorScreen(email, editorState.copy(active = false), navigation, {}, {}, {}, {}, {})
    }

    @Test
    fun editorNoCleanup() = compose.screenshot("profiles_editor_plain_dark", dark = true) {
        ProfileEditorScreen(notes, editorState.copy(active = false, providers = emptyList()), navigation, {}, {}, {}, {}, {})
    }
}
