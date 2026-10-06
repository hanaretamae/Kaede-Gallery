package com.hanaretamae.kaede.ui.gallery

import kotlin.math.abs

internal fun tagOptionMatches(
    category: String,
    name: String,
    fullTag: String,
    query: String,
): Boolean {
    val text = "$category $name $fullTag".takeCodePoints(MAX_SEARCH_CODE_POINTS).lowercase()
    val terms = query.trim().split(WHITESPACE).filter(String::isNotEmpty)
    return terms.any { rawTerm ->
        val term = rawTerm.takeCodePoints(MAX_SEARCH_CODE_POINTS).lowercase()
        if (text.contains(term)) return@any true
        val tolerance = when (term.codePointCount()) {
            in 0..2 -> 0
            in 3..5 -> 1
            else -> 2
        }
        text.split(WORD_BOUNDARY).any { fuzzyWordMatches(it, term, tolerance) }
    }
}

private fun fuzzyWordMatches(word: String, term: String, tolerance: Int): Boolean {
    val wordPoints = word.unicodeCodePoints().takeCodePoints(MAX_SEARCH_CODE_POINTS)
    val termPoints = term.unicodeCodePoints().takeCodePoints(MAX_SEARCH_CODE_POINTS)
    if (wordPoints.isEmpty() || termPoints.isEmpty()) return false
    if (abs(wordPoints.size - termPoints.size) <= tolerance &&
        editDistanceWithin(wordPoints, termPoints, tolerance)
    ) {
        return true
    }
    val minLength = (termPoints.size - tolerance).coerceAtLeast(1)
    val maxLength = (termPoints.size + tolerance).coerceAtMost(wordPoints.size)
    if (minLength > maxLength) return false
    for (length in minLength..maxLength) {
        for (start in 0..wordPoints.size - length) {
            val candidate = wordPoints.copyOfRange(start, start + length)
            if (editDistanceWithin(candidate, termPoints, tolerance)) return true
        }
    }
    return false
}

private fun editDistanceWithin(left: IntArray, right: IntArray, limit: Int): Boolean {
    if (abs(left.size - right.size) > limit) return false
    var previous = IntArray(right.size + 1) { it }
    var current = IntArray(right.size + 1)
    for (leftIndex in left.indices) {
        current[0] = leftIndex + 1
        var rowMinimum = current[0]
        for (rightIndex in right.indices) {
            val substitutionCost = if (left[leftIndex] == right[rightIndex]) 0 else 1
            current[rightIndex + 1] = minOf(
                previous[rightIndex + 1] + 1,
                current[rightIndex] + 1,
                previous[rightIndex] + substitutionCost,
            )
            rowMinimum = minOf(rowMinimum, current[rightIndex + 1])
        }
        if (rowMinimum > limit) return false
        val swap = previous
        previous = current
        current = swap
    }
    return previous[right.size] <= limit
}

private fun String.unicodeCodePoints(): IntArray {
    val result = ArrayList<Int>(length)
    var index = 0
    while (index < length) {
        val first = this[index]
        if (first.isHighSurrogate() && index + 1 < length && this[index + 1].isLowSurrogate()) {
            val second = this[index + 1]
            result += 0x10000 + ((first.code - 0xD800) shl 10) + (second.code - 0xDC00)
            index += 2
        } else {
            result += first.code
            index++
        }
    }
    return result.toIntArray()
}

private fun String.takeCodePoints(limit: Int): String {
    val points = unicodeCodePoints().takeCodePoints(limit)
    return buildString {
        points.forEach { appendCodePoint(it) }
    }
}

private fun IntArray.takeCodePoints(limit: Int): IntArray = copyOfRange(0, minOf(size, limit))

private fun StringBuilder.appendCodePoint(codePoint: Int) {
    if (codePoint <= 0xFFFF) {
        append(codePoint.toChar())
    } else {
        val value = codePoint - 0x10000
        append((0xD800 + (value shr 10)).toChar())
        append((0xDC00 + (value and 0x3FF)).toChar())
    }
}

private fun String.codePointCount(): Int = unicodeCodePoints().size

private val WHITESPACE = Regex("\\s+")
private val WORD_BOUNDARY = Regex("[\\s/_-]+")
private const val MAX_SEARCH_CODE_POINTS = 128
