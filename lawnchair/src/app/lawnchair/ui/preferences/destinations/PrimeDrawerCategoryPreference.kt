package app.lawnchair.ui.preferences.destinations

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.lawnchair.prime.drawer.PrimeDrawerTabsRepository
import app.lawnchair.ui.preferences.LocalNavController
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.navigation.PrimeDrawerCategoryFolders
import com.android.launcher3.R
import com.android.launcher3.util.ComponentKey

@Composable
fun PrimeDrawerCategoryPreference(tabId: String) {
    val context = LocalContext.current
    val repository = remember(context) { PrimeDrawerTabsRepository(context) }
    val navController = LocalNavController.current
    var configuration by remember { mutableStateOf(repository.getConfiguration()) }
    val tab = configuration.tabs.firstOrNull { it.id == tabId } ?: return
    val apps by app.lawnchair.util.appsState()
    var renameOpen by remember { mutableStateOf(false) }
    var appsOpen by remember { mutableStateOf(false) }
    val title = when (tab.id) {
        PrimeDrawerTabsRepository.ALL_TAB_ID -> stringResource(id = R.string.prime_tab_all)
        PrimeDrawerTabsRepository.UNCLASSIFIED_TAB_ID -> stringResource(id = R.string.prime_tab_unclassified)
        else -> tab.title
    }

    PreferenceLayout(
        label = title,
        backArrowVisible = true,
    ) {
        PreferenceGroup(heading = stringResource(id = R.string.prime_category_settings)) {
            ClickablePreference(
                label = stringResource(id = R.string.prime_category_default),
                subtitle = if (configuration.defaultTabId == tab.id) {
                    stringResource(id = R.string.prime_tabs_open_default)
                } else {
                    null
                },
                onClick = {
                    repository.setDefaultTab(tab.id)
                    configuration = repository.getConfiguration()
                },
            )
            if (!tab.isSystem) {
                ClickablePreference(
                    label = stringResource(id = R.string.prime_category_name),
                    subtitle = tab.title,
                    onClick = { renameOpen = true },
                )
                ClickablePreference(
                    label = stringResource(id = R.string.prime_tab_apps),
                    subtitle = context.resources.getQuantityString(
                        R.plurals.apps_count,
                        tab.apps.size,
                        tab.apps.size,
                    ),
                    onClick = { appsOpen = true },
                )
                ClickablePreference(
                    label = stringResource(id = R.string.app_drawer_folder),
                    onClick = { navController.navigate(PrimeDrawerCategoryFolders(tab.id)) },
                )
                ClickablePreference(
                    label = stringResource(id = R.string.prime_tab_advanced) + "*",
                    onClick = {},
                )
            }
        }
    }

    if (renameOpen) {
        var newTitle by remember(tab.title) { mutableStateOf(tab.title) }
        AlertDialog(
            onDismissRequest = { renameOpen = false },
            title = { Text(stringResource(id = R.string.prime_category_name)) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newTitle.trim()
                        if (title.isNotEmpty()) {
                            repository.renameTab(tab.id, title)
                            configuration = repository.getConfiguration()
                            renameOpen = false
                        }
                    },
                ) { Text(stringResource(id = android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { renameOpen = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
        )
    }

    if (appsOpen) {
        val selected = remember(tab.apps, appsOpen) { mutableStateOf(tab.apps) }
        AlertDialog(
            onDismissRequest = { appsOpen = false },
            title = { Text(stringResource(id = R.string.prime_tab_apps)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    items(apps.size) { index ->
                        val app = apps[index]
                        val key = app.key.toString()
                        androidx.compose.material3.Checkbox(
                            checked = key in selected.value,
                            onCheckedChange = { checked ->
                                selected.value = if (checked) selected.value + key else selected.value - key
                            },
                        )
                        Text(app.label)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.setTabApps(
                            tab.id,
                            selected.value.mapNotNull(ComponentKey::fromString).toSet(),
                        )
                        configuration = repository.getConfiguration()
                        appsOpen = false
                    },
                ) { Text(stringResource(id = android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { appsOpen = false }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
        )
    }
}
