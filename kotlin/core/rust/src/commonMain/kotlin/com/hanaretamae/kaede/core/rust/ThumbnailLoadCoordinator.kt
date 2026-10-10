package com.hanaretamae.kaede.core.rust

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class ThumbnailLoadPriority {
    INTERACTIVE,
    VISIBLE,
    PREFETCH,
}

class ThumbnailLoadScheduler(maxConcurrentLoads: Int) {
    private class Waiter(val priority: ThumbnailLoadPriority) {
        val ready = CompletableDeferred<Unit>()
        var granted = false
    }

    private val mutex = Mutex()
    private val queue = mutableListOf<Waiter>()
    private var activeLoads = 0

    init {
        require(maxConcurrentLoads > 0)
    }

    private val limit = maxConcurrentLoads

    suspend fun <T> withPermit(
        priority: ThumbnailLoadPriority,
        block: suspend () -> T,
    ): T {
        val waiter = mutex.withLock {
            if (activeLoads < limit && queue.isEmpty()) {
                activeLoads += 1
                null
            } else {
                Waiter(priority).also(queue::add)
            }
        }

        if (waiter != null) {
            try {
                waiter.ready.await()
            } catch (cancelled: CancellationException) {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (waiter.granted) {
                            waiter.granted = false
                            releaseLocked()
                        } else {
                            queue.remove(waiter)
                        }
                    }
                }
                throw cancelled
            }
        }

        try {
            return block()
        } finally {
            withContext(NonCancellable) {
                mutex.withLock { releaseLocked() }
            }
        }
    }

    private fun releaseLocked() {
        val next = queue.minByOrNull { it.priority.ordinal }
        if (next == null) {
            activeLoads -= 1
        } else {
            queue.remove(next)
            next.granted = true
            next.ready.complete(Unit)
        }
    }
}

class SingleFlight<K : Any, V> {
    private val mutex = Mutex()
    private val inFlight = mutableMapOf<K, CompletableDeferred<V>>()

    suspend fun run(key: K, block: suspend () -> V): V {
        val (result, isOwner) = mutex.withLock {
            val existing = inFlight[key]
            if (existing != null && !existing.isCancelled) {
                existing to false
            } else {
                CompletableDeferred<V>().also { inFlight[key] = it } to true
            }
        }
        if (!isOwner) {
            return try {
                result.await()
            } catch (cancelled: CancellationException) {
                currentCoroutineContext().ensureActive()
                run(key, block)
            }
        }

        try {
            return block().also(result::complete)
        } catch (failure: Throwable) {
            result.completeExceptionally(failure)
            throw failure
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    if (inFlight[key] === result) inFlight.remove(key)
                }
            }
        }
    }
}
