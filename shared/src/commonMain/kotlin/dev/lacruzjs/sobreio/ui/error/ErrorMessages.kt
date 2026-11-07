package dev.lacruzjs.sobreio.ui.error

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.error_email_already_registered
import dev.lacruzjs.sobreio.generated.resources.error_email_not_confirmed
import dev.lacruzjs.sobreio.generated.resources.error_fz001_group_read_only
import dev.lacruzjs.sobreio.generated.resources.error_fz002_archived
import dev.lacruzjs.sobreio.generated.resources.error_fz003_future_date
import dev.lacruzjs.sobreio.generated.resources.error_fz005_voided
import dev.lacruzjs.sobreio.generated.resources.error_fz006_no_delete
import dev.lacruzjs.sobreio.generated.resources.error_fz007_unassigned_rule
import dev.lacruzjs.sobreio.generated.resources.error_fz008_archive_not_empty
import dev.lacruzjs.sobreio.generated.resources.error_fz009_last_owner
import dev.lacruzjs.sobreio.generated.resources.error_fz011_invalid_invitation
import dev.lacruzjs.sobreio.generated.resources.error_fz012_occurrence
import dev.lacruzjs.sobreio.generated.resources.error_fz013_automatic_movement
import dev.lacruzjs.sobreio.generated.resources.error_fz014_immutable
import dev.lacruzjs.sobreio.generated.resources.error_fz015_group_transfer
import dev.lacruzjs.sobreio.generated.resources.error_fz016_permission
import dev.lacruzjs.sobreio.generated.resources.error_fz017_account_scope
import dev.lacruzjs.sobreio.generated.resources.error_fz018_linked_commitment
import dev.lacruzjs.sobreio.generated.resources.error_fz019_cover
import dev.lacruzjs.sobreio.generated.resources.error_fz020_purge
import dev.lacruzjs.sobreio.generated.resources.error_invalid_code
import dev.lacruzjs.sobreio.generated.resources.error_invalid_credentials
import dev.lacruzjs.sobreio.generated.resources.error_network
import dev.lacruzjs.sobreio.generated.resources.error_too_many_requests
import dev.lacruzjs.sobreio.generated.resources.error_unknown
import dev.lacruzjs.sobreio.generated.resources.error_weak_password
import org.jetbrains.compose.resources.StringResource

/** The only place that turns an [AppError] into text for the user. */
fun AppError.messageRes(): StringResource = when (this) {
    AppError.Network -> Res.string.error_network
    is AppError.BusinessRule -> businessRuleMessageRes(code)
    AppError.InvalidCredentials -> Res.string.error_invalid_credentials
    AppError.EmailNotConfirmed -> Res.string.error_email_not_confirmed
    AppError.EmailAlreadyRegistered -> Res.string.error_email_already_registered
    AppError.WeakPassword -> Res.string.error_weak_password
    AppError.InvalidCode -> Res.string.error_invalid_code
    AppError.TooManyRequests -> Res.string.error_too_many_requests
    is AppError.Unknown -> Res.string.error_unknown
}

private fun businessRuleMessageRes(code: String): StringResource = when (code) {
    "FZ001" -> Res.string.error_fz001_group_read_only
    "FZ002" -> Res.string.error_fz002_archived
    "FZ003" -> Res.string.error_fz003_future_date
    "FZ005" -> Res.string.error_fz005_voided
    "FZ006" -> Res.string.error_fz006_no_delete
    "FZ007" -> Res.string.error_fz007_unassigned_rule
    "FZ008" -> Res.string.error_fz008_archive_not_empty
    "FZ009" -> Res.string.error_fz009_last_owner
    "FZ011" -> Res.string.error_fz011_invalid_invitation
    "FZ012" -> Res.string.error_fz012_occurrence
    "FZ013" -> Res.string.error_fz013_automatic_movement
    "FZ014" -> Res.string.error_fz014_immutable
    "FZ015" -> Res.string.error_fz015_group_transfer
    "FZ016" -> Res.string.error_fz016_permission
    "FZ017" -> Res.string.error_fz017_account_scope
    "FZ018" -> Res.string.error_fz018_linked_commitment
    "FZ019" -> Res.string.error_fz019_cover
    "FZ020" -> Res.string.error_fz020_purge
    // FZ004 and FZ010 are reserved and never raised
    else -> Res.string.error_unknown
}
