package app.lawnchair.ui.preferences.destinations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.lawnchair.data.folder.FolderEntry
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.prime.drawer.PrimeDrawerTabsRepository
import app.lawnchair.util.appsState
import com.android.launcher3.util.ComponentKey

@Composable
fun PrimeDrawerCategoryAppsPreference(tabId: String) {
    val context = LocalContext.current
    val repository = remember(context) { PrimeDrawerTabsRepository(context) }
    val configuration = remember { mutableStateOf(repository.getConfiguration()) }
    DisposableEffect(repository) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PrimeDrawerTabsRepository.PREF_CONFIGURATION) {
                configuration.value = repository.getConfiguration()
            }
        }
        repository.registerConfigurationChangeListener(listener)
        onDispose { repository.unregisterConfigurationChangeListener(listener) }
    }
    val apps = appsState().value
    val tab = configuration.value.tabs.firstOrNull { it.id == tabId } ?: return
    val hideFolderApps = PreferenceManager.getInstance(context).primeHideFolderApps.get()
    val folderAppKeys = remember(tab.folders) {
        tab.folders.flatMapTo(mutableSetOf()) { it.apps }
    }
    val visibleTabApps = remember(tab.apps, folderAppKeys, hideFolderApps) {
        if (hideFolderApps) tab.apps - folderAppKeys else tab.apps
    }

    val orderedKeys = if (tab.sortMode == "custom") {
        tab.customOrder.filter(visibleTabApps::contains) +
            visibleTabApps.filterNot(tab.customOrder::contains)
    } else {
        visibleTabApps.sortedBy { key ->
            apps.firstOrNull { it.key.toString() == key }?.label?.lowercase() ?: key
        }
    }

    SelectAppsForDrawerFolder(
        folderEntry = FolderEntry(
            id = 0,
            title = tab.title,
            itemComponentKeys = orderedKeys,
        ),
        apps = apps,
        allFolderPackages = emptySet(),
        onUpdate = { _, componentKeys ->
            val updatedVisibleKeys = componentKeys.mapNotNull(ComponentKey::fromString).toSet()
            val hiddenFolderKeys = if (hideFolderApps) {
                tab.apps.intersect(folderAppKeys).mapNotNull(ComponentKey::fromString).toSet()
            } else {
                emptySet()
            }
            repository.setTabApps(
                tabId,
                updatedVisibleKeys + hiddenFolderKeys,
            )
            if (tab.sortMode == "custom") {
                val visibleKeySet = componentKeys.toSet()
                val updatedOrder = buildList {
                    var visibleIndex = 0
                    tab.customOrder.forEach { key ->
                        if (key in visibleTabApps) {
                            if (visibleIndex < componentKeys.size) add(componentKeys[visibleIndex++])
                        } else {
                            add(key)
                        }
                    }
                    while (visibleIndex < componentKeys.size) add(componentKeys[visibleIndex++])
                    componentKeys.filterNot(visibleKeySet::contains).forEach(::add)
                }.distinct()
                repository.setTabCustomOrder(tabId, updatedOrder)
            }
        },
        showDuplicateFilter = false,
        showStandardMenuActions = false,
        preserveActiveOrder = tab.sortMode == "custom",
        reorderEnabled = tab.sortMode == "custom",
        extraMenuContent = { hideMenu ->
            DropdownMenuItem(
                text = { Text("Alphabétique") },
                trailingIcon = { if (tab.sortMode == "alphabetical") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabSortMode(tabId, "alphabetical"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("Personnalisé") },
                trailingIcon = { if (tab.sortMode == "custom") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabSortMode(tabId, "custom"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("Au début") },
                trailingIcon = { if (tab.folderPlacement == "start") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabFolderPlacement(tabId, "start"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("À la fin") },
                trailingIcon = { if (tab.folderPlacement == "end") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabFolderPlacement(tabId, "end"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("Comme les applications") },
                trailingIcon = { if (tab.folderPlacement == "mixed") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabFolderPlacement(tabId, "mixed"); hideMenu() },
            )
        },
    )
}
