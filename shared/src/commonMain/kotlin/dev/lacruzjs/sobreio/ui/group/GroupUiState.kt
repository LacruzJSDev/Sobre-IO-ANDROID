package dev.lacruzjs.sobreio.ui.group

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.Invitation

data class GroupUiState(
    val group: Group? = null,
    val isLoading: Boolean = true,
    val error: AppError? = null,
    /** Set by the Route, which saves it across recreation. */
    val selectedTab: GroupTab = GroupTab.Group,
    /** The group does not load (it is gone or the user left it): the Route goes back to Groups. */
    val navigateBack: Boolean = false,
    /** The "coming soon" sheet of the "+" in the Accounts, Envelopes and Movements headers is open. */
    val newItemSheetOpen: Boolean = false,
    /** The invitation sheet is open. [invitation] arrives when it is created; [inviteError] if it fails. */
    val inviteSheetOpen: Boolean = false,
    val isInviting: Boolean = false,
    val invitation: Invitation? = null,
    val inviteError: AppError? = null,
)
