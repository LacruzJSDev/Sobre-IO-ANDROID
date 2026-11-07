package dev.lacruzjs.sobreio.data.remote.mapper

import dev.lacruzjs.sobreio.domain.model.User
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** `name` is what sign-up stores (D-43); Google sends `full_name`. */
fun UserInfo.toDomain(): User = User(
    id = id,
    email = email,
    displayName = userMetadata?.displayName(),
)

internal fun JsonObject.displayName(): String? =
    listOf("name", "full_name")
        .firstNotNullOfOrNull { key -> (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } }
