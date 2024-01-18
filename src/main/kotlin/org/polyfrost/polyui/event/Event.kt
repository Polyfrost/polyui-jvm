/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

package org.polyfrost.polyui.event

import org.polyfrost.polyui.event.Event.*
import org.polyfrost.polyui.event.Event.Lifetime.*
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import java.io.File
import org.polyfrost.polyui.input.Mouse as MouseUtils

/**
 * Events are how PolyUI communicates between components and to the user.
 *
 * Everything from [mouse clicks][Mouse.Clicked], [key presses][Focused.KeyPressed],
 * [text input changes][Change.Text], [initialization][Lifetime.Init]
 * and [destruction][Lifetime.Removed] of components are communicated through events.
 *
 * Some events are specific, requiring [constructor parameters][Mouse.Clicked.button] to ensure that for example, only single,
 * left-click events are handled by any given handler, whereas others are non-specific, like [Focused.UnmappedInput] and receive
 * any matching event of the type.
 *
 * These events can be dispatched by your own components, and you can add your own events - though PolyUI comes with 22 events by default.
 *
 */
interface Event {

    /**
     * Events related to the mouse.
     *
     * @see Mouse.Pressed
     * @see Mouse.Released
     * @see Mouse.Clicked
     * @see Mouse.Scrolled
     * @see Mouse.Entered
     * @see Mouse.Exited
     */
    interface Mouse : Event {
        class Pressed internal constructor(val button: Int, val x: Float, val y: Float, mods: Short) : Mouse, WithMods(mods) {
            constructor(button: Int) : this(button, 0f, 0f, 0)

            override fun hashCode(): Int {
                var result = button + 500
                result = 31 * result + mods
                return result
            }

            override fun toString(): String =
                "MousePressed(($x, $y), ${MouseUtils.toStringPretty(MouseUtils.fromValue(button), mods)})"

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Pressed) return false

                if (button != other.button) return false
                return mods == other.mods
            }
        }

        class Released internal constructor(val button: Int, val x: Float, val y: Float, mods: Short) : Mouse, WithMods(mods) {
            constructor(button: Int) : this(button, 0f, 0f, 0)

            override fun hashCode(): Int {
                var result = button + 5000 // avoid conflicts with MousePressed
                result = 31 * result + mods
                return result
            }

            override fun toString(): String =
                "MouseReleased(($x, $y), ${MouseUtils.toStringPretty(MouseUtils.fromValue(button), mods)})"

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Released) return false

                if (button != other.button) return false
                return mods == other.mods
            }
        }

        class Clicked internal constructor(val button: Int, val mouseX: Float, val mouseY: Float, val clicks: Int, mods: Short) : Mouse, WithMods(mods) {
            @JvmOverloads
            constructor(button: Int, amountClicks: Int = 1, mods: Short = 0) : this(button, 0f, 0f, amountClicks, mods)

            override fun toString(): String = "MouseClicked x$clicks($mouseX x $mouseY, ${MouseUtils.toStringPretty(MouseUtils.fromValue(button), mods)})"
            override fun hashCode(): Int {
                var result = button
                result = 31 * result + clicks
                result = 31 * result + mods
                return result
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Clicked) return false

                if (button != other.button) return false
                if (clicks != other.clicks) return false
                return mods == other.mods
            }
        }

        class Scrolled internal constructor(val amountX: Float, val amountY: Float, mods: Short) : Mouse, WithMods(mods) {
            constructor() : this(0f, 0f, 0)

            override fun hashCode() = 893402779

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                return other is Scrolled
            }
        }

        /** acceptable by component and layout, when the mouse enters this drawable.
         * @see Exited */
        object Entered : Mouse

        /** acceptable by component and layout, when the mouse leaves this drawable.
         * @see Entered */
        object Exited : Mouse
    }

    /**
     * Events related to the lifetime of a component.
     *
     * @see Added
     * @see Removed
     * @see Init
     * @see PostInit
     * @see Enabled
     * @see Disabled
     */
    interface Lifetime : Event {
        object Added : Lifetime
        object Removed : Lifetime

        object Init : Lifetime

        object PostInit : Lifetime

        object Enabled : Lifetime

        object Disabled : Lifetime
    }

    /**
     * Events related to specific things that occur in some components, for example
     * when a switch is toggled, a slider is moved or a text is changed.
     *
     * These events can be cancelled, meaning that the change in state will not occur.
     * @since 1.0.3
     */
    abstract class Change : Event {
        /**
         * Weather the event has been cancelled.
         * @since 1.0.3
         */
        var cancelled: Boolean = false
            private set

        /**
         * Cancel this event.
         * @since 1.0.3
         */
        fun cancel() {
            cancelled = true
        }

        class Text(val text: String) : Change() {
            constructor() : this("")

            override fun hashCode() = 859347809
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                return other is Text
            }
        }

        class Number(val amount: kotlin.Number) : Change() {
            constructor() : this(0)

            override fun hashCode() = 57328903
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                return other is Number
            }
        }

        class State(val state: Boolean) : Change() {
            constructor() : this(false)

            override fun hashCode() = 38294781
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                return other is State
            }
        }
    }

    /**
     * Events for focusable components (components which receive keyboard input)
     *
     * In order to receive these events, the [focusable flag][org.polyfrost.polyui.component.Drawable.focusable] must be `true`.
     *
     * @see Focused.Gained
     * @see Focused.Lost
     * @see Focused.KeyTyped
     * @see Focused.KeyPressed
     * @see Focused.KeyReleased
     * @see Focused.UnmappedInput
     */
    interface Focused : Event {
        object Gained : Focused
        object Lost : Focused

        /**
         * called when a key is typed (and modifiers) is pressed.
         *
         * @see [Keys]
         * @see [KeyModifiers]
         * @see [org.polyfrost.polyui.utils.fromModifierMerged]
         */
        class KeyTyped(val key: Char, mods: Short) : Focused, WithMods(mods) {
            override fun toString() = "KeyTyped(${Keys.toStringPretty(key, mods)})"

            override fun hashCode(): Int {
                var result = key.hashCode() + 500
                result = 31 * result + mods
                return result
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is KeyTyped) return false

                if (key != other.key) return false
                return mods == other.mods
            }
        }

        /**
         * called when a non-printable key (and modifiers) is pressed.
         *
         * @see [Keys]
         * @see [KeyModifiers]
         * @see [org.polyfrost.polyui.input.Modifiers.fromModifierMerged]
         */
        class KeyPressed(val key: Keys, mods: Short) : Focused, WithMods(mods) {
            override fun toString(): String = "KeyPressed(${Keys.toString(key, mods)})"

            fun toStringPretty(): String = "KeyPressed(${Keys.toStringPretty(key, mods)})"

            override fun hashCode(): Int {
                var result = key.hashCode() + 5000
                result = 31 * result + mods
                return result
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is KeyPressed) return false

                if (key != other.key) return false
                return mods == other.mods
            }
        }

        class KeyReleased(val key: Keys, mods: Short) : Focused, WithMods(mods) {
            override fun toString(): String = "KeyReleased(${Keys.toString(key, mods)})"

            fun toStringPretty(): String = "KeyReleased(${Keys.toStringPretty(key, mods)})"

            override fun hashCode(): Int {
                var result = key.hashCode() + 50000
                result = 31 * result + mods
                return result
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is KeyPressed) return false

                if (key != other.key) return false
                return mods == other.mods
            }
        }

        class UnmappedInput(val code: Int, val down: Boolean, mods: Short) : Focused, WithMods(mods) {
            constructor() : this(0, false, 0)

            override fun toString(): String = "UnmappedInput(key=$code, down=$down)"

            override fun hashCode(): Int {
                var result = code.hashCode() + 37489208
                result = 31 * result + down.hashCode()
                return result
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                return other is UnmappedInput
            }
        }

        /**
         * This event is dispatched when files are dropped into this PolyUI instance.
         *
         * In order to receive it, your drawable must have focus at the time of drop.
         * @since 1.0.3
         */
        class FileDrop internal constructor(val files: Array<File>) : Focused {
            constructor() : this(arrayOf())

            override fun toString() = "FileDrop($files)"

            override fun hashCode() = 453646123

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                return other is FileDrop
            }
        }
    }

    /**
     * Extend this class if your event has modifiers attached to it.
     *
     * Provides various utility methods to check for modifiers.
     *
     * @since 1.0.5
     */
    abstract class WithMods(val mods: Short) : Event {

        val modifiers: Array<KeyModifiers> by lazy { KeyModifiers.fromModifierMerged(mods) }

        fun hasModifier(modifier: KeyModifiers): Boolean = (mods.toInt() and modifier.value.toInt()) != 0

        fun hasControl() = (mods.toInt() and 0b1100) != 0 // || (PolyUI.isOnMac && hasMeta())

        fun hasAlt() = (mods.toInt() and 0b110000) != 0

        fun hasShift() = (mods.toInt() and 0b11) != 0

        fun hasMeta() = (mods.toInt() and 0b11000000) != 0
    }
}
