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

import org.polyfrost.polyui.PolyUI

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

    ESCAPE("Escape", 20),

    ENTER("Enter", 21),
    TAB("Tab", 22),
    BACKSPACE("Backspace", 23),
    INSERT("Insert", 24),
    DELETE("Delete", 25),
    PAGE_UP("Page Up", 26),
    PAGE_DOWN("Page Down", 27),
    HOME("Home", 28),
    END("End", 29),

    RIGHT("Right", 30),
    LEFT("Left", 31),
    DOWN("Down", 32),
    UP("Up", 33),
    ;

    companion object {
        /** get the key from the given value. */
        @JvmStatic
        fun fromValue(value: Int): Keys {
            return when (value) {
                1 -> F1
                2 -> F2
                3 -> F3
                4 -> F4
                5 -> F5
                6 -> F6
                7 -> F7
                8 -> F8
                9 -> F9
                10 -> F10
                11 -> F11
                12 -> F12

                20 -> ESCAPE

                21 -> ENTER
                22 -> TAB
                23 -> BACKSPACE
                24 -> INSERT
                25 -> DELETE
                26 -> PAGE_UP
                27 -> PAGE_DOWN
                28 -> HOME
                29 -> END

                30 -> RIGHT
                31 -> LEFT
                32 -> DOWN
                33 -> UP

                else -> UNKNOWN
            }
        }

        /**
         * return a string representation of this key combo.
         *
         * For example, [LSHIFT][KeyModifiers.LSHIFT] + [LCONTROL][KeyModifiers.LCONTROL] + [INSERT][Keys.INSERT] would return `"LSHIFT+LCONTROL+INSERT"`
         * */
        @JvmStatic
        @JvmName("toString")
        fun toString(key: Keys, modifiers: Modifiers = Modifiers(0)): String {
            return if (modifiers.isEmpty) {
                key.name
            } else {
                "${modifiers.name}+${key.name}"
            }
        }

        /**
         * return a string representation of this key combo.
         *
         * For example, [LSHIFT][KeyModifiers.LSHIFT] + [LCONTROL][KeyModifiers.LCONTROL] + `a` would return `"LSHIFT+LCONTROL+a"`
         */
        @JvmStatic
        @JvmName("toString")
        fun toString(key: Char, modifiers: Modifiers = Modifiers(0)): String {
            return if (modifiers.isEmpty) {
                key.toString()
            } else {
                "${modifiers.name}+$key"
            }
        }

        /**
         * return a pretty string representation of this key combo.
         *
         * For example, [LSHIFT][KeyModifiers.LSHIFT] + [LCONTROL][KeyModifiers.LCONTROL] + [INSERT][Keys.INSERT] would return `"Left Shift + Left Control + Insert"`
         */
        @JvmStatic
        @JvmName("toStringPretty")
        fun toStringPretty(key: Keys, modifiers: Modifiers = Modifiers(0)): String {
            return if (modifiers.isEmpty) {
                key.keyName
            } else {
                "${modifiers.prettyName} + ${key.keyName}"
            }
        }

        /**
         * return a pretty string representation of this key combo.
         *
         * For example, [LSHIFT][KeyModifiers.LSHIFT] + [LCONTROL][KeyModifiers.LCONTROL] + `a` would return `"Left Shift + Left Control + a"`
         */
        @JvmStatic
        @JvmName("toStringPretty")
        fun toStringPretty(key: Char, modifiers: Modifiers = Modifiers(0)): String {
            return if (modifiers.isEmpty) {
                key.toString()
            } else {
                "${modifiers.prettyName} + $key"
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
    MOUSE_5("Mouse Button 5", 5),
    ;

    companion object {
        @JvmStatic
        fun fromValue(value: Int): Mouse {
            return when (value) {
                0 -> LEFT_MOUSE
                1 -> RIGHT_MOUSE
                2 -> MIDDLE_MOUSE
                3 -> MOUSE_3
                4 -> MOUSE_4
                5 -> MOUSE_5
                else -> UNKNOWN
            }
        }

        @JvmStatic
        @JvmName("toString")
        fun toString(button: Mouse, modifiers: Modifiers = Modifiers(0)): String {
            return if (modifiers.isEmpty) {
                button.name
            } else {
                "${modifiers.name}+${button.name}"
            }
        }

        @JvmStatic
        @JvmName("toStringPretty")
        fun toStringPretty(button: Mouse, modifiers: Modifiers = Modifiers(0)): String {
            return if (modifiers.isEmpty) {
                button.keyName
            } else {
                "${modifiers.prettyName} + ${button.keyName}"
            }
        }
    }
}

/**
 * PolyUI's mapping for modifier keys, in binary form so logical OR can be used to check for multiple modifiers.
 * @see Modifiers
 */
enum class KeyModifiers(val value: Byte) {
    LSHIFT(0b00000001),
    RSHIFT(0b00000010),
    SHIFT(0b00000011),

    LCONTROL(0b00000100),
    RCONTROL(0b00001000),
    CONTROL(0b00001100),

    LALT(0b00010000),
    RALT(0b00100000),
    ALT(0b00110000),

    LMETA(0b01000000),
    RMETA(-0b010000000),
    META(-0b01000000),

    /** you will never receive this value. */
    UNKNOWN(0),
}

/**
 * PolyUI's mapping for modifier keys, in binary form so logical OR can be used to check for multiple modifiers.
 * @see KeyModifiers
 */
@JvmInline
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
value class Modifiers(val value: Byte) {
    @kotlin.internal.InlineOnly
    inline val lShift get() = value.toInt() and 0b00000001 != 0

    @kotlin.internal.InlineOnly
    inline val rShift get() = value.toInt() and 0b00000010 != 0

    @kotlin.internal.InlineOnly
    inline val lCtrl get() = value.toInt() and 0b00000100 != 0

    @kotlin.internal.InlineOnly
    inline val rCtrl get() = value.toInt() and 0b00001000 != 0

    @kotlin.internal.InlineOnly
    inline val lAlt get() = value.toInt() and 0b00010000 != 0

    @kotlin.internal.InlineOnly
    inline val rAlt get() = value.toInt() and 0b00100000 != 0

    @kotlin.internal.InlineOnly
    inline val lMeta get() = value.toInt() and 0b01000000 != 0

    @kotlin.internal.InlineOnly
    inline val rMeta get() = value.toInt() and 0b10000000 != 0

    @kotlin.internal.InlineOnly
    inline val isEmpty get() = value == 0.toByte()

    @kotlin.internal.InlineOnly
    inline val hasShift get() = value.toInt() and 0b00000011 != 0

    @kotlin.internal.InlineOnly
    inline val hasControl get() = value.toInt() and 0b00001100 != 0 || (PolyUI.isOnMac && value.toInt() and 0b11000000 != 0)

    @kotlin.internal.InlineOnly
    inline val hasAlt get() = value.toInt() and 0b00110000 != 0

    @kotlin.internal.InlineOnly
    inline val hasMeta get() = value.toInt() and 0b11000000 != 0

    val prettyName: String
        get() {
            if (isEmpty) return ""
            val sb = StringBuilder()
            if (hasShift) sb.append("Shift + ")
            else {
                if (lShift) sb.append("Left Shift + ")
                if (rShift) sb.append("Right Shift + ")
            }
            if (value.toInt() and 0b00001100 != 0) sb.append("Control + ")
            else {
                if (lCtrl) sb.append("Left Control + ")
                if (rCtrl) sb.append("Right Control + ")
            }
            if (hasAlt) sb.append("Alt + ")
            else {
                if (lAlt) sb.append("Left Alt + ")
                if (rAlt) sb.append("Right Alt + ")
            }
            if (hasMeta) sb.append("Meta + ")
            else {
                if (lMeta) sb.append("Left Meta + ")
                if (rMeta) sb.append("Right Meta + ")
            }
            return sb.substring(0, sb.length - 3)
        }

    val name: String
        get() {
            if (isEmpty) return ""
            val sb = StringBuilder()
            if (hasShift) sb.append("SHIFT+")
            else {
                if (lShift) sb.append("LSHIFT+")
                if (rShift) sb.append("RSHIFT+")
            }
            if (value.toInt() and 0b00001100 != 0) sb.append("CTRL+")
            else {
                if (lCtrl) sb.append("LCTRL+")
                if (rCtrl) sb.append("RCTRL+")
            }
            if (hasAlt) sb.append("ALT+")
            else {
                if (lAlt) sb.append("LALT+")
                if (rAlt) sb.append("RALT+")
            }
            if (hasMeta) sb.append("META+")
            else {
                if (lMeta) sb.append("LMETA+")
                if (rMeta) sb.append("RMETA+")
            }
            return sb.substring(0, sb.length - 1)
        }

    @kotlin.internal.InlineOnly
    inline fun equal(other: Modifiers) = this.equal(other.value)

    @kotlin.internal.InlineOnly
    inline fun equalLenient(other: Byte) = this.equalLenient(Modifiers(other))

    @kotlin.internal.InlineOnly
    inline fun equalLenient(other: Modifiers) = this.hasAlt == other.hasAlt && this.hasControl == other.hasControl && this.hasShift == other.hasShift

    fun equal(other: Byte): Boolean {
        if (PolyUI.isOnMac && this.hasControl) {
            val i = other.toInt()
            return value.toInt() == i and 0b00111111 or (i shr 4)
        }
        return other == value
    }

    override fun toString() = "$name ($value)"
}
