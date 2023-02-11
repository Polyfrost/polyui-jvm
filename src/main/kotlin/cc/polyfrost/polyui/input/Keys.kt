package cc.polyfrost.polyui.input

import cc.polyfrost.polyui.input.Keys.Modifiers
import kotlin.experimental.and

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
            val mods = Modifiers.fromModifierMerged(modifiers)
            if (mods.size == 0) return key.keyName
            val modString = mods.joinToString("+") { it.toString() }
            return "$modString + ${key.keyName}"
        }

        /**
         * return a string representation of this key combo.
         *
         * For example, [LSHIFT][Modifiers.LSHIFT] + [LCONTROL][Modifiers.LCONTROL] + `a` would return `"LSHIFT+LCONTROL+a"`
         */
        @JvmStatic
        fun toString(key: Char, modifiers: Short = 0): String {
            val mods = Modifiers.fromModifierMerged(modifiers)
            if (mods.size == 0) return key.toString()
            val modString = mods.joinToString("+") { it.toString() }
            return "$modString+$key"
        }

        /**
         * return a pretty string representation of this key combo.
         *
         * For example, [LSHIFT][Modifiers.LSHIFT] + [LCONTROL][Modifiers.LCONTROL] + [INSERT][Keys.INSERT] would return `"Left Shift + Left Control + Insert"`
         */
        @JvmStatic
        fun toStringPretty(key: Keys, modifiers: Short = 0): String {
            val mods = Modifiers.fromModifierMerged(modifiers)
            if (mods.size == 0) return key.keyName
            val modString = mods.joinToString(" + ") { it.keyName }
            return "$modString + ${key.keyName}"
        }

        /**
         * return a pretty string representation of this key combo.
         *
         * For example, [LSHIFT][Modifiers.LSHIFT] + [LCONTROL][Modifiers.LCONTROL] + `a` would return `"Left Shift + Left Control + a"`
         */
        @JvmStatic
        fun toStringPretty(key: Char, modifiers: Short = 0): String {
            val mods = Modifiers.fromModifierMerged(modifiers)
            if (mods.size == 0) return key.toString()
            val modString = mods.joinToString(" + ") { it.keyName }
            return "$modString + $key"
        }
    }

    /**
     * PolyUI's mapping for modifier keys, in binary form so logical OR can be used to check for multiple modifiers.
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

        /** you should never receive this value. */
        UNKNOWN("Unknown", 0);

        companion object {
            /** take the given short-merged modifiers and return a list of the modifiers. */
            @JvmStatic
            fun fromModifierMerged(modifiers: Short): MutableList<Modifiers> {
                if (modifiers == 0.toShort()) return mutableListOf()
                val mods = mutableListOf<Modifiers>()
                for (mod in values()) {
                    if (mod.value and modifiers != 0.toShort()) {
                        mods.add(mod)
                    }
                }
                return mods
            }
        }
    }
}

typealias KeyModifiers = Modifiers