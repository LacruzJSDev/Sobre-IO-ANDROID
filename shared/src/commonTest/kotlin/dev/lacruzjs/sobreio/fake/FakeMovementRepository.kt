package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.MovementRepository
import dev.lacruzjs.sobreio.domain.model.DateContext
import dev.lacruzjs.sobreio.domain.model.Movement
import dev.lacruzjs.sobreio.domain.model.MovementFilter
import dev.lacruzjs.sobreio.domain.model.MovementPage
import dev.lacruzjs.sobreio.domain.model.PeriodRange
import kotlinx.datetime.LocalDate

/** One call of [FakeMovementRepository.search], to check what the ViewModel asked for. */
data class SearchCall(val filter: MovementFilter, val limit: Int, val offset: Int, val withTotal: Boolean)

/**
 * Serves [all] in pages of the requested size, whatever the filter (the database filters, not the app), and records every
 * call. Voided movements leave [all].
 */
class FakeMovementRepository(var all: List<Movement> = emptyList()) : MovementRepository {

    val searches = mutableListOf<SearchCall>()
    val voided = mutableListOf<String>()

    var today = LocalDate(2026, 11, 14)
    var context = DateContext(
        today = today,
        currentPeriod = PeriodRange(LocalDate(2026, 11, 1), LocalDate(2026, 11, 30)),
        previousPeriod = PeriodRange(LocalDate(2026, 10, 1), LocalDate(2026, 10, 31)),
    )
    var searchResult: Result<Unit> = Result.success(Unit)
    var voidResult: Result<Unit> = Result.success(Unit)
    var contextResult: Result<DateContext>? = null

    override suspend fun search(
        groupId: String,
        filter: MovementFilter,
        limit: Int,
        offset: Int,
        withTotal: Boolean,
    ): Result<MovementPage> {
        searches += SearchCall(filter, limit, offset, withTotal)
        searchResult.onFailure { return Result.failure(it) }
        return Result.success(MovementPage(all.drop(offset).take(limit), if (withTotal) all.size.toLong() else 0L))
    }

    override suspend fun voidMovement(movement: Movement): Result<Unit> {
        voided += movement.id
        voidResult.onSuccess { all = all.filter { it.id != movement.id } }
        return voidResult
    }

    override suspend fun getDateContext(groupId: String): Result<DateContext> = contextResult ?: Result.success(context)
}
