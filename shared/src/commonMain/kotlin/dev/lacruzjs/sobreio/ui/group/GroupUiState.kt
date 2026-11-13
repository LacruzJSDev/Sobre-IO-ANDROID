package dev.lacruzjs.sobreio.ui.group

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.Group

data class GroupUiState(
    val group: Group? = null,
    val isLoading: Boolean = true,
    val error: AppError? = null,
    /** Set by the Route, which saves it across recreation. */
    val selectedTab: GroupTab = GroupTab.Group,
    /** The group does not load (it is gone or the user left it): the Route goes back to Groups. */
    val navigateBack: Boolean = false,
)
