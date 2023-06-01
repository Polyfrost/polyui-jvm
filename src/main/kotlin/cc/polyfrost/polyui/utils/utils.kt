/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

@file:Suppress("NOTHING_TO_INLINE")
@file:JvmName("Utils")

package cc.polyfrost.polyui.utils

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.unit.TextAlign
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

fun rgba(r: Float, g: Float, b: Float, a: Float): Color {
    return Color(r, g, b, a)
}

/** figma copy-paste accessor */
fun rgba(r: Int, g: Int, b: Int, a: Float): Color {
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
 * @param hue   the hue component of the color
 * @param saturation   the saturation of the color
 * @param brightness   the brightness of the color
 * @return the RGB value of the color with the indicated hue,
 *                            saturation, and brightness.
 */
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

fun Int.toColor(): Color {
    return Color(
        ((this shr 16) and 0xFF) / 255f,
        ((this shr 8) and 0xFF) / 255f,
        (this and 0xFF) / 255f,
        ((this shr 24) and 0xFF) / 255f
    )
}

fun java.awt.Color.toPolyColor() = Color(this.red, this.green, this.blue, this.alpha)

fun Float.rounded(places: Int = 2): Float {
    val f = 10.0.pow(places).toFloat()
    return (this * f).toInt() / f
}

/**
 * Returns the value closer to zero.
 *
 * If either value is `NaN`, then the result is `NaN`.
 *
 * If `a == b`, then the result is `a`.
 */
inline fun clz(a: Float, b: Float): Float {
    return if (abs(a) <= abs(b)) {
        a
    } else {
        b
    }
}

/** convert the given float into an array of 4 floats for radii. */
inline fun Float.radii() = floatArrayOf(this, this, this, this)

/** varargs wrapper */
inline fun varargs(vararg any: Any): Array<out Any> {
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

fun String.dropToLastSpace(maxIndex: Int = length): String {
    val lastSpace = lastIndexOf(' ', maxIndex)
    if (lastSpace == -1) {
        return ""
    }
    return substring(0, lastSpace)
}

fun String.dropAt(index: Int = length, amount: Int = 1): String {
    if (index - amount < 0) return this
    return substring(0, index - amount) + substring(index)
}

/** safe [substring] function.
 * - if [fromIndex] is negative, it will be set to 0
 * - if [toIndex] is greater than the length of the string, or negative, it will be set to the length of the string
 * - if [fromIndex] is greater than [toIndex], they will be swapped, then substring'd as normal
 */
fun String.substringSafe(fromIndex: Int, toIndex: Int = length): String {
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

/** Limit the given string to a width. If the string is too long, it will be cut to the width, and [limitText] will be appended to the end. */
fun String.limit(
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    width: Float,
    limitText: String = "...",
    textAlign: TextAlign = TextAlign.Left
): String {
    val delimiterWidth = renderer.textBounds(font, limitText, fontSize, textAlign).width
    var resultWidth = renderer.textBounds(font, this, fontSize, textAlign).width
    var t = this
    while (resultWidth + delimiterWidth > width) {
        resultWidth = renderer.textBounds(font, t, fontSize, textAlign).width
        t = t.substring(0, t.length - 1)
    }
    t += limitText
    return t
}

/**
 * Wrap the given text to the given width, returning a list of lines.
 * if there are too many lines, the last line will be [cut][limit] to fit the width, and "..." will be appended to the end. The method will also return true if the text was cut.
 */
fun String.wrap(
    maxWidth: Float,
    maxHeight: Float = 0f,
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    textAlign: TextAlign = TextAlign.Left
): Pair<ArrayList<String>, Boolean> {
    if (maxWidth == 0f) return arrayListOf(this) to false
    val words = split(" ").toArrayList()
    val maxLines = floor(maxHeight / fontSize).toInt()
    val lines = arrayListOf<String>()
    var currentLine = StringBuilder()

    words.fastEach { word ->
        val wordLength = renderer.textBounds(font, word, fontSize, textAlign).width

        if (wordLength > maxWidth) {
            // ah. word is longer than the maximum wrap width
            if (currentLine.isNotEmpty()) {
                // Finish current line and start a new one with the long word
                if (addLine(
                        lines,
                        currentLine.toString(),
                        maxLines,
                        renderer,
                        font,
                        fontSize,
                        maxWidth,
                        textAlign
                    )
                ) {
                    return lines to true
                }
                currentLine.clear()
            }

            // add the long word to the lines, splitting it up into smaller chunks if needed
            var remainingWord = word
            while (remainingWord.isNotEmpty()) {
                val chunk = remainingWord.substringToWidth(renderer, font, fontSize, maxWidth, textAlign)
                if (lines.size + 1 >= maxLines) {
                    // we're at the last line, so we need to cut the word to fit (this looks nicer)
                    val s = lines.removeLast()
                    lines.add(s + chunk.first.limit(renderer, font, fontSize, maxWidth, "...", textAlign))
                    return lines to true
                }
                if (addLine(
                        lines,
                        chunk.first,
                        maxLines,
                        renderer,
                        font,
                        fontSize,
                        maxWidth,
                        textAlign
                    )
                ) {
                    return lines to true
                }
                remainingWord = chunk.second
            }
        } else if (currentLine.isEmpty()) {
            currentLine.append(word)
        } else if (renderer.textBounds(
                font,
                currentLine.toString(),
                fontSize,
                textAlign
            ).width + 1 + wordLength <= maxWidth
        ) {
            // ok!
            currentLine.append(" ").append(word)
        } else {
            // asm: word doesn't fit in current line, wrap it to the next line
            if (addLine(
                    lines,
                    currentLine.toString(),
                    maxLines,
                    renderer,
                    font,
                    fontSize,
                    maxWidth,
                    textAlign
                )
            ) {
                return lines to true
            }
            currentLine = currentLine.clear().append(word)
        }
    }

    // Add the last line
    if (currentLine.isNotEmpty()) {
        if (addLine(
                lines,
                currentLine.toString(),
                maxLines,
                renderer,
                font,
                fontSize,
                maxWidth,
                textAlign
            )
        ) {
            return lines to true
        }
    }

    return lines to false
}

internal fun addLine(
    lines: ArrayList<String>,
    currentLine: String,
    maxLines: Int = 0,
    renderer: Renderer,
    font: Font,
    fontSize: Float,
    maxWidth: Float,
    textAlign: TextAlign = TextAlign.Left
): Boolean {
    if (maxLines != 0) {
        if (lines.size + 1 >= maxLines) {
            lines.add(lines.removeLast().limit(renderer, font, fontSize, maxWidth, "...", textAlign))
            return true
        }
    }
    lines.add(currentLine)
    return false
}
