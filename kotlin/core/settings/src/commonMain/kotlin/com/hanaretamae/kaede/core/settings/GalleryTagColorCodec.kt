package com.hanaretamae.kaede.core.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

data class GalleryTagColorRule(
    val prefix: String,
    val color: Int,
)

object GalleryTagColorCodec {
    const val MAX_RULES = 64
    private val json = Json
    private val colorPattern = Regex("#[0-9a-fA-F]{6}")

    val DEFAULT_RULES_HEX = listOf(
        "source/gender/female" to "f59eee",
        "source/gender/male" to "5d5bec",
        "source/gender/unknown" to "b17af5",
        "source/gender/futanari" to "ee8b95",
        "source/type" to "ffaa7f",
        "source/count" to "ffffff",
        "source/service/x" to "ffffff",
        "source/service/pixiv" to "3f90e9",
        "source/service/pixiv-fanbox" to "3f90e9",
        "source/service/misskey" to "adea02",
        "source/service/mastodon" to "3459fb",
        "source/rating" to "ffffff",
        "source/format" to "91eee8",
        "source/meta" to "641ffe",
        "copyright" to "fff950",
        "source/art" to "4dd0e1",
    )
    val DEFAULT_RULES = DEFAULT_RULES_HEX.map { (prefix, color) ->
        GalleryTagColorRule(prefix, color.toInt(16) or 0xff000000.toInt())
    }

    fun isValid(rules: List<GalleryTagColorRule>): Boolean =
        rules.size <= MAX_RULES &&
            rules.map(GalleryTagColorRule::prefix).toSet().size == rules.size &&
            rules.all { rule ->
                rule.prefix.isNotBlank() &&
                    rule.prefix == rule.prefix.trim() &&
                    rule.prefix.encodeToByteArray().size <= GalleryTagPrefixesCodec.MAX_PREFIX_LENGTH &&
                    rule.prefix.none { it.code < 0x20 || it.code in 0x7f..0x9f } &&
                    rule.color ushr 24 == 0xff
            }

    fun decodeRules(tagSettingsJson: String?): List<GalleryTagColorRule> {
        if (tagSettingsJson == null) return DEFAULT_RULES
        val tags = parseObject(tagSettingsJson)
            ?: throw IllegalArgumentException("Stored tag settings are invalid")
        val colors = tags["colors"] as? JsonArray
            ?: throw IllegalArgumentException("Stored tag color rules are invalid")
        val rules = colors.map { element ->
            val rule = element as? JsonObject
                ?: throw IllegalArgumentException("Stored tag color rules are invalid")
            val prefix = (rule["prefix"] as? JsonPrimitive)
                ?.takeIf(JsonPrimitive::isString)
                ?.content
                ?: throw IllegalArgumentException("Stored tag color rules are invalid")
            val color = (rule["color"] as? JsonPrimitive)
                ?.takeIf(JsonPrimitive::isString)
                ?.content
                ?.takeIf(colorPattern::matches)
                ?: throw IllegalArgumentException("Stored tag color rules are invalid")
            GalleryTagColorRule(prefix, color.substring(1).toInt(16) or 0xff000000.toInt())
        }
        require(isValid(rules)) { "Stored tag color rules are invalid" }
        return rules
    }

    fun encodeRules(rules: List<GalleryTagColorRule>): JsonArray {
        require(isValid(rules)) { "Tag color rules are invalid" }
        return JsonArray(rules.map { rule ->
            buildJsonObject {
                put("prefix", JsonPrimitive(rule.prefix))
                put("color", JsonPrimitive("#${rule.color.toUInt().toString(16).takeLast(6)}"))
            }
        })
    }

    fun update(tagSettingsJson: String?, rules: List<GalleryTagColorRule>): String {
        val original = tagSettingsJson?.let(::parseObject)
            ?: if (tagSettingsJson == null) buildJsonObject {
                put(
                    "includedPrefixes",
                    JsonArray(GalleryTagDisplayPrefixesCodec.DEFAULT_INCLUDED.map(::JsonPrimitive)),
                )
                put(
                    "hiddenPrefixes",
                    JsonArray(GalleryTagDisplayPrefixesCodec.DEFAULT_HIDDEN.map(::JsonPrimitive)),
                )
            } else {
                throw IllegalArgumentException("Stored tag settings are invalid")
            }
        val updated = original.toMutableMap()
        updated["colors"] = encodeRules(rules)
        return json.encodeToString(JsonObject.serializer(), JsonObject(updated))
    }

    fun colorFor(tag: String, rules: List<GalleryTagColorRule>): Int {
        rules.filter { rule -> matches(tag, rule.prefix) }
            .maxByOrNull { it.prefix.length }
            ?.let { return it.color }
        val fallback = intArrayOf(0xff80cbc4.toInt(), 0xff90caf9.toInt(), 0xffce93d8.toInt(), 0xffffcc80.toInt())
        val stableHash = tag.fold(0) { hash, char -> (hash * 31 + char.code) and 0x7fffffff }
        return fallback[stableHash % fallback.size]
    }

    private fun matches(tag: String, prefix: String): Boolean =
        prefix == "*" || tag == prefix || tag.startsWith("$prefix/")

    private fun parseObject(content: String): JsonObject? = try {
        json.parseToJsonElement(content) as? JsonObject
    } catch (_: IllegalArgumentException) {
        null
    }
}
