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

package cc.polyfrost.polyui.input

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.event.Event
import cc.polyfrost.polyui.event.FocusedEvent
import cc.polyfrost.polyui.event.MouseClicked
import cc.polyfrost.polyui.utils.fastEach
import org.jetbrains.annotations.ApiStatus

/**
 * # KeyBinder
 *
 * KeyBinder handles key bindings for the PolyUI instance.
 *
 * @see add
 * @see remove
 * @see removeAll
 */
class KeyBinder {
    private val listeners: HashMap<Event, ArrayList<() -> Boolean>> = hashMapOf()

    /**
     * accept a keystroke event. This will call all keybindings that match the event.
     *
     * This method is public, but marked as internal. This is because it should only be called by the PolyUI instance, unless you are trying to externally force a keypress (which you probably shouldn't be)
     */
    @ApiStatus.Internal
    fun accept(event: Event): Boolean {
        listeners[event]?.fastEach {
            if (it()) return true
        }
        return false
    }

    /**
     * Remove the given keybind from the listeners.
     */
    @OverloadResolutionByLambdaReturnType
    fun remove(keybind: () -> Boolean) {
        listeners.values.removeIf { it == keybind }
    }

    /**
     * Remove the given keybind from the listeners.
     */
    @OverloadResolutionByLambdaReturnType
    @JvmName("removeListener")
    fun remove(keybind: () -> Unit) {
        val k = { keybind(); true }
        listeners.values.removeIf { it == k }
    }

    /**
     * Remove all keybindings for the given key and modifiers.
     */
    fun removeAll(key: Char, modifiers: Short) {
        listeners.remove(FocusedEvent.KeyTyped(key, modifiers, false))
    }

    /**
     * Remove all keybindings for the given key and modifiers.
     */
    fun removeAll(key: Keys, modifiers: Short) {
        listeners.remove(FocusedEvent.KeyPressed(key, modifiers, false))
    }

    /**
     * Remove all keybindings for the given key and modifiers.
     */
    fun removeAll(mouse: Mouse, amountClicks: Int, modifiers: Short) {
        listeners.remove(MouseClicked(mouse.value.toInt(), amountClicks, modifiers))
    }

    private fun add0(listeners: ArrayList<() -> Boolean>?, key: Char, durationNanos: Long, mods: Short, keybind: () -> Boolean) {
        if (durationNanos == 0L) {
            if (listeners != null) {
                PolyUI.LOGGER.warn("found {} other keybindings for key {}", listeners.size, Keys.toStringPretty(key, mods))
                listeners.add(keybind)
            } else {
                this.listeners[FocusedEvent.KeyTyped(key, mods)] = arrayListOf(keybind)
            }
        }
    }

    private fun add0(listeners: ArrayList<() -> Boolean>?, key: Keys, durationNanos: Long, mods: Short, keybind: () -> Boolean) {
        if (durationNanos == 0L) {
            if (listeners != null) {
                PolyUI.LOGGER.warn("found {} other keybindings for key {}", listeners.size, Keys.toStringPretty(key, mods))
                listeners.add(keybind)
            } else {
                this.listeners[FocusedEvent.KeyPressed(key, mods)] = arrayListOf(keybind)
            }
        } else {
        }
    }

    private fun add0(listeners: ArrayList<() -> Boolean>?, button: Mouse, durationNanos: Long, amountClicks: Int, mods: Short, keybind: () -> Boolean) {
        if (durationNanos == 0L) {
            if (listeners != null) {
                PolyUI.LOGGER.warn("found {} other keybindings for key {}", listeners.size, Mouse.toStringPretty(button, mods))
                listeners.add(keybind)
            } else {
                this.listeners[MouseClicked(button.value.toInt(), amountClicks, mods)] = arrayListOf(keybind)
            }
        }
    }

    // char //
    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    fun add(key: Char, durationNanos: Long = 0L, mods: Short = 0, keybind: () -> Boolean) {
        add0(listeners[FocusedEvent.KeyTyped(key.uppercaseChar(), mods, false)], key, durationNanos, mods, keybind)
    }

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    @JvmName("addListener")
    fun add(key: Char, durationNanos: Long = 0L, mods: Short = 0, keybind: () -> Unit) {
        add0(listeners[FocusedEvent.KeyTyped(key.uppercaseChar(), mods, false)], key, durationNanos, mods) { keybind(); true }
    }

    // keys //
    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    fun add(key: Keys, durationNanos: Long = 0L, mods: Short = 0, keybind: () -> Boolean) =
        add0(listeners[FocusedEvent.KeyPressed(key, mods, false)], key, durationNanos, mods, keybind)

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    @JvmName("addListener")
    fun add(key: Keys, durationNanos: Long = 0L, mods: Short = 0, keybind: () -> Unit) =
        add0(listeners[FocusedEvent.KeyPressed(key, mods, false)], key, durationNanos, mods) { keybind(); true }

    // mouse //
    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    @JvmName("addListener")
    fun add(button: Mouse, durationNanos: Long = 0L, amountClicks: Int = 1, mods: Short = 0, keybind: () -> Unit) =
        add0(listeners[MouseClicked(button.value.toInt(), amountClicks, mods)], button, durationNanos, amountClicks, mods) { keybind(); true }

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    fun add(button: Mouse, durationNanos: Long = 0L, amountClicks: Int = 1, mods: Short = 0, keybind: () -> Boolean) =
        add0(listeners[MouseClicked(button.value.toInt(), amountClicks, mods)], button, durationNanos, amountClicks, mods, keybind)
}
