package app.lawnchair.ui.preferences.destinations

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.R

@Composable
fun PrimeDevelopmentOptionsPreference() {
    val prefs = preferenceManager()
    PreferenceLayout(
        label = stringResource(R.string.prime_development_options),
        backArrowVisible = true,
    ) {
        PreferenceGroup {
            SwitchPreference(
                adapter = prefs.primeShowEmptyFolders.getAdapter(),
                label = stringResource(R.string.prime_show_empty_folders),
                description = stringResource(R.string.prime_show_empty_folders_description),
            )
        }
    }
}
