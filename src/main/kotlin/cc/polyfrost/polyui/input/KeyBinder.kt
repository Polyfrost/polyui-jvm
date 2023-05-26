/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.input

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.event.Event
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.event.FocusedEvents
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.merge
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
    @JvmName("Remove")
    fun remove(keybind: () -> Unit) {
        val k = { keybind(); true }
        listeners.values.removeIf { it == k }
    }

    /**
     * Remove all keybindings for the given key and modifiers.
     */
    fun removeAll(key: Char, vararg modifiers: KeyModifiers) {
        listeners.remove(FocusedEvents.KeyTyped(key, modifiers.merge(), false))
    }

    /**
     * Remove all keybindings for the given key and modifiers.
     */
    fun removeAll(key: Keys, vararg modifiers: KeyModifiers) {
        listeners.remove(FocusedEvents.KeyPressed(key, modifiers.merge(), false))
    }

    private fun add0(listeners: ArrayList<() -> Boolean>?, key: Char, mods: Short, keybind: () -> Boolean) {
        if (listeners != null) {
            PolyUI.LOGGER.warn("found {} other keybindings for key {}", listeners.size, Keys.toStringPretty(key, mods))
            listeners.add {
                keybind()
            }
        } else {
            this.listeners[FocusedEvents.KeyTyped(key, mods)] = arrayListOf({
                keybind()
            })
        }
    }

    private fun add0(listeners: ArrayList<() -> Boolean>?, key: Keys, mods: Short, keybind: () -> Boolean) {
        if (listeners != null) {
            PolyUI.LOGGER.warn("found {} other keybindings for key {}", listeners.size, Keys.toStringPretty(key, mods))
            listeners.add {
                keybind()
            }
        } else {
            this.listeners[FocusedEvents.KeyPressed(key, mods)] = arrayListOf({
                keybind()
            })
        }
    }

    private fun add0(listeners: ArrayList<() -> Boolean>?, button: Mouse, mods: Short, keybind: () -> Boolean) {
        if (listeners != null) {
            PolyUI.LOGGER.warn("found {} other keybindings for key {}", listeners.size, Mouse.toStringPretty(button, mods))
            listeners.add {
                keybind()
            }
        } else {
            // todo maybe not just 1 click? idk;
            this.listeners[Events.MouseClicked(button.value.toInt(), 1, mods)] = arrayListOf({
                keybind()
            })
        }
    }

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    fun add(key: Char, vararg modifiers: KeyModifiers, keybind: () -> Boolean) {
        val mods = modifiers.merge()
        add0(listeners[FocusedEvents.KeyTyped(key, mods, false)], key, mods, keybind)
    }

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    @JvmName("AddListener") // The following declarations have the same JVM signature add(C[Lcc/polyfrost/polyui/input/Modifiers;Lkotlin/jvm/functions/Function0;)V
    fun add(key: Char, vararg modifiers: KeyModifiers, keybind: () -> Unit) {
        val mods = modifiers.merge()
        add0(listeners[FocusedEvents.KeyTyped(key, mods, false)], key, mods) { keybind(); true }
    }

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    @JvmName("AddListener")
    fun add(key: Keys, vararg modifiers: KeyModifiers, keybind: () -> Unit) {
        val mods = modifiers.merge()
        add0(listeners[FocusedEvents.KeyPressed(key, mods, false)], key, mods) { keybind(); true }
    }

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    fun add(key: Keys, vararg modifiers: KeyModifiers, keybind: () -> Boolean) {
        val mods = modifiers.merge()
        add0(listeners[FocusedEvents.KeyPressed(key, mods, false)], key, mods, keybind)
    }

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    @JvmName("AddListener")
    fun add(button: Mouse, vararg modifiers: KeyModifiers, keybind: () -> Unit) {
        val mods = modifiers.merge()
        add0(listeners[Events.MouseClicked(button.value.toInt(), 1, mods)], button, mods) { keybind(); true }
    }

    /**
     * Add a keybinding for the given key and modifiers.
     * Return true to prevent other keybindings from being called.
     */
    @OverloadResolutionByLambdaReturnType
    fun add(button: Mouse, vararg modifiers: KeyModifiers, keybind: () -> Boolean) {
        val mods = modifiers.merge()
        add0(listeners[Events.MouseClicked(button.value.toInt(), 1, mods)], button, mods, keybind)
    }
}
