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

package org.polyfrost.polyui.input

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.utils.fastEach
import java.lang.StringBuilder

/**
 * # KeyBinder
 *
 * KeyBinder handles key bindings for the PolyUI instance.
 *
 * @see add
 * @see remove
 * @see removeAll
 */
class KeyBinder(private val polyUI: PolyUI) {
    private val listeners = ArrayList<Bind>(5)
    private val downMouseButtons = ArrayList<Int>(5)
    private val downUnmappedKeys = ArrayList<Int>(5)
    private val downKeys = ArrayList<Keys>(5)

    /**
     * accept a keystroke event. This will call all keybindings that match the event.
     *
     * This method is public, but marked as internal. This is because it should only be called by the PolyUI instance, unless you are trying to externally force a keypress (which you probably shouldn't be)
     */
    @ApiStatus.Internal
    fun accept(event: Event): Boolean {
        if (event is MousePressed) {
            downMouseButtons.add(event.button)
        }
        if (event is MouseReleased) {
            downMouseButtons.remove(event.button)
        }
        if (event is FocusedEvent.KeyPressed) {
            downKeys.add(event.key)
        }
        if (event is FocusedEvent.KeyReleased) {
            downKeys.remove(event.key)
        }
        return update(0L)
    }

    /**
     * accept an unmapped keystroke event. This will call all keybindings that match the event.
     *
     * This method is public, but marked as internal. This is because it should only be called by the PolyUI instance, unless you are trying to externally force a keypress (which you probably shouldn't be)
     */
    @ApiStatus.Internal
    fun accept(key: Int, down: Boolean): Boolean {
        if (down) {
            downUnmappedKeys.add(key)
        } else {
            downUnmappedKeys.remove(key)
        }
        return update(0L)
    }

    fun update(deltaTimeNanos: Long): Boolean {
        listeners.fastEach {
            if (it.update(downUnmappedKeys, downKeys, downMouseButtons, polyUI.eventManager.keyModifiers, deltaTimeNanos)) {
                return true
            }
        }
        return false
    }

    /**
     * Add a keybind to this PolyUI instance, that will be ran when the given keys are pressed.
     * @since 0.21.0
     */
    fun add(bind: Bind) {
        listeners.add(bind)
    }

    class Bind @JvmOverloads constructor(val unmappedKeys: IntArray? = null, val keys: Array<Keys>? = null, val mouse: IntArray? = null, val mods: Short = 0, val durationNanos: Long = 0L, val action: () -> Boolean) {
        constructor(chars: CharArray? = null, keys: Array<Keys>? = null, mouse: IntArray? = null, mods: Short = 0, durationNanos: Long = 0L, action: () -> Boolean) : this(chars?.map { it.code }?.toIntArray(), keys, mouse, mods, durationNanos, action)
        constructor(char: Char, keys: Array<Keys>? = null, mouse: IntArray? = null, mods: Short = 0, durationNanos: Long = 0L, action: () -> Boolean) : this(intArrayOf(char.code), keys, mouse, mods, durationNanos, action)
        constructor(unmappedKeys: IntArray? = null, keys: Array<Keys>? = null, mouse: Array<Mouse>? = null, mods: Short = 0, durationNanos: Long = 0L, action: () -> Boolean) : this(unmappedKeys, keys, mouse?.map { it.value.toInt() }?.toIntArray(), mods, durationNanos, action)
        constructor(unmappedKeys: IntArray? = null, keys: Array<Keys>? = null, mouse: Mouse? = null, mods: Short = 0, durationNanos: Long = 0L, action: () -> Boolean) : this(unmappedKeys, keys, mouse?.value?.let { intArrayOf(it.toInt()) }, mods, durationNanos, action)
        constructor(unmappedKeys: IntArray? = null, key: Keys? = null, mouse: Array<Mouse>? = null, mods: Short = 0, durationNanos: Long = 0L, action: () -> Boolean) : this(unmappedKeys, key?.let { arrayOf(it) }, mouse, mods, durationNanos, action)

        private var time = 0L
        private var ran = false

        fun update(c: ArrayList<Int>, k: ArrayList<Keys>, m: ArrayList<Int>, mods: Short, deltaTimeNanos: Long): Boolean {
            if (unmappedKeys?.matches(c) != false && keys?.matches(k) != false && mouse?.matches(m) != false && this.mods == mods) {
                if (!ran) {
                    if (durationNanos != 0L) {
                        time += deltaTimeNanos
                        if (time >= durationNanos) {
                            ran = true
                            return action()
                        }
                    } else {
                        ran = true
                        return action()
                    }
                }
            } else {
                time = 0L
                ran = false
            }
            return false
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("KeyBind(")
            val s = Modifiers.toStringPretty(mods)
            if (s.isNotEmpty()) {
                sb.append(s)
            }
            if (unmappedKeys != null) {
                if (s.isNotEmpty()) sb.append(" + ")
                for (c in unmappedKeys) {
                    sb.append(c.toChar())
                    sb.append(" + ")
                }
            }
            if (keys != null) {
                if (s.isNotEmpty()) sb.append(" + ")
                for (k in keys) {
                    sb.append(Keys.toStringPretty(k))
                    sb.append(" + ")
                }
            }
            if (mouse != null) {
                if (s.isNotEmpty()) sb.append(" + ")
                for (m in mouse) {
                    sb.append(Mouse.toStringPretty(Mouse.fromValue(m)))
                    sb.append(" + ")
                }
            }
            sb.setLength(sb.length - 3)
            sb.append(")")
            return sb.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Bind) return false
            if (other.unmappedKeys?.contentEquals(unmappedKeys) == false) return false
            if (other.keys?.contentEquals(keys) == false) return false
            if (other.mouse?.contentEquals(mouse) == false) return false
            if (other.mods != mods) return false
            if (other.durationNanos != durationNanos) return false
            return true
        }

        override fun hashCode(): Int {
            var result = unmappedKeys?.contentHashCode() ?: 0
            result = 31 * result + (keys?.contentHashCode() ?: 0)
            result = 31 * result + (mouse?.contentHashCode() ?: 0)
            result = 31 * result + mods.hashCode()
            result = 31 * result + durationNanos.hashCode()
            return result
        }

        private fun <T> Array<T>.matches(other: ArrayList<T>): Boolean {
            if (other.size == 0) return false
            for (i in this) {
                if (!other.contains(i)) return false
            }
            return true
        }

        private fun IntArray.matches(other: ArrayList<Int>): Boolean {
            if (other.size == 0) return false
            for (i in this) {
                if (!other.contains(i)) return false
            }
            return true
        }
    }
}
