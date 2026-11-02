package dev.lacruzjs.sobreio.data.remote

import androidx.annotation.StringRes
import dev.lacruzjs.sobreio.R
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import java.io.IOException

/**
 * Turns any error from the backend into a Spanish message resource. The database reports business-rule
 * violations as SQLSTATE class FZ (D-23); its exception text is never shown to the user.
 */
object ErrorMapper {

    @StringRes
    fun messageFor(error: Throwable): Int = when (error) {
        is PostgrestRestException -> messageForCode(error.code)
        is HttpRequestException, is IOException -> R.string.error_network
        else -> R.string.error_unknown
    }

    @StringRes
    fun messageForCode(code: String?): Int = when (code) {
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
}
