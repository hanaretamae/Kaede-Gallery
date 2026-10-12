package com.hanaretamae.kaede.core.settings

import com.hanaretamae.kaede.core.model.GalleryContent
import com.hanaretamae.kaede.core.model.GalleryQuery
import com.hanaretamae.kaede.core.model.GallerySortField
import com.hanaretamae.kaede.core.model.SortDirection
import com.hanaretamae.kaede.core.model.VirtualFilter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class SavedGallerySearch(
    val name: String,
    val content: GalleryContent,
    val searchText: String,
    val fuzzySearchEnabled: Boolean,
    val includeTags: Set<String>,
    val andTags: Set<String>,
    val excludedTags: Set<String>,
    val virtualFilters: Set<VirtualFilter>,
    val sortField: GallerySortField,
    val sortDirection: SortDirection,
) {
    fun toQuery(pageSize: Int): GalleryQuery = GalleryQuery(
        content = content,
        searchText = searchText,
        fuzzySearchEnabled = fuzzySearchEnabled,
        includeTags = includeTags,
        andTags = andTags,
        excludedTags = excludedTags,
        virtualFilters = virtualFilters,
        sortField = sortField,
        sortDirection = sortDirection,
        pageSize = pageSize,
    )

    companion object {
        fun fromQuery(name: String, query: GalleryQuery): SavedGallerySearch = SavedGallerySearch(
            name = name.trim(),
            content = query.content,
            searchText = query.searchText,
            fuzzySearchEnabled = query.fuzzySearchEnabled,
            includeTags = query.includeTags.toSet(),
            andTags = query.andTags.toSet(),
            excludedTags = query.excludedTags.toSet(),
            virtualFilters = query.virtualFilters.toSet(),
            sortField = query.sortField,
            sortDirection = query.sortDirection,
        )
    }
}

object SavedGallerySearchCodec {
    const val MAX_SAVED_SEARCHES = 32
    private const val MAX_NAME_BYTES = 256
    private const val MAX_SEARCH_TEXT_BYTES = 2_048
    private const val MAX_FILTER_TAGS = 256
    private const val MAX_TAG_BYTES = 512
    private const val MAX_STORAGE_BYTES = 64 * 1024
    private val json = Json

    fun isValid(searches: List<SavedGallerySearch>): Boolean =
        searches.size <= MAX_SAVED_SEARCHES &&
            searches.map { it.name.lowercase() }.distinct().size == searches.size &&
            searches.all(::isValid) &&
        encodeStorageUnchecked(searches).encodeToByteArray().size <= MAX_STORAGE_BYTES

    fun encodeStorage(searches: List<SavedGallerySearch>): String {
        require(isValid(searches)) { "Saved searches are invalid" }
        return encodeStorageUnchecked(searches)
    }

    fun decodeStorage(content: String): List<SavedGallerySearch>? {
        if (content.encodeToByteArray().size > MAX_STORAGE_BYTES) return null
        return try {
            decodeJson(json.parseToJsonElement(content))
        } catch (_: SerializationException) {
            null
        }
    }

    internal fun encodeJson(searches: List<SavedGallerySearch>): JsonArray {
        require(isValid(searches)) { "Saved searches are invalid" }
        return encodeUnchecked(searches)
    }

    internal fun decodeJson(element: JsonElement?): List<SavedGallerySearch>? {
        if (element == null) return emptyList()
        val array = element as? JsonArray ?: return null
        if (array.size > MAX_SAVED_SEARCHES) return null
        val searches = array.map { item ->
            val values = item as? JsonObject ?: return null
            val name = string(values, "name") ?: return null
            val content = string(values, "content")
                ?.let { value -> GalleryContent.entries.firstOrNull { it.name == value } }
                ?: return null
            val searchText = string(values, "searchText") ?: return null
            val fuzzySearchEnabled = (values["fuzzySearchEnabled"] as? JsonPrimitive)
                ?.booleanOrNull ?: return null
            val includeTags = stringSet(values["includeTags"]) ?: return null
            val andTags = stringSet(values["andTags"]) ?: return null
            val excludedTags = stringSet(values["excludedTags"]) ?: return null
            val virtualFilters = enumSet(values["virtualFilters"], VirtualFilter.entries)
                ?: return null
            val sortField = string(values, "sortField")
                ?.let { value -> GallerySortField.entries.firstOrNull { it.name == value } }
                ?: return null
            val sortDirection = string(values, "sortDirection")
                ?.let { value -> SortDirection.entries.firstOrNull { it.name == value } }
                ?: return null
            SavedGallerySearch(
                name = name,
                content = content,
                searchText = searchText,
                fuzzySearchEnabled = fuzzySearchEnabled,
                includeTags = includeTags,
                andTags = andTags,
                excludedTags = excludedTags,
                virtualFilters = virtualFilters,
                sortField = sortField,
                sortDirection = sortDirection,
            )
        }
        return searches.takeIf(::isValid)
    }

    private fun isValid(search: SavedGallerySearch): Boolean =
        search.name.isNotBlank() &&
            search.name == search.name.trim() &&
            isSafeText(search.name) &&
            search.name.encodeToByteArray().size <= MAX_NAME_BYTES &&
            search.searchText.encodeToByteArray().size <= MAX_SEARCH_TEXT_BYTES &&
            isSafeText(search.searchText) &&
            search.includeTags.size + search.andTags.size + search.excludedTags.size <=
            MAX_FILTER_TAGS &&
            (search.includeTags intersect search.andTags).isEmpty() &&
            (search.includeTags intersect search.excludedTags).isEmpty() &&
            (search.andTags intersect search.excludedTags).isEmpty() &&
            (search.includeTags + search.andTags + search.excludedTags).all(::isValidTag)

    private fun encodeUnchecked(searches: List<SavedGallerySearch>): JsonArray =
        JsonArray(searches.map { search ->
            buildJsonObject {
                put("name", JsonPrimitive(search.name))
                put("content", JsonPrimitive(search.content.name))
                put("searchText", JsonPrimitive(search.searchText))
                put("fuzzySearchEnabled", JsonPrimitive(search.fuzzySearchEnabled))
                put("includeTags", strings(search.includeTags))
                put("andTags", strings(search.andTags))
                put("excludedTags", strings(search.excludedTags))
                put(
                    "virtualFilters",
                    JsonArray(search.virtualFilters.map { it.name }.sorted().map(::JsonPrimitive)),
                )
                put("sortField", JsonPrimitive(search.sortField.name))
                put("sortDirection", JsonPrimitive(search.sortDirection.name))
            }
        })

    private fun encodeStorageUnchecked(searches: List<SavedGallerySearch>): String =
        json.encodeToString(JsonArray.serializer(), encodeUnchecked(searches))

    private fun strings(values: Set<String>): JsonArray =
        JsonArray(values.sorted().map(::JsonPrimitive))

    private fun string(values: JsonObject, key: String): String? =
        (values[key] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

    private fun stringSet(element: JsonElement?): Set<String>? {
        val array = element as? JsonArray ?: return null
        val values = array.map { value ->
            (value as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content ?: return null
        }
        return values.toSet().takeIf { it.size == values.size }
    }

    private fun <T : Enum<T>> enumSet(element: JsonElement?, values: List<T>): Set<T>? {
        val strings = stringSet(element) ?: return null
        val enums = strings.map { name -> values.firstOrNull { it.name == name } ?: return null }
        return enums.toSet().takeIf { it.size == enums.size }
    }

    private fun isValidTag(tag: String): Boolean =
        tag.isNotBlank() &&
            tag == tag.trim() &&
            tag.encodeToByteArray().size <= MAX_TAG_BYTES &&
            isSafeText(tag)

    private fun isSafeText(value: String): Boolean =
        value.none { it.code < 0x20 || it.code in 0x7f..0x9f }
}
