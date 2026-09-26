package app.lawnchair.ui.preferences.destinations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.lawnchair.prime.drawer.PrimeDrawerFolderVisualOverrides
import app.lawnchair.prime.drawer.PrimeDrawerTabsRepository
import app.lawnchair.prime.drawer.PrimeDrawerVisualOverrides
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.controls.SliderPreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import com.android.launcher3.R

private const val NOT_IMPLEMENTED = " *"

@Composable
fun PrimeDrawerCategoryAdvancedPreference(tabId: String) {
    val context = LocalContext.current
    val repository = remember { PrimeDrawerTabsRepository(context) }
    val initial = remember(tabId) {
        repository.getConfiguration().tabs.firstOrNull { it.id == tabId }?.visualOverrides
            ?: PrimeDrawerVisualOverrides()
    }
    val overrides = remember(tabId) { mutableStateOf(initial) }
    fun update(value: PrimeDrawerVisualOverrides) {
        overrides.value = value
        repository.setTabVisualOverrides(tabId, value)
    }

    PreferenceLayout(label = "Options avancées", backArrowVisible = true) {
        PrimeCategoryDrawerOptions(overrides.value, ::update)
        PrimeCategoryFolderOptions(overrides.value, ::update)
    }
}

@Composable
fun PrimeDrawerFolderAdvancedPreference(tabId: String, folderId: String) {
    val context = LocalContext.current
    val repository = remember { PrimeDrawerTabsRepository(context) }
    val initial = remember(tabId, folderId) {
        repository.getConfiguration().tabs.firstOrNull { it.id == tabId }
            ?.folders?.firstOrNull { it.id == folderId }?.visualOverrides
            ?: PrimeDrawerFolderVisualOverrides()
    }
    val overrides = remember(tabId, folderId) { mutableStateOf(initial) }
    fun update(value: PrimeDrawerFolderVisualOverrides) {
        overrides.value = value
        repository.setFolderVisualOverrides(tabId, folderId, value)
    }

    PreferenceLayout(label = stringResource(id = R.string.folders_label), backArrowVisible = true) {
        PrimeFolderOptions(overrides.value, ::update)
    }
}

@Composable
private fun PrimeCategoryDrawerOptions(
    value: PrimeDrawerVisualOverrides,
    update: (PrimeDrawerVisualOverrides) -> Unit,
) {
    PreferenceGroup(heading = stringResource(id = R.string.style)) {
        NullableColorPreference("Couleur de l’onglet de cette catégorie", value.tabColor) {
            update(value.copy(tabColor = it))
        }
        NullableColorPreference("Couleur d’arrière-plan", value.drawerBackgroundColor) {
            update(value.copy(drawerBackgroundColor = it))
        }
        NullableFloatSlider(stringResource(id = R.string.background_opacity), value.drawerBackgroundOpacity, 0f..1f, 0.1f) {
            update(value.copy(drawerBackgroundOpacity = it))
        }
    }
    PreferenceGroup(heading = stringResource(id = R.string.grid)) {
        NullableIntSlider(stringResource(id = R.string.app_drawer_columns), value.drawerColumns, 2..10) {
            update(value.copy(drawerColumns = it))
        }
        NullableFloatSlider(stringResource(id = R.string.row_height_label), value.drawerRowHeight, 0.5f..1.5f, 0.1f) {
            update(value.copy(drawerRowHeight = it))
        }
        NullableFloatSlider(stringResource(id = R.string.app_drawer_indent_label), value.drawerHorizontalMargin, 0f..2f, 0.1f) {
            update(value.copy(drawerHorizontalMargin = it))
        }
        NullableFloatSlider(stringResource(id = R.string.top_padding_label), value.drawerTopPadding, 0f..2f, 0.1f) {
            update(value.copy(drawerTopPadding = it))
        }
    }
    PreferenceGroup(heading = stringResource(id = R.string.icons)) {
        AdvancedPlaceholder("Forme des icônes", "Par défaut (Lawnchair)")
        NullableFloatSlider(stringResource(id = R.string.icon_sizes), value.drawerIconSize, 0.5f..1.5f, 0.1f) {
            update(value.copy(drawerIconSize = it))
        }
        NullableSwitch(stringResource(id = R.string.show_labels), value.showLabels) {
            update(value.copy(showLabels = it))
        }
        NullableFloatSlider(stringResource(id = R.string.label_size), value.labelSize, 0.5f..1.5f, 0.1f) {
            update(value.copy(labelSize = it))
        }
        NullableSwitch(stringResource(id = R.string.twoline_label), value.twoLineLabels) {
            update(value.copy(twoLineLabels = it))
        }
    }
    PreferenceGroup(heading = stringResource(id = R.string.advanced)) {
        NullableSwitch(stringResource(id = R.string.pref_all_apps_remember_position_title), value.rememberPosition) {
            update(value.copy(rememberPosition = it))
        }
        NullableSwitch(stringResource(id = R.string.pref_all_apps_show_scrollbar_title), value.showScrollbar) {
            update(value.copy(showScrollbar = it))
        }
    }
}

@Composable
private fun PrimeCategoryFolderOptions(
    value: PrimeDrawerVisualOverrides,
    update: (PrimeDrawerVisualOverrides) -> Unit,
) {
    PreferenceGroup(heading = stringResource(id = R.string.folders_label)) {
        AdvancedPlaceholder("Forme des icônes dans les dossiers", "Par défaut (Lawnchair)")
    }
    PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
        AdvancedPlaceholder(stringResource(id = R.string.folder_shape_label), "Par défaut (Lawnchair)")
        AdvancedPlaceholder("Couleur de l’arrière-plan des icônes", "Par défaut (Lawnchair)")
        NullableFloatSlider(stringResource(id = R.string.folder_preview_bg_opacity_label), value.folderPreviewOpacity, 0f..1f, 0.1f) {
            update(value.copy(folderPreviewOpacity = it))
        }
        NullableFloatSlider(stringResource(id = R.string.folder_bg_opacity_label), value.folderBackgroundOpacity, 0f..1f, 0.1f) {
            update(value.copy(folderBackgroundOpacity = it))
        }
    }
    PreferenceGroup(heading = stringResource(id = R.string.grid)) {
        NullableIntSlider(stringResource(id = R.string.max_folder_columns), value.folderColumns, 2..6) {
            update(value.copy(folderColumns = it))
        }
        NullableIntSlider(stringResource(id = R.string.max_folder_rows), value.folderRows, 2..6) {
            update(value.copy(folderRows = it))
        }
    }
    PreferenceGroup(heading = stringResource(id = R.string.icons)) {
        NullableSwitch(stringResource(id = R.string.show_labels), value.folderShowLabels) {
            update(value.copy(folderShowLabels = it))
        }
        NullableFloatSlider(stringResource(id = R.string.label_size), value.folderLabelSize, 0.5f..1.5f, 0.1f) {
            update(value.copy(folderLabelSize = it))
        }
    }
}

@Composable
private fun PrimeFolderOptions(
    value: PrimeDrawerFolderVisualOverrides,
    update: (PrimeDrawerFolderVisualOverrides) -> Unit,
) {
    PreferenceGroup(heading = stringResource(id = R.string.folders_label)) {
        AdvancedPlaceholder("Forme des icônes dans les dossiers", "Par défaut (catégorie)")
    }
    PreferenceGroup(heading = stringResource(id = R.string.general_label)) {
        AdvancedPlaceholder(stringResource(id = R.string.folder_shape_label), "Par défaut (catégorie)")
        AdvancedPlaceholder("Couleur de l’arrière-plan des icônes", "Par défaut (catégorie)")
        NullableFloatSlider(stringResource(id = R.string.folder_preview_bg_opacity_label), value.previewOpacity, 0f..1f, 0.1f) {
            update(value.copy(previewOpacity = it))
        }
        NullableFloatSlider(stringResource(id = R.string.folder_bg_opacity_label), value.backgroundOpacity, 0f..1f, 0.1f) {
            update(value.copy(backgroundOpacity = it))
        }
    }
    PreferenceGroup(heading = stringResource(id = R.string.grid)) {
        NullableIntSlider(stringResource(id = R.string.max_folder_columns), value.columns, 2..6) {
            update(value.copy(columns = it))
        }
        NullableIntSlider(stringResource(id = R.string.max_folder_rows), value.rows, 2..6) {
            update(value.copy(rows = it))
        }
    }
    PreferenceGroup(heading = stringResource(id = R.string.icons)) {
        NullableSwitch(stringResource(id = R.string.show_labels), value.showLabels) {
            update(value.copy(showLabels = it))
        }
        NullableFloatSlider(stringResource(id = R.string.label_size), value.labelSize, 0.5f..1.5f, 0.1f) {
            update(value.copy(labelSize = it))
        }
    }
}

@Composable
private fun NullableColorPreference(label: String, value: Int?, update: (Int?) -> Unit) {
    val palette = listOf(
        0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF607D8B.toInt(),
        0xFF2196F3.toInt(), 0xFF3F51B5.toInt(), 0xFF673AB7.toInt(),
        0xFFE91E63.toInt(), 0xFFF44336.toInt(), 0xFFFF9800.toInt(),
        0xFF4CAF50.toInt(), 0xFF009688.toInt(),
    )
    if (value == null) {
        ClickablePreference(label = label, subtitle = "Par défaut — toucher pour personnaliser") {
            update(palette[3])
        }
    } else {
        val index = palette.indexOf(value).let { if (it < 0) 0 else it }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { update(palette[(index + 1) % palette.size]) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(text = "$label — toucher pour changer", modifier = Modifier.weight(1f))
            Text(text = "   ", modifier = Modifier.background(Color(value)))
        }
        ClickablePreference(label = "Utiliser la valeur par défaut", onClick = { update(null) })
    }
}

@Composable
private fun NullableSwitch(label: String, value: Boolean?, update: (Boolean?) -> Unit) {
    if (value == null) {
        ClickablePreference(label = label, subtitle = "Par défaut — toucher pour personnaliser") { update(true) }
    } else {
        SwitchPreference(
            checked = value,
            onCheckedChange = { update(it) },
            label = label,
            description = "Personnalisé — toucher le libellé pour revenir à Par défaut",
            onClick = { update(null) },
        )
    }
}

@Composable
private fun NullableFloatSlider(
    label: String,
    value: Float?,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    update: (Float?) -> Unit,
) {
    if (value == null) {
        ClickablePreference(label = label, subtitle = "Par défaut — toucher pour personnaliser") {
            update((range.start + range.endInclusive) / 2f)
        }
    } else {
        SliderPreference(label = label, value = value, onValueChangeFinished = { update(it) }, valueRange = range, step = step)
        ClickablePreference(label = "Utiliser la valeur par défaut", onClick = { update(null) })
    }
}

@Composable
private fun NullableIntSlider(label: String, value: Int?, range: ClosedRange<Int>, update: (Int?) -> Unit) {
    if (value == null) {
        ClickablePreference(label = label, subtitle = "Par défaut — toucher pour personnaliser") {
            update((range.start + range.endInclusive) / 2)
        }
    } else {
        SliderPreference(label = label, value = value.toFloat(), onValueChangeFinished = { update(it.toInt()) }, valueRange = range.start.toFloat()..range.endInclusive.toFloat(), step = 1f)
        ClickablePreference(label = "Utiliser la valeur par défaut", onClick = { update(null) })
    }
}

@Composable
private fun SliderPreference(
    label: String,
    value: Float,
    onValueChangeFinished: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
) {
    val state = remember(value) { mutableStateOf(value) }
    app.lawnchair.ui.preferences.components.controls.SliderPreference(
        label = label,
        adapter = app.lawnchair.preferences.customPreferenceAdapter(state.value) {
            state.value = it
            onValueChangeFinished(it)
        },
        valueRange = valueRange,
        step = step,
        showAsPercentage = valueRange.endInclusive <= 1.5f,
    )
}

@Composable
private fun AdvancedPlaceholder(label: String, subtitle: String) {
    ClickablePreference(label = label + NOT_IMPLEMENTED, subtitle = subtitle, onClick = {})
}
