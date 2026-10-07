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

        assertNotNull(apache)
        assertNotNull(lgpl)
        assertNotNull(rust)
        assertTrue(apache.contains("Apache License"))
        assertTrue(lgpl.contains("GNU LESSER GENERAL PUBLIC LICENSE"))
        assertTrue(rust.contains("Mozilla Public License Version 2.0"))
        assertTrue(rust.contains("uniffi_pipeline 0.32.0"))
    }
}
