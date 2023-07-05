/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("Utils")

package cc.polyfrost.polyui.utils

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.unit.TextAlign
import kotlin.math.*

fun rgba(r: Float, g: Float, b: Float, a: Float): Color {
    return Color((r * 255f).toInt(), (g * 255f).toInt(), (b * 255f).toInt(), a)
}

/** figma copy-paste accessor */
@JvmOverloads
fun rgba(r: Int, g: Int, b: Int, a: Float = 1f): Color {
    return Color(r, g, b, (a * 255f).toInt())
}

/**
 * Converts the components of a color, as specified by the HSB
 * model, to an equivalent set of values for the default RGB model.
 *
 * The `saturation` and `brightness` components
 * should be floating-point values between zero and one
 * (numbers in the range 0.0-1.0).  The `hue` component
 * can be any floating-point number.  The floor of this number is
 * subtracted from it to create a fraction between 0 and 1.  This
 * fractional number is then multiplied by 360 to produce the hue
 * angle in the HSB color model.
 *
 * The integer that is returned by [HSBtoRGB] encodes the
 * value of a color in bits 0-23 of an integer value that is the same
 * format used by the method [getARGB()][Color.argb]
 * This integer can be supplied as an argument to the
 * [toColor] method that takes a single integer argument to create a [Color].
 * @param hue the hue component of the color
 * @param saturation the saturation of the color
 * @param brightness the brightness of the color
 * @return the RGB value of the color with the indicated hue,
 *                            saturation, and brightness.
 */
@Suppress("FunctionName")
fun HSBtoRGB(hue: Float, saturation: Float, brightness: Float): Int {
    var r = 0
    var g = 0
    var b = 0
    if (saturation == 0f) {
        b = (brightness * 255.0f + 0.5f).toInt()
        g = b
        r = g
    } else {
        val h = (hue - floor(hue)) * 6.0f
        val f = h - floor(h)
        val p = brightness * (1.0f - saturation)
        val q = brightness * (1.0f - saturation * f)
        val t = brightness * (1.0f - saturation * (1.0f - f))
        when (h.toInt()) {
            0 -> {
                r = (brightness * 255.0f + 0.5f).toInt()
                g = (t * 255.0f + 0.5f).toInt()
                b = (p * 255.0f + 0.5f).toInt()
            }

            1 -> {
                r = (q * 255.0f + 0.5f).toInt()
                g = (brightness * 255.0f + 0.5f).toInt()
                b = (p * 255.0f + 0.5f).toInt()
            }

            2 -> {
                r = (p * 255.0f + 0.5f).toInt()
                g = (brightness * 255.0f + 0.5f).toInt()
                b = (t * 255.0f + 0.5f).toInt()
            }

            3 -> {
                r = (p * 255.0f + 0.5f).toInt()
                g = (q * 255.0f + 0.5f).toInt()
                b = (brightness * 255.0f + 0.5f).toInt()
            }

            4 -> {
                r = (t * 255.0f + 0.5f).toInt()
                g = (p * 255.0f + 0.5f).toInt()
                b = (brightness * 255.0f + 0.5f).toInt()
            }

            5 -> {
                r = (brightness * 255.0f + 0.5f).toInt()
                g = (p * 255.0f + 0.5f).toInt()
                b = (q * 255.0f + 0.5f).toInt()
            }
        }
    }
    return -0x1000000 or (r shl 16) or (g shl 8) or (b shl 0)
}

/**
 * Converts the components of a color, as specified by the default RGB
 * model, to an equivalent set of values for hue, saturation, and
 * brightness that are the three components of the HSB model.
 *
 * If the [out] argument is `null`, then a
 * new array is allocated to return the result. Otherwise, the method
 * returns the array [out], with the values put into that array.
 * @param r the red component of the color
 * @param g the green component of the color
 * @param b the blue component of the color
 * @param out the array used to return the three HSB values, or `null`
 * @return an array of three elements containing the hue, saturation, and brightness (in that order), of the color with the indicated red, green, and blue components.
 * @see Color
 * @since 0.18.2
 */
@Suppress("FunctionName", "NAME_SHADOWING")
fun RGBtoHSB(r: Int, g: Int, b: Int, out: FloatArray? = null): FloatArray {
    var hue: Float
    val saturation: Float
    val brightness: Float
    val out = out ?: FloatArray(3)
    var cmax = if (r > g) r else g
    if (b > cmax) cmax = b
    var cmin = if (r < g) r else g
    if (b < cmin) cmin = b

    brightness = cmax.toFloat() / 255.0f
    saturation = if (cmax != 0) (cmax - cmin).toFloat() / cmax.toFloat() else 0f
    if (saturation == 0f) {
        hue = 0f
    } else {
        val redc = (cmax - r).toFloat() / (cmax - cmin).toFloat()
        val greenc = (cmax - g).toFloat() / (cmax - cmin).toFloat()
        val bluec = (cmax - b).toFloat() / (cmax - cmin).toFloat()
        hue = if (r == cmax) bluec - greenc else if (g == cmax) 2.0f + redc - bluec else 4.0f + greenc - redc
        hue /= 6.0f
        if (hue < 0) hue += 1.0f
    }
    out[0] = hue
    out[1] = saturation
    out[2] = brightness
    return out
}

/**
 * Takes an ARGB integer color and returns a [Color] object.
 */
fun Int.toColor() = Color(RGBtoHSB(this shr 16 and 0xFF, this shr 8 and 0xFF, this and 0xFF), this shr 24 and 0xFF)

inline val Int.red get() = this shr 16 and 0xFF

inline val Int.green get() = this shr 8 and 0xFF

inline val Int.blue get() = this and 0xFF

inline val Int.alpha get() = this shr 24 and 0xFF

fun Double.toRadians() = (this % 360.0) * (PI / 180.0)

/**
 * Calculate the greatest common denominator of two integers.
 * @since 0.18.4
 */
@Suppress("NAME_SHADOWING")
fun Int.gcd(b: Int): Int {
    var a = this
    var b = b
    while (b != 0) {
        val t = b
        b = a % b
        a = t
    }
    return a
}

/**
 * Simplify a ratio of two integers.
 * @since 0.18.4
 */
fun Pair<Int, Int>.simplifyRatio(): Pair<Int, Int> {
    val gcd = first.gcd(second)
    return Pair(first / gcd, second / gcd)
}

/**
 * Returns the value closer to zero.
 *
 * If either value is `NaN`, then the result is `NaN`.
 *
 * If `a == b`, then the result is `a`.
 */
inline fun cl0(a: Float, b: Float): Float {
    return if (abs(a) <= abs(b)) {
        a
    } else {
        b
    }
}

/**
 * Returns the value closer to one.
 *
 * If either value is `NaN`, then the result is `NaN`.
 *
 * If `a == b`, then the result is `a`.
 */
inline fun cl1(a: Float, b: Float): Float {
    return if (abs(a - 1f) <= abs(b - 1f)) {
        a
    } else {
        b
    }
}

/**
 * Returns the minimum value among three given ints.
 *
 * @param a the first value
 * @param b the second value
 * @param c the third value
 * @return the minimum value among a, b, and c
 */
inline fun min(a: Int, b: Int, c: Int): Int = min(min(a, b), c)

/** convert the given float into an array of 4 floats for radii. */
inline fun Float.radii() = floatArrayOf(this, this, this, this)

/** convert the given floats into an array of 4 floats for radii. */
inline fun radii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) = floatArrayOf(topLeft, topRight, bottomLeft, bottomRight)

/** varargs wrapper */
inline fun <T> varargs(vararg any: T): Array<out T> {
    return any
}

/** print the object to stdout, then return it. */
inline fun <T> T.stdout(): T {
    println(this)
    return this
}

/** print the given string to stdout, then return the object. */
inline fun <T> T.prints(string: String): T {
    println(string)
    return this
}

fun Short.fromModifierMerged(): Array<KeyModifiers> = KeyModifiers.fromModifierMerged(this)

fun Array<out KeyModifiers>.merge(): Short = KeyModifiers.merge(*this)

// strutils.kt

/**
 * append the given [CharSequence][c] to this [StringBuilder], repeated [repeats] times.
 *
 * This function is equivalent to doing [sb][StringBuilder]`.append(`[c].[repeat][CharSequence.repeat]`(`[repeats]`)`, but it uses an already existing StringBuilder.
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

fun String.dropAt(index: Int = lastIndex, amount: Int = 1): String {
    if (index - amount == 0) return ""
    if (index - amount < 0) return this
    return substring(0, index - amount) + substring(index)
}

/** safe [substring] function.
 * - if [fromIndex] is negative, it will be set to 0
 * - if [toIndex] is greater than the length of the string, or negative, it will be set to the length of the string
 * - if [fromIndex] is greater than [toIndex], they will be swapped, then substring'd as normal
 */
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

/** take the given string and cut it to the length, returning a pair of the string cut to the length, and the remainder.
 *
 * if the string fits within the width, the first string will be the entire string, and the second will be empty.
 */
fun String.substringToWidth(
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    width: Float,
    textAlign: TextAlign = TextAlign.Left
): Pair<String, String> {
    if (renderer.settings.debug && renderer.textBounds(
            font,
            "W",
            fontSize,
            textAlign
        ).width > width
    ) { // this is enabled only on debug mode for performance in prod
        throw RuntimeException("Text box maximum width is too small for the given font size! (string: $this, font: ${font.resourceName}, fontSize: $fontSize, width: $width)")
    }
    if (renderer.textBounds(font, this, fontSize, textAlign).width <= width) {
        return this to ""
    }

    var left = 0
    var right = length - 1
    var result = ""
    while (left <= right) {
        val mid = (left + right) / 2
        val substring = substring(0, mid + 1)
        if (renderer.textBounds(font, substring, fontSize, textAlign).width <= width) {
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
                d[i][j] = min(
                    d[i - 1][j] + 1, // deletion
                    d[i][j - 1] + 1, // insertion
                    d[i - 1][j - 1] + 1 // substitution
                )
            }
        }
    }

    return d[length][other.length]
}

/**
 * Wrap the given text to the given width, returning a list of lines.
 */
fun String.wrap(
    maxWidth: Float,
    maxHeight: Float = 0f,
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    textAlign: TextAlign = TextAlign.Left
): ArrayList<String> {
    if (maxWidth == 0f) return arrayListOf(this)
    val words = split(" ").toArrayList()
    if (words.size == 0) return arrayListOf("")
    val lines = arrayListOf<String>()
    var currentLine = StringBuilder()

    words.fastEach { word ->
        val wordLength = renderer.textBounds(font, word, fontSize, textAlign).width

        if (wordLength > maxWidth) {
            // ah. word is longer than the maximum wrap width
            if (currentLine.isNotEmpty()) {
                // Finish current line and start a new one with the long word
                lines.add(currentLine.toString())
                currentLine.clear()
            }

            // add the long word to the lines, splitting it up into smaller chunks if needed
            var remainingWord = word
            while (remainingWord.isNotEmpty()) {
                val chunk = remainingWord.substringToWidth(renderer, font, fontSize, maxWidth, textAlign)
                lines.add(chunk.first)
                remainingWord = chunk.second
            }
        } else if (currentLine.isEmpty()) {
            currentLine.append(word)
        } else if (renderer.textBounds(font, currentLine.toString(), fontSize, textAlign).width + wordLength <= maxWidth) {
            // ok!
            currentLine.append(' ').append(word)
        } else {
            // asm: word doesn't fit in current line, wrap it to the next line
            lines.add(currentLine.toString())
            currentLine = currentLine.clear().append(word)
        }
    }

    // Add the last line
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine.toString())
    }

    return lines
}

/**
 * Returns the closest character index from the given string to the given point [x].
 *
 * @return the index of the character closest to the given point [x], or `-1` if it could not be found.
 *
 * @since 0.18.5
 */
fun String.closestToPoint(renderer: Renderer, font: Font, fontSize: Float, alignment: TextAlign, x: Float): Int {
    var prev = 0f
    for (c in this.indices) {
        val w = renderer.textBounds(
            font,
            this.substring(0, c),
            fontSize,
            alignment
        ).width
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
