package com.hanaretamae.kaede.desktop

import com.hanaretamae.kaede.core.settings.SettingsTransferCodec
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.nio.ByteBuffer

internal object DesktopSettingsTransfer {
    fun read(path: Path): String {
        require(!Files.isSymbolicLink(path))
        require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
        require(Files.size(path) <= SettingsTransferCodec.MAX_BYTES)
        val bytes = Files.newInputStream(path).use { input ->
            ByteArrayOutputStream().use { output ->
                val buffer = ByteArray(1024)
                var total = 0
                while (true) {
                    val count = input.read(
                        buffer,
                        0,
                        minOf(buffer.size, SettingsTransferCodec.MAX_BYTES + 1 - total),
                    )
                    if (count < 0) break
                    if (count == 0) throw IOException("Settings file could not be read.")
                    total += count
                    require(total <= SettingsTransferCodec.MAX_BYTES)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        }
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }

    fun write(path: Path, content: String, vaultPath: Path) {
        require(content.encodeToByteArray().size <= SettingsTransferCodec.MAX_BYTES)
        require(SettingsTransferCodec.decode(content) != null)
        val target = path.toAbsolutePath().normalize()
        val parent = target.parent?.toRealPath()
            ?: throw IOException("Settings destination has no parent.")
        val vault = vaultPath.toRealPath()
        require(!parent.startsWith(vault))
        require(!Files.isSymbolicLink(target))

        val temporary = Files.createTempFile(parent, ".kaede-settings-", ".tmp")
        try {
            try {
                Files.setPosixFilePermissions(
                    temporary,
                    PosixFilePermissions.fromString("rw-------"),
                )
            } catch (_: UnsupportedOperationException) {
                // The private temporary file is still created with platform defaults.
            }
            Files.writeString(temporary, content, StandardCharsets.UTF_8)
            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
