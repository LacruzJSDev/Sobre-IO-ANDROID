package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.DeletionPreviewDto
import dev.lacruzjs.sobreio.domain.model.DeletionOutcome
import dev.lacruzjs.sobreio.domain.model.DeletionPreviewItem

/** An unknown outcome is read as the most cautious one, so the app never promises less than the database may do. */
internal fun String.toDeletionOutcome(): DeletionOutcome = when (this) {
    "delete" -> DeletionOutcome.Delete
    "leave" -> DeletionOutcome.Leave
    else -> DeletionOutcome.Blocked
}

internal fun DeletionPreviewDto.toDomain(): DeletionPreviewItem = DeletionPreviewItem(
    groupId = groupId,
    name = name,
    color = color,
    icon = icon,
    outcome = outcome.toDeletionOutcome(),
)
