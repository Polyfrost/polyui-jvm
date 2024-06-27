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

package org.polyfrost.polyui.event

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.event.Event.*
import org.polyfrost.polyui.event.Event.Lifetime.*
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.Modifiers
import java.nio.file.Path
import org.polyfrost.polyui.input.Mouse as MouseUtils

/**
 * Events are how PolyUI communicates between components and to the user.
 *
 * Everything from [mouse clicks][Mouse.Clicked], [key presses][Focused.KeyPressed],
 * [text input changes][Change.Text], [initialization][Lifetime.Init]
 * and [destruction][Lifetime.Removed] of components are communicated through events.
 *
 *
 * *(since 1.3.1)* events can be destructured, like so:
 * ```
 * drawable.onClick { (x, y) ->
 *  // access and use the x and y position of the mouse with a prettier syntax
 * }
 * ```
 *
 * These events can be dispatched by your own components, and you can add your own events - though PolyUI comes with 22 events by default.
 *
 * ## event specificity
 * Some events are specific, requiring [constructor parameters][Mouse.Clicked.button] to ensure that for example, only single,
 * left-click events are handled by any given handler. others are non-specific, like [Focused.UnmappedInput] and receive
 * any matching event of the type.
 *
 * by default, an event is non-specific. in order to make it specific, override the [equals] and [hashCode] methods.
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
        abstract class ButtonBase(val button: Int, x: Float, y: Float, val mods: Modifiers) : Mouse {
            var x = x
                private set
            var y = y
                private set

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ButtonBase

                if (button != other.button) return false
                if (mods != other.mods) return false
                return true
            }

            override fun hashCode(): Int {
                var result = button
                result = 31 * result + mods.hashCode()
                return result
            }

            internal fun set(mx: Float, my: Float): ButtonBase {
                this.x = mx
                this.y = my
                return this
            }

            operator fun component1() = x
            operator fun component2() = y
        }

        class Pressed internal constructor(button: Int, x: Float, y: Float, mods: Modifiers) : ButtonBase(button, x, y, mods) {
            constructor(button: Int, mods: Modifiers = Modifiers(0)) : this(button, 0f, 0f, mods)

            override fun toString(): String =
                "MousePressed(($x, $y), ${MouseUtils.toStringPretty(MouseUtils.fromValue(button), mods)})"
        }

        class Released internal constructor(button: Int, x: Float, y: Float, mods: Modifiers) : ButtonBase(button, x, y, mods) {
            constructor(button: Int, mods: Modifiers = Modifiers(0)) : this(button, 0f, 0f, mods)

            override fun toString(): String =
                "MouseReleased(($x, $y), ${MouseUtils.toStringPretty(MouseUtils.fromValue(button), mods)})"
        }

        class Clicked internal constructor(button: Int, x: Float, y: Float, val clicks: Int, mods: Modifiers) : ButtonBase(button, x, y, mods) {
            constructor(button: Int, amountClicks: Int = 1, mods: Modifiers = Modifiers(0)) : this(button, 0f, 0f, amountClicks, mods)

            override fun hashCode(): Int {
                var result = super.hashCode()
                result = 31 * result + clicks
                return result
            }

            override fun equals(other: Any?): Boolean {
                return super.equals(other) && other is Clicked && clicks == other.clicks
            }

            override fun toString(): String = "MouseClicked x$clicks(($x, $y), ${MouseUtils.toStringPretty(MouseUtils.fromValue(button), mods)})"
        }

        class Scrolled internal constructor(val amountX: Float, val amountY: Float, val mods: Modifiers) : Mouse {}

        /** acceptable by component and layout, when the mouse enters this drawable.
         * @see Exited */
        object Entered : Mouse

        /** acceptable by component and layout, when the mouse leaves this drawable.
         * @see Entered */
        object Exited : Mouse

        /**
         * dispatched when the mouse is moved while left click is down. Use `polyUI.mouseX` and `polyUI.mouseY` to get the current position.
         * @since 1.0.8
         */
        object Dragged : Mouse

        /**
         * dispatched when the mouse is moved. Use `polyUI.mouseX` and `polyUI.mouseY` to get the current position.
         * @since 1.5.0
         */
        object Moved : Mouse

        companion object {
            /**
             * Constant for [Event.Mouse.Released], for single left click events.
             * @since 1.2.0
             */
            @JvmStatic
            val Released = Released(0)

            /**
             * Constant for [Event.Mouse.Pressed], for single left click events.
             * @since 1.2.0
             */
            @JvmStatic
            val Pressed = Pressed(0)

            /**
             * Constant for [Event.Mouse.Released], for single left click events.
             * @since 1.2.0
             */
            @JvmStatic
            val Clicked = Clicked(0)

            @JvmStatic
            val Scrolled = Scrolled(0f, 0f, Modifiers(0))
        }
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

        /**
         * Cancel this event.
         * @since 1.0.3
         */
        fun cancel() {
            cancelled = true
        }

        class Text @ApiStatus.Internal constructor(val text: String) : Change() {
            operator fun component1() = text
            override fun toString() = text
        }

        class Number @ApiStatus.Internal constructor(val amount: kotlin.Number) : Change() {
            operator fun component1() = amount
            override fun toString() = amount.toString()
        }

        class State @ApiStatus.Internal constructor(val state: Boolean) : Change() {
            operator fun component1() = state
            override fun toString() = state.toString()
        }

        companion object {
            @JvmStatic
            val Text = Text("")

            @JvmStatic
            val Number = Number(0)

            @JvmStatic
            val State = State(false)
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
     * @see Focused.FileDrop
     */
    interface Focused : Event {
        object Gained : Focused

        object Lost : Focused

        /**
         * called when a key is typed (and modifiers) is pressed.
         *
         * @see [Keys]
         * @see [Modifiers]
         */
        class KeyTyped internal constructor(val key: Char, val mods: Modifiers) : Focused {
            override fun toString() = "KeyTyped(${Keys.toStringPretty(key, mods)})"

            operator fun component1() = key
            operator fun component2() = mods
        }

        /**
         * called when a non-printable key (and modifiers) is pressed.
         *
         * @see [Keys]
         * @see [Modifiers]
         */
        class KeyPressed internal constructor(val key: Keys, val mods: Modifiers) : Focused {
            override fun toString(): String = "KeyPressed(${Keys.toString(key, mods)})"

            fun toStringPretty(): String = "KeyPressed(${Keys.toStringPretty(key, mods)})"

            operator fun component1() = key
            operator fun component2() = mods
        }

        class KeyReleased internal constructor(val key: Keys, val mods: Modifiers) : Focused {
            override fun toString(): String = "KeyReleased(${Keys.toString(key, mods)})"

            fun toStringPretty(): String = "KeyReleased(${Keys.toStringPretty(key, mods)})"

            operator fun component1() = key
            operator fun component2() = mods
        }

        class UnmappedInput internal constructor(val code: Int, val down: Boolean, val mods: Modifiers) : Focused {
            override fun toString(): String = "UnmappedInput(key=$code ('${code.toChar()}'), down=$down, mods=$mods)"
        }

        /**
         * This event is dispatched when files are dropped into this PolyUI instance.
         *
         * In order to receive it, your drawable must have focus at the time of drop.
         * @since 1.0.3
         */
        class FileDrop internal constructor(val files: Array<Path>) : Focused {
            override fun toString() = "FileDrop(${files.joinToString()})"

            operator fun component1() = files
        }

        companion object {
            @JvmStatic
            val KeyTyped = KeyTyped(0.toChar(), Modifiers(0))

            @JvmStatic
            val KeyPressed = KeyPressed(Keys.UNKNOWN, Modifiers(0))

            @JvmStatic
            val KeyReleased = KeyReleased(Keys.UNKNOWN, Modifiers(0))

            @JvmStatic
            val UnmappedInput = UnmappedInput(0, false, Modifiers(0))

            @JvmStatic
            val FileDrop = FileDrop(arrayOf())
        }
    }
}
