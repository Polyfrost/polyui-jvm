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

package org.polyfrost.polyui.input

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.Settings
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.utils.IntArraySet
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.nullIfEmpty

/**
 * # KeyBinder
 *
 * KeyBinder handles key bindings for the PolyUI instance.
 *
 * @see add
 * @see remove
 * @see removeAll
 */
class KeyBinder(private val settings: Settings) {
    private val listeners = ArrayList<Bind>()

    /**
     * Property which enables an optimization where the KeyBinder will only update after every frame if there are time-sensitive listeners.
     * @since 1.1.1
     */
    var hasTimeSensitiveListeners = false
        private set
    private val downMouseButtons = IntArraySet(5)
    private val downUnmappedKeys = IntArraySet(5)
    private val downKeys = ArrayList<Keys>(5)

    private var recordingBind: Bind? = null
    private var recordingCallback: ((Bind?) -> Unit)? = null

    /**
     * accept a keystroke event. This will call all keybindings that match the event.
     *
     * This method is public, but marked as internal. This is because it should only be called by the PolyUI instance, unless you are trying to externally force a keypress (which you probably shouldn't be)
     */
    @ApiStatus.Internal
    fun accept(event: Event, mods: Byte) = when (event) {
        is Event.Mouse.Pressed -> {
            if (event.mods.isEmpty) {
                cancelRecord("Cannot bind to just left click")
                false
            } else {
                downMouseButtons.add(event.button)
                completeRecording(mods)
                update(0L, mods, true)
            }
        }

        is Event.Mouse.Released -> {
            downMouseButtons.remove(event.button)
            update(0L, mods, false)
        }

        is Event.Focused.KeyPressed -> {
            if (event.key == Keys.ESCAPE) {
                cancelRecord("ESC key pressed")
                false
            } else {
                downKeys.add(event.key)
                completeRecording(mods)
                update(0L, mods, true)
            }
        }

        is Event.Focused.KeyReleased -> {
            downKeys.remove(event.key)
            update(0L, mods, false)
        }

        else -> false
    }

    /**
     * accept an unmapped keystroke event. This will call all keybindings that match the event.
     *
     * This method is public, but marked as internal. This is because it should only be called by the PolyUI instance, unless you are trying to externally force a keypress (which you probably shouldn't be)
     */
    @ApiStatus.Internal
    fun accept(key: Int, down: Boolean, mods: Byte) = if (down) {
        downUnmappedKeys.add(key)
        completeRecording(mods)
        update(0L, mods, true)
    } else {
        downUnmappedKeys.remove(key)
        update(0L, mods, false)
    }

    @ApiStatus.Internal
    fun update(deltaTimeNanos: Long, mods: Byte, down: Boolean): Boolean {
        if (!hasTimeSensitiveListeners && deltaTimeNanos > 0L) return false
        var ret = false
        listeners.fastEach {
            if (it.update(downUnmappedKeys, downKeys, downMouseButtons, mods, deltaTimeNanos, down)) {
                ret = true
            }
        }
        return ret
    }

    /**
     * Return a list of duplicates of the given keybind.
     */
    fun getDuplicatesIfAny(bind: Bind) = listeners.filter { it == bind }

    /**
     * Add a keybind to this PolyUI instance, that will be run when the given keys are pressed.
     * @since 1.1.7
     */
    fun add(vararg binds: Bind) {
        for (bind in binds) {
            add(bind)
        }
    }

    /**
     * Add a keybind to this PolyUI instance, that will be run when the given keys are pressed.
     * @since 0.21.0
     */
    fun add(bind: Bind) {
        if (bind.durationNanos > 0L) hasTimeSensitiveListeners = true
        listeners.add(bind)
    }

    fun remove(bind: Bind) {
        listeners.remove(bind)
        if (hasTimeSensitiveListeners) {
            hasTimeSensitiveListeners = false
            listeners.fastEach {
                if (it.durationNanos > 0L) {
                    hasTimeSensitiveListeners = true
                    return
                }
            }
        }
    }

    fun remove(vararg binds: Bind) {
        for (bind in binds) {
            remove(bind)
        }
    }

    /**
     * Begin recording for a keybind. The resulting keybind, will be registered if [register] is true.
     *
     * recording may be cancelled for the following reasons:
     * - the ESC key is pressed
     * - [record] is called again before the recording is completed, (i.e. starting a new one before the old one is finished)
     * - the instance already has a keybind which matched the one being recorded
     * - the keybind was attempted to be set to just left-click (which is not allowed)
     *
     * @param holdDurationNanos the duration that the keys have to be pressed in the resultant keybind
     * @param function the function that will be run when the keybind is pressed. **The parameter will be `true` if the keybind is pressed, and `false` if it is released.**
     * @param callback a callback which is executed when the recording is finished. If the recording was successful,
     * the same bind as in the return of this method is supplied as the parameter. Else, for example if it was canceled, `null` is used.
     * @throws IllegalStateException if the keybind is already present
     * @since 0.24.0
     */
    fun record(register: Boolean, holdDurationNanos: Long = 0L, callback: ((Bind?) -> Unit)?, function: (Boolean) -> Boolean): Bind {
        // stupid complier
        val out = Bind(null as IntArray?, null as Array<Keys>?, null as IntArray?, Modifiers(0), durationNanos = holdDurationNanos, action = function)
        record(out, callback)
        if (register) add(out)
        return out
    }

    /**
     * Begin recording for a keybind. This will **overwrite** the keys in the given [bind] with the currently pressed keys when the recording is completed.
     * In the event that the recording is cancelled, the [bind] will not be modified.
     * @param callback a callback which is executed when the recording is finished. If the recording was successful,
     * the same bind as in the return of this method is supplied as the parameter. Else, for example if it was canceled, `null` is used.
     * @since 1.12.4
     */
    fun record(bind: Bind, callback: ((Bind?) -> Unit)?) {
        if (settings.debug) PolyUI.LOGGER.info("Recording keybind began")
        if (recordingBind != null) cancelRecord("New recording started")
        bind.resetState()
        release()
        recordingBind = bind
        recordingCallback = callback
    }

    @ApiStatus.Internal
    fun cancelRecord(reason: String) {
        if (recordingBind != null) PolyUI.LOGGER.warn("Keybind recording cancelled: $reason")
        recordingCallback?.invoke(null)
        recordingCallback = null
        recordingBind = null
        release()
    }

    private fun completeRecording(mods: Byte) {
        val bind = recordingBind ?: return
        bind.unmappedKeys = if (downUnmappedKeys.isEmpty()) null else downUnmappedKeys.toIntArray()
        bind.keys = if (downKeys.isEmpty()) null else downKeys.toTypedArray()
        bind.mouse = if (downMouseButtons.isEmpty()) null else downMouseButtons.toIntArray()
        bind.mods = Modifiers(mods)
        bind.resetState()
        if (settings.debug) PolyUI.LOGGER.info("Bind created: $bind")
        recordingCallback?.invoke(bind)
        recordingCallback = null
        recordingBind = null
        release()
    }

    /**
     * Synthetically drop all pressed keys, and will reset the state of all keybinds. (including generating "up" polls/aka `action(false)` for all binds that were pressed)
     */
    @ApiStatus.Internal
    fun release() {
        downKeys.clear()
        downMouseButtons.clear()
        downUnmappedKeys.clear()
        listeners.fastEach { it.resetState() }
    }

    open class Bind(unmappedKeys: IntArray? = null, keys: Array<Keys>? = null, mouse: IntArray? = null, @get:JvmName("getMods") mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, @Transient val action: (Boolean) -> Boolean) {
        constructor(chars: CharArray? = null, keys: Array<Keys>? = null, mouse: IntArray? = null, mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, action: (Boolean) -> Boolean) : this(
            chars?.map {
                it.lowercaseChar().code
            }?.toIntArray(),
            keys, mouse, mods, durationNanos, action,
        )

        constructor(char: Char, keys: Array<Keys>? = null, mouse: IntArray? = null, mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, action: (Boolean) -> Boolean) : this(intArrayOf(char.lowercaseChar().code), keys, mouse, mods, durationNanos, action)
        constructor(unmappedKeys: IntArray? = null, keys: Array<Keys>? = null, mouse: Array<Mouse>? = null, mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, action: (Boolean) -> Boolean) : this(
            unmappedKeys, keys,
            mouse?.map {
                it.value.toInt()
            }?.toIntArray(),
            mods, durationNanos, action,
        )

        constructor(unmappedKeys: IntArray? = null, keys: Array<Keys>? = null, mouse: Mouse? = null, mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, action: (Boolean) -> Boolean) : this(
            unmappedKeys, keys,
            mouse?.value?.let {
                intArrayOf(it.toInt())
            },
            mods, durationNanos, action,
        )

        constructor(unmappedKeys: IntArray? = null, key: Keys? = null, mouse: Array<Mouse>? = null, mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, action: (Boolean) -> Boolean) : this(
            unmappedKeys,
            key?.let {
                arrayOf(it)
            },
            mouse, mods, durationNanos, action,
        )

        constructor(mods: Modifiers = Modifiers(0), action: (Boolean) -> Boolean) : this(unmappedKeys = null, key = null, mouse = null, mods = mods, durationNanos = 0L, action = action)

        var unmappedKeys = unmappedKeys.nullIfEmpty()
            internal set
        var keys = keys?.nullIfEmpty()
            internal set
        var mouse = mouse?.nullIfEmpty()
            internal set

        @get:JvmName("getMods")
        var mods = mods
            internal set
        var durationNanos = durationNanos
            internal set

        @Transient
        private var time = 0L

        @Transient
        private var ran = false

        val size get() = (unmappedKeys?.size ?: 0) + (keys?.size ?: 0) + (mouse?.size ?: 0) + mods.size
        val isBound get() = size > 0

        internal fun update(c: IntArraySet, k: ArrayList<Keys>, m: IntArraySet, mods: Byte, deltaTimeNanos: Long, down: Boolean): Boolean {
            if (!test(c, k, m, mods, deltaTimeNanos, down)) {
                time = 0L
                if (ran) {
                    ran = false
                    return action(false)
                }
                return false
            }
            if (durationNanos != 0L) {
                time += deltaTimeNanos
                if (time >= durationNanos && !ran) {
                    ran = true
                    return action(true)
                }
            } else if (down && !ran) {
                ran = true
                return action(true)
            }
            return false
        }

        /**
         * Reset the state of this keybind, meaning that it will call [action] with `false` if it was previously ran (i.e. is actively being held down),
         * and reset its internal state so that it can be used again. To be used in conjunction with [KeyBinder.release].
         */
        internal fun resetState() {
            if (ran) action(false)
            time = 0L
            ran = false
        }

        protected open fun test(c: IntArraySet, k: ArrayList<Keys>, m: IntArraySet, mods: Byte, deltaTimeNanos: Long, down: Boolean): Boolean {
            if (durationNanos == 0L && deltaTimeNanos > 0L) return false
            if (!unmappedKeys.matches(c)) return false
            if (!keys.matches(k)) return false
            if (!mouse.matches(m)) return false
            return this.mods.containedBy(Modifiers(mods))
        }

        final override fun toString(): String {
            val sb = StringBuilder()
            sb.append("KeyBind(")
            sb.append(keysToString())
            sb.append(')')
            return sb.toString()
        }

        fun keysToString(ifNotBound: String = ""): String {
            if (!isBound) return ifNotBound
            val sb = StringBuilder()
            val s = mods.prettyName
            sb.append(s)
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
            return sb.substring(sb.length - 3)
        }

        final override fun equals(other: Any?): Boolean {
            if (other !is Bind) return false
            if (other.unmappedKeys?.contentEquals(unmappedKeys) == false) return false
            if (other.keys?.contentEquals(keys) == false) return false
            if (other.mouse?.contentEquals(mouse) == false) return false
            if (other.mods != mods) return false
            if (other.durationNanos != durationNanos) return false
            return true
        }

        final override fun hashCode(): Int {
            var result = unmappedKeys?.contentHashCode() ?: 0
            result = 31 * result + (keys?.contentHashCode() ?: 0)
            result = 31 * result + (mouse?.contentHashCode() ?: 0)
            result = 31 * result + mods.hashCode()
            result = 31 * result + durationNanos.hashCode()
            return result
        }

        protected fun <T> Array<T>?.matches(other: ArrayList<T>): Boolean {
            if (this == null) return true
            for (i in this) {
                if (i !in other) return false
            }
            return true
        }

        protected fun IntArray?.matches(other: IntArraySet): Boolean {
            if (this == null) return true
            for (i in this) {
                if (i !in other) return false
            }
            return true
        }
    }
}
