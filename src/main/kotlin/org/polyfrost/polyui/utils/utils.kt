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

@file:Suppress("UNUSED", "NOTHING_TO_INLINE", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmName("Utils")

package org.polyfrost.polyui.utils

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.renderer.Window
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Vec2
import kotlin.enums.EnumEntries
import kotlin.jvm.internal.Ref
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min

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
@kotlin.internal.InlineOnly
inline fun EnumEntries<*>.names() = this.map { it.name }

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

/** print the object to stdout, then return it. */
@Deprecated("remove in prod")
inline fun <T> T.stdout(arg: Any? = null): T {
    if (arg != null) print("$arg -> ")
    println(this)
    return this
}

@kotlin.internal.InlineOnly
inline fun String.image() = PolyImage(this)

@JvmName("image")
@kotlin.internal.InlineOnly
inline fun String.image(size: Vec2) = PolyImage(this).also { it.size = size }

@kotlin.internal.InlineOnly
inline fun String.translated(vararg args: Any?) = Translator.Text.Formatted(Translator.Text.Simple(this), *args)

@kotlin.internal.InlineOnly
inline fun String.translated(): Translator.Text = Translator.Text.Simple(this)

@kotlin.internal.InlineOnly
inline fun Translator.Text.dont(): Translator.Text.Dont = if (this is Translator.Text.Dont) this else Translator.Text.Dont(this)

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
 * Perform the given [transform] on every element in this array, and return a new array with the results.
 *
 * Equivalent to `this.map { transform(it) }.toTypedArray()`, but saves on the creation of an intermediate list.
 */
@kotlin.internal.InlineOnly
inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R): Array<R> {
    return Array(size) {
        transform(this[it])
    }
}

/**
 * Perform the given [transform] on every element in this list, and return a new array with the results.
 *
 * Equivalent to `this.map { transform(it) }.toTypedArray()`, but saves on the creation of an intermediate list.
 */
@kotlin.internal.InlineOnly
inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> {
    val out = arrayOfNulls<R>(size)
    var i = 0
    for (element in this) {
        out[i] = transform(element)
        i++
    }
    @Suppress("UNCHECKED_CAST")
    return out as Array<R>
}

/**
 * Perform the given [transform] on every element in this map, and return a new array with the results.
 *
 * Equivalent to `this.map { transform(it) }.toTypedArray()`, but saves on the creation of an intermediate list.
 */
@kotlin.internal.InlineOnly
inline fun <K, V, reified R> Map<K, V>.mapToArray(transform: (Map.Entry<K, V>) -> R): Array<R> {
    val out = arrayOfNulls<R>(size)
    var i = 0
    for (entry in this) {
        out[i] = transform(entry)
        i++
    }
    @Suppress("UNCHECKED_CAST")
    return out as Array<R>
}

fun FloatArray.elementsEqual(): Boolean {
    if (isEmpty()) return true
    val first = this[0]
    for (i in 1 until size) {
        if (this[i] != first) return false
    }
    return true
}

@kotlin.internal.InlineOnly
inline fun PolyUI.open(window: Window) {
    window.open(this)
}

fun FloatArray.set(value: Float): FloatArray {
    for (i in this.indices) {
        this[i] = value
    }
    return this
}

/**
 * Ensure that this list is at least [size] elements long, and if it is not, add elements to it using the given [initializer].
 * @since 1.0.7
 */
inline fun <T> MutableList<T>.ensureSize(size: Int, initializer: (Int) -> T): MutableList<T> {
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
@kotlin.internal.InlineOnly
inline fun <T> T.ref(): Ref.ObjectRef<T> {
    val ref = Ref.ObjectRef<T>()
    ref.element = this
    return ref
}

/**
 * Return the value of this [Ref.ObjectRef].
 */
@kotlin.internal.InlineOnly
inline fun <T> Ref.ObjectRef<T>.deref(): T {
    return this.element
}

/**
 * Returns the value of the given [key] in the map, and if [shouldRemove] is `true` the value is also removed from the map.
 * @since 1.0.2
 */
@kotlin.internal.InlineOnly
inline fun <K, V> MutableMap<K, V>.maybeRemove(key: K, shouldRemove: Boolean): V? = if (shouldRemove) remove(key) else get(key)

/**
 * Perform the given function on both elements of this pair. The highest common type of both elements is used as the type of the parameter.
 */
@kotlin.internal.InlineOnly
inline fun <T> Pair<T, T>.both(func: (T) -> Unit) {
    func(this.first)
    func(this.second)
}
