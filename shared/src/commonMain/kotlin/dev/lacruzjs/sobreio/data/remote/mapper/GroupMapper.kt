package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.data.remote.dto.GroupDto
import dev.lacruzjs.sobreio.domain.model.Group

internal fun GroupDto.toDomain(): Group = Group(id = id, name = name, currencyCode = currency)
