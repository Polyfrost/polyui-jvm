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

package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.event.Event
import cc.polyfrost.polyui.event.MouseClicked
import cc.polyfrost.polyui.property.impl.StatedProperties
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2

/**
 * Represents a component that can be either in an active or inactive state, such as a [Checkbox][cc.polyfrost.polyui.component.impl.Checkbox] or a [Switch][cc.polyfrost.polyui.component.impl.Switch].
 * @since 0.20.0
 */
abstract class StateBlock(
    properties: StatedProperties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>,
    rawResize: Boolean = false,
    defaultState: Boolean = false,
    protected open val onStateChange: (StateBlock.(Boolean) -> kotlin.Unit)? = null,
    vararg events: Event.Handler
) : Block(properties, at, size, rawResize, true, *events) {
    override val properties
        get() = super.properties as StatedProperties

    /**
     * This is the variable which holds what state the component is in.
     * @since 0.20.0
     * @see onStateChanged
     */
    var active = defaultState
        set(value) {
            if (value == field) return
            field = value
            onStateChanged(value)
        }

    override fun accept(event: Event): Boolean {
        if (event is MouseClicked && event.button == 0) {
            active = !active
            return true
        }
        return super.accept(event)
    }

    /**
     * This function is called when the state of the component changes.
     * @param state The new state of the component.
     * @since 0.20.0
     */
    open fun onStateChanged(state: Boolean) {
        onStateChange?.invoke(this, state)
        val palette = if (state) {
            properties.onActivate?.invoke(this)
            properties.activePalette
        } else {
            properties.onDeactivate?.invoke(this)
            properties.palette
        }
        recolor(if (mouseOver) palette.hovered else palette.normal, properties.activateAnimation, properties.activateAnimationDuration)
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        if (active) color.recolor(properties.activePalette.normal)
    }
}
