package com.hanaretamae.kaede.desktop

import com.sun.jna.Pointer
import com.sun.jna.NativeLong
import com.sun.jna.ptr.PointerByReference
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

internal fun isPortalAppearanceSettingChanged(namespace: String?, key: String?): Boolean =
    namespace == PORTAL_APPEARANCE_NAMESPACE &&
        key in setOf(PORTAL_COLOR_SCHEME_KEY, PORTAL_ACCENT_COLOR_KEY)

internal object LinuxPortalSettings {
    fun read(key: String): String? {
        if (key !in setOf(PORTAL_COLOR_SCHEME_KEY, PORTAL_ACCENT_COLOR_KEY)) return null
        return try {
            readPortalSetting(key)
        } catch (_: IOException) {
            null
        } catch (_: LinkageError) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    fun readGtkAccentColor(): String? = try {
        readGtkSetting(PORTAL_GTK_INTERFACE_SCHEMA, PORTAL_GTK_ACCENT_COLOR_KEY)
    } catch (_: IOException) {
        null
    } catch (_: LinkageError) {
        null
    } catch (_: SecurityException) {
        null
    }

    fun subscribe(onSettingChanged: () -> Unit): AutoCloseable {
        val gio = try {
            NativeApis.gio
        } catch (_: LinkageError) {
            return AutoCloseable {}
        }
        val glib = try {
            NativeApis.glib
        } catch (_: LinkageError) {
            return AutoCloseable {}
        }
        val gobject = try {
            NativeApis.gobject
        } catch (_: LinkageError) {
            return AutoCloseable {}
        }
        val context = glib.g_main_context_new() ?: return AutoCloseable {}
        val ready = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        val running = AtomicBoolean(true)
        val callback = PortalSignalCallback { _, _, _, _, _, parameters, _ ->
            if (hasRelevantAppearanceSetting(glib, parameters)) {
                runCatching(onSettingChanged)
            }
        }
        val worker = thread(name = "kaede-portal-settings", isDaemon = true) {
            var connection: Pointer? = null
            var subscription = 0
            glib.g_main_context_push_thread_default(context)
            try {
                val sessionConnection = sessionBus(gio, glib)
                connection = sessionConnection
                subscription = gio.g_dbus_connection_signal_subscribe(
                    sessionConnection,
                    PORTAL_BUS_NAME,
                    PORTAL_SETTINGS_INTERFACE,
                    PORTAL_SETTING_CHANGED_SIGNAL,
                    PORTAL_OBJECT_PATH,
                    null,
                    DBUS_SIGNAL_FLAGS_NONE,
                    callback,
                    null,
                    null,
                )
                if (subscription == 0) {
                    throw IOException("The system appearance settings are unavailable.")
                }
            } catch (error: Exception) {
                failure.set(error)
            } catch (error: LinkageError) {
                failure.set(error)
            } finally {
                ready.countDown()
            }

            try {
                if (failure.get() == null) {
                    while (running.get()) {
                        glib.g_main_context_iteration(context, MAY_BLOCK)
                    }
                }
            } finally {
                if (subscription != 0 && connection != null) {
                    gio.g_dbus_connection_signal_unsubscribe(connection, subscription)
                }
                connection?.let(gobject::g_object_unref)
                glib.g_main_context_pop_thread_default(context)
                glib.g_main_context_unref(context)
            }
        }

        val initialized = try {
            ready.await(PORTAL_READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (!initialized || failure.get() != null) {
            running.set(false)
            joinWorker(worker)
            return AutoCloseable {}
        }

        return AutoCloseable {
            if (running.getAndSet(false)) {
                glib.g_main_context_wakeup(context)
                joinWorker(worker)
            }
        }
    }

    private fun readPortalSetting(key: String): String? {
        val gio = NativeApis.gio
        val glib = NativeApis.glib
        val connection = sessionBus(gio, glib)
        return try {
            val parameters = parseVariant(
                glib,
                "(ss)",
                "('$PORTAL_APPEARANCE_NAMESPACE', '$key')",
            )
            val reply = try {
                callSync(
                    gio = gio,
                    glib = glib,
                    connection = connection,
                    objectPath = PORTAL_OBJECT_PATH,
                    interfaceName = PORTAL_SETTINGS_INTERFACE,
                    methodName = PORTAL_READ_ONE_METHOD,
                    parameters = parameters,
                )
            } finally {
                glib.g_variant_unref(parameters)
            }
            try {
                val variant = glib.g_variant_get_child_value(reply, NativeLong(0))
                    ?: return null
                try {
                    val value = glib.g_variant_get_variant(variant) ?: return null
                    try {
                        val printed = glib.g_variant_print(value, TYPE_ANNOTATE)
                            ?: return null
                        try {
                            printed.getString(0, StandardCharsets.UTF_8.name())
                        } finally {
                            glib.g_free(printed)
                        }
                    } finally {
                        glib.g_variant_unref(value)
                    }
                } finally {
                    glib.g_variant_unref(variant)
                }
            } finally {
                glib.g_variant_unref(reply)
            }
        } finally {
            NativeApis.gobject.g_object_unref(connection)
        }
    }

    private fun readGtkSetting(schemaId: String, key: String): String? {
        val gio = NativeApis.gio
        val glib = NativeApis.glib
        val source = gio.g_settings_schema_source_get_default() ?: return null
        val schema = gio.g_settings_schema_source_lookup(source, schemaId, RECURSIVE_LOOKUP)
            ?: return null
        return try {
            if (gio.g_settings_schema_has_key(schema, key) == 0) return null
            val settings = gio.g_settings_new_full(schema, null, null) ?: return null
            try {
                val value = gio.g_settings_get_string(settings, key) ?: return null
                try {
                    value.getString(0, StandardCharsets.UTF_8.name())
                } finally {
                    glib.g_free(value)
                }
            } finally {
                NativeApis.gobject.g_object_unref(settings)
            }
        } finally {
            gio.g_settings_schema_unref(schema)
        }
    }

    private fun hasRelevantAppearanceSetting(glib: GlibApi, parameters: Pointer): Boolean {
        val namespaceValue = glib.g_variant_get_child_value(parameters, NativeLong(0))
            ?: return false
        val namespace = try {
            glib.g_variant_get_string(namespaceValue, null)
                ?.getString(0, StandardCharsets.UTF_8.name())
        } finally {
            glib.g_variant_unref(namespaceValue)
        }
        val keyValue = glib.g_variant_get_child_value(parameters, NativeLong(1))
            ?: return false
        val key = try {
            glib.g_variant_get_string(keyValue, null)
                ?.getString(0, StandardCharsets.UTF_8.name())
        } finally {
            glib.g_variant_unref(keyValue)
        }
        return isPortalAppearanceSettingChanged(namespace, key)
    }

    private fun sessionBus(gio: GioApi, glib: GlibApi): Pointer {
        val error = PointerByReference()
        return gio.g_bus_get_sync(GBUS_TYPE_SESSION, null, error)
            ?: throw nativeFailure(glib, error, "The system appearance settings are unavailable.")
    }

    private fun parseVariant(glib: GlibApi, typeString: String, text: String): Pointer {
        val type = glib.g_variant_type_new(typeString)
            ?: throw IOException("The system appearance settings are unavailable.")
        try {
            val error = PointerByReference()
            return glib.g_variant_parse(type, text, null, null, error)
                ?: throw nativeFailure(glib, error, "The system appearance settings are unavailable.")
        } finally {
            glib.g_variant_type_free(type)
        }
    }

    private fun callSync(
        gio: GioApi,
        glib: GlibApi,
        connection: Pointer,
        objectPath: String,
        interfaceName: String,
        methodName: String,
        parameters: Pointer,
    ): Pointer {
        val error = PointerByReference()
        return gio.g_dbus_connection_call_sync(
            connection,
            PORTAL_BUS_NAME,
            objectPath,
            interfaceName,
            methodName,
            parameters,
            null,
            DBUS_CALL_FLAGS_NONE,
            PORTAL_READ_TIMEOUT_MILLIS.toInt(),
            null,
            error,
        ) ?: throw nativeFailure(glib, error, "The system appearance settings are unavailable.")
    }

    private fun nativeFailure(
        glib: GlibApi,
        error: PointerByReference,
        message: String,
    ): IOException {
        error.value?.let(glib::g_error_free)
        return IOException(message)
    }

    private fun joinWorker(worker: Thread) {
        try {
            worker.join(PORTAL_READ_TIMEOUT_MILLIS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

private const val PORTAL_APPEARANCE_NAMESPACE = "org.freedesktop.appearance"
private const val PORTAL_COLOR_SCHEME_KEY = "color-scheme"
private const val PORTAL_ACCENT_COLOR_KEY = "accent-color"
private const val PORTAL_GTK_INTERFACE_SCHEMA = "org.gnome.desktop.interface"
private const val PORTAL_GTK_ACCENT_COLOR_KEY = "accent-color"
private const val PORTAL_OBJECT_PATH = "/org/freedesktop/portal/desktop"
private const val PORTAL_BUS_NAME = "org.freedesktop.portal.Desktop"
private const val PORTAL_SETTINGS_INTERFACE = "org.freedesktop.portal.Settings"
private const val PORTAL_READ_ONE_METHOD = "ReadOne"
private const val PORTAL_SETTING_CHANGED_SIGNAL = "SettingChanged"
private const val GBUS_TYPE_SESSION = 2
private const val DBUS_SIGNAL_FLAGS_NONE = 0
private const val DBUS_CALL_FLAGS_NONE = 0
private const val PORTAL_READ_TIMEOUT_MILLIS = 750L
private const val TYPE_ANNOTATE = 1
private const val MAY_BLOCK = 1
private const val RECURSIVE_LOOKUP = 1
