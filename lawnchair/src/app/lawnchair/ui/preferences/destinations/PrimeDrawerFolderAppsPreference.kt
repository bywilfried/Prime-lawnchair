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
fun PrimeDrawerFolderAppsPreference(tabId: String, folderId: String) {
    val context = LocalContext.current
    val repository = remember(context) { PrimeDrawerTabsRepository(context) }
    val appsState = appsState()
    val apps = appsState.value
    val tab = repository.getConfiguration().tabs.firstOrNull { it.id == tabId } ?: return
    val folder = tab.folders.firstOrNull { it.id == folderId } ?: return
    val categoryAppKeys = buildSet<String> {
        addAll(tab.apps)
        tab.folders.forEach { addAll(it.apps) }
    }
    val categoryApps = apps.filter { app ->
        app.key.toString() in categoryAppKeys
    }

    SelectAppsForDrawerFolder(
        folderEntry = FolderEntry(
            id = 0,
            title = folder.title,
            itemComponentKeys = if (folder.sortMode == "custom") {
                folder.customOrder.filter(folder.apps::contains) + folder.apps.filterNot(folder.customOrder::contains)
            } else {
                folder.apps.sortedBy { key -> apps.firstOrNull { it.key.toString() == key }?.label?.lowercase() ?: key }
            },
        ),
        apps = categoryApps,
        allFolderPackages = emptySet(),
        onUpdate = { _, componentKeys ->
            repository.setFolderApps(
                tabId,
                folderId,
                componentKeys.mapNotNull(ComponentKey::fromString).toSet(),
            )
            if (folder.sortMode == "custom") repository.setFolderCustomOrder(tabId, folderId, componentKeys)
        },
        showDuplicateFilter = false,
        showStandardMenuActions = false,
        preserveActiveOrder = folder.sortMode == "custom",
        reorderEnabled = folder.sortMode == "custom",
        extraMenuContent = { hideMenu ->
            DropdownMenuItem(
                text = { Text("Ordre alphabétique") },
                trailingIcon = { if (folder.sortMode == "alphabetical") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setFolderSortMode(tabId, folderId, "alphabetical"); hideMenu() },
            )
            DropdownMenuItem(
                text = { Text("Ordre personnalisé") },
                trailingIcon = { if (folder.sortMode == "custom") Icon(Icons.Rounded.Check, null) },
                onClick = { repository.setFolderSortMode(tabId, folderId, "custom"); hideMenu() },
            )
        },
    )
}
