package com.hanaretamae.kaede.desktop

import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinuxPortalDirectoryPickerTest {
    @Test
    fun onlyFallsBackBeforeARequestCanBeOpen() {
        assertTrue(shouldUseSwingFallback(IOException("Portal is unavailable.")))
        assertFalse(shouldUseSwingFallback(PortalSelectionException("Portal request failed.")))
    }

    @Test
    fun buildsAFileChooserRequestForOneDirectory() {
        assertEquals(
            "('', 'Select an existing Vault', " +
                "{'directory': <true>, 'multiple': <false>, 'modal': <true>})",
            linuxPortalOpenOptions("Select an existing Vault"),
        )
        assertEquals(
            "('', 'A \\'Vault\\'', " +
                "{'directory': <true>, 'multiple': <false>, 'modal': <true>})",
            linuxPortalOpenOptions("A 'Vault'"),
        )
    }

    @Test
    fun createsResponsesFromPortalCodesAndSelectedUris() {
        assertEquals(
            PortalFileChooserResponse(
                responseCode = 0,
                selectedUri = java.net.URI("file:///fictional%20vault"),
            ),
            portalFileChooserResponse(0, "file:///fictional%20vault"),
        )
        assertEquals(
            PortalFileChooserResponse(responseCode = 1, selectedUri = null),
            portalFileChooserResponse(1, null),
        )
        assertEquals(
            PortalFileChooserResponse(responseCode = 2, selectedUri = null),
            portalFileChooserResponse(2, "file:///ignored"),
        )
        assertNull(portalFileChooserResponse(0, "not a URI").selectedUri)
    }

    @Test
    fun loadsGioAndParsesTheRequestVariantOnLinux() {
        if (!System.getProperty("os.name").contains("linux", ignoreCase = true)) return

        assertNotNull(NativeApis.gio)
        val glib = NativeApis.glib
        val parameters = LinuxPortalDirectoryPicker.openFileParameters(
            glib,
            "Select an existing Vault",
        )
        try {
            assertEquals(3L, glib.g_variant_n_children(parameters).toLong())
        } finally {
            glib.g_variant_unref(parameters)
        }
    }

    @Test
    fun readsTheRequestHandleFromThePortalReply() {
        val glib = NativeApis.glib
        val type = assertNotNull(glib.g_variant_type_new("(o)"))
        val error = PointerByReference()
        val reply = try {
            glib.g_variant_parse(
                type,
                "('/org/freedesktop/portal/desktop/request/1_42/test',)",
                null,
                null,
                error,
            )
        } finally {
            glib.g_variant_type_free(type)
        }
        val portalReply = assertNotNull(reply)
        try {
            assertEquals(
                "/org/freedesktop/portal/desktop/request/1_42/test",
                LinuxPortalDirectoryPicker.readRequestHandle(glib, portalReply),
            )
        } finally {
            glib.g_variant_unref(portalReply)
        }
    }

    @Test
    fun subscribesToPortalResponsesOnTheSessionBusWhenAvailable() {
        if (!System.getProperty("os.name").contains("linux", ignoreCase = true)) return
        if (System.getenv("DBUS_SESSION_BUS_ADDRESS").isNullOrBlank()) return

        val gio = NativeApis.gio
        val glib = NativeApis.glib
        val gobject = NativeApis.gobject
        val context = assertNotNull(glib.g_main_context_new())
        glib.g_main_context_push_thread_default(context)
        var connection: Pointer? = null
        var subscription = 0
        try {
            val error = PointerByReference()
            val sessionBus = gio.g_bus_get_sync(2, null, error)
            if (sessionBus == null) {
                error.value?.let(glib::g_error_free)
                assertNotNull(sessionBus, "Unable to connect to the session bus.")
            }
            error.value?.let(glib::g_error_free)
            connection = sessionBus
            val callback = PortalSignalCallback { _, _, _, _, _, _, _ -> }
            subscription = gio.g_dbus_connection_signal_subscribe(
                sessionBus,
                "org.freedesktop.portal.Desktop",
                "org.freedesktop.portal.Request",
                "Response",
                null,
                null,
                0,
                callback,
                null,
                null,
            )
            assertTrue(subscription > 0)
        } finally {
            if (subscription > 0 && connection != null) {
                gio.g_dbus_connection_signal_unsubscribe(connection, subscription)
            }
            connection?.let(gobject::g_object_unref)
            glib.g_main_context_pop_thread_default(context)
            glib.g_main_context_unref(context)
        }
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
