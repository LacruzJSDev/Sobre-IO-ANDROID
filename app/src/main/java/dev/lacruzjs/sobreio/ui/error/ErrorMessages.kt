package dev.lacruzjs.sobreio.ui.error

import androidx.annotation.StringRes
import dev.lacruzjs.sobreio.R
import dev.lacruzjs.sobreio.domain.error.AppError

/** The only place that turns an [AppError] into text for the user. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    is AppError.BusinessRule -> businessRuleMessageRes(code)
    AppError.InvalidCredentials -> R.string.error_invalid_credentials
    AppError.EmailNotConfirmed -> R.string.error_email_not_confirmed
    AppError.EmailAlreadyRegistered -> R.string.error_email_already_registered
    AppError.WeakPassword -> R.string.error_weak_password
    AppError.InvalidCode -> R.string.error_invalid_code
    AppError.TooManyRequests -> R.string.error_too_many_requests
    is AppError.Unknown -> R.string.error_unknown
}

@StringRes
private fun businessRuleMessageRes(code: String): Int = when (code) {
    "FZ001" -> R.string.error_fz001_group_read_only
    "FZ002" -> R.string.error_fz002_archived
    "FZ003" -> R.string.error_fz003_future_date
    "FZ005" -> R.string.error_fz005_voided
    "FZ006" -> R.string.error_fz006_no_delete
    "FZ007" -> R.string.error_fz007_unassigned_rule
    "FZ008" -> R.string.error_fz008_archive_not_empty
    "FZ009" -> R.string.error_fz009_last_owner
    "FZ011" -> R.string.error_fz011_invalid_invitation
    "FZ012" -> R.string.error_fz012_occurrence
    "FZ013" -> R.string.error_fz013_automatic_movement
    "FZ014" -> R.string.error_fz014_immutable
    "FZ015" -> R.string.error_fz015_group_transfer
    "FZ016" -> R.string.error_fz016_permission
    "FZ017" -> R.string.error_fz017_account_scope
    "FZ018" -> R.string.error_fz018_linked_commitment
    "FZ019" -> R.string.error_fz019_cover
    "FZ020" -> R.string.error_fz020_purge
    // FZ004 and FZ010 are reserved and never raised
    else -> R.string.error_unknown
}
