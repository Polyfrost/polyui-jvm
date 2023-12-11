/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

@file:Suppress("NOTHING_TO_INLINE", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED")
@file:JvmName("Utils")

package org.polyfrost.polyui.utils

import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Modifiers
import org.polyfrost.polyui.renderer.data.PolyImage
import kotlin.experimental.and
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

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
 * format used by the method getARGB().
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

@kotlin.internal.InlineOnly
inline val Int.red get() = this shr 16 and 0xFF

@kotlin.internal.InlineOnly
inline val Int.green get() = this shr 8 and 0xFF

@kotlin.internal.InlineOnly
inline val Int.blue get() = this and 0xFF

@kotlin.internal.InlineOnly
inline val Int.alpha get() = this shr 24 and 0xFF

@kotlin.internal.InlineOnly
inline fun Double.toRadians() = (this % 360.0) * (PI / 180.0)

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
@kotlin.internal.InlineOnly
inline fun cl0(a: Float, b: Float) = if (abs(a) <= abs(b)) a else b

/**
 * Returns the value closer to one.
 *
 * If either value is `NaN`, then the result is `NaN`.
 *
 * If `a == b`, then the result is `a`.
 */
@kotlin.internal.InlineOnly
inline fun cl1(a: Float, b: Float) = if (abs(a - 1f) <= abs(b - 1f)) a else b

/**
 * Returns the minimum value among three given ints.
 *
 * @param a the first value
 * @param b the second value
 * @param c the third value
 * @return the minimum value among a, b, and c
 */
@kotlin.internal.InlineOnly
inline fun min3(a: Int, b: Int, c: Int): Int = min(min(a, b), c)

/** convert the given float into an array of 4 floats for radii. */
@kotlin.internal.InlineOnly
inline fun Number.radii() = floatArrayOf(this.toFloat(), this.toFloat(), this.toFloat(), this.toFloat())

/** convert the given floats into an array of 4 floats for radii. */
@kotlin.internal.InlineOnly
inline fun radii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) = floatArrayOf(topLeft, topRight, bottomLeft, bottomRight)

/** print the object to stdout, then return it. */
@Deprecated("remove in prod")
inline fun <T> T.stdout(arg: Any? = null): T {
    if (arg != null) print("$arg -> ")
    println(this)
    return this
}

fun String.image() = PolyImage(this)

@kotlin.internal.InlineOnly
inline fun Any?.identityHashCode() = System.identityHashCode(this)

/**
 * Return true if the Collection is empty or null.
 */
@kotlin.internal.InlineOnly
inline fun Collection<*>?.isEmpty() = this?.isEmpty() ?: true

fun Short.fromModifierMerged(): Array<KeyModifiers> = KeyModifiers.fromModifierMerged(this)

@kotlin.internal.InlineOnly
inline fun Short.hasModifier(mod: Modifiers): Boolean = this and mod.value != 0.toShort()

@kotlin.internal.InlineOnly
inline fun Array<out KeyModifiers>.merge(): Short = KeyModifiers.merge(*this)

/**
 * Moves the given element from the [from] index to the [to] index.
 *
 * **Note**: this method makes absolutely no attempt to verify if the given
 * indices are valid.
 *
 * @param from the index of the element to move
 * @param to the index to move the element to
 */
@kotlin.internal.InlineOnly
inline fun <E> Array<E>.moveElement(from: Int, to: Int) {
    val item = this[from]
    this[from] = this[to]
    this[to] = item
}

/**
 * Return this collection as an LinkedList. **Note:** if it is already a LinkedList, it will be returned as-is.
 */
@kotlin.internal.InlineOnly
inline fun <T> Collection<T>.asLinkedList(): LinkedList<T> = if (this is LinkedList) this else LinkedList(this)

@kotlin.internal.InlineOnly
inline fun <T> Array<T>.asLinkedList(): LinkedList<T> = LinkedList(*this)

/**
 * Returns the value of the given [key] in the map, and if [shouldRemove] is `true` the value is also removed from the map.
 * @since 1.0.2
 */
@kotlin.internal.InlineOnly
inline fun <K, V> MutableMap<K, V>.maybeRemove(key: K, shouldRemove: Boolean) = if (shouldRemove) remove(key) else get(key)

fun Drawable.printInfo() {
    var c: Drawable? = parent
    var i = 0
    val sb = StringBuilder().append("Tree for $this:\n")
    while (c != null) {
        sb.append("\t", i).append(c.toString()).append('\n')
        c = c.parent
        i++
    }
    i++
    sb.append("\t", i).append(polyUI.toString())
    println(sb.toString())
}

/**
 * Perform the given function on both elements of this pair. The highest common type of both elements is used as the type of the parameter.
 */
inline fun <T> Pair<T, T>.both(func: (T) -> Unit) {
    func(this.first)
    func(this.second)
}
