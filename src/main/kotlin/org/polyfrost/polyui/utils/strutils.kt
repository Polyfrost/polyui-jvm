/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:JvmName("StringUtils")

package org.polyfrost.polyui.utils

import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Font

/**
 * append the given [CharSequence][c] to this [StringBuilder], repeated [repeats] times.
 *
 * This function is equivalent to doing [sb][StringBuilder]`.append(c.repeat(`[repeats]`))`, but it uses an already existing StringBuilder.
 * @throws [IllegalArgumentException] when n < 0.
 */
fun StringBuilder.append(c: CharSequence, repeats: Int): StringBuilder {
    require(repeats >= 0) { "Count 'n' must be non-negative, but was $repeats." }
    if (repeats == 0) return this
    if (repeats == 1) {
        this.append(c)
    } else {
        for (i in 1..repeats) {
            this.append(c)
        }
    }
    return this
}

fun String.substringSafe(fromIndex: Int, toIndex: Int = lastIndex): String {
    if (fromIndex > toIndex) {
        return substringSafe(toIndex, fromIndex)
    }
    if (fromIndex < 0) {
        return substringSafe(0, toIndex)
    }
    if (toIndex > length) {
        return substringSafe(fromIndex, length)
    }
    return substring(fromIndex, toIndex)
}

fun String.dropAt(index: Int = lastIndex, amount: Int = 1): String {
    if (index - amount == 0) return ""
    if (index - amount < 0) return this
    return substring(0, index - amount) + substring(index)
}

/**
 * Limit the given string to a width.
 * If the string is too long, it will be cut to the width, and [limitText] will be appended to the end.
 * @since 0.22.0
 */
fun String.truncate(
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    width: Float,
    limitText: String = "...",
): String {
    require(width != 0f) { "Cannot truncate to zero width" }
    var resultWidth = renderer.textBounds(font, this, fontSize).x
    if (resultWidth < width) return this
    val delimiterWidth = renderer.textBounds(font, limitText, fontSize).x
    var t = this
    while (resultWidth + delimiterWidth > width) {
        resultWidth = renderer.textBounds(font, t, fontSize).x
        t = t.substring(0, t.length - 1)
    }
    t += limitText
    return t
}

/** take the given string and cut it to the length, returning a pair of the string cut to the length, and the remainder.
 *
 * if the string fits within the width, the first string will be the entire string, and the second will be empty.
 */
fun String.substringToWidth(
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    width: Float,
): Pair<String, String> {
    if (renderer.settings.debug && renderer.textBounds(
            font,
            "W",
            fontSize,
        ).x > width
    ) { // this is enabled only on debug mode for performance in prod
        throw RuntimeException("Text box maximum width is too small for the given font size! (string: $this, font: ${font.resourcePath}, fontSize: $fontSize, width: $width)")
    }
    if (renderer.textBounds(font, this, fontSize).x <= width) {
        return this to ""
    }

    var left = 0
    var right = length - 1
    var result = ""
    while (left <= right) {
        val mid = (left + right) / 2
        val substring = substring(0, mid + 1)
        if (renderer.textBounds(font, substring, fontSize).x <= width) {
            result = substring
            left = mid + 1
        } else {
            right = mid - 1
        }
    }
    return result to this.substring(result.length)
}

/**
 * calculate the levenshtein distance between this string and the other string.
 * @see <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein distance</a>
 * @since 0.19.1
 */
fun String.levenshteinDistance(other: String): Int {
    val d = Array(length + 1) { IntArray(other.length + 1) }

    for (i in 0..length) {
        d[i][0] = i
    }

    for (j in 0..other.length) {
        d[0][j] = j
    }

    for (j in 1..other.length) {
        for (i in 1..length) {
            if (this[i - 1] == other[j - 1]) {
                d[i][j] = d[i - 1][j - 1]
            } else {
                d[i][j] = min3(
                    d[i - 1][j] + 1, // deletion
                    d[i][j - 1] + 1, // insertion
                    d[i - 1][j - 1] + 1, // substitution
                )
            }
        }
    }

    return d[length][other.length]
}

/**
 * Wrap the given text to the given width, inserting them into [lines], or a new list is created if it is null. It also returned.
 */
fun String.wrap(
    maxWidth: Float,
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    lines: LinkedList<MutablePair<String, Float>>?,
): LinkedList<MutablePair<String, Float>> {
    val ls = lines ?: LinkedList()
    if (maxWidth == 0f) {
        ls.add(this with renderer.textBounds(font, this, fontSize).x)
        return ls
    }
    val words = split(" ")
    if (words.isEmpty()) {
        ls.clear()
        return ls
    }
    var currentLine = StringBuilder()

    words.forEach { word ->
        val wordLength = renderer.textBounds(font, word, fontSize).x

        if (wordLength > maxWidth) {
            // ah. word is longer than the maximum wrap width
            if (currentLine.isNotEmpty()) {
                // Finish current line and start a new one with the long word
                val out = currentLine.toString()
                ls.add(out with renderer.textBounds(font, out, fontSize).x)
                currentLine.clear()
            }

            // add the long word to the lines, splitting it up into smaller chunks if needed
            var remainingWord = word
            while (remainingWord.isNotEmpty()) {
                val chunk = remainingWord.substringToWidth(renderer, font, fontSize, maxWidth)
                ls.add(chunk.first with renderer.textBounds(font, chunk.first, fontSize).x)
                remainingWord = chunk.second
            }
        } else if (currentLine.isEmpty()) {
            currentLine.append(word)
        } else if (renderer.textBounds(font, currentLine.toString(), fontSize).x + wordLength <= maxWidth) {
            // ok!
            currentLine.append(' ').append(word)
        } else {
            // asm: word doesn't fit in current line, wrap it to the next line
            val out = currentLine.append(' ').toString()
            ls.add(out with renderer.textBounds(font, out, fontSize).x)
            currentLine = currentLine.clear().append(word)
        }
    }

    // Add the last line
    if (currentLine.isNotEmpty()) {
        val out = currentLine.toString()
        ls.add(out with renderer.textBounds(font, out, fontSize).x)
    }

    return ls
}

/**
 * [split], putting the data into the given [dest] list.
 */
fun String.splitTo(delim: Char, ignoreCase: Boolean = false, dest: MutableList<MutablePair<String, Float>>) {
    var start = 0
    var end = indexOf(delim, 0, ignoreCase)
    while (end != -1) {
        dest.add(substring(start, end) with 0f)
        start = end + 1
        end = indexOf(delim, start, ignoreCase)
    }
    dest.add(substring(start) with 0f)
}

/**
 * Returns the closest character index from the given string to the given point [x].
 *
 * @return the index of the character closest to the given point [x], or `-1` if it could not be found.
 *
 * @since 0.18.5
 */
fun CharSequence.closestToPoint(renderer: Renderer, font: Font, fontSize: Float, x: Float): Int {
    var prev = 0f
    for (c in this.indices) {
        val w = renderer.textBounds(
            font,
            this.substring(0, c),
            fontSize,
        ).x
        // get closest char (not necessarily more)
        if (x < w) {
            return if (x - prev < w - x) {
                c - 1
            } else {
                c
            }
        }
        prev = w
    }
    return -1
}
