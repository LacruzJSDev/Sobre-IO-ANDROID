package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.CommitmentDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentLinkDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentOccurrenceDto
import dev.lacruzjs.sobreio.data.remote.dto.CommitmentVersionDto
import dev.lacruzjs.sobreio.domain.model.Commitment
import dev.lacruzjs.sobreio.domain.model.CommitmentKind
import dev.lacruzjs.sobreio.domain.model.CommitmentLink
import dev.lacruzjs.sobreio.domain.model.CommitmentOccurrence
import dev.lacruzjs.sobreio.domain.model.CommitmentVersion
import dev.lacruzjs.sobreio.domain.model.FrequencyUnit
import dev.lacruzjs.sobreio.domain.model.OccurrenceStatus
import kotlinx.datetime.LocalDate

internal fun CommitmentVersionDto.toDomain() = CommitmentVersion(
    id = id,
    commitmentId = commitmentId,
    effectiveFrom = LocalDate.parse(effectiveFrom),
    effectiveTo = effectiveTo?.let(LocalDate::parse),
    kind = kind.toCommitmentKind(),
    amountCents = amount,
    accountId = accountId,
    toAccountId = toAccountId,
    envelopeId = envelopeId,
    firstDueDate = LocalDate.parse(firstDueDate),
    isRecurring = isRecurring,
    everyN = everyN,
    unit = unit?.toFrequencyUnit(),
    anchorDay = anchorDay,
    endDate = endDate?.let(LocalDate::parse),
    funded = funded,
    autoRegister = autoRegister,
    notes = notes,
)

/** [versions] in any order: the commitment keeps them newest first, which is how the history is shown. */
internal fun CommitmentDto.toDomain(versions: List<CommitmentVersionDto>) = Commitment(
    id = id,
    groupId = groupId,
    name = name,
    notes = notes,
    archivedAt = archivedAt,
    versions = versions.map { it.toDomain() }.sortedByDescending { it.effectiveFrom },
)

internal fun CommitmentLinkDto.toDomain() = CommitmentLink(
    id = id,
    outGroupId = outGroupId,
    inGroupId = inGroupId,
    outCommitmentId = outCommitmentId,
    inCommitmentId = inCommitmentId,
)

/** A skipped occurrence has no movement; a registered one takes the amount and date of its transaction or transfer. */
internal fun CommitmentOccurrenceDto.toDomain(): CommitmentOccurrence {
    val fact = transactions ?: transfer
    return CommitmentOccurrence(
        dueDate = LocalDate.parse(dueDate),
        status = if (status == "skipped") OccurrenceStatus.Skipped else OccurrenceStatus.Materialized,
        amountCents = fact?.amount,
        date = fact?.date?.let(LocalDate::parse),
        fromAccountId = transfer?.fromAccountId,
    )
}

/** An unknown value (a kind added by a newer database) is read as an expense rather than failing the list. */
internal fun String?.toCommitmentKind(): CommitmentKind = when (this) {
    "income" -> CommitmentKind.Income
    "transfer" -> CommitmentKind.Transfer
    "group_transfer_out" -> CommitmentKind.GroupTransferOut
    "group_transfer_in" -> CommitmentKind.GroupTransferIn
    else -> CommitmentKind.Expense
}

/** The value of `commitment_kind_enum`. */
internal fun CommitmentKind.toDbValue(): String = when (this) {
    CommitmentKind.Expense -> "expense"
    CommitmentKind.Income -> "income"
    CommitmentKind.Transfer -> "transfer"
    CommitmentKind.GroupTransferOut -> "group_transfer_out"
    CommitmentKind.GroupTransferIn -> "group_transfer_in"
}

internal fun String.toFrequencyUnit(): FrequencyUnit = when (this) {
    "day" -> FrequencyUnit.Day
    "week" -> FrequencyUnit.Week
    "year" -> FrequencyUnit.Year
    else -> FrequencyUnit.Month
}

/** The value of `frequency_unit_enum`. */
internal fun FrequencyUnit.toDbValue(): String = when (this) {
    FrequencyUnit.Day -> "day"
    FrequencyUnit.Week -> "week"
    FrequencyUnit.Month -> "month"
    FrequencyUnit.Year -> "year"
}
