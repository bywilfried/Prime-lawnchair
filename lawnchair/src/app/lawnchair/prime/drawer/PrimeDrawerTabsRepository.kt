package app.lawnchair.prime.drawer

import android.content.Context
import androidx.core.content.edit
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.util.ComponentKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent Prime-owned drawer organization.
 *
 * This deliberately does not reuse Lawnchair drawer folders: switching between the classic
 * drawer and Prime tabs must never mutate either mode's organization.
 */
class PrimeDrawerTabsRepository(context: Context) {
    private val prefs = LauncherPrefs.getPrefs(context)

    fun getConfiguration(): PrimeDrawerTabsConfiguration =
        decode(prefs.getString(PREF_CONFIGURATION, null))

    fun saveConfiguration(configuration: PrimeDrawerTabsConfiguration) {
        prefs.edit { putString(PREF_CONFIGURATION, encode(configuration.normalized())) }
    }

    fun createTab(title: String): PrimeDrawerTab {
        val configuration = getConfiguration()
        val tab = PrimeDrawerTab(
            id = nextTabId(configuration.tabs),
            title = title.trim(),
        )
        saveConfiguration(configuration.copy(tabs = configuration.tabs + tab))
        return tab
    }

    fun renameTab(tabId: String, title: String) {
        updateTab(tabId) { it.copy(title = title.trim()) }
    }

    fun deleteTab(tabId: String) {
        if (tabId == ALL_TAB_ID || tabId == UNCLASSIFIED_TAB_ID) return
        val configuration = getConfiguration()
        saveConfiguration(
            configuration.copy(
                tabs = configuration.tabs.filterNot { it.id == tabId },
                defaultTabId = configuration.defaultTabId.takeUnless { it == tabId } ?: ALL_TAB_ID,
                selectedTabId = configuration.selectedTabId.takeUnless { it == tabId } ?: ALL_TAB_ID,
            ),
        )
    }

    fun reorderTabs(orderedTabIds: List<String>) {
        val configuration = getConfiguration()
        val tabsById = configuration.tabs.associateBy { it.id }
        val reordered = orderedTabIds.mapNotNull(tabsById::get) +
            configuration.tabs.filter { it.id !in orderedTabIds }
        saveConfiguration(configuration.copy(tabs = reordered))
    }

    fun setDefaultTab(tabId: String) {
        val configuration = getConfiguration()
        if (configuration.tabs.none { it.id == tabId }) return
        saveConfiguration(configuration.copy(defaultTabId = tabId))
    }

    fun setSelectedTab(tabId: String) {
        val configuration = getConfiguration()
        if (configuration.tabs.none { it.id == tabId }) return
        saveConfiguration(configuration.copy(selectedTabId = tabId))
    }

    fun setTabApps(tabId: String, apps: Set<ComponentKey>) {
        if (tabId == ALL_TAB_ID || tabId == UNCLASSIFIED_TAB_ID) return
        updateTab(tabId) { tab -> tab.copy(apps = apps.mapTo(linkedSetOf(), ComponentKey::toString)) }
    }

    fun isAppInTab(componentKey: ComponentKey, tabId: String): Boolean {
        val configuration = getConfiguration()
        return when (tabId) {
            ALL_TAB_ID -> true
            UNCLASSIFIED_TAB_ID -> configuration.tabs
                .asSequence()
                .filterNot { it.isSystem }
                .none { componentKey.toString() in it.apps }
            else -> configuration.tabs.firstOrNull { it.id == tabId }
                ?.apps
                ?.contains(componentKey.toString()) == true
        }
    }

    private fun updateTab(tabId: String, transform: (PrimeDrawerTab) -> PrimeDrawerTab) {
        val configuration = getConfiguration()
        saveConfiguration(
            configuration.copy(
                tabs = configuration.tabs.map { if (it.id == tabId) transform(it) else it },
            ),
        )
    }

    private fun encode(configuration: PrimeDrawerTabsConfiguration): String = JSONObject().apply {
        put("version", CONFIG_VERSION)
        put("defaultTabId", configuration.defaultTabId)
        put("selectedTabId", configuration.selectedTabId)
        put("tabs", JSONArray().apply {
            configuration.tabs.forEach { tab ->
                put(JSONObject().apply {
                    put("id", tab.id)
                    put("title", tab.title)
                    put("apps", JSONArray(tab.apps.toList()))
                    put("folders", JSONArray().apply {
                        tab.folders.forEach { folder ->
                            put(JSONObject().apply {
                                put("id", folder.id)
                                put("title", folder.title)
                                put("apps", JSONArray(folder.apps.toList()))
                            })
                        }
                    })
                })
            }
        })
    }.toString()

    private fun decode(raw: String?): PrimeDrawerTabsConfiguration {
        if (raw.isNullOrBlank()) return PrimeDrawerTabsConfiguration.initial()
        return runCatching {
            val json = JSONObject(raw)
            val tabsJson = json.optJSONArray("tabs") ?: JSONArray()
            val tabs = buildList {
                for (index in 0 until tabsJson.length()) {
                    val tab = tabsJson.getJSONObject(index)
                    add(
                        PrimeDrawerTab(
                            id = tab.getString("id"),
                            title = tab.optString("title"),
                            apps = tab.optJSONArray("apps").toStringSet(),
                            folders = tab.optJSONArray("folders").toFolders(),
                        ),
                    )
                }
            }
            PrimeDrawerTabsConfiguration(
                tabs = tabs,
                defaultTabId = json.optString("defaultTabId", ALL_TAB_ID),
                selectedTabId = json.optString("selectedTabId", ALL_TAB_ID),
            ).normalized()
        }.getOrElse { PrimeDrawerTabsConfiguration.initial() }
    }

    private fun JSONArray?.toStringSet(): Set<String> = buildSet {
        val array = this@toStringSet ?: return@buildSet
        for (index in 0 until array.length()) add(array.getString(index))
    }

    private fun JSONArray?.toFolders(): List<PrimeDrawerFolder> {
        val array = this ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val folder = array.getJSONObject(index)
                add(
                    PrimeDrawerFolder(
                        id = folder.getString("id"),
                        title = folder.optString("title"),
                        apps = folder.optJSONArray("apps").toStringSet(),
                    ),
                )
            }
        }
    }

    private fun nextTabId(tabs: List<PrimeDrawerTab>): String {
        var suffix = 1
        val ids = tabs.mapTo(hashSetOf()) { it.id }
        while ("tab_$suffix" in ids) suffix++
        return "tab_$suffix"
    }

    companion object {
        const val ALL_TAB_ID = "system_all"
        const val UNCLASSIFIED_TAB_ID = "system_unclassified"
        private const val PREF_CONFIGURATION = "prime_drawer_tabs_configuration"
        private const val CONFIG_VERSION = 1
    }
}

data class PrimeDrawerTabsConfiguration(
    val tabs: List<PrimeDrawerTab>,
    val defaultTabId: String,
    val selectedTabId: String,
) {
    fun normalized(): PrimeDrawerTabsConfiguration {
        val uniqueTabs = tabs.distinctBy { it.id }.toMutableList()
        if (uniqueTabs.none { it.id == PrimeDrawerTabsRepository.ALL_TAB_ID }) {
            uniqueTabs.add(0, PrimeDrawerTab(PrimeDrawerTabsRepository.ALL_TAB_ID))
        }
        if (uniqueTabs.none { it.id == PrimeDrawerTabsRepository.UNCLASSIFIED_TAB_ID }) {
            val allIndex = uniqueTabs.indexOfFirst {
                it.id == PrimeDrawerTabsRepository.ALL_TAB_ID
            }
            uniqueTabs.add(allIndex + 1, PrimeDrawerTab(PrimeDrawerTabsRepository.UNCLASSIFIED_TAB_ID))
        }
        val normalizedTabs = uniqueTabs.toList()
        val ids = normalizedTabs.mapTo(hashSetOf()) { it.id }
        return copy(
            tabs = normalizedTabs,
            defaultTabId = defaultTabId.takeIf(ids::contains) ?: PrimeDrawerTabsRepository.ALL_TAB_ID,
            selectedTabId = selectedTabId.takeIf(ids::contains) ?: PrimeDrawerTabsRepository.ALL_TAB_ID,
        )
    }

    companion object {
        fun initial() = PrimeDrawerTabsConfiguration(
            tabs = listOf(
                PrimeDrawerTab(PrimeDrawerTabsRepository.ALL_TAB_ID),
                PrimeDrawerTab(PrimeDrawerTabsRepository.UNCLASSIFIED_TAB_ID),
            ),
            defaultTabId = PrimeDrawerTabsRepository.ALL_TAB_ID,
            selectedTabId = PrimeDrawerTabsRepository.ALL_TAB_ID,
        )
    }
}

data class PrimeDrawerTab(
    val id: String,
    val title: String = "",
    val apps: Set<String> = emptySet(),
    val folders: List<PrimeDrawerFolder> = emptyList(),
) {
    val isSystem: Boolean
        get() = id == PrimeDrawerTabsRepository.ALL_TAB_ID ||
            id == PrimeDrawerTabsRepository.UNCLASSIFIED_TAB_ID
}

data class PrimeDrawerFolder(
    val id: String,
    val title: String,
    val apps: Set<String> = emptySet(),
)
