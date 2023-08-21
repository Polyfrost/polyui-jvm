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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.property.impl.StatedProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2

/**
 * Represents a component that can be either in an active or inactive state, such as a [Checkbox][org.polyfrost.polyui.component.impl.Checkbox] or a [Switch][org.polyfrost.polyui.component.impl.Switch].
 * @since 0.20.0
 */
@Suppress("UNCHECKED_CAST")
abstract class StateBlock(
    properties: StatedProperties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>,
    rawResize: Boolean = false,
    defaultState: Boolean = false,
    @Transient protected open val onStateChange: (StateBlock.(Boolean) -> kotlin.Unit)? = null,
    events: EventDSL<StateBlock>.() -> kotlin.Unit = {},
) : Block(properties, at, size, rawResize, true, events as EventDSL<Block>.() -> kotlin.Unit) {
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

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        if (active) recolor(properties.activePalette.normal, properties.activateAnimation, properties.activateAnimationDuration)
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
