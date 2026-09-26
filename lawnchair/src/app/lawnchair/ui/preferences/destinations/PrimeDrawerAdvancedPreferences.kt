package app.lawnchair.ui.preferences.destinations

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.R

private const val NOT_IMPLEMENTED = " *"

@Composable
fun PrimeDrawerCategoryAdvancedPreference(tabId: String) {
    PreferenceLayout(
        label = "Options avancées",
        backArrowVisible = true,
    ) {
        PreferenceGroup(heading = "Apparence de la catégorie") {
            AdvancedPlaceholder("Couleur de l’onglet (bouton uniquement)")
            AdvancedPlaceholder("Couleur d’arrière-plan de la catégorie")
            AdvancedPlaceholder("Forme des icônes")
            AdvancedPlaceholder("Taille des icônes")
        }
        PrimeFolderAppearancePlaceholders(inheritedFrom = "Lawnchair")
    }
}

@Composable
fun PrimeDrawerFolderAdvancedPreference(tabId: String, folderId: String) {
    PreferenceLayout(
        label = stringResource(id = R.string.folders_label),
        backArrowVisible = true,
    ) {
        PrimeFolderAppearancePlaceholders(inheritedFrom = "catégorie")
    }
}

/**
 * Prime counterpart of FolderPreferences. The rows intentionally mirror Lawnchair's folder
 * settings, but remain placeholders until each value is backed by Prime per-category/per-folder
 * overrides. This keeps the global Lawnchair preferences completely untouched.
 */
@Composable
private fun PrimeFolderAppearancePlaceholders(inheritedFrom: String) {
    val inherited = "Par défaut ($inheritedFrom)"
    PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
        AdvancedPlaceholder(stringResource(id = R.string.folder_shape_label), inherited)
        AdvancedPlaceholder("Couleur de l’arrière-plan des icônes", inherited)
        AdvancedPlaceholder(stringResource(id = R.string.folder_preview_bg_opacity_label), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.folder_bg_opacity_label), inherited)
    }
    PreferenceGroup(heading = stringResource(id = R.string.grid)) {
        AdvancedPlaceholder(stringResource(id = R.string.max_folder_columns), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.max_folder_rows), inherited)
    }
    PreferenceGroup(heading = stringResource(id = R.string.icons)) {
        AdvancedPlaceholder(stringResource(id = R.string.show_labels), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.label_size), inherited)
    }
}

@Composable
private fun AdvancedPlaceholder(label: String, subtitle: String = "Par défaut") {
    ClickablePreference(
        label = label + NOT_IMPLEMENTED,
        subtitle = subtitle,
        onClick = {},
    )
}
