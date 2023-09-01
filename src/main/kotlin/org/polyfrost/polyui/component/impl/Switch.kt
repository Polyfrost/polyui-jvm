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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.PolyColor as Color
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.input.PolyText
import org.polyfrost.polyui.property.impl.SwitchProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.Size
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.cl1
import org.polyfrost.polyui.utils.radii

/**
 * # Switch
 *
 * A simple switch component, which can be on or off.
 *
 * @param switchSize the size of the switch. Note that this value has to have a width that is at least twice as large as its height. Due to this, only one of the parameters has to be set, and the other one will be inferred.
 */
@Suppress("UNCHECKED_CAST")
class Switch(
    properties: SwitchProperties? = null,
    at: Point<Unit>,
    label: PolyText? = null,
    switchSize: Size<Unit>,
    enabled: Boolean = false,
    onSwitch: (Switch.(Boolean) -> kotlin.Unit)? = null,
    events: EventDSL<Switch>.() -> kotlin.Unit = {},
) : StateBlock(properties, at, switchSize, defaultState = enabled, onStateChange = onSwitch as (StateBlock.(Boolean) -> kotlin.Unit)?, events = events as EventDSL<StateBlock>.() -> kotlin.Unit) {
    @Transient
    private var anim: Animation? = null

    @Transient
    private var bitSize = 0f

    @Transient
    private var bitRadius = 0f

    @Transient
    private var dist = 0f

    @Transient
    private var labelw = 0f

    @Transient
    private lateinit var bitColor: Color.Animated
    var fontSize = 0f
    var label = label
        set(value) {
            value?.translator = polyUI.translator
            value?.string
            field = value
        }

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
        if (label != null) {
            renderer.text(properties.labelFont, x - properties.lateralPadding.px - labelw, y + height / 2f - fontSize / 2f, label!!.string, properties.labelColor, fontSize)
        }
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

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        bitColor = properties.bitColor.toAnimatable()
        val fs = properties.labelFontSize.clone()
        (fs as? Unit.Dynamic)?.set(this.size!!.a)
        fontSize = fs.px
        (properties.lateralPadding as? Unit.Dynamic)?.set(this.size!!.a)
        (properties.verticalPadding as? Unit.Dynamic)?.set(this.size!!.b)
        if (properties.labelFontSize.px == 0f) {
            fontSize = height - properties.verticalPadding.px * 2f
        }
        label?.let {
            it.translator = polyUI.translator
            labelw = renderer.textBounds(properties.labelFont, it.string, properties.labelFontSize.px).width
        }
    }

    override fun calculateBounds() {
        super.calculateBounds()
        calcRadii()
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        calcRadii()
        fontSize *= cl1(scaleX, scaleY)
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
