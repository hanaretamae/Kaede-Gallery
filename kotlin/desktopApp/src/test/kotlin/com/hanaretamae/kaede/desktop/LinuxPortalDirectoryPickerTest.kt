package com.hanaretamae.kaede.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinuxPortalDirectoryPickerTest {
    @Test
    fun opensThePortalFileChooserInSingleDirectoryMode() {
        assertEquals(
            listOf(
                "gdbus",
                "call",
                "--session",
                "--dest=org.freedesktop.portal.Desktop",
                "--object-path=/org/freedesktop/portal/desktop",
                "--method=org.freedesktop.portal.FileChooser.OpenFile",
                "",
                "Select an existing Vault",
                "{'directory': <true>, 'multiple': <false>, 'modal': <true>}",
            ),
            linuxPortalOpenDirectoryCommand("Select an existing Vault"),
        )
    }

    @Test
    fun parsesOnlyRequestHandlesFromThePortal() {
        assertEquals(
            "/org/freedesktop/portal/desktop/request/1_42/handle",
            parsePortalRequestHandle(
                "(objectpath '/org/freedesktop/portal/desktop/request/1_42/handle',)",
            ),
        )
        assertNull(parsePortalRequestHandle("(objectpath '/org/example/handle',)"))
    }

    @Test
    fun parsesSuccessfulAndCancelledPortalResponses() {
        val handle = "/org/freedesktop/portal/desktop/request/1_42/handle"

        assertEquals(
            PortalFileChooserResponse(
                responseCode = 0,
                selectedUri = java.net.URI("file:///fictional%20vault"),
            ),
            parsePortalResponseLine(
                handle,
                "$handle: org.freedesktop.portal.Request.Response " +
                    "(uint32 0, {'uris': <['file:///fictional%20vault']>})",
            ),
        )
        assertEquals(
            PortalFileChooserResponse(responseCode = 1, selectedUri = null),
            parsePortalResponseLine(
                handle,
                "$handle: org.freedesktop.portal.Request.Response (uint32 1, {})",
            ),
        )
        assertNull(
            parsePortalResponseLine(
                handle,
                "/org/freedesktop/portal/desktop/request/other/handle: " +
                    "org.freedesktop.portal.Request.Response (uint32 1, {})",
            ),
        )
    }

    @Test
    fun resolvesOnlyExistingLocalDirectories() {
        val directory = Files.createTempDirectory("kaede-portal-directory")
        val file = Files.write(directory.resolve("fictional.txt"), byteArrayOf(1))
        try {
            assertEquals(directory.toRealPath(), resolvePortalDirectory(directory.toUri()))
            assertNull(resolvePortalDirectory(file.toUri()))
            assertNull(resolvePortalDirectory(java.net.URI("https://example.invalid/vault")))
            assertFalse(Files.exists(directory.resolve("missing")))
            assertTrue(Files.isDirectory(directory))
        } finally {
            Files.deleteIfExists(file)
            Files.deleteIfExists(directory)
        }
    }
}
