package dev.lacruzjs.sobreio.ui.export

import dev.lacruzjs.sobreio.data.repository.ExportRepository
import dev.lacruzjs.sobreio.domain.export.ExportPeriod
import dev.lacruzjs.sobreio.domain.export.ExportRecordType
import dev.lacruzjs.sobreio.domain.export.ExportRow
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.fake.FakeGroupRepository
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExportViewModelTest : MainDispatcherTest() {

    private val current = ExportPeriod("p2", LocalDate(2026, 10, 25), LocalDate(2026, 11, 24))
    private val previous = ExportPeriod("p1", LocalDate(2026, 9, 25), LocalDate(2026, 10, 24))
    private val row = ExportRow(ExportRecordType.Transaction, LocalDate(2026, 11, 3), "expense", -4_510, "Bank", null, "Food", null, null, null, "Ana")

    private val exports = object : ExportRepository {
        val exported = mutableListOf<String>()
        override suspend fun getPeriods(groupId: String, limit: Int) = Result.success(listOf(current, previous))
        override suspend fun exportPeriod(groupId: String, periodId: String): Result<List<ExportRow>> {
            exported += periodId
            return Result.success(listOf(row))
        }
    }

    private fun vm() = ExportViewModel("g1", exports, FakeGroupRepository(listOf(Group("g1", "Casa", "EUR", GroupRole.Owner))))

    @Test
    fun openingReadsThePeriodsAndChoosesTheNewest() {
        val vm = vm()
        vm.onEvent(ExportEvent.Opened)

        assertTrue(vm.state.value.open)
        assertEquals(listOf("p2", "p1"), vm.state.value.periods.map { it.id })
        assertEquals("p2", vm.state.value.selectedPeriodId)
    }

    @Test
    fun exportingHandsTheRowsAndANameToTheRouteUntilTheFileIsHandled() {
        val vm = vm()
        vm.onEvent(ExportEvent.Opened)
        vm.onEvent(ExportEvent.PeriodSelected("p1"))
        vm.onEvent(ExportEvent.ExportClicked)

        assertEquals(listOf("p1"), exports.exported)
        assertEquals(listOf(row), vm.state.value.pendingRows)
        assertEquals("sobreio-casa-2026-09-25_2026-10-24.csv", vm.state.value.pendingFileName)
        assertFalse(vm.state.value.canExport)

        vm.onEvent(ExportEvent.FileHandled(SaveResult.Saved))

        assertNull(vm.state.value.pendingRows)
        assertTrue(vm.state.value.saved)
        assertTrue(vm.state.value.canExport)
    }
}
