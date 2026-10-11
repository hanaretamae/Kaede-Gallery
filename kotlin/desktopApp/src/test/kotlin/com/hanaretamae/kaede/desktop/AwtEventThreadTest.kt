package com.hanaretamae.kaede.desktop

import java.awt.EventQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AwtEventThreadTest {
    @Test
    fun executesSwingActionsOnTheAwtEventThread() {
        assertEquals(
            "selected",
            runOnAwtEventThread {
                assertTrue(EventQueue.isDispatchThread())
                "selected"
            },
        )
    }

    @Test
    fun propagatesSwingActionFailuresToTheCaller() {
        val failure = assertFailsWith<IllegalStateException> {
            runOnAwtEventThread<String> {
                throw IllegalStateException("fictional picker failure")
            }
        }
        assertEquals("fictional picker failure", failure.message)
    }
}
