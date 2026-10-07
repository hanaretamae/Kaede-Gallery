package com.hanaretamae.kaede.core.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

data class GalleryTagCategoryRule(
    val name: String,
    val path: String,
    val splitDeep: Boolean = false,
)

data class GalleryOtherCategorySettings(
    val enabled: Boolean = true,
    val name: String = "Other",
    val splitDeep: Boolean = false,
)

data class GalleryTagCategorySettings(
    val categories: List<GalleryTagCategoryRule>,
    val other: GalleryOtherCategorySettings = GalleryOtherCategorySettings(),
)

object GalleryTagCategoryCodec {
    const val MAX_CATEGORIES = 64
    private val json = Json

    val DEFAULT_CATEGORIES_EN = listOf(
        GalleryTagCategoryRule("Source", "source/art"),
        GalleryTagCategoryRule("People", "source/count/*"),
        GalleryTagCategoryRule("Art style", "source/format/*"),
        GalleryTagCategoryRule("Gender", "source/gender/*"),
        GalleryTagCategoryRule("Meta", "source/meta/*"),
        GalleryTagCategoryRule("Rating", "source/rating/*"),
        GalleryTagCategoryRule("Source", "source/*"),
        GalleryTagCategoryRule("Type", "source/type/*"),
        GalleryTagCategoryRule("Works", "copyright/*"),
    )
    val DEFAULT_CATEGORIES_JA = listOf(
        GalleryTagCategoryRule("ソース", "source/art"),
        GalleryTagCategoryRule("人数", "source/count/*"),
        GalleryTagCategoryRule("アートスタイル", "source/format/*"),
        GalleryTagCategoryRule("性別", "source/gender/*"),
        GalleryTagCategoryRule("メタ", "source/meta/*"),
        GalleryTagCategoryRule("レーティング", "source/rating/*"),
        GalleryTagCategoryRule("ソース", "source/*"),
        GalleryTagCategoryRule("タイプ", "source/type/*"),
        GalleryTagCategoryRule("作品", "copyright/*"),
    )

    fun decodeSettings(
        tagSettingsJson: String?,
        japaneseDefaults: Boolean,
    ): GalleryTagCategorySettings {
        if (tagSettingsJson == null) {
            return GalleryTagCategorySettings(
                if (japaneseDefaults) DEFAULT_CATEGORIES_JA else DEFAULT_CATEGORIES_EN,
                GalleryOtherCategorySettings(
                    name = if (japaneseDefaults) "その他" else "Other",
                ),
            )
        }
        val tags = parseObject(tagSettingsJson)
            ?: throw IllegalArgumentException("Stored tag settings are invalid")
        val categoryElement = tags["tagCategories"]?.takeUnless { it == JsonNull }
        val categories = categoryElement?.let {
            it as? JsonObject
                ?: throw IllegalArgumentException("Stored tag category settings are invalid")
        }
            ?: return GalleryTagCategorySettings(
                if (japaneseDefaults) DEFAULT_CATEGORIES_JA else DEFAULT_CATEGORIES_EN,
                GalleryOtherCategorySettings(
                    name = if (japaneseDefaults) "その他" else "Other",
                ),
            )
        val rules = categories["categories"]?.takeUnless { it == JsonNull }?.let {
            it as? JsonArray
                ?: throw IllegalArgumentException("Stored tag category rules are invalid")
        }
        val decodedRules = rules?.map { value ->
            val rule = value as? JsonObject
                ?: throw IllegalArgumentException("Stored tag category rules are invalid")
            val splitDeep = optionalBoolean(rule, "splitDeep", false)
                ?: throw IllegalArgumentException("Stored tag category rules are invalid")
            GalleryTagCategoryRule(
                name = string(rule, "name")
                    ?: throw IllegalArgumentException("Stored tag category rules are invalid"),
                path = string(rule, "path")
                    ?: throw IllegalArgumentException("Stored tag category rules are invalid"),
                splitDeep = splitDeep,
            )
        } ?: if (japaneseDefaults) DEFAULT_CATEGORIES_JA else DEFAULT_CATEGORIES_EN
        val otherElement = categories["other"]?.takeUnless { it == JsonNull }
        val otherObject = otherElement?.let {
            it as? JsonObject
                ?: throw IllegalArgumentException("Stored other category settings are invalid")
        }
        val other = if (otherObject == null) {
            GalleryOtherCategorySettings(
                name = if (japaneseDefaults) "その他" else "Other",
            )
        } else {
            val name = if (otherObject["name"] == null) {
                if (japaneseDefaults) "その他" else "Other"
            } else {
                string(otherObject, "name")
                    ?: throw IllegalArgumentException("Stored other category settings are invalid")
            }
            GalleryOtherCategorySettings(
                enabled = optionalBoolean(otherObject, "enabled", true)
                    ?: throw IllegalArgumentException("Stored other category settings are invalid"),
                name = name,
                splitDeep = optionalBoolean(otherObject, "splitDeep", false)
                    ?: throw IllegalArgumentException("Stored other category settings are invalid"),
            )
        }
        val settings = GalleryTagCategorySettings(decodedRules, other)
        require(isValid(settings)) { "Stored tag category rules are invalid" }
        return settings
    }

    fun isValid(settings: GalleryTagCategorySettings): Boolean =
        settings.categories.size <= MAX_CATEGORIES &&
            settings.categories.all { rule ->
                rule.name.isNotBlank() &&
                    rule.name.encodeToByteArray().size <= 128 &&
                    isValidPath(rule.path)
            } &&
            settings.other.name.isNotBlank() &&
            settings.other.name.encodeToByteArray().size <= 128

    fun update(
        tagSettingsJson: String?,
        settings: GalleryTagCategorySettings,
    ): String {
        require(isValid(settings)) { "Tag category settings are invalid" }
        val original = tagSettingsJson?.let(::parseObject)
            ?: if (tagSettingsJson == null) defaultTags() else {
                throw IllegalArgumentException("Stored tag settings are invalid")
            }
        val categories = buildJsonObject {
            put(
                "categories",
                JsonArray(settings.categories.map { rule ->
                    buildJsonObject {
                        put("name", JsonPrimitive(rule.name))
                        put("path", JsonPrimitive(rule.path))
                        put("splitDeep", JsonPrimitive(rule.splitDeep))
                    }
                }),
            )
            put(
                "other",
                buildJsonObject {
                    put("enabled", JsonPrimitive(settings.other.enabled))
                    put("name", JsonPrimitive(settings.other.name))
                    put("splitDeep", JsonPrimitive(settings.other.splitDeep))
                },
            )
        }
        return json.encodeToString(
            JsonObject.serializer(),
            JsonObject(original.toMutableMap().apply { put("tagCategories", categories) }),
        )
    }

    fun encodeRustSettings(
        prefixes: List<String>,
        tagSettingsJson: String?,
    ): String {
        require(GalleryTagPrefixesCodec.isValid(prefixes)) {
            "galleryTagPrefixes are invalid"
        }
        val tags = tagSettingsJson?.let(::parseObject)
            ?: if (tagSettingsJson == null) defaultTags() else {
                throw IllegalArgumentException("Stored tag settings are invalid")
            }
        val structureElement = tags["noteStructure"]?.takeUnless { it == JsonNull }
        val noteStructure = structureElement?.let {
            it as? JsonObject
                ?: throw IllegalArgumentException("Stored note structure settings are invalid")
        }?.toMutableMap() ?: JsonObject(emptyMap()).toMutableMap()
        noteStructure["galleryTagPrefixes"] = JsonArray(prefixes.map(::JsonPrimitive))
        val categoryElement = tags["tagCategories"]?.takeUnless { it == JsonNull }
        categoryElement?.let {
            if (it !is JsonObject) {
                throw IllegalArgumentException("Stored tag category settings are invalid")
            }
            decodeSettings(
                json.encodeToString(JsonObject.serializer(), tags),
                japaneseDefaults = false,
            )
        }
        val rustSettings = mutableMapOf(
            "noteStructure" to JsonObject(noteStructure),
        )
        categoryElement?.let {
            rustSettings["tagCategories"] = it as JsonObject
        }
        val encoded = json.encodeToString(
            JsonObject.serializer(),
            JsonObject(rustSettings),
        )
        require(encoded.encodeToByteArray().size <= GalleryTagPrefixesCodec.MAX_ENCODED_LENGTH) {
            "Rust tag settings exceed their size limit"
        }
        return encoded
    }

    private fun defaultTags(): JsonObject = buildJsonObject {
        put("includedPrefixes", JsonArray(listOf(JsonPrimitive("*"))))
        put(
            "hiddenPrefixes",
            JsonArray(GalleryTagDisplayPrefixesCodec.DEFAULT_HIDDEN.map(::JsonPrimitive)),
        )
        put("colors", GalleryTagColorCodec.encodeRules(GalleryTagColorCodec.DEFAULT_RULES))
    }

    private fun isValidPath(path: String): Boolean {
        if (path == "*") return true
        val base = path.removeSuffix("/*")
        return base.isNotEmpty() &&
            base.encodeToByteArray().size <= 256 &&
            !base.contains('*') &&
            base.split('/').all { it.isNotBlank() }
    }

    private fun string(obj: JsonObject, key: String): String? =
        (obj[key] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

    private fun optionalBoolean(obj: JsonObject, key: String, default: Boolean): Boolean? {
        val value = obj[key] ?: return default
        return (value as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
    }

    private fun parseObject(content: String): JsonObject? = try {
        json.parseToJsonElement(content) as? JsonObject
    } catch (_: IllegalArgumentException) {
        null
    }
}
