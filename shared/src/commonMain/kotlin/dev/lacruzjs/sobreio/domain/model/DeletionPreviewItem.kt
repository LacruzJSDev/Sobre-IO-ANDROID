package dev.lacruzjs.sobreio.domain.model

/** What deleting the account does to one group the user belongs to (`account_deletion_preview`, D-51). */
enum class DeletionOutcome {
    /** The user is the only active member: the group is deleted for good. */
    Delete,

    /** The user leaves it; their past movements stay, signed as a deleted user. */
    Leave,

    /** The user is the only owner of a group with other members: another owner must be named first. */
    Blocked,
}

data class DeletionPreviewItem(
    val groupId: String,
    val name: String,
    val color: String?,
    val icon: String?,
    val outcome: DeletionOutcome,
)
