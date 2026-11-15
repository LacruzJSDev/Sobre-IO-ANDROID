package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import dev.lacruzjs.sobreio.domain.model.MovementPage

/**
 * The group's ledger as the Movements screen reads it. Filtering, ordering and paging are done by the database
 * (`search_movements`); nothing is summed here. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s.
 */
interface MovementRepository {

    /**
     * One page of the rows that match [filter], newest first, starting at [offset]. With [withTotal] it also asks
     * `search_movements_count` for how many rows match in all (the first page does); otherwise [MovementPage.total] is 0.
     * With no account in the filter an account transfer appears once; with one, from that account's side.
     */
    suspend fun search(
        groupId: String,
        filter: MovementFilter,
        limit: Int,
        offset: Int,
        withTotal: Boolean,
    ): Result<MovementPage>

    /**
     * Voids the movement (`voided_at`) in the table it lives in. The ledger is never deleted. Fails with FZ005 when it
     * is already voided and with FZ016 when row level security hides it.
     */
    suspend fun voidMovement(movement: Movement): Result<Unit>

    /** The group's today and its current and previous periods, for the date presets of the Dates chip. */
    suspend fun getDateContext(groupId: String): Result<DateContext>
}
