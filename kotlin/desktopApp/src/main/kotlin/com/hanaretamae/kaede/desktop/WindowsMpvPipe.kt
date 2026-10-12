package com.hanaretamae.kaede.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal interface WindowsPipeApi : StdCallLibrary {
    fun CreateFileW(
        name: WString,
        access: Int,
        shareMode: Int,
        securityAttributes: Pointer?,
        creationDisposition: Int,
        flags: Int,
        template: Pointer?,
    ): Pointer?

    fun PeekNamedPipe(
        pipe: Pointer,
        buffer: Pointer?,
        bufferSize: Int,
        bytesRead: Pointer?,
        bytesAvailable: IntByReference,
        bytesLeftThisMessage: Pointer?,
    ): Boolean

    fun ReadFile(
        pipe: Pointer, buffer: ByteArray, length: Int,
        bytesRead: IntByReference, overlapped: Pointer?,
    ): Boolean

    fun WriteFile(
        pipe: Pointer, buffer: ByteArray, length: Int,
        bytesWritten: IntByReference, overlapped: Pointer?,
    ): Boolean

    fun CloseHandle(pipe: Pointer): Boolean
}

private val windowsPipeApi by lazy { Native.load("kernel32", WindowsPipeApi::class.java) }

internal fun windowsMpvPipePath(): String = "\\\\.\\pipe\\kaede-mpv-${UUID.randomUUID()}"

internal class WindowsMpvPipe private constructor(
    private val handle: Pointer,
    private val api: WindowsPipeApi,
) : MpvIpcConnection {
    private val closed = AtomicBoolean(false)

    override val input: InputStream = object : InputStream() {
        override fun read(): Int {
            val byte = ByteArray(1)
            return if (read(byte, 0, 1) == -1) -1 else byte[0].toInt() and 0xff
        }

        override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
            java.util.Objects.checkFromIndexSize(offset, length, bytes.size)
            if (length == 0) return 0
            while (!closed.get()) {
                val available = IntByReference()
                if (!api.PeekNamedPipe(handle, null, 0, null, available, null)) {
                    throw IOException("In-app video controls lost their player connection.")
                }
                if (available.value < 0) {
                    throw IOException("In-app video controls received an invalid player response.")
                }
                if (available.value > 0) {
                    val buffer = ByteArray(minOf(length, available.value))
                    val count = IntByReference()
                    if (!api.ReadFile(handle, buffer, buffer.size, count, null) ||
                        count.value !in 1..buffer.size
                    ) {
                        throw IOException("In-app video controls could not read from the player.")
                    }
                    buffer.copyInto(bytes, offset, 0, count.value)
                    return count.value
                }
                try {
                    Thread.sleep(25)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("In-app video controls were interrupted.")
                }
            }
            return -1
        }
    }

    override val output: OutputStream = object : OutputStream() {
        override fun write(value: Int) {
            write(byteArrayOf(value.toByte()), 0, 1)
        }

        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            java.util.Objects.checkFromIndexSize(offset, length, bytes.size)
            var written = 0
            while (written < length) {
                if (closed.get()) throw IOException("In-app video controls are disconnected.")
                val buffer = bytes.copyOfRange(offset + written, offset + length)
                val count = IntByReference()
                if (!api.WriteFile(handle, buffer, buffer.size, count, null) ||
                    count.value !in 1..buffer.size
                ) {
                    throw IOException("In-app video controls could not write to the player.")
                }
                written += count.value
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true) && !api.CloseHandle(handle)) {
            throw IOException("In-app video controls could not close the player connection.")
        }
    }

    companion object {
        fun open(name: String, api: WindowsPipeApi = windowsPipeApi): WindowsMpvPipe {
            require(name.startsWith("\\\\.\\pipe\\kaede-mpv-")) {
                "Invalid player control endpoint."
            }
            val handle = api.CreateFileW(WString(name), 0xC0000000.toInt(), 0, null, 3, 0, null)
            if (handle == null || Pointer.nativeValue(handle) == -1L) {
                throw IOException("In-app video controls could not connect to the player.")
            }
            return WindowsMpvPipe(handle, api)
        }
    }
}
