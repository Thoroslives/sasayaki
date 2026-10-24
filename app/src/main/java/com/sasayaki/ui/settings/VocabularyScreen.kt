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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sasayaki.ui.components.EmptyState
import com.sasayaki.ui.components.SettingsPage
import com.sasayaki.ui.components.pageIntro
import com.sasayaki.ui.theme.SasayakiIcons
import com.sasayaki.ui.theme.Spacing

@Composable
fun VocabularyRoute(onBack: () -> Unit, viewModel: VocabularyViewModel = hiltViewModel()) {
    val terms by viewModel.terms.collectAsStateWithLifecycle()
    VocabularyScreen(terms = terms, onAdd = viewModel::add, onRemove = viewModel::remove, onBack = onBack)
}

@Composable
fun VocabularyScreen(terms: List<String>?, onAdd: (String) -> Unit, onRemove: (String) -> Unit, onBack: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(draft)
        draft = ""
    }
    SettingsPage(title = "Vocabulary", onBack = onBack) {
        pageIntro("Names, brands and jargon your speech model gets wrong. Both models get this list, so they come out spelled the way you write them.")
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
                        supportingText = { Text("Separate several with commas") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (draft.isNotBlank()) submit() }),
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(onClick = submit, enabled = draft.isNotBlank()) {
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    terms.forEach { term ->
                        InputChip(
                            selected = false,
                            onClick = { onRemove(term) },
                            label = { Text(term) },
                            trailingIcon = {
                                Icon(
                                    SasayakiIcons.Close,
                                    contentDescription = "Remove $term",
                                    modifier = Modifier.size(InputChipDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
