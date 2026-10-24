package com.sasayaki.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sasayaki.domain.processing.MAX_VOCABULARY_TERM_LENGTH
import com.sasayaki.domain.processing.hasOverlongTerm
import com.sasayaki.domain.processing.vocabularyReach
import com.sasayaki.ui.components.EmptyState
import com.sasayaki.ui.components.SettingsPage
import com.sasayaki.ui.components.pageIntro
import com.sasayaki.ui.theme.SasayakiIcons
import com.sasayaki.ui.theme.Spacing
import kotlinx.coroutines.launch

@Composable
fun VocabularyRoute(onBack: () -> Unit, viewModel: VocabularyViewModel = hiltViewModel()) {
    val terms by viewModel.terms.collectAsStateWithLifecycle()
    VocabularyScreen(terms = terms, onAdd = viewModel::add, onRemove = viewModel::remove, onBack = onBack)
}

@Composable
fun VocabularyScreen(terms: List<String>?, onAdd: (String) -> Unit, onRemove: (String) -> Unit, onBack: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    val overlong = hasOverlongTerm(draft)
    val canSubmit = draft.isNotBlank() && !overlong
    val submit = {
        onAdd(draft)
        draft = ""
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val remove: (String) -> Unit = { term ->
        onRemove(term)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar("Removed $term", actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onAdd(term)
        }
    }
    SettingsPage(title = "Vocabulary", onBack = onBack, snackbarHostState = snackbarHostState) {
        pageIntro(
            "Names, brands and jargon your speech model gets wrong. Both models get this list, " +
                "so they come out spelled the way you write them."
        )
        item(key = "add") {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceBright) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                    modifier = Modifier.padding(Spacing.l)
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Add words") },
                        supportingText = {
                            Text(
                                if (overlong) {
                                    "Keep each word under $MAX_VOCABULARY_TERM_LENGTH characters"
                                } else {
                                    "Separate several with commas"
                                }
                            )
                        },
                        isError = overlong,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (canSubmit) submit() }),
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(onClick = submit, enabled = canSubmit) {
                        Icon(SasayakiIcons.Add, contentDescription = "Add")
                    }
                }
            }
        }
        when {
            terms == null -> Unit

            terms.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = SasayakiIcons.Spellcheck,
                    title = "No words yet",
                    body = "Add the names and terms you dictate often, like your colleagues or the tools you use."
                )
            }

            else -> item(key = "terms") {
                val reach = remember(terms) { vocabularyReach(terms) }
                if (reach.speech < terms.size) {
                    Text(
                        text = if (reach.cleanup < terms.size) {
                            "The speech model fits the first ${reach.speech} words and cleanup the first ${reach.cleanup}."
                        } else {
                            "The speech model fits the first ${reach.speech} words; cleanup gets them all."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Spacing.xs, end = Spacing.xs, bottom = Spacing.s)
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    terms.forEach { term ->
                        key(term) {
                            // InputChip announces itself as a checkbox; a removable term is
                            // read as the word with a remove action instead.
                            InputChip(
                                selected = false,
                                onClick = { remove(term) },
                                label = { Text(term) },
                                trailingIcon = {
                                    Icon(
                                        SasayakiIcons.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(InputChipDefaults.IconSize)
                                    )
                                },
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription = term
                                    onClick(label = "Remove $term") {
                                        remove(term)
                                        true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
