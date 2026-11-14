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
}
