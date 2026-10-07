package com.hanaretamae.kaede.core.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

enum class GalleryLinkResolution {
    SHORTEST_PATH,
    RELATIVE_PATH,
    ABSOLUTE_PATH,
}

enum class GalleryNoteBlock {
    AUTHOR,
    MEDIA,
    POST_TEXT,
    POST_TEXT_END,
    RELATED,
    MEMO;

    companion object {
        fun fromFlutterName(value: String): GalleryNoteBlock? = when (value) {
            "author" -> AUTHOR
            "media" -> MEDIA
            "postText" -> POST_TEXT
            "postTextEnd" -> POST_TEXT_END
            "related" -> RELATED
            "memo" -> MEMO
            else -> null
        }
    }
}

private val defaultNoteBlockOrder = listOf(
    GalleryNoteBlock.AUTHOR,
    GalleryNoteBlock.MEDIA,
    GalleryNoteBlock.POST_TEXT,
    GalleryNoteBlock.POST_TEXT_END,
    GalleryNoteBlock.RELATED,
    GalleryNoteBlock.MEMO,
)

data class GalleryFrontmatterSettings(
    val tagsKeys: List<String> = listOf("tags"),
    val titleKeys: List<String> = listOf("title"),
    val urlKeys: List<String> = listOf("url"),
    val publishedKeys: List<String> = listOf("published"),
    val createdKeys: List<String> = listOf("created"),
    val updatedKeys: List<String> = listOf("updated"),
    val coverKeys: List<String> = listOf("cover"),
)

data class GalleryNoteStructureSettings(
    val memoHeadings: List<String> = listOf("覚書", "メモ", "Memo", "Notes"),
    val relatedHeadings: List<String> = listOf("関連", "Related"),
    val postTextEndHeadings: List<String> = listOf("文書", "Document"),
    val galleryTagPrefixes: List<String> = GalleryTagPrefixesCodec.DEFAULT_PREFIXES,
    val frontmatter: GalleryFrontmatterSettings = GalleryFrontmatterSettings(),
    val linkResolution: GalleryLinkResolution = GalleryLinkResolution.RELATIVE_PATH,
    val postTextIncludeQuote: Boolean = true,
    val blockOrder: List<GalleryNoteBlock> = defaultNoteBlockOrder,
    val hiddenBlocks: List<GalleryNoteBlock> = emptyList(),
)

object GalleryNoteStructureCodec {
    private val json = Json

    fun decodeSettings(
        tagSettingsJson: String?,
        galleryTagPrefixes: List<String>,
    ): GalleryNoteStructureSettings {
        if (tagSettingsJson == null) {
            return GalleryNoteStructureSettings(galleryTagPrefixes = galleryTagPrefixes)
        }
        val tags = parseObject(tagSettingsJson)
            ?: throw IllegalArgumentException("Stored tag settings are invalid")
        val structureElement = tags["noteStructure"]?.takeUnless { it == JsonNull }
        val structure = structureElement?.let {
            it as? JsonObject
                ?: throw IllegalArgumentException("Stored note structure settings are invalid")
        } ?: JsonObject(emptyMap())
        val frontmatterElement = structure["frontmatter"]?.takeUnless { it == JsonNull }
        val frontmatter = frontmatterElement?.let {
            it as? JsonObject
                ?: throw IllegalArgumentException("Stored frontmatter settings are invalid")
        } ?: JsonObject(emptyMap())
        val settings = GalleryNoteStructureSettings(
            memoHeadings = stringList(
                structure,
                "memoHeadings",
                GalleryNoteStructureSettings().memoHeadings,
                MAX_HEADING_BYTES,
            ),
            relatedHeadings = stringList(
                structure,
                "relatedHeadings",
                GalleryNoteStructureSettings().relatedHeadings,
                MAX_HEADING_BYTES,
            ),
            postTextEndHeadings = stringList(
                structure,
                "postTextEndHeadings",
                GalleryNoteStructureSettings().postTextEndHeadings,
                MAX_HEADING_BYTES,
            ),
            galleryTagPrefixes = galleryTagPrefixes,
            frontmatter = GalleryFrontmatterSettings(
                tagsKeys = stringList(frontmatter, "tagsKeys", listOf("tags"), MAX_KEY_BYTES),
                titleKeys = stringList(frontmatter, "titleKeys", listOf("title"), MAX_KEY_BYTES),
                urlKeys = stringList(frontmatter, "urlKeys", listOf("url"), MAX_KEY_BYTES),
                publishedKeys = stringList(
                    frontmatter,
                    "publishedKeys",
                    listOf("published"),
                    MAX_KEY_BYTES,
                ),
                createdKeys = stringList(
                    frontmatter,
                    "createdKeys",
                    listOf("created"),
                    MAX_KEY_BYTES,
                ),
                updatedKeys = stringList(
                    frontmatter,
                    "updatedKeys",
                    listOf("updated"),
                    MAX_KEY_BYTES,
                ),
                coverKeys = stringList(frontmatter, "coverKeys", listOf("cover"), MAX_KEY_BYTES),
            ),
            linkResolution = when (string(structure, "linkResolution", "relativePath")) {
                "shortestPath" -> GalleryLinkResolution.SHORTEST_PATH
                "relativePath" -> GalleryLinkResolution.RELATIVE_PATH
                "absolutePath" -> GalleryLinkResolution.ABSOLUTE_PATH
                else -> throw IllegalArgumentException("Stored link resolution is invalid")
            },
            postTextIncludeQuote = boolean(structure, "postTextIncludeQuote", true),
            blockOrder = noteBlockOrder(structure),
            hiddenBlocks = hiddenNoteBlocks(structure),
        )
        require(isValid(settings)) { "Stored note structure settings are invalid" }
        return settings
    }

    fun isValid(settings: GalleryNoteStructureSettings): Boolean =
        GalleryTagPrefixesCodec.isValid(settings.galleryTagPrefixes) &&
            validHeadings(settings.memoHeadings) &&
            validHeadings(settings.relatedHeadings) &&
            validHeadings(settings.postTextEndHeadings) &&
            validKeys(settings.frontmatter.tagsKeys, mustNotBeEmpty = true) &&
            validKeys(settings.frontmatter.titleKeys) &&
            validKeys(settings.frontmatter.urlKeys) &&
            validKeys(settings.frontmatter.publishedKeys) &&
            validKeys(settings.frontmatter.createdKeys) &&
            validKeys(settings.frontmatter.updatedKeys) &&
            validKeys(settings.frontmatter.coverKeys) &&
            settings.blockOrder.size == GalleryNoteBlock.entries.size &&
            settings.blockOrder.toSet().size == GalleryNoteBlock.entries.size &&
            settings.hiddenBlocks.distinct().size == settings.hiddenBlocks.size &&
            GalleryNoteBlock.POST_TEXT_END !in settings.hiddenBlocks

    fun update(
        tagSettingsJson: String?,
        settings: GalleryNoteStructureSettings,
    ): String {
        require(isValid(settings)) { "Note structure settings are invalid" }
        val original = tagSettingsJson?.let(::parseObject)
            ?: if (tagSettingsJson == null) defaultTags() else {
                throw IllegalArgumentException("Stored tag settings are invalid")
            }
        val structureElement = original["noteStructure"]?.takeUnless { it == JsonNull }
        val structure = (
            structureElement?.let {
                it as? JsonObject
                    ?: throw IllegalArgumentException("Stored note structure settings are invalid")
            } ?: JsonObject(emptyMap())
            ).toMutableMap()
        structure["memoHeadings"] = JsonArray(settings.memoHeadings.map(::JsonPrimitive))
        structure["relatedHeadings"] = JsonArray(settings.relatedHeadings.map(::JsonPrimitive))
        structure["postTextEndHeadings"] =
            JsonArray(settings.postTextEndHeadings.map(::JsonPrimitive))
        structure["galleryTagPrefixes"] =
            JsonArray(settings.galleryTagPrefixes.map(::JsonPrimitive))
        structure["frontmatter"] = buildJsonObject {
            put("tagsKeys", JsonArray(settings.frontmatter.tagsKeys.map(::JsonPrimitive)))
            put("titleKeys", JsonArray(settings.frontmatter.titleKeys.map(::JsonPrimitive)))
            put("urlKeys", JsonArray(settings.frontmatter.urlKeys.map(::JsonPrimitive)))
            put("publishedKeys", JsonArray(settings.frontmatter.publishedKeys.map(::JsonPrimitive)))
            put("createdKeys", JsonArray(settings.frontmatter.createdKeys.map(::JsonPrimitive)))
            put("updatedKeys", JsonArray(settings.frontmatter.updatedKeys.map(::JsonPrimitive)))
            put("coverKeys", JsonArray(settings.frontmatter.coverKeys.map(::JsonPrimitive)))
        }
        structure["linkResolution"] = JsonPrimitive(
            when (settings.linkResolution) {
                GalleryLinkResolution.SHORTEST_PATH -> "shortestPath"
                GalleryLinkResolution.RELATIVE_PATH -> "relativePath"
                GalleryLinkResolution.ABSOLUTE_PATH -> "absolutePath"
            },
        )
        structure["postTextIncludeQuote"] = JsonPrimitive(settings.postTextIncludeQuote)
        structure["blockOrder"] = JsonArray(
            settings.blockOrder.map { JsonPrimitive(it.toFlutterName()) },
        )
        structure["hiddenBlocks"] = JsonArray(
            settings.hiddenBlocks.map { JsonPrimitive(it.toFlutterName()) },
        )
        return json.encodeToString(
            JsonObject.serializer(),
            JsonObject(original.toMutableMap().apply { put("noteStructure", JsonObject(structure)) }),
        )
    }

    private fun validHeadings(values: List<String>): Boolean =
        values.all { it.isNotBlank() && it.encodeToByteArray().size <= MAX_HEADING_BYTES }

    private fun validKeys(values: List<String>, mustNotBeEmpty: Boolean = false): Boolean =
        (!mustNotBeEmpty || values.isNotEmpty()) &&
            values.all { it.isNotBlank() && it.encodeToByteArray().size <= MAX_KEY_BYTES }

    private fun stringList(
        values: JsonObject,
        key: String,
        default: List<String>,
        maxLength: Int,
    ): List<String> {
        val element = values[key]?.takeUnless { it == JsonNull } ?: return default
        val array = element as? JsonArray
            ?: throw IllegalArgumentException("Stored note structure lists are invalid")
        return array.map { item ->
            val value = (item as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content
                ?: throw IllegalArgumentException("Stored note structure lists are invalid")
            require(value.isNotBlank() && value.encodeToByteArray().size <= maxLength) {
                "Stored note structure lists are invalid"
            }
            value
        }
    }

    private fun string(values: JsonObject, key: String, default: String): String {
        val element = values[key]?.takeUnless { it == JsonNull } ?: return default
        return (element as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content
            ?: throw IllegalArgumentException("Stored note structure values are invalid")
    }

    private fun boolean(values: JsonObject, key: String, default: Boolean): Boolean {
        val element = values[key]?.takeUnless { it == JsonNull } ?: return default
        return (element as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
            ?: throw IllegalArgumentException("Stored note structure values are invalid")
    }

    private fun noteBlockOrder(values: JsonObject): List<GalleryNoteBlock> {
        val element = values["blockOrder"]?.takeUnless { it == JsonNull }
            ?: return defaultNoteBlockOrder
        val array = element as? JsonArray
            ?: throw IllegalArgumentException("Stored note block order is invalid")
        val blocks = array.map { item ->
            val name = (item as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content
                ?: throw IllegalArgumentException("Stored note block order is invalid")
            GalleryNoteBlock.fromFlutterName(name)
                ?: throw IllegalArgumentException("Stored note block order is invalid")
        }
        require(blocks.distinct().size == blocks.size) {
            "Stored note block order is invalid"
        }
        val legacyDefaultOrders = listOf(
            listOf(
                GalleryNoteBlock.AUTHOR,
                GalleryNoteBlock.MEDIA,
                GalleryNoteBlock.POST_TEXT,
                GalleryNoteBlock.MEMO,
                GalleryNoteBlock.RELATED,
                GalleryNoteBlock.POST_TEXT_END,
            ),
            listOf(
                GalleryNoteBlock.AUTHOR,
                GalleryNoteBlock.MEDIA,
                GalleryNoteBlock.POST_TEXT,
                GalleryNoteBlock.RELATED,
                GalleryNoteBlock.MEMO,
                GalleryNoteBlock.POST_TEXT_END,
            ),
        )
        if (blocks in legacyDefaultOrders) return defaultNoteBlockOrder
        val ordered = blocks.toMutableList()
        defaultNoteBlockOrder.forEach { if (it !in ordered) ordered += it }
        return ordered
    }

    private fun hiddenNoteBlocks(values: JsonObject): List<GalleryNoteBlock> {
        val element = values["hiddenBlocks"]?.takeUnless { it == JsonNull } ?: return emptyList()
        val array = element as? JsonArray
            ?: throw IllegalArgumentException("Stored hidden note blocks are invalid")
        val blocks = array.map { item ->
            val name = (item as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content
                ?: throw IllegalArgumentException("Stored hidden note blocks are invalid")
            GalleryNoteBlock.fromFlutterName(name)
                ?.takeUnless { it == GalleryNoteBlock.POST_TEXT_END }
                ?: throw IllegalArgumentException("Stored hidden note blocks are invalid")
        }
        require(blocks.distinct().size == blocks.size) {
            "Stored hidden note blocks are invalid"
        }
        return blocks
    }

    private fun GalleryNoteBlock.toFlutterName(): String = when (this) {
        GalleryNoteBlock.AUTHOR -> "author"
        GalleryNoteBlock.MEDIA -> "media"
        GalleryNoteBlock.POST_TEXT -> "postText"
        GalleryNoteBlock.POST_TEXT_END -> "postTextEnd"
        GalleryNoteBlock.RELATED -> "related"
        GalleryNoteBlock.MEMO -> "memo"
    }

    private fun defaultTags(): JsonObject = buildJsonObject {
        put("includedPrefixes", JsonArray(listOf(JsonPrimitive("*"))))
        put(
            "hiddenPrefixes",
            JsonArray(GalleryTagDisplayPrefixesCodec.DEFAULT_HIDDEN.map(::JsonPrimitive)),
        )
        put("colors", GalleryTagColorCodec.encodeRules(GalleryTagColorCodec.DEFAULT_RULES))
    }

    private fun parseObject(content: String): JsonObject? = try {
        json.parseToJsonElement(content) as? JsonObject
    } catch (_: IllegalArgumentException) {
        null
    }

    private const val MAX_HEADING_BYTES = 256
    private const val MAX_KEY_BYTES = 128
}
