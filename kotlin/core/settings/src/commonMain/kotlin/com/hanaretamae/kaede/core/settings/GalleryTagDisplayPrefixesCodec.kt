package com.hanaretamae.kaede.core.settings

object GalleryTagDisplayPrefixesCodec {
    val DEFAULT_INCLUDED = listOf("*")
    val DEFAULT_HIDDEN = listOf("moc", "add", "pin", "source/art")

    fun isValid(prefixes: List<String>): Boolean =
        GalleryTagPrefixesCodec.isValid(prefixes, allowDuplicates = true)

    fun encodeStorage(prefixes: List<String>): String =
        GalleryTagPrefixesCodec.encodeStorage(prefixes)

    fun decodeStorage(value: String): List<String>? =
        GalleryTagPrefixesCodec.decodeStorage(value, allowDuplicates = true)
}

fun isGalleryTagDisplayed(
    tag: String,
    includedPrefixes: List<String>,
    hiddenPrefixes: List<String>,
): Boolean {
    fun matches(prefix: String): Boolean =
        prefix == "*" || tag == prefix || tag.startsWith("$prefix/")

    return includedPrefixes.any(::matches) && hiddenPrefixes.none(::matches)
}
