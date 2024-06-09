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

@file:Suppress("UNUSED")
@file:JvmName("ColorUtils")

package org.polyfrost.polyui.utils

import org.polyfrost.polyui.color.PolyColor
import kotlin.math.floor

/** create a color from the given red, green, and blue integer values, and an alpha value `0f..1f` */
@JvmOverloads
fun rgba(r: Int, g: Int, b: Int, a: Float = 1f): PolyColor {
    val cmps = RGBtoHSB(r, g, b)
    return PolyColor.Static(cmps[0], cmps[1], cmps[2], a)
}

fun hsba(h: Float, s: Float, b: Float, a: Float = 1f) = PolyColor.Static(h, s, b, a)

/**
 * Takes an ARGB integer color and returns a [PolyColor] object.
 */
fun Int.toColor(): PolyColor {
    val cmps = RGBtoHSB(this shr 16 and 0xFF, this shr 8 and 0xFF, this and 0xFF)
    return PolyColor.Static(cmps[0], cmps[1], cmps[2], (this shr 24 and 0xFF) / 255f)
}

fun PolyColor.Gradient.toAnimatableGradient() = if (this is PolyColor.Gradient.Animatable) this else PolyColor.Gradient.Animatable(this.toAnimatable(), color2.toAnimatable(), type)

fun PolyColor.toAnimatable() = if (this is PolyColor.Animatable) this else PolyColor.Animatable(this.hue, this.saturation, this.brightness, this.alpha)

fun PolyColor.toMutable() = if (this is PolyColor.Mutable) this else PolyColor.Mutable(this.hue, this.saturation, this.brightness, this.alpha)

/**
 * Turn the given hex string into a color.
 *
 * - If it is 8 characters long, it is assumed to be in the format `#RRGGBBAA` (alpha optional)
 * - If there is a leading `#`, it will be removed.
 * - If it is 1 character long, the character is repeated e.g. `#f` -> `#ffffff`
 * - If it is 2 characters long, the character is repeated e.g. `#0f` -> `#0f0f0f`
 * - If it is 3 characters long, the character is repeated e.g. `#0fe` -> `#00ffee`
 *
 * @throws IllegalArgumentException if the hex string is not in a valid format
 * @throws NumberFormatException if the hex string is not a valid hex string
 */
fun String.toColor(): PolyColor {
    var hexColor = this.removePrefix("#")
    when (hexColor.length) {
        1 -> hexColor = hexColor.repeat(6)
        2 -> hexColor = hexColor.repeat(3)
        3 -> hexColor = run {
            var newHex = ""
            hexColor.forEach {
                newHex += it.toString().repeat(2)
            }
            newHex
        }

        6 -> {}
        8 -> {}
        else -> throw IllegalArgumentException("Invalid hex color: $this")
    }
    val r = hexColor.substring(0, 2).toInt(16)
    val g = hexColor.substring(2, 4).toInt(16)
    val b = hexColor.substring(4, 6).toInt(16)
    val a = if (hexColor.length == 8) hexColor.substring(6, 8).toInt(16) else 255
    return rgba(r, g, b, a / 255f)
}

/** @see toColor */
fun String.toColor(alpha: Float = 1f) = (this + (alpha * 255f).toInt().toString(16)).toColor()

@OptIn(ExperimentalStdlibApi::class)
fun PolyColor.toHex(alpha: Boolean = true, hash: Boolean = true): String {
    val c = if (alpha) argb else argb and 0x00FFFFFF
    return when (hash) {
        true -> "#${c.toHexString(HexFormat.UpperCase)}"
        false -> c.toHexString(HexFormat.UpperCase)
    }
}

inline val Int.red get() = this shr 16 and 0xFF

inline val Int.green get() = this shr 8 and 0xFF

inline val Int.blue get() = this and 0xFF

inline val Int.alpha get() = this shr 24 and 0xFF

/**
 * Return a PolyUI compatible color object representative of this Java color object.
 * @see PolyColor.toJavaColor
 * @since 1.1.51
 */
fun java.awt.Color.toPolyColor() = rgba(red, green, blue, alpha / 255f)

/**
 * Return an animatable PolyUI color object representative of this Java color object.
 * @see PolyColor.toJavaColor
 * @since 1.1.51
 */
fun java.awt.Color.toAnimatedPolyColor(): PolyColor.Animatable {
    val hsb = RGBtoHSB(red, green, blue)
    return PolyColor.Animatable(hsb[0], hsb[1], hsb[2], alpha / 255f)
}

/**
 * Return a static java [Color][java.awt.Color] object of this color at the instant this method is called.
 *
 * Future changes to this color will not be reflected in the returned object.
 *
 * @since 1.1.51
 */
fun PolyColor.toJavaColor() = java.awt.Color(argb, true)

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
 * format used by the method getARGB().
 * This integer can be supplied as an argument to the
 * [toColor] method that takes a single integer argument to create a [PolyColor].
 * @param hue the hue component of the color
 * @param saturation the saturation of the color
 * @param brightness the brightness of the color
 * @param alpha *(since 1.3)* the alpha of the color
 * @return the RGB value of the color with the indicated hue,
 *                            saturation, and brightness.
 */
@Suppress("FunctionName")
fun HSBtoRGB(hue: Float, saturation: Float, brightness: Float, alpha: Float = 1f): Int {
    val r: Int
    val g: Int
    val b: Int
    if (saturation == 0f) {
        r = (brightness * 255.0f + 0.5f).toInt()
        g = r
        b = r
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

            else -> {
                r = 0
                g = 0
                b = 0
            }
        }
    }
    return ((alpha * 255f).toInt() shl 24) or (r shl 16) or (g shl 8) or b
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
 * @see PolyColor
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
    val diff = (cmax - cmin).toFloat()

    brightness = cmax.toFloat() / 255.0f
    saturation = if (cmax != 0) diff / cmax.toFloat() else 0f
    if (saturation == 0f) {
        hue = 0f
    } else {
        val redc = (cmax - r).toFloat() / diff
        val greenc = (cmax - g).toFloat() / diff
        val bluec = (cmax - b).toFloat() / diff
        hue = if (r == cmax) bluec - greenc else if (g == cmax) 2.0f + redc - bluec else 4.0f + greenc - redc
        hue /= 6.0f
        if (hue < 0f) hue += 1.0f
    }
    out[0] = hue
    out[1] = saturation
    out[2] = brightness
    return out
}

