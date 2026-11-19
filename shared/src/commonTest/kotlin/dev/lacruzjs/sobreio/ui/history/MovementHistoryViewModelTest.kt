package dev.lacruzjs.sobreio.ui.history

import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuditAction
import dev.lacruzjs.sobreio.domain.model.AuditEntry
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.domain.model.MovementSource
import dev.lacruzjs.sobreio.fake.FakeAuditRepository
import dev.lacruzjs.sobreio.fake.FakeMemberRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class MovementHistoryViewModelTest : MainDispatcherTest() {

    private val created = AuditEntry(AuditAction.Created, "u1", Instant.parse("2026-11-01T09:00:00Z"), emptyList())
    private val changed = AuditEntry(AuditAction.Changed, "u2", Instant.parse("2026-11-03T18:30:00Z"), listOf("amount"))
    private val gone = AuditEntry(AuditAction.Changed, "u9", Instant.parse("2026-11-04T08:00:00Z"), listOf("date"))
    private val daily = AuditEntry(AuditAction.Changed, null, Instant.parse("2026-11-05T00:05:00Z"), listOf("notes"))

    private val audit = FakeAuditRepository(listOf(created, changed, gone, daily))
    private val members = FakeMemberRepository(listOf(Member("u1", "Ana", GroupRole.Owner), Member("u2", "Luis", GroupRole.Member)))

    private fun viewModel(source: String? = MovementSource.Transaction.name) =
        MovementHistoryViewModel("g1", "m1", source, audit, members)

    @Test
    fun aTransactionReadsItsRowsWithTheNamesOfWhoMadeThem() {
        val lines = viewModel().lines.value

        assertEquals(listOf("getHistory(transactions, m1)"), audit.calls)
        assertEquals(listOf(created, changed, gone, daily), lines.map { it.entry })
        // Someone who left and the daily task have no name: the section words them itself
        assertEquals(listOf("Ana", "Luis", null, null), lines.map { it.authorName })
    }

    @Test
    fun anAccountTransferReadsItsOwnTable() {
        viewModel(MovementSource.AccountTransfer.name)

        assertEquals(listOf("getHistory(account_transfers, m1)"), audit.calls)
    }

    @Test
    fun withoutTheMembersTheLinesStillShowWithNoNames() {
        members.result = Result.failure(AppError.Network)

        val lines = viewModel().lines.value

        assertEquals(4, lines.size)
        assertTrue(lines.all { it.authorName == null })
    }

    @Test
    fun aFailedReadLeavesTheSectionEmpty() {
        audit.result = Result.failure(AppError.Network)

        assertTrue(viewModel().lines.value.isEmpty())
    }
}
