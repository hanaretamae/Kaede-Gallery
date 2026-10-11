package com.hanaretamae.kaede.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPathsTest {
    @Test
    fun stripsWindowsVerbatimPrefixes() {
        assertEquals("C:\\Vault\\a.png", stripWindowsVerbatimPrefix("\\\\?\\C:\\Vault\\a.png"))
        assertEquals("\\\\host\\share\\a.png", stripWindowsVerbatimPrefix("\\\\?\\UNC\\host\\share\\a.png"))
        assertEquals("/home/a.png", stripWindowsVerbatimPrefix("/home/a.png"))
    }

    @Test
    fun fallsBackToPathLookupWithoutBundledTool() {
        assertEquals("kaede-nonexistent-tool", desktopToolCommand("kaede-nonexistent-tool", "Linux"))
    }
}

class WindowsIntegrationTest {
    @Test
    fun parsesRegistryDwordAndAccent() {
        val output = "\r\nHKEY_CURRENT_USER\\x\r\n    AccentColor    REG_DWORD    0xffd77800\r\n"
        val value = parseWindowsRegistryDword(output)
        assertEquals(0xffd77800L, value)
        val accent = parseWindowsAccentColor(value)!!
        assertEquals(0x00, (accent.red * 255).toInt())
        assertEquals(0x78, (accent.green * 255).toInt())
        assertEquals(0xd7, (accent.blue * 255).toInt())
        assertEquals(null, parseWindowsRegistryDword("ERROR"))
    }

    @Test
    fun folderPickerUsesEncodedCommand() {
        val command = windowsFolderPickerCommand()
        assertEquals("powershell.exe", command.first())
        assertEquals("-EncodedCommand", command[command.size - 2])
    }
}
