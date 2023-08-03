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

import cc.polyfrost.polyui.input.Mouse

/** acceptable by component and layout, when the mouse goes down on this drawable.
 * @see MouseReleased
 * @see MouseClicked
 * @see MouseEntered
 */
data class MousePressed internal constructor(val button: Int, val x: Float, val y: Float, val mods: Short = 0) : Event {
    constructor(button: Int) : this(button, 0f, 0f)

    override fun hashCode(): Int {
        var result = button + 500
        result = 31 * result + mods
        return result
    }

    override fun toString(): String =
        "MousePressed(($x, $y), ${Mouse.toStringPretty(Mouse.fromValue(button), mods)})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MousePressed) return false

        if (button != other.button) return false
        return mods == other.mods
    }
}

/** acceptable by component and layout, when the mouse is released on this component.
 *
 * Note that this event is **not dispatched** if the mouse leaves the drawable while being pressed, as it technically is not released on that drawable.
 * @see MousePressed
 * @see MouseClicked
 * @see MouseEntered
 */
data class MouseReleased internal constructor(val button: Int, val x: Float, val y: Float, val mods: Short = 0) : Event {
    constructor(button: Int) : this(button, 0f, 0f)

    override fun hashCode(): Int {
        var result = button + 5000 // avoid conflicts with MousePressed
        result = 31 * result + mods
        return result
    }

    override fun toString(): String =
        "MouseReleased(($x, $y), ${Mouse.toStringPretty(Mouse.fromValue(button), mods)})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MouseReleased) return false

        if (button != other.button) return false
        return mods == other.mods
    }
}

/** acceptable by components and layouts, and is dispatched when the mouse is clicked on this component.
 *
 * Note that this event is **not dispatched** if the mouse leaves the drawable while being pressed, as it technically is not released on that drawable.
 * @see MouseReleased
 * @see MouseClicked
 * @see MouseEntered
 */
data class MouseClicked internal constructor(val button: Int, val mouseX: Float, val mouseY: Float, val clicks: Int, val mods: Short) : Event {

    @JvmOverloads
    constructor(button: Int, amountClicks: Int = 1, mods: Short = 0) : this(button, 0f, 0f, amountClicks, mods)

    override fun toString(): String = "MouseClicked x$clicks($mouseX x $mouseY, ${Mouse.toStringPretty(Mouse.fromValue(button), mods)})"
    override fun hashCode(): Int {
        var result = button
        result = 31 * result + clicks
        result = 31 * result + mods
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MouseClicked) return false

        if (button != other.button) return false
        if (clicks != other.clicks) return false
        return mods == other.mods
    }
}

/** acceptable by component and layout, when the mouse enters this drawable.
 * @see MouseExited */
data object MouseEntered : Event

/** acceptable by component and layout, when the mouse leaves this drawable.
 * @see MouseEntered */
data object MouseExited : Event

/**
 * acceptable by component and layout, when the mouse is moved on this drawable.
 *
 * This does not have any data attached from it so that it is not instanced every time the mouse moves. It can be accessed with [polyUI.mouseX][cc.polyfrost.polyui.PolyUI.mouseX] and [polyUI.mouseY][cc.polyfrost.polyui.PolyUI.mouseY].
 */
data object MouseMoved : Event

/** Dispatched when the mouse is scrolled on this component/layout.
 *
 * acceptable by component and layout */
data class MouseScrolled internal constructor(val amountX: Int, val amountY: Int, val mods: Short = 0) : Event {
    constructor() : this(0, 0)

    override fun hashCode() = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is MouseScrolled
    }
}

/**
 * This event is dispatched when a component/layout is added **after** it has been initialized (i.e. not in the UI creation block), using the [addComponent][cc.polyfrost.polyui.layout.Layout.addComponent] function.
 *
 * acceptable by component and layout. */
object Added : Event

/**
 * This event is dispatched when a component/layout is removed, using the [removeComponent][cc.polyfrost.polyui.layout.Layout.removeComponent] function.
 *
 * acceptable by component and layout. */
object Removed : Event
