package com.hanaretamae.kaede.core.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SettingsTransferCodec {
    const val MAX_BYTES = 16 * 1024
    private const val LEGACY_HEADER = "kaede-gallery-settings=1"
    private val json = Json { prettyPrint = false }

    fun encode(settings: GallerySettings): String {
        val tags = settings.flutterTagSettingsJson?.let { content ->
            val parsed = try {
                json.parseToJsonElement(content)
            } catch (_: SerializationException) {
                throw IllegalArgumentException("Stored tag settings are invalid")
            }
            val tagObject = parsed as? JsonObject
                ?: throw IllegalArgumentException("Stored tag settings are invalid")
            require(hasFlutterRequiredTagSettings(tagObject)) {
                "Stored tag settings are invalid"
            }
            GalleryTagColorCodec.decodeRules(content)
            GalleryTagCategoryCodec.decodeSettings(content, japaneseDefaults = false)
            GalleryNoteStructureCodec.decodeSettings(content, settings.galleryTagPrefixes)
            tagObject
        } ?: defaultTags(settings)
        val currentTags = tags.toMutableMap()
        currentTags["includedPrefixes"] = JsonArray(
            settings.includedTagPrefixes.map(::JsonPrimitive),
        )
        currentTags["hiddenPrefixes"] = JsonArray(
            settings.hiddenTagPrefixes.map(::JsonPrimitive),
        )
        val noteStructure = (tags["noteStructure"] as? JsonObject ?: JsonObject(emptyMap()))
            .toMutableMap()
        noteStructure["galleryTagPrefixes"] = JsonArray(
            settings.galleryTagPrefixes.map(::JsonPrimitive),
        )
        currentTags["noteStructure"] = JsonObject(noteStructure)
        val pagination = (tags["pagination"] as? JsonObject ?: JsonObject(emptyMap()))
            .toMutableMap()
        pagination["pageSize"] = JsonPrimitive(settings.pageSize)
        pagination["showItemCount"] = JsonPrimitive(settings.showLoadedRange)
        pagination["showItemNumberOnTiles"] = JsonPrimitive(settings.showTilePosition)
        pagination["showMissingMediaIcon"] = JsonPrimitive(settings.showMissingMediaIcon)
        currentTags["pagination"] = JsonObject(pagination)
        val encoded = json.encodeToString(
            JsonElement.serializer(),
            buildJsonObject {
                put("version", JsonPrimitive(1))
                put(
                    "appearance",
                    buildJsonObject {
                        put(
                            "brightness",
                            JsonPrimitive(
                                when (settings.appearance.theme) {
                                    ThemePreference.SYSTEM -> "system"
                                    ThemePreference.LIGHT -> "light"
                                    ThemePreference.DARK -> "dark"
                                },
                            ),
                        )
                        put("useSystemColor", JsonPrimitive(settings.appearance.useSystemColor))
                        put("pureBlack", JsonPrimitive(settings.appearance.pureBlack))
                        put(
                            "language",
                            JsonPrimitive(
                                when (settings.appearance.language) {
                                    LanguagePreference.SYSTEM -> "system"
                                    LanguagePreference.JAPANESE -> "ja"
                                    LanguagePreference.ENGLISH -> "en"
                                },
                            ),
                        )
                    },
                )
                put("tags", JsonObject(currentTags))
                put(
                    "kmp",
                    buildJsonObject {
                        put("showLoadedRange", JsonPrimitive(settings.showLoadedRange))
                        put("showCounts", JsonPrimitive(settings.showCounts))
                    },
                )
            },
        )
        require(encoded.encodeToByteArray().size <= MAX_BYTES) {
            "Settings export exceeds its size limit"
        }
        return encoded
    }

    fun decode(content: String): GallerySettings? {
        if (content.encodeToByteArray().size > MAX_BYTES) return null
        if (content.lineSequence().firstOrNull() == LEGACY_HEADER) {
            return decodeLegacy(content)
        }
        return try {
            val root = json.parseToJsonElement(content).jsonObject
            if (root["version"]?.let(::integerValue) != 1) return null
            val appearance = root["appearance"]?.jsonObject ?: return null
            val tags = root["tags"]?.jsonObject ?: return null
            if (
                json.encodeToString(JsonElement.serializer(), JsonObject(tags))
                    .encodeToByteArray().size > MAX_BYTES - 1024
            ) {
                return null
            }
            val brightness = appearance["brightness"]?.jsonPrimitive?.content ?: return null
            val theme = when (brightness) {
                "system" -> ThemePreference.SYSTEM
                "light" -> ThemePreference.LIGHT
                "dark" -> ThemePreference.DARK
                else -> return null
            }
            val languageValue = appearance["language"]?.let {
                (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content ?: return null
            } ?: "system"
            val language = when (languageValue) {
                "system" -> LanguagePreference.SYSTEM
                "ja" -> LanguagePreference.JAPANESE
                "en" -> LanguagePreference.ENGLISH
                else -> LanguagePreference.SYSTEM
            }
            val useSystemColor = optionalBoolean(appearance, "useSystemColor", false)
                ?: return null
            val pureBlack = appearance["pureBlack"]?.jsonPrimitive?.booleanOrNull ?: return null
            if (!hasFlutterRequiredTagSettings(tags)) return null
            GalleryTagColorCodec.decodeRules(json.encodeToString(JsonElement.serializer(), tags))
            GalleryTagCategoryCodec.decodeSettings(
                json.encodeToString(JsonElement.serializer(), tags),
                japaneseDefaults = false,
            )
            val includedTagPrefixes = tagPrefixes(tags, "includedPrefixes") ?: return null
            val hiddenTagPrefixes = tagPrefixes(tags, "hiddenPrefixes") ?: return null
            if (
                !GalleryTagDisplayPrefixesCodec.isValid(includedTagPrefixes) ||
                !GalleryTagDisplayPrefixesCodec.isValid(hiddenTagPrefixes)
            ) {
                return null
            }
            val noteStructure = tags["noteStructure"]?.jsonObject ?: JsonObject(emptyMap())
            val galleryTagPrefixes = noteStructure["galleryTagPrefixes"]?.let {
                val prefixes = it as? JsonArray ?: return null
                if (prefixes.any { prefix -> prefix !is JsonPrimitive || !prefix.isString }) {
                    return null
                }
                prefixes.map { prefix -> prefix.jsonPrimitive.content }
            } ?: GalleryTagPrefixesCodec.DEFAULT_PREFIXES
            if (!GalleryTagPrefixesCodec.isValid(galleryTagPrefixes)) return null
            GalleryNoteStructureCodec.decodeSettings(
                json.encodeToString(JsonElement.serializer(), JsonObject(tags)),
                galleryTagPrefixes,
            )
            val pagination = tags["pagination"]?.jsonObject ?: JsonObject(emptyMap())
            val pageSize = pagination["pageSize"]?.let {
                integerValue(it) ?: return null
            } ?: GallerySettings.DEFAULT_PAGE_SIZE
            if (pageSize !in GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE) {
                return null
            }
            val kmp = root["kmp"]?.jsonObject ?: JsonObject(emptyMap())
            val legacyKmpExport =
                kmp["showLoadedRange"] != null && kmp["showCounts"] == null
            val showLoadedRange = if (legacyKmpExport) {
                optionalBoolean(kmp, "showLoadedRange", true) ?: return null
            } else {
                optionalBoolean(
                    pagination,
                    "showItemCount",
                    optionalBoolean(kmp, "showLoadedRange", true) ?: return null,
                ) ?: return null
            }
            val showCounts = optionalBoolean(
                if (legacyKmpExport) pagination else kmp,
                if (legacyKmpExport) "showItemCount" else "showCounts",
                true,
            ) ?: return null
            val showTilePosition = optionalBoolean(
                pagination,
                "showItemNumberOnTiles",
                pagination["showMediaCountOnTiles"]?.let {
                    optionalBoolean(pagination, "showMediaCountOnTiles", false) ?: return null
                } ?: false,
            ) ?: return null
            val showMissingMediaIcon =
                optionalBoolean(pagination, "showMissingMediaIcon", false) ?: return null
            GallerySettings(
                appearance = AppearanceSettings(
                    theme = theme,
                    language = language,
                    useSystemColor = useSystemColor,
                    pureBlack = pureBlack,
                ),
                pageSize = pageSize,
                galleryTagPrefixes = galleryTagPrefixes,
                includedTagPrefixes = includedTagPrefixes,
                hiddenTagPrefixes = hiddenTagPrefixes,
                showMissingMediaIcon = showMissingMediaIcon,
                showLoadedRange = showLoadedRange,
                showTilePosition = showTilePosition,
                showCounts = showCounts,
                flutterTagSettingsJson = json.encodeToString(
                    JsonElement.serializer(),
                    JsonObject(tags),
                ),
            )
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
    }

    private fun hasFlutterRequiredTagSettings(tags: JsonObject): Boolean {
        val included = tags["includedPrefixes"] as? JsonArray ?: return false
        val hidden = tags["hiddenPrefixes"] as? JsonArray ?: return false
        val colors = tags["colors"] as? JsonArray ?: return false
        return included.all { it is JsonPrimitive && it.isString } &&
            hidden.all { it is JsonPrimitive && it.isString } &&
            colors.all { color ->
                val rule = color as? JsonObject ?: return@all false
                val prefix = rule["prefix"] as? JsonPrimitive
                val value = rule["color"] as? JsonPrimitive
                prefix?.isString == true &&
                    !prefix.content.isEmpty() &&
                    value?.isString == true &&
                    value.content.matches(COLOR_VALUE)
            }
    }

    private fun tagPrefixes(tags: JsonObject, key: String): List<String>? {
        val prefixes = tags[key] as? JsonArray ?: return null
        if (prefixes.any { it !is JsonPrimitive || !it.isString }) return null
        return prefixes.map { it.jsonPrimitive.content }
    }

    private fun optionalBoolean(
        values: JsonObject,
        key: String,
        default: Boolean,
    ): Boolean? {
        val value = values[key] ?: return default
        if (value == JsonNull) return default
        return (value as? JsonPrimitive)?.booleanOrNull
    }

    private fun integerValue(value: JsonElement): Int? =
        (value as? JsonPrimitive)
            ?.takeUnless(JsonPrimitive::isString)
            ?.intOrNull

    private fun defaultTags(settings: GallerySettings): JsonObject = buildJsonObject {
        put("includedPrefixes", JsonArray(settings.includedTagPrefixes.map(::JsonPrimitive)))
        put(
            "hiddenPrefixes",
            JsonArray(settings.hiddenTagPrefixes.map(::JsonPrimitive)),
        )
        put(
            "colors",
            JsonArray(
                GalleryTagColorCodec.DEFAULT_RULES_HEX.map { (prefix, color) ->
                    buildJsonObject {
                        put("prefix", JsonPrimitive(prefix))
                        put("color", JsonPrimitive("#$color"))
                    }
                },
            ),
        )
        put(
            "noteStructure",
            buildJsonObject {
                put(
                    "galleryTagPrefixes",
                    JsonArray(settings.galleryTagPrefixes.map(::JsonPrimitive)),
                )
            },
        )
    }

    private val COLOR_VALUE = Regex("#[0-9a-fA-F]{6}")

    private fun decodeLegacy(content: String): GallerySettings? {
        val lines = content.split('\n').map { it.removeSuffix("\r") }
        if (lines.firstOrNull() != LEGACY_HEADER) return null
        val values = mutableMapOf<String, String>()
        for (line in lines.drop(1)) {
            if (line.isEmpty()) continue
            val separator = line.indexOf('=')
            if (separator <= 0) return null
            val key = line.substring(0, separator)
            if (key !in LEGACY_KEYS || values.put(key, line.substring(separator + 1)) != null) {
                return null
            }
        }
        if (!values.keys.containsAll(LEGACY_REQUIRED_KEYS)) return null
        val theme = ThemePreference.entries.firstOrNull { it.name == values["theme"] }
            ?: return null
        val language = LanguagePreference.entries.firstOrNull { it.name == values["language"] }
            ?: return null
        val pageSize = values["page_size"]?.toIntOrNull()
            ?.takeIf { it in GallerySettings.MIN_PAGE_SIZE..GallerySettings.MAX_PAGE_SIZE }
            ?: return null
        val prefixes = values["gallery_tag_prefixes"]?.let {
            GalleryTagPrefixesCodec.decodeStorage(it) ?: return null
        } ?: GalleryTagPrefixesCodec.DEFAULT_PREFIXES
        fun boolean(key: String): Boolean? = when (values[key]) {
            "true" -> true
            "false" -> false
            else -> null
        }
        return GallerySettings(
            appearance = AppearanceSettings(
                theme = theme,
                language = language,
                useSystemColor = boolean("use_system_color") ?: return null,
                pureBlack = boolean("pure_black") ?: return null,
            ),
            pageSize = pageSize,
            galleryTagPrefixes = prefixes,
            showLoadedRange = boolean("show_loaded_range") ?: return null,
            showTilePosition = boolean("show_tile_position") ?: return null,
            showCounts = boolean("show_counts") ?: return null,
        )
    }

    private val LEGACY_REQUIRED_KEYS = setOf(
        "theme",
        "language",
        "use_system_color",
        "pure_black",
        "page_size",
        "show_loaded_range",
        "show_tile_position",
        "show_counts",
    )
    private val LEGACY_KEYS = LEGACY_REQUIRED_KEYS + "gallery_tag_prefixes"
}
