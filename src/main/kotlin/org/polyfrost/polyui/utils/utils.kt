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

import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Modifiers
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Vec2
import kotlin.enums.EnumEntries
import kotlin.experimental.and
import kotlin.jvm.internal.Ref
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min


/** create a color from the given red, green, and blue integer values, and an alpha value `0f..1f` */
@JvmOverloads
fun rgba(r: Int, g: Int, b: Int, a: Float = 1f) = PolyColor(r, g, b, a)

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
 * @return the RGB value of the color with the indicated hue,
 *                            saturation, and brightness.
 */
@Suppress("FunctionName")
fun HSBtoRGB(hue: Float, saturation: Float, brightness: Float): Int {
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
    return -0x1000000 or (r shl 16) or (g shl 8) or b
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

/**
 * Takes an ARGB integer color and returns a [PolyColor] object.
 */
fun Int.toColor() = PolyColor(RGBtoHSB(this shr 16 and 0xFF, this shr 8 and 0xFF, this and 0xFF), (this shr 24 and 0xFF) / 255f)

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
 * Get an enum constant by its name, or `null` if [name] is `null`; or does not match any of this enum's constants.
 */
fun <E : Enum<E>> EnumEntries<E>.getByName(name: String?, ignoreCase: Boolean = false): E? {
    if (name == null) return null
    for (entry in this) {
        if (entry.name.equals(name, ignoreCase)) return entry
    }
    return null
}

/**
 * Return a list of the names of the entries in this enum.
 */
fun EnumEntries<*>.names() = this.map { it.name }

/**
 * Simplify a ratio of two integers.
 * @since 0.18.4
 */
fun Pair<Int, Int>.simplifyRatio(): Pair<Int, Int> {
    val gcd = first.gcd(second)
    return Pair(first / gcd, second / gcd)
}

/**
 * Return an immutable copy of this vector.
 */
fun Vec2.makeImmutable() = Vec2.Immutable(this.x, this.y)

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

@kotlin.internal.InlineOnly
inline fun String.image() = PolyImage(this)

@kotlin.internal.InlineOnly
inline fun String.translated(vararg args: Any?) = Translator.Text.Formatted(Translator.Text.Simple(this), *args)

@kotlin.internal.InlineOnly
inline fun String.translated(): Translator.Text = Translator.Text.Simple(this)

@kotlin.internal.InlineOnly
inline fun Any?.identityHashCode() = System.identityHashCode(this)

fun Short.fromModifierMerged(): Array<KeyModifiers> = KeyModifiers.fromModifierMerged(this)

@kotlin.internal.InlineOnly
inline fun Short.hasModifier(mod: Modifiers): Boolean = this and mod.value != 0.toShort()

@kotlin.internal.InlineOnly
inline fun Array<KeyModifiers>.merge(): Short = KeyModifiers.merge(*this)

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

fun FloatArray.areEqual(): Boolean {
    if (isEmpty()) return true
    val first = this[0]
    for (i in 1 until size) {
        if (this[i] != first) return false
    }
    return true
}

/**
 * Ensure that this list is at least [size] elements long, and if it is not, add elements to it using the given [initializer].
 * @since 1.0.7
 */
fun <T> MutableList<T>.ensureSize(size: Int, initializer: (Int) -> T): MutableList<T> {
    if (this.size < size) {
        for (i in this.size until size) {
            this.add(initializer(i))
        }
    }
    return this
}

/**
 * Box the given value into a [Ref.ObjectRef].
 */
fun <T> T.ref(): Ref.ObjectRef<T> {
    val ref = Ref.ObjectRef<T>()
    ref.element = this
    return ref
}

/**
 * Return the value of this [Ref.ObjectRef].
 */
fun <T> Ref.ObjectRef<T>.deref(): T {
    return this.element
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
inline fun <K, V> MutableMap<K, V>.maybeRemove(key: K, shouldRemove: Boolean): V? = if (shouldRemove) remove(key) else get(key)

/**
 * Perform the given function on both elements of this pair. The highest common type of both elements is used as the type of the parameter.
 */
inline fun <T> Pair<T, T>.both(func: (T) -> Unit) {
    func(this.first)
    func(this.second)
}
