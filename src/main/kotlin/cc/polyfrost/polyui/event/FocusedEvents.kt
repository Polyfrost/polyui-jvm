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

package cc.polyfrost.polyui.event

import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.input.Keys
import kotlin.experimental.and

open class FocusedEvents : Event {
    object FocusGained : FocusedEvents()
    object FocusLost : FocusedEvents()

    /**
     * called when a key is typed (and modifiers) is pressed.
     *
     * @see [Keys]
     * @see [KeyModifiers]
     * @see [cc.polyfrost.polyui.utils.fromModifierMerged]
     */
    data class KeyTyped(val key: Char, val mods: Short = 0, val isRepeat: Boolean = false) : FocusedEvents() {
        override fun toString() = "KeyTyped(${Keys.toStringPretty(key, mods)})"

        inline val modifiers: Array<KeyModifiers> get() = KeyModifiers.fromModifierMerged(mods)

        fun hasModifier(modifier: KeyModifiers): Boolean = (mods and modifier.value) != 0.toShort()

        override fun hashCode(): Int {
            var result = key.hashCode() + 500
            result = 31 * result + mods
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            other as KeyTyped

            if (key != other.key) return false
            return mods == other.mods
        }
    }

    /**
     * called when a non-printable key (and modifiers) is pressed.
     *
     * @see [Keys]
     * @see [KeyModifiers]
     * @see [cc.polyfrost.polyui.input.Modifiers.fromModifierMerged]
     */
    data class KeyPressed(val key: Keys, val mods: Short = 0, val isRepeat: Boolean = false) : FocusedEvents() {
        override fun toString(): String = "KeyPressed(${Keys.toString(key, mods)})"

        fun toStringPretty(): String = "KeyPressed(${Keys.toStringPretty(key, mods)})"

        inline val modifiers get() = KeyModifiers.fromModifierMerged(mods)

        fun hasModifier(modifier: KeyModifiers): Boolean = (mods and modifier.value) != 0.toShort()

        override fun hashCode(): Int {
            var result = key.hashCode() + 5000
            result = 31 * result + mods
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            other as KeyPressed

            if (key != other.key) return false
            return mods == other.mods
        }
    }
}
