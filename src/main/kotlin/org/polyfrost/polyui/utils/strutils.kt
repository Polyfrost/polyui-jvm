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

import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.fix

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

/**
 * Advanced Float toString() that supports a given number of decimal places.
 *
 * Additionally, it will remove trailing zeros from the decimal part, so that it will not show unnecessary zeros.
 *
 * @since 1.11.7
 */
fun Float?.toString(dps: Int): String {
    if (this == null || this == 0f) return "0"
    val v = this.fix(dps)
    // remove trailing zeros
    val vi = v.toInt()
    return if(v == vi.toFloat()) {
        // no decimal part
        vi.toString()
    } else v.toString()
}

fun String.removeSurrounding(char: Char): String {
    if (this.length < 2) return this
    if (this[0] == char && this[lastIndex] == char) {
        return this.substring(1, lastIndex)
    }
    return this
}

fun String.substringOr(fromIndex: Int, toIndex: Int, other: String): String {
    if (fromIndex < 0 || toIndex < 0 || fromIndex > toIndex || toIndex > length) return other
    return substring(fromIndex, toIndex)
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

fun String.remove(char: Char): String {
    if (this.indexOf(char) == -1) return this
    val sb = StringBuilder(this.length)
    for (c in this) {
        if (c == char) continue
        sb.append(c)
    }
    return sb.toString()
}

fun String.dropAt(index: Int = lastIndex, amount: Int = 1): String {
    val index = index.coerceIn(0, length)
    if (index - amount == 0) return substring(index)
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
    if (resultWidth <= width) return this
    val delimiterWidth = renderer.textBounds(font, limitText, fontSize).x
    var t = this
    while (resultWidth + delimiterWidth > width) {
        resultWidth = renderer.textBounds(font, t, fontSize).x
        t = t.dropLast(1)
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

data class Line(val string: String, @get:JvmName("bounds") val bounds: Vec2)

private val currentLine = StringBuilder(32)

/**
 * Wrap the given text to the given width, inserting them into [lines], or a new list is created if it is null. It also returned.
 */
fun String.wrap(
    maxWidth: Float,
    renderer: Renderer,
    font: Font, fontSize: Float,
    lines: ArrayList<Line>?,
): ArrayList<Line> {
    val ls = lines ?: ArrayList(1)
    if (this.isEmpty()) {
        ls.add(Line("", Vec2(1f, fontSize)))
        return ls
    }

    // asm: i have documented both these methods with comments as they are kinda hard to understand
    // note: these are performance sensitive.
    val line = currentLine

    // firstly, create outer loop for each line and inner loop for each word in that line
    var lns = 0
    var lne = this.indexOf('\n')
    // simple method of making it still process when there is no \n
    if (lne == -1) lne = this.length
    while (lns < this.length) {
        // char before is a newline, so just skip and don't bother processing
        if (lne == lns) {
            ls.add(Line("", Vec2(1f, fontSize)))
        } else {
            if (maxWidth == 0f) {
                val l = this.substring(lns, lne)
                ls.add(Line(l, renderer.textBounds(font, l, fontSize)))
            } else {
                line.clear()
                var s = lns
                var e = this.indexOf(' ', s)
                while (e != -1 && e < lne) {
                    // last character was a space, so just skip again as above
                    if (s == e) line.append(' ')
                    else this.substring(s, e).wrapWord(maxWidth, renderer, font, fontSize, ls)
                    s = e + 1
                    e = this.indexOf(' ', s)
                }
                // finish processing anything remaining on this line
                if (s < lne) this.substring(s, lne).wrapWord(maxWidth, renderer, font, fontSize, ls)
                if (s == lne) line.append(' ')
                if (line.isNotEmpty()) {
                    val out = line.toString()
                    ls.add(Line(out, renderer.textBounds(font, out, fontSize)))
                }
            }
        }
        lns = lne + 1
        lne = this.indexOf('\n', lns)
        if (lne == -1) lne = this.length
    }
    // uncomment to debug:
//    val input = this.remove('\n')
//    val output = ls.joinToString(separator = "") { it.first }
//    require(output == input) { "wrap failed (wrong by ${input.levenshteinDistance(output)}) \n$input<END\n$output<END" }
    return ls
}

private fun String.wrapWord(
    maxWidth: Float,
    renderer: Renderer,
    font: Font, fontSize: Float,
    lines: ArrayList<Line>,
) {
    if (this.isEmpty()) return
    val wordLength = renderer.textBounds(font, this, fontSize).x
    val line = currentLine

    if (wordLength > maxWidth) {
        // ah. word is longer than the maximum wrap width
        if (line.isNotEmpty()) {
            // Finish current line and start a new one with the long word
            val out = line.toString()
            lines.add(Line(out, renderer.textBounds(font, out, fontSize)))
            line.clear()
        }

        // add the long word to the lines, splitting it up into smaller chunks if needed
        var remainder = this
        // lightweight method of stopping it getting stuck in this method if it can't fit even a single character in the given width
        var trap = 0
        while (remainder.isNotEmpty()) {
            val (slice, rem) = remainder.substringToWidth(renderer, font, fontSize, maxWidth)
            lines.add(Line(slice, renderer.textBounds(font, slice, fontSize)))
            remainder = rem
            trap++
            if (trap > 100) throw IllegalStateException("trapped trying to trim '$this' at size $fontSize to width $maxWidth")
        }
    } else if (line.isEmpty()) {
        line.append(this)
    } else if (renderer.textBounds(font, line.toString(), fontSize).x + wordLength <= maxWidth) {
        // ok!
        line.append(' ').append(this)
    } else {
        // asm: word doesn't fit in current line, wrap it to the next line
        val out = line.append(' ').toString()
        lines.add(Line(out, renderer.textBounds(font, out, fontSize)))
        line.clear().append(this)
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
