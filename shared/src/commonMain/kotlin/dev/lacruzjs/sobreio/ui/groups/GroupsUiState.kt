package dev.lacruzjs.sobreio.ui.groups

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import org.jetbrains.compose.resources.StringResource

/** Currencies a group can be created in. The choice is final, so the list stays short and fixed. */
val GroupCurrencies = listOf("EUR", "USD", "GBP", "CHF")

const val MAX_GROUP_NAME_LENGTH = 100

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    /** True until the first load ends; later refreshes keep showing the list. */
    val isLoading: Boolean = true,
    /** Failure of loading the list. */
    val error: AppError? = null,
    val sheet: GroupsSheet = GroupsSheet.None,
    val name: String = "",
    val nameError: StringResource? = null,
    val currency: String = GroupCurrencies.first(),
    val code: String = "",
    val codeError: StringResource? = null,
    val isSubmitting: Boolean = false,
    /** Failure of creating or joining, shown inside the open sheet. */
    val submitError: AppError? = null,
    val isSigningOut: Boolean = false,
    /** Failure of signing out, shown above the bottom section. */
    val signOutError: AppError? = null,
    /** Id of the group to open after creating or joining one. The Route consumes it. */
    val navigateToGroup: String? = null,
)
