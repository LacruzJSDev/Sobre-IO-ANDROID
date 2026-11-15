package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_apply
import dev.lacruzjs.sobreio.generated.resources.entity_select_search
import org.jetbrains.compose.resources.stringResource

private val PickerMaxHeight = 420.dp

/**
 * A bottom sheet that lists [options] as rows (avatar, name, a check on the selected one), the same look as the list of an
 * [EntitySelect], with a search box from [ENTITY_SELECT_SEARCH_THRESHOLD] options on. [allOption] is the first row, the
 * one that clears the filter (null id). Tapping a row calls [onSelect] and the caller closes the sheet.
 */
@Composable
fun EntityPickerSheet(
    title: String,
    allOption: EntityOption,
    options: List<EntityOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    FlatBottomSheet(title = title, onDismiss = onDismiss) {
        var query by rememberSaveable { mutableStateOf("") }
        if (options.size > ENTITY_SELECT_SEARCH_THRESHOLD) {
            FormTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(Res.string.entity_select_search),
                imeAction = ImeAction.Done,
            )
        }
        val visible = filterEntityOptions(options, query)
        // Bounded height: the list scrolls on its own inside the sheet
        LazyColumn(modifier = Modifier.heightIn(max = PickerMaxHeight)) {
            if (query.isBlank()) {
                item(key = "all") { OptionRow(allOption, selected = selectedId == null, onClick = { onSelect(null) }) }
            }
            items(visible, key = { it.id ?: "" }) { option ->
                OptionRow(option, selected = option.id == selectedId, onClick = { onSelect(option.id) })
            }
        }
    }
}

/**
 * Like [EntityPickerSheet] but several rows can be checked; nothing is applied until the apply button is pressed. Used by the tags
 * filter, the only multiple choice.
 */
@Composable
fun EntityMultiPickerSheet(
    title: String,
    options: List<EntityOption>,
    selectedIds: Set<String>,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    emptyText: String,
) {
    FlatBottomSheet(title = title, onDismiss = onDismiss) {
        var selection by rememberSaveable { mutableStateOf(selectedIds.toList()) }
        var query by rememberSaveable { mutableStateOf("") }
        if (options.isEmpty()) {
            Text(text = emptyText, style = MaterialTheme.typography.bodyMedium)
        } else {
            if (options.size > ENTITY_SELECT_SEARCH_THRESHOLD) {
                FormTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = stringResource(Res.string.entity_select_search),
                    imeAction = ImeAction.Done,
                )
            }
            val visible = filterEntityOptions(options, query)
            Column {
                LazyColumn(modifier = Modifier.heightIn(max = PickerMaxHeight)) {
                    items(visible, key = { it.id ?: "" }) { option ->
                        OptionRow(
                            option,
                            selected = option.id in selection,
                            onClick = {
                                val id = option.id ?: return@OptionRow
                                selection = if (id in selection) selection - id else selection + id
                            },
                        )
                    }
                }
            }
            PrimaryButton(
                text = stringResource(Res.string.action_apply),
                onClick = { onApply(selection.toSet()) },
            )
        }
    }
}
