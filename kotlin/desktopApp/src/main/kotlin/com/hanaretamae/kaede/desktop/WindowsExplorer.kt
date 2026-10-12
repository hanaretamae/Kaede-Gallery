package com.hanaretamae.kaede.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import java.nio.file.Path

internal interface WindowsShell : StdCallLibrary {
    fun SHParseDisplayName(
        name: WString,
        bindContext: Pointer?,
        pidl: PointerByReference,
        attributes: Int,
        resultAttributes: Pointer?,
    ): Int

    fun ILFindLastID(pidl: Pointer): Pointer?
    fun SHOpenFolderAndSelectItems(
        folder: Pointer,
        count: Int,
        items: PointerByReference,
        flags: Int,
    ): Int
}

internal interface WindowsCom : StdCallLibrary {
    fun CoInitializeEx(reserved: Pointer?, flags: Int): Int
    fun CoUninitialize()
    fun CoTaskMemFree(pointer: Pointer?)
}

private val windowsShell by lazy { Native.load("shell32", WindowsShell::class.java) }
private val windowsCom by lazy { Native.load("ole32", WindowsCom::class.java) }

internal fun selectInWindowsExplorer(
    media: Path,
    shell: WindowsShell = windowsShell,
    com: WindowsCom = windowsCom,
): Boolean {
    val init = com.CoInitializeEx(null, 2)
    if (init < 0) return false
    try {
        val folder = PointerByReference()
        val file = PointerByReference()
        try {
            if (shell.SHParseDisplayName(WString(media.parent.toString()), null, folder, 0, null) < 0 ||
                folder.value == null
            ) return false
            if (shell.SHParseDisplayName(WString(media.toString()), null, file, 0, null) < 0 ||
                file.value == null
            ) return false
            val item = shell.ILFindLastID(file.value) ?: return false
            return shell.SHOpenFolderAndSelectItems(
                folder.value, 1, PointerByReference(item), 0,
            ) >= 0
        } finally {
            com.CoTaskMemFree(file.value)
            com.CoTaskMemFree(folder.value)
        }
    } finally {
        com.CoUninitialize()
    }
}
