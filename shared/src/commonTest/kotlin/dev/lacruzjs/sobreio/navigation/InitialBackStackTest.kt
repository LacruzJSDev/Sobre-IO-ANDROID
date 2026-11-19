package dev.lacruzjs.sobreio.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class InitialBackStackTest {

    @Test
    fun withoutLastGroupItStartsOnGroups() {
        assertEquals(listOf<NavKey>(Groups), initialBackStack(null))
    }

    @Test
    fun withLastGroupItIsTheOnlyRoot() {
        assertEquals(listOf<NavKey>(Group("g1")), initialBackStack("g1"))
    }

    @Test
    fun backFromTheGroupRootGoesNowhere() {
        val stack = mutableListOf<NavKey>(Group("g1"))
        stack.popToPrevious()
        assertEquals(listOf<NavKey>(Group("g1")), stack)
    }

    @Test
    fun theMenuLeavesOnlyGroupsWithNoWayBack() {
        val stack = mutableListOf<NavKey>(Group("g1"))
        stack.showOnlyGroups()
        assertEquals(listOf<NavKey>(Groups), stack)
        stack.popToPrevious()
        assertEquals(listOf<NavKey>(Groups), stack)
    }

    @Test
    fun openingAGroupReplacesEverything() {
        val stack = mutableListOf<NavKey>(Group("g1"), Groups, GroupForm(null))
        stack.openGroup("g2")
        assertEquals(listOf<NavKey>(Group("g2")), stack)
    }

    @Test
    fun aGoneGroupLeavesOnlyGroups() {
        val stack = mutableListOf<NavKey>(Group("g1"), GroupSettings("g1"))
        stack.showOnlyGroups()
        assertEquals(listOf<NavKey>(Groups), stack)
    }

    @Test
    fun openingADetailPushesItAboveTheList() {
        val stack = mutableListOf<NavKey>(Group("g1"))
        stack.openDetail(AccountDetail("g1", "a1"))
        assertEquals(listOf<NavKey>(Group("g1"), AccountDetail("g1", "a1")), stack)
    }

    @Test
    fun openingAnotherDetailReplacesTheOpenOne() {
        val stack = mutableListOf<NavKey>(Group("g1"), AccountDetail("g1", "a1"))
        stack.openDetail(AccountDetail("g1", "a2"))
        assertEquals(listOf<NavKey>(Group("g1"), AccountDetail("g1", "a2")), stack)
        stack.openDetail(EnvelopeDetail("g1", "e1"))
        assertEquals(listOf<NavKey>(Group("g1"), EnvelopeDetail("g1", "e1")), stack)
    }

    @Test
    fun openingADetailAlsoDropsTheExtraPanesAboveTheOpenOne() {
        val stack = mutableListOf<NavKey>(
            Group("g1"),
            AccountDetail("g1", "a1"),
            CommitmentDetail("g1", "a1", "c1"),
            AccountMovements("g1", "a1"),
        )
        stack.openDetail(EnvelopeDetail("g1", "e1"))
        assertEquals(listOf<NavKey>(Group("g1"), EnvelopeDetail("g1", "e1")), stack)
    }

    @Test
    fun aFormAboveTheListIsNeverReplaced() {
        val stack = mutableListOf<NavKey>(Group("g1"), AccountForm("g1", null))
        stack.openDetail(AccountDetail("g1", "a1"))
        assertEquals(listOf<NavKey>(Group("g1"), AccountForm("g1", null), AccountDetail("g1", "a1")), stack)
    }

    @Test
    fun theRootStaysWhenReplacingFromGroups() {
        val stack = mutableListOf<NavKey>(Groups, Profile)
        stack.openDetail(Profile)
        assertEquals(listOf<NavKey>(Groups, Profile), stack)
    }

    @Test
    fun closingDetailsLeavesTheListAlone() {
        val stack = mutableListOf<NavKey>(Group("g1"), AccountDetail("g1", "a1"), CommitmentDetail("g1", "a1", "c1"))
        stack.closeDetails()
        assertEquals(listOf<NavKey>(Group("g1")), stack)
    }

    @Test
    fun closingDetailsKeepsTheRootAndIgnoresAListAlone() {
        val stack = mutableListOf<NavKey>(Group("g1"))
        stack.closeDetails()
        assertEquals(listOf<NavKey>(Group("g1")), stack)
    }

    @Test
    fun onlyDetailsAndExtraPanesAreReplacedBesideTheList() {
        val panes = listOf(
            AccountDetail("g", "a"), EnvelopeDetail("g", "e"), EnvelopeTransfers("g"), GroupSettings("g"), GroupReview("g"),
            Profile, CommitmentDetail("g", "a", "c"), AccountMovements("g", "a"), EnvelopeMovements("g", "e"),
            EnvelopeTransfersOf("g", "e"), Tags("g"), TagMovements("g", "t"),
        )
        val others = listOf(Groups, Group("g"), GroupForm(null), AccountForm("g", null), BankImport("g", "a"), DeleteAccount)

        assertEquals(panes, panes.filter { it.isPaneDetail() })
        assertEquals(emptyList(), others.filter { it.isPaneDetail() })
    }
}
