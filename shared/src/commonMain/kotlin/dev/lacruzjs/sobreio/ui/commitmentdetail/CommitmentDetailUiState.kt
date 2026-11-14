package dev.lacruzjs.sobreio.ui.commitmentdetail

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import kotlinx.datetime.LocalDate

/**
 * A resolved due item as this account sees it. [signedCents] is the amount of the movement it registered, with the sign
 * for the account (null when it was skipped); [date] is the day the movement is dated, only set when it differs from [dueDate].
 */
data class OccurrenceRow(
    val dueDate: LocalDate,
    val skipped: Boolean,
    val signedCents: Long?,
    val date: LocalDate?,
)

/**
 * One version of the commitment. [lastDay] is the last day it was in force (the day before `effective_to`, which is
 * exclusive), null while it is open. [accountName] is set only when the account differs from the previous version;
 * [envelopeName] / [unassigned] describe the envelope of an expense.
 */
data class VersionRow(
    val version: CommitmentVersion,
    val signedCents: Long,
    val lastDay: LocalDate?,
    val accountName: String? = null,
    val envelopeName: String? = null,
    val unassigned: Boolean = false,
)

data class CommitmentDetailUiState(
    val name: String = "",
    /** The name of the account of the version shown. */
    val accountName: String = "",
    /** The version shown: the one in force for the account (or the newest). */
    val version: CommitmentVersion? = null,
    val signedCents: Long = 0,
    val isArchived: Boolean = false,
    val currencyCode: String = "EUR",
    /** The pencil: owner or admin, live group, live commitment and a kind that can be edited from this account. */
    val canEdit: Boolean = false,
    /** Resolved due items, newest first. */
    val occurrences: List<OccurrenceRow> = emptyList(),
    /** Versions, newest first. */
    val versions: List<VersionRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: AppError? = null,
)
