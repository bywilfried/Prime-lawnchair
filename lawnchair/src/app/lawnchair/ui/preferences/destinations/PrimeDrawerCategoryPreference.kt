package app.lawnchair.ui.preferences.destinations

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.lawnchair.prime.drawer.PrimeDrawerTabsRepository
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.R

@Composable
fun PrimeDrawerCategoryPreference(tabId: String) {
    val context = LocalContext.current
    val repository = remember(context) { PrimeDrawerTabsRepository(context) }
    val configuration = repository.getConfiguration()
    val tab = configuration.tabs.firstOrNull { it.id == tabId } ?: return
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
                onClick = { repository.setDefaultTab(tab.id) },
            )
            if (!tab.isSystem) {
                ClickablePreference(
                    label = stringResource(id = R.string.prime_category_name),
                    subtitle = tab.title,
                    onClick = {},
                )
                ClickablePreference(
                    label = stringResource(id = R.string.prime_tab_apps),
                    subtitle = context.resources.getQuantityString(
                        R.plurals.apps_count,
                        tab.apps.size,
                        tab.apps.size,
                    ),
                    onClick = {},
                )
                ClickablePreference(
                    label = stringResource(id = R.string.prime_category_folders_pending),
                    onClick = {},
                )
                ClickablePreference(
                    label = stringResource(id = R.string.prime_tab_advanced) + "*",
                    onClick = {},
                )
            }
        }
    }
}
