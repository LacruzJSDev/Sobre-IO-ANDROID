package dev.lacruzjs.sobreio.ui.groups

import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.ui.auth.validateRequired
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource

/** Currencies a group can be created in. The choice is final, so the list stays short and fixed. */
val GroupCurrencies = listOf("EUR", "USD", "GBP", "CHF")

const val MAX_GROUP_NAME_LENGTH = 100
const val MIN_PERIOD_START_DAY = 1
const val MAX_PERIOD_START_DAY = 28

/**
 * The fields of the group sheet, shared by the Groups screen (create and edit) and the group settings (edit). The
 * colour is the `#RRGGBB` the database stores, so the state holds no Compose types. [editing] is the group being
 * edited: its currency is shown read-only because it never changes.
 */
data class GroupFormState(
    val name: String = "",
    val nameError: StringResource? = null,
    val currency: String = GroupCurrencies.first(),
    val color: String? = null,
    val icon: String? = null,
    val periodStartDay: Int = MIN_PERIOD_START_DAY,
    val timezone: String = TimeZone.currentSystemDefault().id,
    val editing: Group? = null,
) {
    val isEditing: Boolean get() = editing != null

    /** This state with [nameError] set when the trimmed name is blank or too long. */
    fun validated(): GroupFormState = copy(nameError = validateGroupName(name.trim()))

    companion object {
        fun forNew() = GroupFormState()

        fun forEdit(group: Group) = GroupFormState(
            name = group.name,
            currency = group.currencyCode,
            color = group.color,
            icon = group.icon,
            periodStartDay = group.periodStartDay,
            timezone = group.timezone,
            editing = group,
        )
    }
}

internal fun validateGroupName(name: String): StringResource? = when {
    name.isEmpty() -> validateRequired(name)
    name.length > MAX_GROUP_NAME_LENGTH -> Res.string.validation_name_too_long
    else -> null
}
