package com.hanaretamae.kaede.desktop

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

internal data class PortalFileChooserResponse(
    val responseCode: Int,
    val selectedUri: URI?,
)

internal fun linuxPortalOpenOptions(title: String): String =
    "('', ${quoteGVariantString(title)}, " +
        "{'directory': <true>, 'multiple': <false>, 'modal': <true>})"

internal fun portalFileChooserResponse(
    responseCode: Int,
    selectedUri: String?,
): PortalFileChooserResponse = PortalFileChooserResponse(
    responseCode = responseCode,
    selectedUri = if (responseCode == PORTAL_RESPONSE_SUCCESS) {
        selectedUri?.let { value -> runCatching { URI(value) }.getOrNull() }
    } else {
        null
    },
)

private fun quoteGVariantString(value: String): String = "'" + value
    .replace("\\", "\\\\")
    .replace("'", "\\'")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t") + "'"

internal fun resolvePortalDirectory(uri: URI): Path? {
    if (!uri.scheme.equals("file", ignoreCase = true)) return null
    return try {
        Path.of(uri).toRealPath().takeIf {
            Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS)
        }
    } catch (_: IOException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

internal object LinuxPortalDirectoryPicker {
    fun chooseDirectory(title: String): Path? = try {
        chooseDirectoryOnSessionBus(title)
    } catch (_: LinkageError) {
        throw IOException("The system directory picker is unavailable.")
    } catch (_: SecurityException) {
        throw IOException("The system directory picker is unavailable.")
    }

    private fun chooseDirectoryOnSessionBus(title: String): Path? {
        val gio = NativeApis.gio
        val glib = NativeApis.glib
        val gobject = NativeApis.gobject
        val context = glib.g_main_context_new()
            ?: throw IOException("The system directory picker is unavailable.")
        glib.g_main_context_push_thread_default(context)

        var connection: Pointer? = null
        var subscription = 0
        try {
            val sessionConnection = sessionBus(gio, glib)
            connection = sessionConnection
            val pendingResponses = LinkedBlockingQueue<PortalSignal>(MAX_PENDING_RESPONSES)
            val signalCallback = PortalSignalCallback { _, _, objectPath, _, _, parameters, _ ->
                if (objectPath != null && objectPath.length <= MAX_OBJECT_PATH_LENGTH) {
                    parsePortalSignal(glib, objectPath, parameters)?.let(pendingResponses::offer)
                }
            }
            subscription = gio.g_dbus_connection_signal_subscribe(
                sessionConnection,
                PORTAL_BUS_NAME,
                PORTAL_REQUEST_INTERFACE,
                PORTAL_RESPONSE_SIGNAL,
                null,
                null,
                DBUS_SIGNAL_FLAGS_NONE,
                signalCallback,
                null,
                null,
            )

            val parameters = openFileParameters(glib, title)
            val reply = try {
                try {
                    callSync(
                        gio = gio,
                        glib = glib,
                        connection = sessionConnection,
                        destination = PORTAL_BUS_NAME,
                        objectPath = PORTAL_OBJECT_PATH,
                        interfaceName = PORTAL_FILE_CHOOSER_INTERFACE,
                        methodName = PORTAL_OPEN_FILE_METHOD,
                        parameters = parameters,
                        timeoutMillis = DBUS_CALL_TIMEOUT_MILLIS,
                    )
                } catch (_: IOException) {
                    throw PortalSelectionException("The system folder picker did not complete.")
                }
            } finally {
                glib.g_variant_unref(parameters)
            }
            val requestHandle = try {
                try {
                    readRequestHandle(glib, reply)
                } catch (_: IOException) {
                    throw PortalSelectionException("The system folder picker did not complete.")
                }
            } finally {
                glib.g_variant_unref(reply)
            }

            val response = awaitResponse(glib, context, pendingResponses, requestHandle)
                ?: run {
                    closeRequest(gio, glib, sessionConnection, requestHandle)
                    throw PortalSelectionException("The system folder picker did not respond.")
                }
            return when (response.responseCode) {
                PORTAL_RESPONSE_SUCCESS -> response.selectedUri
                    ?.let(::resolvePortalDirectory)
                    ?: throw PortalSelectionException("The selected folder is unavailable.")
                PORTAL_RESPONSE_CANCELLED -> null
                else -> throw PortalSelectionException("The system folder picker failed.")
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

    private fun sessionBus(gio: GioApi, glib: GlibApi): Pointer {
        val error = PointerByReference()
        val connection = gio.g_bus_get_sync(GBUS_TYPE_SESSION, null, error)
        return connection ?: throw nativeFailure(
            glib,
            error,
            "The system directory picker is unavailable.",
        )
    }

    internal fun openFileParameters(glib: GlibApi, title: String): Pointer {
        val parameterType = glib.g_variant_type_new(OPEN_FILE_PARAMETER_TYPE)
            ?: throw IOException("The system directory picker is unavailable.")
        try {
            val error = PointerByReference()
            return glib.g_variant_parse(
                parameterType,
                linuxPortalOpenOptions(title),
                null,
                null,
                error,
            ) ?: throw nativeFailure(
                glib,
                error,
                "The system directory picker is unavailable.",
            )
        } finally {
            glib.g_variant_type_free(parameterType)
        }
    }

    private fun callSync(
        gio: GioApi,
        glib: GlibApi,
        connection: Pointer,
        destination: String,
        objectPath: String,
        interfaceName: String,
        methodName: String,
        parameters: Pointer?,
        timeoutMillis: Int,
    ): Pointer {
        val error = PointerByReference()
        val reply = gio.g_dbus_connection_call_sync(
            connection,
            destination,
            objectPath,
            interfaceName,
            methodName,
            parameters,
            null,
            DBUS_CALL_FLAGS_NONE,
            timeoutMillis,
            null,
            error,
        )
        return reply ?: throw nativeFailure(
            glib,
            error,
            "The system directory picker is unavailable.",
        )
    }

    internal fun readRequestHandle(glib: GlibApi, reply: Pointer): String {
        val handleValue = glib.g_variant_get_child_value(reply, NativeLong(0))
            ?: throw IOException("The system directory picker is unavailable.")
        val handle = try {
            glib.g_variant_get_string(handleValue, null)
                ?.getString(0, Charsets.UTF_8.name())
                ?: throw IOException("The system directory picker is unavailable.")
        } finally {
            glib.g_variant_unref(handleValue)
        }
        if (!handle.startsWith(PORTAL_REQUEST_PATH_PREFIX)) {
            throw IOException("The system directory picker is unavailable.")
        }
        return handle
    }

    private fun awaitResponse(
        glib: GlibApi,
        context: Pointer,
        pendingResponses: LinkedBlockingQueue<PortalSignal>,
        requestHandle: String,
    ): PortalFileChooserResponse? {
        val deadline = System.nanoTime() +
            TimeUnit.SECONDS.toNanos(PORTAL_RESPONSE_TIMEOUT_SECONDS)
        while (System.nanoTime() < deadline) {
            glib.g_main_context_iteration(context, MAY_NOT_BLOCK)
            while (true) {
                val signal = pendingResponses.poll() ?: break
                if (signal.objectPath == requestHandle) return signal.response
            }
            try {
                Thread.sleep(RESPONSE_POLL_INTERVAL_MILLIS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw PortalSelectionException("The system folder picker was interrupted.")
            }
        }
        return null
    }

    private fun parsePortalSignal(
        glib: GlibApi,
        objectPath: String,
        parameters: Pointer,
    ): PortalSignal? {
        val responseCodeValue = glib.g_variant_get_child_value(parameters, NativeLong(0))
            ?: return null
        val responseCode = try {
            glib.g_variant_get_uint32(responseCodeValue)
        } finally {
            glib.g_variant_unref(responseCodeValue)
        }

        val selectedUri = if (responseCode == PORTAL_RESPONSE_SUCCESS) {
            val results = glib.g_variant_get_child_value(parameters, NativeLong(1))
                ?: return null
            try {
                val uris = glib.g_variant_lookup_value(results, PORTAL_URIS_KEY, null)
                    ?: return PortalSignal(
                        objectPath,
                        portalFileChooserResponse(responseCode, null),
                    )
                try {
                    if (glib.g_variant_n_children(uris).toLong() == 0L) {
                        null
                    } else {
                        val firstUri = glib.g_variant_get_child_value(uris, NativeLong(0))
                            ?: return null
                        try {
                            glib.g_variant_get_string(firstUri, null)
                                ?.getString(0, Charsets.UTF_8.name())
                        } finally {
                            glib.g_variant_unref(firstUri)
                        }
                    }
                } finally {
                    glib.g_variant_unref(uris)
                }
            } finally {
                glib.g_variant_unref(results)
            }
        } else {
            null
        }
        return PortalSignal(
            objectPath,
            portalFileChooserResponse(responseCode, selectedUri),
        )
    }

    private fun closeRequest(
        gio: GioApi,
        glib: GlibApi,
        connection: Pointer,
        requestHandle: String,
    ) {
        val error = PointerByReference()
        val reply = gio.g_dbus_connection_call_sync(
            connection,
            PORTAL_BUS_NAME,
            requestHandle,
            PORTAL_REQUEST_INTERFACE,
            PORTAL_CLOSE_METHOD,
            null,
            null,
            DBUS_CALL_FLAGS_NONE,
            CLOSE_REQUEST_TIMEOUT_MILLIS,
            null,
            error,
        )
        if (reply != null) glib.g_variant_unref(reply)
        error.value?.let(glib::g_error_free)
    }

    private fun nativeFailure(
        glib: GlibApi,
        error: PointerByReference,
        message: String,
    ): IOException {
        error.value?.let(glib::g_error_free)
        return IOException(message)
    }
}

private data class PortalSignal(
    val objectPath: String,
    val response: PortalFileChooserResponse,
)


internal class PortalSelectionException(message: String) : IOException(message)

internal fun shouldUseSwingFallback(failure: IOException): Boolean =
    failure !is PortalSelectionException

internal fun interface PortalSignalCallback : Callback {
    fun invoke(
        connection: Pointer,
        senderName: String?,
        objectPath: String?,
        interfaceName: String?,
        signalName: String?,
        parameters: Pointer,
        userData: Pointer?,
    )
}

internal interface GioApi : Library {
    fun g_bus_get_sync(
        busType: Int,
        cancellable: Pointer?,
        error: PointerByReference,
    ): Pointer?

    fun g_dbus_connection_call_sync(
        connection: Pointer,
        busName: String,
        objectPath: String,
        interfaceName: String,
        methodName: String,
        parameters: Pointer?,
        replyType: Pointer?,
        flags: Int,
        timeoutMsec: Int,
        cancellable: Pointer?,
        error: PointerByReference,
    ): Pointer?

    fun g_dbus_connection_signal_subscribe(
        connection: Pointer,
        sender: String?,
        interfaceName: String?,
        member: String?,
        objectPath: String?,
        arg0: String?,
        flags: Int,
        callback: PortalSignalCallback,
        userData: Pointer?,
        userDataFreeFunc: Callback?,
    ): Int

    fun g_dbus_connection_signal_unsubscribe(connection: Pointer, subscriptionId: Int)
}

internal interface GlibApi : Library {
    fun g_variant_type_new(typeString: String): Pointer?
    fun g_variant_type_free(type: Pointer)
    fun g_variant_parse(
        type: Pointer,
        text: String,
        limit: Pointer?,
        endptr: Pointer?,
        error: PointerByReference,
    ): Pointer?

    fun g_variant_get_child_value(value: Pointer, index: NativeLong): Pointer?
    fun g_variant_get_uint32(value: Pointer): Int
    fun g_variant_lookup_value(
        value: Pointer,
        key: String,
        expectedType: Pointer?,
    ): Pointer?

    fun g_variant_n_children(value: Pointer): NativeLong
    fun g_variant_get_string(value: Pointer, length: Pointer?): Pointer?
    fun g_variant_unref(value: Pointer)
    fun g_main_context_new(): Pointer?
    fun g_main_context_push_thread_default(context: Pointer)
    fun g_main_context_pop_thread_default(context: Pointer)
    fun g_main_context_iteration(context: Pointer?, mayBlock: Int): Int
    fun g_main_context_unref(context: Pointer)
    fun g_error_free(error: Pointer)
}

internal interface GObjectApi : Library {
    fun g_object_unref(instance: Pointer)
}

internal object NativeApis {
    val gio: GioApi by lazy { Native.load("gio-2.0", GioApi::class.java) }
    val glib: GlibApi by lazy { Native.load("glib-2.0", GlibApi::class.java) }
    val gobject: GObjectApi by lazy { Native.load("gobject-2.0", GObjectApi::class.java) }
}

private const val PORTAL_RESPONSE_SUCCESS = 0
private const val PORTAL_RESPONSE_CANCELLED = 1
private const val GBUS_TYPE_SESSION = 2
private const val PORTAL_BUS_NAME = "org.freedesktop.portal.Desktop"
private const val PORTAL_OBJECT_PATH = "/org/freedesktop/portal/desktop"
private const val PORTAL_REQUEST_PATH_PREFIX = "/org/freedesktop/portal/desktop/request/"
private const val PORTAL_FILE_CHOOSER_INTERFACE = "org.freedesktop.portal.FileChooser"
private const val PORTAL_REQUEST_INTERFACE = "org.freedesktop.portal.Request"
private const val PORTAL_RESPONSE_SIGNAL = "Response"
private const val PORTAL_CLOSE_METHOD = "Close"
private const val PORTAL_OPEN_FILE_METHOD = "OpenFile"
private const val PORTAL_URIS_KEY = "uris"
private const val OPEN_FILE_PARAMETER_TYPE = "(ssa{sv})"
private const val DBUS_SIGNAL_FLAGS_NONE = 0
private const val DBUS_CALL_FLAGS_NONE = 0
private const val DBUS_CALL_TIMEOUT_MILLIS = 15_000
private const val CLOSE_REQUEST_TIMEOUT_MILLIS = 5_000
private const val PORTAL_RESPONSE_TIMEOUT_SECONDS = 120L
private const val RESPONSE_POLL_INTERVAL_MILLIS = 50L
private const val MAX_PENDING_RESPONSES = 32
private const val MAX_OBJECT_PATH_LENGTH = 256
private const val MAY_NOT_BLOCK = 0
