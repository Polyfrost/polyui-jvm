/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.input

import kotlin.experimental.and
import kotlin.experimental.or

/**
 * PolyUI's mapping for unprintable key codes.
 *
 * - Unknown: -1
 * - F1-F12: 1-12
 * - Escape: 100
 * - Enter -> End: 101-109
 * - Arrow Keys (Right, Left, Down, Up): 200-203
 *
 * @see Modifiers
 * */
enum class Keys(val keyName: String, val value: Short) {
    UNKNOWN("Unknown", -1),
    F1("F1", 1),
    F2("F2", 2),
    F3("F3", 3),
    F4("F4", 4),
    F5("F5", 5),
    F6("F6", 6),
    F7("F7", 7),
    F8("F8", 8),
    F9("F9", 9),
    F10("F10", 10),
    F11("F11", 11),
    F12("F12", 12),

    ESCAPE("Escape", 100),

    ENTER("Enter", 101),
    TAB("Tab", 102),
    BACKSPACE("Backspace", 103),
    INSERT("Insert", 104),
    DELETE("Delete", 105),
    PAGE_UP("Page Up", 106),
    PAGE_DOWN("Page Down", 107),
    HOME("Home", 108),
    END("End", 109),

    RIGHT("Right", 200),
    LEFT("Left", 201),
    DOWN("Down", 202),
    UP("Up", 203);

    companion object {

        /** get the key from the given value. */
        @JvmStatic
        fun fromValue(value: Int): Keys {
            val v = value.toShort()
            for (key in values()) {
                if (key.value == v) {
                    return key
                }
            }
            return UNKNOWN
        }

        /**
         * return a string representation of this key combo.
         *
         * For example, [LSHIFT][Modifiers.LSHIFT] + [LCONTROL][Modifiers.LCONTROL] + [INSERT][Keys.INSERT] would return `"LSHIFT+LCONTROL+INSERT"`
         * */
        @JvmStatic
        fun toString(key: Keys, modifiers: Short = 0): String {
            return if (modifiers == 0.toShort()) {
                key.name
            } else {
                "${Modifiers.toString(modifiers)}+${key.name}"
            }
        }

        /**
         * return a string representation of this key combo.
         *
         * For example, [LSHIFT][Modifiers.LSHIFT] + [LCONTROL][Modifiers.LCONTROL] + `a` would return `"LSHIFT+LCONTROL+a"`
         */
        @JvmStatic
        fun toString(key: Char, modifiers: Short = 0): String {
            return if (modifiers == 0.toShort()) {
                key.toString()
            } else {
                "${Modifiers.toString(modifiers)}+$key"
            }
        }

        /**
         * return a pretty string representation of this key combo.
         *
         * For example, [LSHIFT][Modifiers.LSHIFT] + [LCONTROL][Modifiers.LCONTROL] + [INSERT][Keys.INSERT] would return `"Left Shift + Left Control + Insert"`
         */
        @JvmStatic
        fun toStringPretty(key: Keys, modifiers: Short = 0): String {
            return if (modifiers == 0.toShort()) {
                key.keyName
            } else {
                "${Modifiers.toStringPretty(modifiers)} + ${key.keyName}"
            }
        }

        /**
         * return a pretty string representation of this key combo.
         *
         * For example, [LSHIFT][Modifiers.LSHIFT] + [LCONTROL][Modifiers.LCONTROL] + `a` would return `"Left Shift + Left Control + a"`
         */
        @JvmStatic
        fun toStringPretty(key: Char, modifiers: Short = 0): String {
            return if (modifiers == 0.toShort()) {
                key.toString()
            } else {
                "${Modifiers.toStringPretty(modifiers)} + $key"
            }
        }
    }
}

enum class Mouse(val keyName: String, val value: Short) {
    UNKNOWN("Mouse Button ?", -1),
    LEFT_MOUSE("Left Click", 0),
    RIGHT_MOUSE("Right Click", 1),
    MIDDLE_MOUSE("Middle Click", 2),
    MOUSE_3("Mouse Button 3", 3),
    MOUSE_4("Mouse Button 4", 4),
    MOUSE_5("Mouse Button 5", 5);

    companion object {
        @JvmStatic
        fun fromValue(value: Int): Mouse {
            val v = value.toShort()
            for (btn in Mouse.values()) {
                if (btn.value == v) {
                    return btn
                }
            }
            return UNKNOWN
        }

        @JvmStatic
        fun toString(button: Mouse, modifiers: Short = 0): String {
            return if (modifiers == 0.toShort()) {
                button.name
            } else {
                "${Modifiers.toString(modifiers)}+${button.name}"
            }
        }

        @JvmStatic
        fun toStringPretty(button: Mouse, modifiers: Short = 0): String {
            return if (modifiers == 0.toShort()) {
                button.keyName
            } else {
                "${Modifiers.toStringPretty(modifiers)} + ${button.keyName}"
            }
        }
    }
}

/**
 * PolyUI's mapping for modifier keys, in binary form so logical OR can be used to check for multiple modifiers.
 *
 * @see fromModifierMerged
 * @see merge
 */
enum class Modifiers(val keyName: String, val value: Short) {
    LSHIFT("Left Shift", 0b00000001),
    RSHIFT("Right Shift", 0b00000010),

    LCONTROL("Left Control", 0b00000100),
    RCONTROL("Right Control", 0b00001000),

    LALT("Left Alt", 0b00010000),
    RALT("Right Alt", 0b00100000),

    LMETA("Left Meta", 0b01000000),
    RMETA("Right Meta", 0b10000000),

    /** you will never this value. */
    UNKNOWN("Unknown", 0);

    companion object {
        /**
         * take the given short-merged modifiers and return a list of the modifiers.
         * @see merge
         */
        @JvmStatic
        fun fromModifierMerged(modifiers: Short): Array<Modifiers> {
            if (modifiers == 0.toShort()) return emptyArray()
            val mods = mutableListOf<Modifiers>()
            for (mod in values()) {
                if (mod.value and modifiers != 0.toShort()) {
                    mods.add(mod)
                }
            }
            return mods.toTypedArray()
        }

        /**
         * merge the given modifiers into a single short.
         * @see fromModifierMerged
         */
        @JvmStatic
        fun merge(vararg modifiers: Modifiers): Short {
            var merged = 0.toShort()
            for (mod in modifiers) {
                merged = merged or mod.value
            }
            return merged
        }

        @JvmStatic
        inline fun toString(modifiers: Short) = toString(*fromModifierMerged(modifiers))

        @JvmStatic
        inline fun toStringPretty(modifiers: Short) = toStringPretty(*fromModifierMerged(modifiers))

        @JvmStatic
        fun toString(vararg modifiers: Modifiers) = modifiers.joinToString("+") { it.name }

        @JvmStatic
        fun toStringPretty(vararg modifiers: Modifiers) = modifiers.joinToString(" + ") { it.keyName }
    }
}

typealias KeyModifiers = Modifiers
typealias MouseButtons = Mouse
