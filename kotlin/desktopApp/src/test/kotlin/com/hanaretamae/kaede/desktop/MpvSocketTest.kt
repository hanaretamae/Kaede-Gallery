package com.hanaretamae.kaede.desktop

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.Channels
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class MpvSocketTest {
    @Test
    fun commandsCanBeWrittenWhileThePlayerReaderIsWaitingForEvents() {
        if (!System.getProperty("os.name").lowercase().contains("linux")) return
        val directory = Files.createTempDirectory("kaede-ipc-test-")
        val socket = directory.resolve("control.sock")
        val executor = Executors.newFixedThreadPool(2)
        try {
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).use { server ->
                server.bind(UnixDomainSocketAddress.of(socket))
                SocketChannel.open(StandardProtocolFamily.UNIX).use { client ->
                    client.connect(UnixDomainSocketAddress.of(socket))
                    server.accept().use { peer ->
                        val reader = BufferedReader(InputStreamReader(mpvSocketInputStream(client)))
                        val waitingRead = executor.submit<String> { reader.readLine() }
                        val writer = Channels.newOutputStream(client)
                        val command = """{"command":["set_property","pause",false]}""" + "\n"
                        val pendingWrite = executor.submit { writer.write(command.toByteArray()) }
                        try {
                            pendingWrite.get(2, TimeUnit.SECONDS)
                            assertEquals(
                                command.trimEnd(),
                                BufferedReader(InputStreamReader(mpvSocketInputStream(peer))).readLine(),
                            )
                            Channels.newOutputStream(peer).write("event\n".toByteArray())
                            assertEquals("event", waitingRead.get(2, TimeUnit.SECONDS))
                        } finally {
                            client.close()
                        }
                    }
                }
            }
        } finally {
            executor.shutdownNow()
            Files.deleteIfExists(socket)
            Files.deleteIfExists(directory)
        }
    }
}
