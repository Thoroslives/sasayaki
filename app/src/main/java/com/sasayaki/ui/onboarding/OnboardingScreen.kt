package com.sasayaki.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sasayaki.ui.common.SetupItem
import com.sasayaki.ui.common.rememberPermissionItems
import com.sasayaki.ui.common.rememberRestrictedSettingsLikely
import com.sasayaki.ui.components.SetupGroup
import com.sasayaki.ui.settings.ConnectionResultCard
import com.sasayaki.ui.settings.DefaultModels
import com.sasayaki.ui.settings.ProviderConnectionForm
import com.sasayaki.ui.settings.ProviderEditorActions
import com.sasayaki.ui.settings.ProviderEditorUiState
import com.sasayaki.ui.settings.ProviderForm
import com.sasayaki.ui.settings.providerEditorActions
import com.sasayaki.ui.theme.SasayakiIcons
import com.sasayaki.ui.theme.SasayakiLogo
import com.sasayaki.ui.theme.Spacing

enum class OnboardingStep { WELCOME, PROVIDER, PERMISSIONS, TRY }

@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    val providerState by viewModel.providerState.collectAsStateWithLifecycle()
    val serviceRunning by viewModel.serviceRunning.collectAsStateWithLifecycle()
    val permissions = rememberPermissionItems()
    val restricted = rememberRestrictedSettingsLikely()
    val ready = permissions.none { it.required && !it.done }

    LaunchedEffect(step, ready) {
        if (step == OnboardingStep.TRY && ready && !serviceRunning) viewModel.startBubble()
    }

    OnboardingScreen(
        step = step,
        providerForm = viewModel.draft.form,
        providerState = providerState,
        providerActions = providerEditorActions(viewModel.draft, onAdd = {}, onDelete = {}, onBack = {}),
        permissions = permissions,
        restrictedSettingsLikely = restricted,
        bubbleRunning = serviceRunning,
        onStep = { step = it },
        onSaveProvider = { viewModel.saveProvider { step = OnboardingStep.PERMISSIONS } },
        onFinish = { viewModel.complete(onFinished) }
    )
}

@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    providerForm: ProviderForm,
    providerState: ProviderEditorUiState,
    providerActions: ProviderEditorActions,
    permissions: List<SetupItem>,
    restrictedSettingsLikely: Boolean,
    bubbleRunning: Boolean,
    onStep: (OnboardingStep) -> Unit,
    onSaveProvider: () -> Unit,
    onFinish: () -> Unit
) {
    val previous = OnboardingStep.entries.getOrNull(step.ordinal - 1)
    BackHandler(enabled = previous != null) { previous?.let(onStep) }
    val progress by animateFloatAsState(
        targetValue = (step.ordinal + 1f) / OnboardingStep.entries.size,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "progress"
    )
    val permissionsReady = permissions.none { it.required && !it.done }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            if (step != OnboardingStep.WELCOME) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xxl, start = Spacing.xs, end = Spacing.xl),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { previous?.let(onStep) }) {
                        Icon(SasayakiIcons.ArrowBack, contentDescription = "Back")
                    }
                    LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f))
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.l),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    OnboardingStep.WELCOME -> PrimaryButton("Get started") { onStep(OnboardingStep.PROVIDER) }

                    OnboardingStep.PROVIDER -> {
                        PrimaryButton(
                            "Continue",
                            enabled = providerForm.url.isNotBlank() && providerForm.speechModel != null,
                            onClick = onSaveProvider
                        )
                        TextButton(onClick = { onStep(OnboardingStep.PERMISSIONS) }) { Text("Set up later") }
                    }

                    OnboardingStep.PERMISSIONS -> {
                        PrimaryButton("Continue", enabled = permissionsReady) { onStep(OnboardingStep.TRY) }
                        TextButton(onClick = { onStep(OnboardingStep.TRY) }) { Text("Skip for now") }
                    }

                    OnboardingStep.TRY -> PrimaryButton("Done", onClick = onFinish)
                }
            }
        }
    ) { padding ->
        val forward = MaterialTheme.motionScheme.defaultEffectsSpec<IntOffset>()
        val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(forward) { it / 3 * direction } + fadeIn(fade)) togetherWith
                    (slideOutHorizontally(forward) { -it / 3 * direction } + fadeOut(fade))
            },
            label = "onboarding"
        ) { current ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.screenHorizontal,
                    end = Spacing.screenHorizontal,
                    top = padding.calculateTopPadding() + Spacing.l,
                    bottom = padding.calculateBottomPadding() + Spacing.l
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.l)
            ) {
                when (current) {
                    OnboardingStep.WELCOME -> welcome()
                    OnboardingStep.PROVIDER -> provider(providerForm, providerState, providerActions)
                    OnboardingStep.PERMISSIONS -> permissionsStep(permissions, restrictedSettingsLikely)
                    OnboardingStep.TRY -> tryIt(bubbleRunning, permissionsReady)
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.MediumContainerHeight),
        contentPadding = ButtonDefaults.MediumContentPadding
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

private fun LazyListScope.stepHeader(title: String, body: String) {
    item(key = "header") {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(horizontal = Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun LazyListScope.welcome() {
    item(key = "hero") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xxxl, bottom = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            WelcomeMark()
            Text(
                "Speak, and it's typed.",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Text(
                "Sasayaki turns your voice into clean text in any app, using servers you choose.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.l)
            )
        }
    }
    item(key = "points") {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceBright) {
            Column(modifier = Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
                WelcomePoint(SasayakiIcons.Keyboard, "Works in every app", "A bubble appears whenever your keyboard opens.")
                WelcomePoint(SasayakiIcons.Shield, "Your servers, your words", "Recordings go only to the providers you add.")
                WelcomePoint(SasayakiIcons.AutoAwesome, "Cleans up as you talk", "Filler words and self-corrections are handled for you.")
            }
        }
    }
}

@Composable
private fun WelcomeMark() {
    val transition = rememberInfiniteTransition(label = "mark")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing)),
        label = "markRotation"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(152.dp)
                .graphicsLayer { rotationZ = rotation }
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        SasayakiLogo(size = 104.dp)
    }
}

@Composable
private fun WelcomePoint(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.l), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun LazyListScope.provider(form: ProviderForm, state: ProviderEditorUiState, actions: ProviderEditorActions) {
    stepHeader(
        "Connect speech to text",
        "Add the server that turns your voice into text. Any OpenAI-compatible API works: your own or a hosted one."
    )
    item(key = "form") { ProviderConnectionForm(form, state, actions) }
    item(key = "test") {
        Button(
            onClick = actions.onTest,
            enabled = !state.testing && form.url.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.testing) {
                LoadingIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Test connection")
            }
        }
    }
    state.result?.let { result -> item(key = "result") { ConnectionResultCard(result) } }
    item(key = "models") { DefaultModels(form, state, actions) }
}

private fun LazyListScope.permissionsStep(permissions: List<SetupItem>, restricted: Boolean) {
    stepHeader("Let Sasayaki listen and type", "These let the bubble record when you tap it and type the result where you are.")
    item(key = "permissions") { SetupGroup(items = permissions, restrictedSettingsLikely = restricted) }
}

private fun LazyListScope.tryIt(bubbleRunning: Boolean, ready: Boolean) {
    stepHeader(
        "Try it",
        if (ready) {
            "Tap the field below, then tap the bubble that appears and say something."
        } else {
            "Finish the permissions to use the bubble. You can do that later from Home."
        }
    )
    item(key = "field") { TryField(enabled = ready) }
    if (ready && !bubbleRunning) {
        item(key = "starting") {
            Text("Starting the bubble…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TryField(enabled: Boolean) {
    var value by rememberSaveable { mutableStateOf("") }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceBright) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.l),
            placeholder = { Text("Your words appear here") },
            minLines = 5
        )
    }
}
