package dev.lacruzjs.sobreio.ui.undo

import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.role_changed
import dev.lacruzjs.sobreio.testing.MainDispatcherTest
import kotlinx.coroutines.CoroutineScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UndoControllerTest : MainDispatcherTest() {

    private var message: UndoMessage? = null
    private val controller = UndoController(
        scope = CoroutineScope(testDispatcher),
        onMessage = { message = it },
        onError = {},
    )

    @Test
    fun aNoticeHasNoUndoAndGoesAwayWhenItEnds() {
        controller.notify(Res.string.role_changed, "Ana")
        assertEquals("Ana", message?.argument)
        assertFalse(message?.undoable == true)

        controller.commit()

        assertNull(message)
    }

    @Test
    fun anActionAfterANoticeReplacesIt() {
        controller.notify(Res.string.role_changed, "Ana")
        controller.show(UndoableAction(Res.string.role_changed, "Luis", { Result.success(Unit) }, { Result.success(Unit) }, {}))

        assertEquals("Luis", message?.argument)
        assertTrue(message?.undoable == true)
    }
}
