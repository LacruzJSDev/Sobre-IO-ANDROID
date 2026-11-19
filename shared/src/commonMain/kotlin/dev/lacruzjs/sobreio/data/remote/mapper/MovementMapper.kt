package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.MovementSearchDto
import dev.lacruzjs.sobreio.data.remote.dto.TagDto
import dev.lacruzjs.sobreio.data.remote.dto.TagUsageDto
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.domain.model.Tag
import dev.lacruzjs.sobreio.domain.model.TagUsage
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun MovementSearchDto.toDomain() = Movement(
    id = id,
    // An unknown source reads as a transaction, the common one
    source = if (source == "account_transfer") MovementSource.AccountTransfer else MovementSource.Transaction,
    accountId = accountId,
    counterAccountId = counterAccountId,
    envelopeId = envelopeId,
    kind = kind.toMovementKind(),
    amountCents = amount,
    date = LocalDate.parse(date),
    notes = notes?.takeIf { it.isNotBlank() },
    createdBy = createdBy,
    createdAt = createdAt,
    tagIds = tagIds,
)

internal fun TagDto.toDomain() = Tag(id = id, groupId = groupId, name = name, color = color)

internal fun TagUsageDto.toDomain() = TagUsage(id = tagId, name = name, color = color, archivedAt = archivedAt, movements = movements)

/**
 * The arguments of `search_movements` (and, without the page, of `search_movements_count`) for a filter. A filter that is
 * not set is left out, so the function's defaults apply.
 */
internal fun searchParams(groupId: String, filter: MovementFilter, limit: Int? = null, offset: Int? = null): JsonObject =
    buildJsonObject {
        put("p_group_id", groupId)
        filter.accountId?.let { put("p_account_id", it) }
        filter.envelopeId?.let { put("p_envelope_id", it) }
        filter.type?.let { put("p_kinds", JsonArray(listOf(JsonPrimitive(it.wireName)))) }
        filter.from?.let { put("p_from", it.toString()) }
        filter.to?.let { put("p_to", it.toString()) }
        if (filter.tagIds.isNotEmpty()) put("p_tag_ids", JsonArray(filter.tagIds.sorted().map { JsonPrimitive(it) }))
        filter.createdBy?.let { put("p_created_by", it) }
        filter.query.trim().takeIf { it.isNotEmpty() }?.let { put("p_query", it) }
        limit?.let { put("p_limit", it) }
        offset?.let { put("p_offset", it) }
    }
