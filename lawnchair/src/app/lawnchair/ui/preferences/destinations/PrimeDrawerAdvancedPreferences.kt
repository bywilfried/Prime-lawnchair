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
            AdvancedPlaceholder("Forme des dossiers")
            AdvancedPlaceholder("Couleur des dossiers")
        }
    }
}

@Composable
fun PrimeDrawerFolderAdvancedPreference(tabId: String, folderId: String) {
    PreferenceLayout(
        label = "Options avancées",
        backArrowVisible = true,
    ) {
        PreferenceGroup(heading = "Apparence de ce dossier") {
            AdvancedPlaceholder("Forme de ce dossier", "Par défaut (catégorie)")
            AdvancedPlaceholder("Couleur de ce dossier", "Par défaut (catégorie)")
        }
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
