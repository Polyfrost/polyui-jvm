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
    private val listeners = ArrayList<PolyBind>()

    /**
     * Property which enables an optimization where the KeyBinder will only update after every frame if there are time-sensitive listeners.
     * @since 1.1.1
     */
    var hasTimeSensitiveListeners = false
        private set
    private val downMouseButtons = IntArraySet(5)
    private val downUnmappedKeys = IntArraySet(5)
    private val downKeys = ArrayList<Keys>(5)

    private var recordingBind: PolyBind? = null
    private var recordingCallback: ((PolyBind?) -> Unit)? = null

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

    @ApiStatus.Internal
    fun modifierRemoved(oldMods: Byte) {
        if (recordingBind != null) completeRecording(oldMods)
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
    fun getDuplicatesIfAny(bind: PolyBind) = listeners.filter { it == bind }

    /**
     * Add a keybind to this PolyUI instance, that will be run when the given keys are pressed.
     * @since 1.1.7
     */
    fun add(vararg binds: PolyBind) {
        for (bind in binds) {
            add(bind)
        }
    }

    /**
     * Add a keybind to this PolyUI instance, that will be run when the given keys are pressed.
     * @since 0.21.0
     */
    fun add(bind: PolyBind) {
        if (bind.durationNanos > 0L) hasTimeSensitiveListeners = true
        listeners.add(bind)
    }

    fun remove(bind: PolyBind) {
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

    fun remove(vararg binds: PolyBind) {
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
    fun record(register: Boolean, holdDurationNanos: Long = 0L, callback: ((PolyBind?) -> Unit)?, function: (Boolean) -> Boolean): PolyBind {
        // stupid complier
        val out = PolyBind(null as IntArray?, null as Array<Keys>?, null as IntArray?, Modifiers(0), durationNanos = holdDurationNanos, action = function)
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
    fun record(bind: PolyBind, callback: ((PolyBind?) -> Unit)? = null) {
        if (settings.debug) PolyUI.LOGGER.info("Recording keybind began")
        if (recordingBind != null) cancelRecord("New recording started")
        bind.resetState()
        bind.muted = true
        release()
        recordingBind = bind
        recordingCallback = callback
    }

    @ApiStatus.Internal
    fun cancelRecord(reason: String) {
        if (recordingBind == null) return
        PolyUI.LOGGER.warn("Keybind recording cancelled: $reason")
        recordingCallback?.invoke(null)
        recordingCallback = null
        recordingBind?.muted = false
        recordingBind = null
        release()
    }

    private fun completeRecording(mods: Byte) {
        val bind = recordingBind ?: return
        bind.unmappedKeys = if (downUnmappedKeys.isEmpty()) null else downUnmappedKeys.toIntArray()
        bind.keys = if (downKeys.isEmpty()) null else downKeys.toTypedArray()
        bind.mouse = if (downMouseButtons.isEmpty()) null else downMouseButtons.toIntArray()
        bind.mods = Modifiers(mods, lenient = !(bind.unmappedKeys == null && bind.keys == null && bind.mouse == null))
        bind.resetState()
        bind.muted = false
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
}
