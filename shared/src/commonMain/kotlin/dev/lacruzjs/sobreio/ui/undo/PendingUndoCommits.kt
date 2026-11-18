package dev.lacruzjs.sobreio.ui.undo

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll

/**
 * The deletes that became final when their screen closed ([UndoController.commitDetached]) and are still being written.
 * Leaving a screen right after a swipe Delete commits it while the screen below reloads: without this, that reload can
 * read the data before the write and show figures from before the delete. Screens that reload money when shown call
 * [await] before reading.
 */
object PendingUndoCommits {
    private val jobs = MutableStateFlow<Set<Job>>(emptySet())

    fun track(job: Job) {
        jobs.update { it + job }
        job.invokeOnCompletion { jobs.update { set -> set - job } }
    }

    /** Waits for every commit in flight; returns at once when there is none. */
    suspend fun await() {
        jobs.value.joinAll()
    }
}
