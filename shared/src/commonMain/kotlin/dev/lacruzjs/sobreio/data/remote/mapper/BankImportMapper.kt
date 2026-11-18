package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.BankImportOverviewDto
import dev.lacruzjs.sobreio.data.remote.dto.BankRowPreviewDto
import dev.lacruzjs.sobreio.domain.bankimport.StatementRow
import dev.lacruzjs.sobreio.domain.model.BankImportSummary
import dev.lacruzjs.sobreio.domain.model.BankRowAction
import dev.lacruzjs.sobreio.domain.model.BankRowChoice
import dev.lacruzjs.sobreio.domain.model.BankRowPreview
import dev.lacruzjs.sobreio.domain.model.BankRowStatus
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.ExistingCandidate
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.PendingCandidate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

private fun String.toSource() = if (this == "account_transfer") MovementSource.AccountTransfer else MovementSource.Transaction

private fun MovementSource.wireName() = if (this == MovementSource.AccountTransfer) "account_transfer" else "transaction"

internal fun BankRowPreviewDto.toDomain() = BankRowPreview(
    index = idx,
    status = when (status) {
        "imported" -> BankRowStatus.Imported
        "future" -> BankRowStatus.Future
        "before_opening" -> BankRowStatus.BeforeOpening
        else -> BankRowStatus.New
    },
    suggestedAction = BankRowAction.entries.firstOrNull { it.wireName == suggestedAction } ?: BankRowAction.Ignore,
    commitmentId = commitmentId,
    dueDate = dueDate?.let(LocalDate::parse),
    matchSource = matchSource?.toSource(),
    matchId = matchId,
    envelopeId = envelopeId,
    pending = pending.map { PendingCandidate(it.commitmentId, it.name, LocalDate.parse(it.dueDate), it.amount) },
    existing = existing.map {
        ExistingCandidate(it.source.toSource(), it.id, LocalDate.parse(it.date), it.amount, it.notes, it.kind.toMovementKind())
    },
)

internal fun BankImportOverviewDto.toDomain() = BankImportSummary(
    id = id,
    accountId = accountId,
    fileName = fileName,
    tagId = tagId,
    createdDay = runCatching { Instant.parse(createdAt).toLocalDateTime(TimeZone.currentSystemDefault()).date }.getOrNull(),
    rowsTotal = rowsTotal,
    created = created,
    confirmed = confirmed,
    linked = linked,
    ignored = ignored,
    firstDate = firstDate?.let(LocalDate::parse),
    lastDate = lastDate?.let(LocalDate::parse),
    isVoided = voidedAt != null,
)

/** The rows as `bank_import_preview` reads them. */
internal fun previewRowsJson(rows: List<StatementRow>): JsonArray = buildJsonArray {
    rows.forEach { row ->
        add(
            buildJsonObject {
                put("ref", row.ref)
                put("date", row.date.toString())
                put("amount", row.amountCents)
                put("description", row.description)
            },
        )
    }
}

/** The decisions as `apply_bank_import` reads them. */
internal fun choiceRowsJson(choices: List<BankRowChoice>): JsonArray = buildJsonArray {
    choices.forEach { choice ->
        add(
            buildJsonObject {
                put("ref", choice.ref)
                put("date", choice.date.toString())
                put("amount", choice.amountCents)
                put("description", choice.description)
                put("action", choice.action.wireName)
                when (choice.action) {
                    BankRowAction.Create -> {
                        put(
                            "kind",
                            when (choice.kind) {
                                EntryKind.Transfer -> "transfer"
                                else -> if (choice.amountCents < 0) "expense" else "income"
                            },
                        )
                        choice.envelopeId?.let { put("envelope_id", it) }
                        choice.otherAccountId?.let { put("other_account_id", it) }
                        choice.notes?.let { put("notes", it) }
                    }
                    BankRowAction.Confirm -> {
                        put("commitment_id", choice.commitmentId)
                        put("due_date", choice.dueDate?.toString())
                    }
                    BankRowAction.Link -> {
                        put("source", choice.linkSource?.wireName())
                        put("target_id", choice.linkId)
                    }
                    BankRowAction.Ignore -> Unit
                }
            },
        )
    }
}
