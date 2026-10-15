package com.hanaretamae.kaede.desktop

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BundledLicenseResourcesTest {
    @Test
    fun desktopLicenseResourcesContainFullTextAndRustNotices() {
        val classLoader = javaClass.classLoader
        val apache = classLoader.getResourceAsStream("licenses/Apache-2.0.txt")
            ?.bufferedReader()
            ?.use { it.readText() }
        val lgpl = classLoader.getResourceAsStream("licenses/LGPL-2.1.txt")
            ?.bufferedReader()
            ?.use { it.readText() }
        val rust = classLoader.getResourceAsStream("licenses/RUST-DEPENDENCY-LICENSES.txt")
            ?.bufferedReader()
            ?.use { it.readText() }
        val googleSansFlex =
            classLoader.getResourceAsStream("licenses/GoogleSansFlex-OFL.txt")
                ?.bufferedReader()
                ?.use { it.readText() }
        val application =
            classLoader.getResourceAsStream("licenses/Kaede-Gallery-MIT.txt")
                ?.bufferedReader()
                ?.use { it.readText() }
        val notices =
            classLoader.getResourceAsStream("licenses/THIRD_PARTY_NOTICES.md")
                ?.bufferedReader()
                ?.use { it.readText() }
        val runtime =
            classLoader.getResourceAsStream("licenses/Runtime-LICENSES.txt")
                ?.bufferedReader()
                ?.use { it.readText() }

        assertNotNull(apache)
        assertNotNull(lgpl)
        assertNotNull(rust)
        assertNotNull(googleSansFlex)
        assertNotNull(application)
        assertNotNull(notices)
        assertNotNull(runtime)
        assertTrue(apache.contains("Apache License"))
        assertTrue(lgpl.contains("GNU LESSER GENERAL PUBLIC LICENSE"))
        assertTrue(rust.contains("Mozilla Public License Version 2.0"))
        assertTrue(rust.contains("uniffi_pipeline 0.32.0"))
        assertTrue(googleSansFlex.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(application.contains("MIT License"))
        assertTrue(notices.contains("Linux desktop packages"))
        assertTrue(notices.contains("Windows desktop distributions"))
        assertTrue(runtime.contains("GPL-3.0.txt"))
        assertTrue(runtime.contains("LGPL-3.0.txt"))
        assertTrue(runtime.contains("MICROSOFT-WINDOWS-SDK-REDIST.txt"))
    }
}
