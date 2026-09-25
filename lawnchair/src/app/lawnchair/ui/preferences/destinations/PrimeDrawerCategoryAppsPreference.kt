package app.lawnchair.ui.preferences.destinations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.lawnchair.data.folder.FolderEntry
import app.lawnchair.prime.drawer.PrimeDrawerTabsRepository
import app.lawnchair.util.appsState
import com.android.launcher3.util.ComponentKey

@Composable
fun PrimeDrawerCategoryAppsPreference(tabId: String) {
    val context = LocalContext.current
    val repository = remember(context) { PrimeDrawerTabsRepository(context) }
    val apps = appsState().value
    val tab = repository.getConfiguration().tabs.firstOrNull { it.id == tabId } ?: return

    val orderedKeys = if (tab.sortMode == "custom") {
        tab.customOrder.filter(tab.apps::contains) + tab.apps.filterNot(tab.customOrder::contains)
    } else {
        tab.apps.sortedBy { key ->
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
            repository.setTabApps(
                tabId,
                componentKeys.mapNotNull(ComponentKey::fromString).toSet(),
            )
            if (tab.sortMode == "custom") {
                repository.setTabCustomOrder(tabId, componentKeys)
            }
        },
        showDuplicateFilter = false,
        showStandardMenuActions = false,
        preserveActiveOrder = tab.sortMode == "custom",
        extraMenuContent = { hideMenu ->
            DropdownMenuItem(
                text = { Text("Ordre alphabétique") },
                trailingIcon = { if (tab.sortMode == "alphabetical") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabSortMode(tabId, "alphabetical"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("Ordre personnalisé") },
                trailingIcon = { if (tab.sortMode == "custom") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabSortMode(tabId, "custom"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("Dossiers au début") },
                trailingIcon = { if (tab.folderPlacement == "start") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabFolderPlacement(tabId, "start"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("Dossiers à la fin") },
                trailingIcon = { if (tab.folderPlacement == "end") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabFolderPlacement(tabId, "end"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("Dossiers personnalisés") },
                trailingIcon = { if (tab.folderPlacement == "mixed") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setTabFolderPlacement(tabId, "mixed"); hideMenu() },
            )
        },
    )
}
