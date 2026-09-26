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
        PrimeCategoryDrawerPlaceholders()
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
 * Mirrors the parts of AppDrawerPreferences that can sensibly become per-category overrides.
 * Values remain placeholders until they are persisted in the Prime tab model.
 */
@Composable
private fun PrimeCategoryDrawerPlaceholders() {
    val inherited = "Par défaut (Lawnchair)"
    PreferenceGroup(heading = stringResource(id = R.string.style)) {
        AdvancedPlaceholder("Couleur de l’onglet de cette catégorie", inherited)
        AdvancedPlaceholder(stringResource(id = R.string.background_color), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.background_opacity), inherited)
    }
    PreferenceGroup(heading = stringResource(id = R.string.grid)) {
        AdvancedPlaceholder(stringResource(id = R.string.app_drawer_columns), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.row_height_label), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.app_drawer_indent_label), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.top_padding_label), inherited)
    }
    PreferenceGroup(heading = stringResource(id = R.string.icons)) {
        AdvancedPlaceholder("Forme des icônes", inherited)
        AdvancedPlaceholder(stringResource(id = R.string.icon_sizes), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.show_labels), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.label_size), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.twoline_label), inherited)
    }
    PreferenceGroup(heading = stringResource(id = R.string.advanced)) {
        AdvancedPlaceholder(stringResource(id = R.string.pref_all_apps_remember_position_title), inherited)
        AdvancedPlaceholder(stringResource(id = R.string.pref_all_apps_show_scrollbar_title), inherited)
    }
}

/**
 * Prime counterpart of FolderPreferences. The rows mirror Lawnchair's folder settings but target
 * a future Prime override instead of touching the global Lawnchair preferences.
 */
@Composable
private fun PrimeFolderAppearancePlaceholders(inheritedFrom: String) {
    val inherited = "Par défaut ($inheritedFrom)"
    PreferenceGroup(heading = stringResource(id = R.string.folders_label)) {
        AdvancedPlaceholder("Forme des icônes dans les dossiers", inherited)
    }
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
