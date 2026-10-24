package com.sasayaki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.sasayaki.ui.theme.Spacing

@Composable
fun SettingsPage(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    floatingActionButton: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    itemSpacing: Dp = Spacing.xl,
    bottomInset: Dp = Spacing.xl,
    content: LazyListScope.() -> Unit
) {
    SasayakiScaffold(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        floatingActionButton = floatingActionButton,
        topBar = { scrollBehavior ->
            LargeTitleBar(title = title, scrollBehavior = scrollBehavior, onBack = onBack, actions = actions)
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = Spacing.screenHorizontal,
                end = Spacing.screenHorizontal,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + bottomInset
            ),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content
        )
    }
}

fun LazyListScope.pageIntro(text: String) {
    item(key = "intro") {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xs)
        )
    }
}
