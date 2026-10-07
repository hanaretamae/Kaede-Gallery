package com.hanaretamae.kaede.core.settings

object GalleryTagPrefixesCodec {
    const val MAX_PREFIXES = 64
    const val MAX_PREFIX_LENGTH = 128
    const val MAX_ENCODED_LENGTH = 16 * 1024
    val DEFAULT_PREFIXES = listOf("source/art")

    fun isValid(prefixes: List<String>, allowDuplicates: Boolean = false): Boolean =
        prefixes.size <= MAX_PREFIXES &&
            (allowDuplicates || prefixes.toSet().size == prefixes.size) &&
            prefixes.all { prefix ->
                prefix.isNotEmpty() &&
                    prefix.length <= MAX_PREFIX_LENGTH &&
                    prefix.encodeToByteArray().size <= MAX_PREFIX_LENGTH &&
                    prefix == prefix.trim() &&
                    prefix.none { it.code < 0x20 || it.code in 0x7f..0x9f } &&
                    prefix.hasValidUnicodeScalars()
            } &&
            encodeStorage(prefixes).length <= MAX_ENCODED_LENGTH

    fun encodeStorage(prefixes: List<String>): String = buildString {
        prefixes.forEach { prefix ->
            append(prefix.length)
            append(':')
            append(prefix)
        }
    }

    fun decodeStorage(value: String, allowDuplicates: Boolean = false): List<String>? {
        if (value.length > MAX_ENCODED_LENGTH) return null
        val prefixes = mutableListOf<String>()
        var offset = 0
        while (offset < value.length) {
            if (prefixes.size == MAX_PREFIXES) return null
            val delimiter = value.indexOf(':', offset)
            if (delimiter <= offset || delimiter - offset > 3) return null
            val encodedLength = value.substring(offset, delimiter)
            val length = encodedLength.toIntOrNull()
                ?.takeIf { it in 1..MAX_PREFIX_LENGTH }
                ?: return null
            if (length.toString() != encodedLength) return null
            val start = delimiter + 1
            val end = start + length
            if (end > value.length) return null
            prefixes += value.substring(start, end)
            offset = end
        }
        return prefixes.takeIf { isValid(it, allowDuplicates) }
    }

    fun encodeRustSettings(prefixes: List<String>): String {
        require(isValid(prefixes)) { "galleryTagPrefixes are invalid" }
        return """{"noteStructure":{"galleryTagPrefixes":[${prefixes.joinToString(",") { it.toJsonString() }}]}}"""
    }

    private fun String.toJsonString(): String = buildString {
        append('"')
        for (character in this@toJsonString) {
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }

    private fun String.hasValidUnicodeScalars(): Boolean {
        var index = 0
        while (index < length) {
            val value = this[index].code
            when (value) {
                in 0xd800..0xdbff -> {
                    if (index + 1 == length || this[index + 1].code !in 0xdc00..0xdfff) {
                        return false
                    }
                    index += 2
                }
                in 0xdc00..0xdfff -> return false
                else -> index++
            }
        }
        return true
    }
}
