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
@Suppress("DEPRECATION")
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

    C("C", 'C'.toShort()),
    V("V", 'V'.toShort()),
    X("X", 'X'.toShort()),
    Z("Z", 'Z'.toShort()),
    A("A", 'A'.toShort()),
    S("S", 'S'.toShort()),
    P("P", 'P'.toShort()),
    I("I", 'I'.toShort()),
    R("R", 'R'.toShort()),
    MINUS("-", '-'.toShort()),
    EQUALS("=", '='.toShort()),

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
        fun fromValue(value: Int) = when (value) {
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

        /**
         * return a string representation of this key combo.
         *
         * For example, [LSHIFT][KeyModifiers.LSHIFT] + [LCONTROL][KeyModifiers.LPRIMARY] + [INSERT][Keys.INSERT] would return `"LSHIFT+LCONTROL+INSERT"`
         * */
        @JvmStatic
        @JvmName("toString")
        fun toString(key: Keys, modifiers: Modifiers = Modifiers(0)) = if (modifiers.isEmpty) {
            key.name
        } else {
            "${modifiers.name}+${key.name}"
        }

        /**
         * return a string representation of this key combo.
         *
         * For example, [LSHIFT][KeyModifiers.LSHIFT] + [LCONTROL][KeyModifiers.LPRIMARY] + `a` would return `"LSHIFT+LCONTROL+a"`
         */
        @JvmStatic
        @JvmName("toString")
        fun toString(key: Char, modifiers: Modifiers = Modifiers(0)) = if (modifiers.isEmpty) {
            key.toString()
        } else {
            "${modifiers.name}+$key"
        }

        /**
         * return a pretty string representation of this key combo.
         *
         * For example, [LSHIFT][KeyModifiers.LSHIFT] + [LCONTROL][KeyModifiers.LPRIMARY] + [INSERT][Keys.INSERT] would return `"Left Shift + Left Control + Insert"`
         */
        @JvmStatic
        @JvmName("toStringPretty")
        fun toStringPretty(key: Keys, modifiers: Modifiers = Modifiers(0)) = if (modifiers.isEmpty) {
            key.keyName
        } else {
            "${modifiers.fullName} + ${key.keyName}"
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
        fun fromValue(value: Int) = when (value) {
            0 -> LEFT_MOUSE
            1 -> RIGHT_MOUSE
            2 -> MIDDLE_MOUSE
            3 -> MOUSE_3
            4 -> MOUSE_4
            5 -> MOUSE_5
            else -> UNKNOWN
        }

        @JvmStatic
        @JvmName("toString")
        fun toString(button: Mouse, modifiers: Modifiers = Modifiers(0)) = if (modifiers.isEmpty) {
            button.name
        } else {
            "${modifiers.name}+${button.name}"
        }

        @JvmStatic
        @JvmName("toStringPretty")
        fun toStringPretty(button: Mouse, modifiers: Modifiers = Modifiers(0)) = if (modifiers.isEmpty) {
            button.keyName
        } else {
            "${modifiers.fullName} + ${button.keyName}"
        }
    }
}

/**
 * PolyUI's mapping for modifier keys, in binary form so logical OR can be used to check for multiple modifiers.
 * @see Modifiers
 */
enum class KeyModifiers(val value: Byte, val keyName: String, val fullName: String) {
    LSHIFT(0b00000001, shift(), "Left Shift"),
    RSHIFT(0b00000010, shift(), "Right Shift"),
    SHIFT(0b00000011, shift(), "Shift"),

    LPRIMARY(0b00000100, primary(), "Left " + primaryFull()),
    RPRIMARY(0b00001000, primary(), "Right " + primaryFull()),
    PRIMARY(0b00001100, primary(), primaryFull()),

    LSECONDARY(0b00010000, secondary(), "Left " + secondary()),
    RSECONDARY(0b00100000, secondary(), "Right " + secondary()),
    SECONDARY(0b00110000, secondary(), secondaryFull()),

    LMETA(0b01000000, meta(), "Left " + metaFull()),
    RMETA(-0b010000000, meta(), "Right " + metaFull()),
    META(-0b01000000, meta(), metaFull()), ;
}

private fun shift(): String = if (PolyUI.isOnMac) "⇧" else "Shift"
private fun primary(): String = if (PolyUI.isOnMac) "⌘" else "Ctrl"
private fun primaryFull(): String = if (PolyUI.isOnMac) "⌘Cmd" else "Control"
private fun secondary(): String = if (PolyUI.isOnMac) "⌥" else "Alt"
private fun secondaryFull(): String = if (PolyUI.isOnMac) "⌥Opt" else "Alt"
private fun meta() = if (PolyUI.isOnMac) "^" else if (PolyUI.isOnWindows) "\u229E" else "Meta"
private fun metaFull() = if (PolyUI.isOnMac) "^Ctrl" else if (PolyUI.isOnWindows) "\u229EWin" else "Meta"

/**
 * PolyUI's mapping for modifier keys, in binary form so logical OR can be used to check for multiple modifiers.
 * @see KeyModifiers
 */
@JvmInline
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
value class Modifiers(val value: Byte) {
    constructor(vararg modifiers: KeyModifiers) : this(modifiers.fold(0.toByte()) { acc, key -> acc or key.value })
    constructor(value: Byte, lenient: Boolean) : this(if (lenient) {
        // asm: when lenient, LSHIFT and RSHIFT become SHIFT, LPRIMARY and RPRIMARY become PRIMARY, LSECONDARY and RSECONDARY become SECONDARY, LMETA and RMETA become META
        var v = 0.toByte()
        if (value.toInt() and 0b00000011 != 0) v = v or 0b00000011
        if (value.toInt() and 0b00001100 != 0) v = v or 0b00001100
        if (value.toInt() and 0b00110000 != 0) v = v or 0b00110000
        if (value.toInt() and 0b11000000 != 0) v = v or 0b11000000.toByte()
        v
    } else value)

    @kotlin.internal.InlineOnly
    inline val lShift get() = value.toInt() and 0b00000001 != 0

    @kotlin.internal.InlineOnly
    inline val rShift get() = value.toInt() and 0b00000010 != 0

    @kotlin.internal.InlineOnly
    inline val lPrimary get() = value.toInt() and 0b00000100 != 0

    @kotlin.internal.InlineOnly
    inline val rPrimary get() = value.toInt() and 0b00001000 != 0

    @kotlin.internal.InlineOnly
    inline val lSecondary get() = value.toInt() and 0b00010000 != 0

    @kotlin.internal.InlineOnly
    inline val rSecondary get() = value.toInt() and 0b00100000 != 0

    @kotlin.internal.InlineOnly
    inline val lMeta get() = value.toInt() and 0b01000000 != 0

    @kotlin.internal.InlineOnly
    inline val rMeta get() = value.toInt() and 0b10000000 != 0

    @kotlin.internal.InlineOnly
    inline val isEmpty get() = value == 0.toByte()

    @kotlin.internal.InlineOnly
    inline val hasShift get() = value.toInt() and 0b00000011 != 0

    @kotlin.internal.InlineOnly
    inline val hasPrimary get() = value.toInt() and 0b00001100 != 0

    @kotlin.internal.InlineOnly
    inline val hasAlt get() = value.toInt() and 0b00110000 != 0

    @kotlin.internal.InlineOnly
    inline val hasMeta get() = value.toInt() and 0b11000000 != 0

    /**
     * Returns the number of modifiers set in this instance.
     */
    @kotlin.internal.InlineOnly
    inline val size get() = value.countOneBits()

    val name: String
        get() = this.toKeyModifiers().joinToString(" + ") { it.keyName }

    val fullName: String
        get() = this.toKeyModifiers().joinToString(" + ") { it.fullName }

    /**
     * *precise* equals, meaning that LSHIFT and RSHIFT are considered different, as well as LCONTROL and RCONTROL, etc.
     * @see equalLenient
     */
    @kotlin.internal.InlineOnly
    inline fun equal(other: Modifiers) = this.equal(other.value)

    /**
     * *lenient* equals, meaning that LSHIFT and RSHIFT are considered equal, as well as LCONTROL and RCONTROL, etc.
     * @see equal
     */
    @kotlin.internal.InlineOnly
    inline fun equalLenient(other: Byte) = this.equalLenient(Modifiers(other))

    /**
     * *lenient* equals, meaning that LSHIFT and RSHIFT are considered equal, as well as LCONTROL and RCONTROL, etc.
     * @see equal
     */
    @kotlin.internal.InlineOnly
    inline fun equalLenient(other: Modifiers) = this.hasAlt == other.hasAlt && this.hasPrimary == other.hasPrimary && this.hasShift == other.hasShift

    /**
     * *precise* equals, meaning that LSHIFT and RSHIFT are considered different, as well as LCONTROL and RCONTROL, etc.
     *
     */
    fun equal(other: Byte): Boolean {
        return other == value
    }

    /**
     * @return true if [other] is contained in this modifier sequence.
     *
     * For the inverse, see [containedBy].
     */
    fun contains(other: Byte): Boolean {
        return value == other || (value.toInt() and other.toInt() == value.toInt())
    }

    /**
     * @return true if this modifier instance is contained in the given [other] byte.
     *
     * For the inverse, see [contains].
     * @since 1.11.8
     */
    fun containedBy(other: Modifiers): Boolean {
        if (isEmpty) return true
        if (lPrimary && rPrimary) return other.hasPrimary
        else if (lPrimary) return other.lPrimary
        else if (rPrimary) return other.rPrimary

        if (lShift && rShift) return other.hasShift
        else if (lShift) return other.lShift
        else if (rShift) return other.rShift

        if (lSecondary && rSecondary) return other.hasAlt
        else if (lSecondary) return other.lSecondary
        else if (rSecondary) return other.rSecondary

        if (lMeta && rMeta) return other.hasMeta
        else if (lMeta) return other.lMeta
        else if (rMeta) return other.rMeta

        return false
    }

    fun toKeyModifiers(): Array<KeyModifiers> {
        val out = ArrayList<KeyModifiers>(size)
        if (lShift && rShift) out.add(KeyModifiers.SHIFT)
        else {
            if (lShift) out.add(KeyModifiers.LSHIFT)
            if (rShift) out.add(KeyModifiers.RSHIFT)
        }
        if (lPrimary && rPrimary) out.add(KeyModifiers.PRIMARY)
        else {
            if (lPrimary) out.add(KeyModifiers.LPRIMARY)
            if (rPrimary) out.add(KeyModifiers.RPRIMARY)
        }
        if (lSecondary && rSecondary) out.add(KeyModifiers.SECONDARY)
        else {
            if (lSecondary) out.add(KeyModifiers.LSECONDARY)
            if (rSecondary) out.add(KeyModifiers.RSECONDARY)
        }
        if (lMeta && rMeta) out.add(KeyModifiers.META)
        else {
            if (lMeta) out.add(KeyModifiers.LMETA)
            if (rMeta) out.add(KeyModifiers.RMETA)
        }
        return out.toTypedArray()
    }

    override fun toString() = "$name ($value)"
}
