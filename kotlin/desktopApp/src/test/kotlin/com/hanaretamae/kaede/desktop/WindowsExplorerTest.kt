package com.hanaretamae.kaede.desktop

import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.PointerByReference
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowsExplorerTest {
    @Test
    fun selectsTheFileThroughShellApiWithoutLaunchingExplorer() {
        val media = Path.of("/fictional vault/image with spaces.png")
        val parsed = mutableListOf<String>()
        val released = mutableListOf<Pointer?>()
        val folderPidl = Pointer.createConstant(1)
        val file = Pointer.createConstant(2)
        val child = Pointer.createConstant(3)
        var selected = false
        var uninitialized = false
        val shell = object : WindowsShell {
            override fun SHParseDisplayName(
                name: WString, bindContext: Pointer?, pidl: PointerByReference,
                attributes: Int, resultAttributes: Pointer?,
            ): Int {
                parsed += name.toString()
                pidl.value = if (parsed.size == 1) folderPidl else file
                return 0
            }

            override fun ILFindLastID(pidl: Pointer): Pointer? {
                assertEquals(file, pidl)
                return child
            }

            override fun SHOpenFolderAndSelectItems(
                folder: Pointer, count: Int, items: PointerByReference, flags: Int,
            ): Int {
                assertEquals(folderPidl, folder)
                assertEquals(1, count)
                assertEquals(child, items.value)
                selected = true
                return 0
            }
        }
        val com = object : WindowsCom {
            override fun CoInitializeEx(reserved: Pointer?, flags: Int): Int {
                assertEquals(2, flags)
                return 0
            }

            override fun CoUninitialize() { uninitialized = true }
            override fun CoTaskMemFree(pointer: Pointer?) { released += pointer }
        }

        assertTrue(selectInWindowsExplorer(media, shell, com))
        assertEquals(listOf(media.parent.toString(), media.toString()), parsed)
        assertTrue(selected)
        assertTrue(uninitialized)
        assertEquals(listOf(file, folderPidl), released)
    }

    @Test
    fun failedShellSelectionIsNotReportedAsSuccess() {
        val shell = object : WindowsShell {
            override fun SHParseDisplayName(
                name: WString, bindContext: Pointer?, pidl: PointerByReference,
                attributes: Int, resultAttributes: Pointer?,
            ): Int = -1
            override fun ILFindLastID(pidl: Pointer): Pointer? = error("Not reached")
            override fun SHOpenFolderAndSelectItems(
                folder: Pointer, count: Int, items: PointerByReference, flags: Int,
            ): Int = error("Not reached")
        }
        var uninitialized = false
        val com = object : WindowsCom {
            override fun CoInitializeEx(reserved: Pointer?, flags: Int): Int = 0
            override fun CoUninitialize() { uninitialized = true }
            override fun CoTaskMemFree(pointer: Pointer?) = Unit
        }

        assertFalse(selectInWindowsExplorer(Path.of("/fictional/image.png"), shell, com))
        assertTrue(uninitialized)
    }
}
