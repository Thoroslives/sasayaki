package com.sasayaki.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingDestination

@Serializable
data object HomeDestination

@Serializable
data object HistoryDestination

@Serializable
data object ProfilesDestination

/** [id] is an existing profile; new profiles are created before the editor opens. */
@Serializable
data class ProfileEditorDestination(val id: Long)

@Serializable
data object SettingsDestination

@Serializable
data object ProvidersDestination

/** [id] 0 opens the editor for a new provider. */
@Serializable
data class ProviderEditorDestination(val id: Long)

@Serializable
data object RecordingSettingsDestination

@Serializable
data object HistorySettingsDestination

@Serializable
data object AppearanceSettingsDestination

@Serializable
data object PermissionsDestination

@Serializable
data object ReplacementRulesDestination

@Serializable
data object PromptsDestination
