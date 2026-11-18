package dev.lacruzjs.sobreio.ui.undo

import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.role_changed
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PendingUndoCommitsTest {

    @Test
    fun aReloadWaitsForADeleteMadeFinalWhenTheScreenClosed() = runBlocking {
        val release = CompletableDeferred<Unit>()
        var written = false
        val controller = UndoController(scope = CoroutineScope(Dispatchers.Default), onMessage = {}, onError = {})
        controller.show(
            UndoableAction(
                message = Res.string.role_changed,
                argument = "",
                commit = {
                    release.await()
                    written = true
                    Result.success(Unit)
                },
                undo = { Result.success(Unit) },
                restore = {},
            ),
        )

        controller.commitDetached()
        val reload = async(Dispatchers.Default) { PendingUndoCommits.await() }
        assertFalse(reload.isCompleted)

        release.complete(Unit)
        withTimeout(5_000) { reload.await() }
        assertTrue(written)
    }

    @Test
    fun withNothingPendingItReturnsAtOnce() = runBlocking {
        withTimeout(1_000) { PendingUndoCommits.await() }
    }
}
