package com.hanaretamae.kaede.core.rust

import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ThumbnailLoadCoordinatorTest {
    @Test
    fun interactiveWorkRunsBeforeQueuedPrefetch() = runTest {
        val scheduler = ThumbnailLoadScheduler(maxConcurrentLoads = 1)
        val blocker = CompletableDeferred<Unit>()
        val order = mutableListOf<ThumbnailLoadPriority>()

        val active = async {
            scheduler.withPermit(ThumbnailLoadPriority.VISIBLE) { blocker.await() }
        }
        runCurrent()
        val prefetch = async {
            scheduler.withPermit(ThumbnailLoadPriority.PREFETCH) {
                order += ThumbnailLoadPriority.PREFETCH
            }
        }
        val interactive = async {
            scheduler.withPermit(ThumbnailLoadPriority.INTERACTIVE) {
                order += ThumbnailLoadPriority.INTERACTIVE
            }
        }
        runCurrent()

        blocker.complete(Unit)
        active.await()
        interactive.await()
        prefetch.await()

        assertEquals(
            listOf(ThumbnailLoadPriority.INTERACTIVE, ThumbnailLoadPriority.PREFETCH),
            order,
        )
    }

    @Test
    fun concurrentIdenticalRequestsShareOneLoad() = runTest {
        val requests = SingleFlight<String, Int>()
        val loadGate = CompletableDeferred<Unit>()
        var loadCount = 0

        val first = async {
            requests.run("media:512") {
                loadCount += 1
                loadGate.await()
                42
            }
        }
        runCurrent()
        val second = async {
            requests.run("media:512") {
                loadCount += 1
                24
            }
        }
        runCurrent()
        loadGate.complete(Unit)

        assertEquals(42, first.await())
        assertEquals(42, second.await())
        assertEquals(1, loadCount)
    }

    @Test
    fun cancellingQueuedWorkDoesNotLeakCapacity() = runTest {
        val scheduler = ThumbnailLoadScheduler(maxConcurrentLoads = 1)
        val blocker = CompletableDeferred<Unit>()
        val active = async {
            scheduler.withPermit(ThumbnailLoadPriority.VISIBLE) { blocker.await() }
        }
        runCurrent()
        val cancelled = async {
            scheduler.withPermit(ThumbnailLoadPriority.PREFETCH) { error("Cancelled load ran.") }
        }
        runCurrent()
        cancelled.cancelAndJoin()

        blocker.complete(Unit)
        active.await()
        var completed = false
        scheduler.withPermit(ThumbnailLoadPriority.INTERACTIVE) { completed = true }
        assertEquals(true, completed)
    }

    @Test
    fun cancelledOwnerLetsAnotherCallerRetrySharedWork() = runTest {
        val requests = SingleFlight<String, Int>()
        val gate = CompletableDeferred<Unit>()
        var loadCount = 0
        val owner = async {
            requests.run("media") {
                loadCount += 1
                gate.await()
                1
            }
        }
        runCurrent()
        val waiter = async {
            requests.run("media") {
                loadCount += 1
                2
            }
        }
        runCurrent()
        owner.cancelAndJoin()

        assertEquals(2, waiter.await())
        assertEquals(2, loadCount)
    }
}
