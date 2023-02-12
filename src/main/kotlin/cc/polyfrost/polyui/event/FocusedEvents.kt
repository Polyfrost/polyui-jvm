/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.event

import cc.polyfrost.polyui.input.Keys

open class FocusedEvents : Event {
    /**
     * called when a key is typed (and modifiers) is pressed.
     *
     * @see [Keys]
     * @see [Keys.Modifiers]
     * @see [Keys.Modifiers.Companion.fromModifierMerged]
     */
    data class KeyTyped(val key: Char, val mods: Short = 0, val isRepeat: Boolean = false) : FocusedEvents() {
        override fun toString(): String {
            return "KeyTyped(${Keys.toString(key, mods)})"
        }

        fun toStringPretty(): String {
            return "KeyTyped(${Keys.toStringPretty(key, mods)})"
        }

        fun getModifiers(): MutableList<Keys.Modifiers> {
            return Keys.Modifiers.fromModifierMerged(mods)
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + mods
            return result
        }
    }

    /**
     * called when a non-printable key (and modifiers) is pressed.
     *
     * @see [Keys]
     * @see [Keys.Modifiers]
     * @see [Keys.Modifiers.Companion.fromModifierMerged]
     */
    data class KeyPressed(val key: Keys, val mods: Short = 0, val isRepeat: Boolean = false) : FocusedEvents() {
        override fun toString(): String {
            return "KeyPressed(${Keys.toString(key, mods)})"
        }

        fun toStringPretty(): String {
            return "KeyPressed(${Keys.toStringPretty(key, mods)})"
        }

        fun getModifiers(): MutableList<Keys.Modifiers> {
            return Keys.Modifiers.fromModifierMerged(mods)
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + mods
            return result
        }
    }
}
