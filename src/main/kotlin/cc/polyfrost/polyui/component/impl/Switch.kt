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

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.event.Event
import cc.polyfrost.polyui.property.impl.SwitchProperties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.radii

/**
 * # Switch
 *
 * A simple switch component, which can be on or off.
 *
 * @param size the size of the switch. Note that this value has to have a width that is at least twice as large as its height. Due to this, only one of the parameters has to be set, and the other one will be inferred.
 */
@Suppress("UNCHECKED_CAST")
class Switch(
    properties: SwitchProperties? = null,
    at: Point<Unit>,
    size: Size<Unit>,
    enabled: Boolean = false,
    onEnable: (Switch.(Boolean) -> kotlin.Unit)? = null,
    vararg events: Event.Handler
) : StateBlock(properties, at, size, defaultState = enabled, onStateChange = onEnable as (StateBlock.(Boolean) -> kotlin.Unit)?, events = events) {
    private var anim: Animation? = null
    private var bitSize = 0f
    private var bitRadius = 0f
    private var dist = 0f
    private lateinit var bitColor: Color.Animated

    override val properties
        get() = super.properties as SwitchProperties

    override fun preRender(deltaTimeNanos: Long) {
        super.preRender(deltaTimeNanos)
        anim?.update(deltaTimeNanos)
        if (anim?.isFinished == true) {
            anim = null
        }
    }

    override fun render() {
        super.render()
        val value = anim?.value ?: if (active) 1f else 0f
        val pad = properties.bitPadding.px
        renderer.rect(x + pad + (dist * value), y + pad, bitSize, bitSize, bitColor, cornerRadii)
    }

    override fun onStateChanged(state: Boolean) {
        super.onStateChanged(state)
        anim = if (state) {
            properties.activateAnimation?.create(properties.activateAnimationDuration, anim?.value ?: 0f, 1f)
        } else {
            properties.activateAnimation?.create(properties.activateAnimationDuration, anim?.value ?: 1f, 0f)
        }
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        bitColor = properties.bitColor.toAnimatable()
    }

    override fun calculateBounds() {
        super.calculateBounds()
        calcRadii()
    }

    private fun calcRadii() {
        cornerRadii = (height / 2f).radii()
        bitSize = height - properties.bitPadding.px * 2f
        bitRadius = bitSize / 2f
        dist = width - height
    }

    override fun onInitComplete() {
        if (height == 0f && width != 0f) {
            height = width / 2f
        } else if (width == 0f && height != 0f) {
            width = height * 2f
        } else if (width < height * 2f) {
            PolyUI.LOGGER.warn("Switch width is less than twice its height, resizing to fit.")
            width = height * 2f
        }
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        calcRadii()
        bitColor.recolor(properties.bitColor)
    }
}
