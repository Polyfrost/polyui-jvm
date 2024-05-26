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
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by

/**
 * append the given [Char][c] to this [StringBuilder], repeated [repeats] times.
 *
 * This function is equivalent to doing [sb][StringBuilder]`.append(c.repeat(`[repeats]`))`, but it uses an already existing StringBuilder.
 * @throws [IllegalArgumentException] when n < 0.
 */
fun StringBuilder.append(c: Char, repeats: Int): StringBuilder {
    require(repeats >= 0) { "Count 'n' must be non-negative, but was $repeats." }
    when (repeats) {
        0 -> return this
        1 -> this.append(c)
        else -> {
            for (i in 1..repeats) {
                this.append(c)
            }
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
    if (renderer.textBounds(font, this, fontSize).x <= width) {
        return this to ""
    }

    var left = 0
    var right = length
    var result = ""
    while (left < right) {
        val mid = (left + right + 1) / 2
        val substring = substring(0, mid)
        if (renderer.textBounds(font, substring, fontSize).x <= width) {
            result = substring
            left = mid
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

typealias Line = Pair<String, Vec2>

private val currentLine = StringBuilder(32)

/**
 * Wrap the given text to the given width, inserting them into [lines], or a new list is created if it is null. It also returned.
 */
fun String.wrap(
    maxWidth: Float,
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    lines: ArrayList<Line>?,
): ArrayList<Line> {
    val ls = lines ?: ArrayList(5)
    if (this.isEmpty()) {
        ls.add("" to (0f by fontSize))
        return ls
    }
    if (maxWidth == 0f) {
        ls.add(this to renderer.textBounds(font, this, fontSize))
        return ls
    }


    var s = 0
    var e = this.indexOf(' ')
    while (e != -1) {
        processWord(this.substring(s, e), currentLine, fontSize, ls, renderer, font, maxWidth)
        s = e + 1
        e = this.indexOf(' ', s)
    }

    // Add the last line
    if (currentLine.isNotEmpty()) {
        val out = currentLine.toString()
        ls.add(out to renderer.textBounds(font, out, fontSize))
    }
    currentLine.clear()

    return ls
}

private fun processWord(
    word: String,
    currentLine: StringBuilder,
    fontSize: Float,
    ls: ArrayList<Line>, renderer: Renderer, font: Font, maxWidth: Float
) {
    if (word.isEmpty()) return
    val nl = word.indexOf('\n')
    if (nl != -1) {
        if (word.length == 1) {
            // solo newline character
            val out = currentLine.toString()
            ls.add(out to renderer.textBounds(font, out, fontSize))
            currentLine.clear()
        } else {
            if (nl != 0) currentLine.append(' ').append(word.substring(0, nl))
            val out = currentLine.toString()
            ls.add(out to renderer.textBounds(font, out, fontSize))
            currentLine.clear()
            processWord(word.substring(nl + 1), currentLine, fontSize, ls, renderer, font, maxWidth)
        }
        return
    }

    val wordLength = renderer.textBounds(font, word, fontSize).x


    if (wordLength > maxWidth) {
        // ah. word is longer than the maximum wrap width
        if (currentLine.isNotEmpty()) {
            // Finish current line and start a new one with the long word
            val out = currentLine.toString()
            ls.add(out to renderer.textBounds(font, out, fontSize))
            currentLine.clear()
        }

        // add the long word to the lines, splitting it up into smaller chunks if needed
        var remainingWord = word
        var trap = 0
        while (remainingWord.isNotEmpty()) {
            val chunk = remainingWord.substringToWidth(renderer, font, fontSize, maxWidth)
            ls.add(chunk.first to renderer.textBounds(font, chunk.first, fontSize))
            remainingWord = chunk.second
            trap++
            if (trap > 100) throw IllegalStateException("trapped trying to trim '$word' at size $fontSize")
        }
    } else if (currentLine.isEmpty()) {
        currentLine.append(word)
    } else if (renderer.textBounds(font, currentLine.toString(), fontSize).x + wordLength <= maxWidth) {
        // ok!
        currentLine.append(' ').append(word)
    } else {
        // asm: word doesn't fit in current line, wrap it to the next line
        val out = currentLine.append(' ').toString()
        ls.add(out to renderer.textBounds(font, out, fontSize))
        currentLine.clear().append(word)
    }
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
