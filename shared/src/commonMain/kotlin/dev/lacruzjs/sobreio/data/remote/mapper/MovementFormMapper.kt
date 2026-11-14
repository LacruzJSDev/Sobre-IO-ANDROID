package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.TransactionRowDto
import dev.lacruzjs.sobreio.data.remote.dto.TransferRowDto
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.CoverItem
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.domain.model.StoredTransaction
import dev.lacruzjs.sobreio.domain.model.StoredTransfer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.abs

/** Only expenses and incomes are edited here; a voided row is frozen, so it reads as gone. */
internal fun TransactionRowDto.toDomain(): StoredTransaction {
    val entryKind = when (kind) {
        "expense" -> EntryKind.Expense
        "income" -> EntryKind.Income
        else -> throw AppError.NotFound
    }
    if (voidedAt != null) throw AppError.NotFound
    return StoredTransaction(
        id = id,
        groupId = groupId,
        accountId = accountId,
        envelopeId = envelopeId,
        kind = entryKind,
        amountCents = abs(amount),
        date = LocalDate.parse(date),
        notes = notes?.takeIf { it.isNotBlank() },
    )
}

internal fun TransferRowDto.toDomain(): StoredTransfer {
    if (voidedAt != null) throw AppError.NotFound
    return StoredTransfer(
        id = id,
        groupId = groupId,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        amountCents = amount,
        date = LocalDate.parse(date),
        notes = notes?.takeIf { it.isNotBlank() },
    )
}

/** The signed amount `transactions` stores: an expense is negative. */
internal fun EntryKind.signed(amountCents: Long): Long = if (this == EntryKind.Expense) -amountCents else amountCents

/** `p_cover` as the database reads it: `[{"envelope_id": ..., "amount": ...}]`, or null (not sent) when there is none. */
internal fun List<CoverItem>.toCoverJson(): JsonElement? =
    if (isEmpty()) {
        null
    } else {
        JsonArray(
            map {
                buildJsonObject {
                    put("envelope_id", it.envelopeId)
                    put("amount", it.amountCents)
                }
            },
        )
    }
