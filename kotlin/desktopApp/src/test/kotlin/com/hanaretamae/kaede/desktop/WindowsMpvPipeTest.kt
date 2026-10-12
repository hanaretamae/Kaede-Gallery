package com.hanaretamae.kaede.desktop

import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WindowsMpvPipeTest {
    @Test
    fun writesCommandsWhileWaitingForPlayerEvents() {
        val incoming = ConcurrentLinkedQueue<Byte>()
        val output = ByteArrayOutputStream()
        val handle = Pointer.createConstant(42)
        var closed = false
        val pipe = object : WindowsPipeApi {
            override fun CreateFileW(
                name: WString, access: Int, shareMode: Int, securityAttributes: Pointer?,
                creationDisposition: Int, flags: Int, template: Pointer?,
            ): Pointer {
                assertTrue(name.toString().startsWith("\\\\.\\pipe\\kaede-mpv-"))
                assertEquals(0xC0000000.toInt(), access)
                assertEquals(3, creationDisposition)
                return handle
            }

            override fun PeekNamedPipe(
                pipe: Pointer, buffer: Pointer?, bufferSize: Int, bytesRead: Pointer?,
                bytesAvailable: IntByReference, bytesLeftThisMessage: Pointer?,
            ): Boolean {
                assertEquals(handle, pipe)
                bytesAvailable.value = incoming.size
                return !closed
            }

            override fun ReadFile(
                pipe: Pointer, buffer: ByteArray, length: Int,
                bytesRead: IntByReference, overlapped: Pointer?,
            ): Boolean {
                repeat(length) { index -> buffer[index] = requireNotNull(incoming.poll()) }
                bytesRead.value = length
                return true
            }

            override fun WriteFile(
                pipe: Pointer, buffer: ByteArray, length: Int,
                bytesWritten: IntByReference, overlapped: Pointer?,
            ): Boolean {
                assertEquals(handle, pipe)
                output.write(buffer, 0, length)
                bytesWritten.value = length
                return true
            }

            override fun CloseHandle(pipe: Pointer): Boolean {
                assertEquals(handle, pipe)
                closed = true
                return true
            }
        }
        val connection = WindowsMpvPipe.open(windowsMpvPipePath(), pipe)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val reader = BufferedReader(InputStreamReader(connection.input, StandardCharsets.UTF_8))
            val waitingRead = executor.submit<String> { reader.readLine() }
            val pendingWrite = executor.submit {
                val writer = OutputStreamWriter(connection.output, StandardCharsets.UTF_8)
                writer.write("""{"command":["cycle","pause"]}""" + "\n")
                writer.flush()
            }
            pendingWrite.get(2, TimeUnit.SECONDS)
            assertEquals("""{"command":["cycle","pause"]}""" + "\n", output.toString(StandardCharsets.UTF_8))
            "event\n".toByteArray().forEach(incoming::add)
            assertEquals("event", waitingRead.get(2, TimeUnit.SECONDS))
            connection.close()
            assertEquals(-1, connection.input.read())
            assertTrue(closed)
        } finally {
            connection.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun rejectsUnavailablePipeAndUnrelatedEndpoints() {
        val api = object : WindowsPipeApi {
            override fun CreateFileW(
                name: WString, access: Int, shareMode: Int, securityAttributes: Pointer?,
                creationDisposition: Int, flags: Int, template: Pointer?,
            ): Pointer = Pointer.createConstant(-1L)
            override fun PeekNamedPipe(
                pipe: Pointer, buffer: Pointer?, bufferSize: Int, bytesRead: Pointer?,
                bytesAvailable: IntByReference, bytesLeftThisMessage: Pointer?,
            ): Boolean = error("Not connected")
            override fun ReadFile(
                pipe: Pointer, buffer: ByteArray, length: Int,
                bytesRead: IntByReference, overlapped: Pointer?,
            ): Boolean = error("Not connected")
            override fun WriteFile(
                pipe: Pointer, buffer: ByteArray, length: Int,
                bytesWritten: IntByReference, overlapped: Pointer?,
            ): Boolean = error("Not connected")
            override fun CloseHandle(pipe: Pointer): Boolean = error("Not connected")
        }
        assertFailsWith<IOException> { WindowsMpvPipe.open(windowsMpvPipePath(), api) }
        assertFailsWith<IllegalArgumentException> { WindowsMpvPipe.open("\\\\.\\pipe\\other", api) }
    }
}
