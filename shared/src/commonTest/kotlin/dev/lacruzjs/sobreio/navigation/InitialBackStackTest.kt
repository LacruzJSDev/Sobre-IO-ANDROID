package dev.lacruzjs.sobreio.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class InitialBackStackTest {

    @Test
    fun withoutLastGroupOnlyGroupsIsShown() {
        assertEquals(listOf(Groups), initialBackStack(null))
    }

    @Test
    fun withLastGroupItOpensOnTopOfGroups() {
        assertEquals(listOf(Groups, Group("g1")), initialBackStack("g1"))
    }
}
