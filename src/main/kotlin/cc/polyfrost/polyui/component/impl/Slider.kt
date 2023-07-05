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
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.impl.SliderProperties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit

/**
 * A slider component.
 * @since 0.19.0
 */
class Slider(
    properties: SliderProperties? = null,
    at: Point<Unit>,
    size: Size<Unit>,
    val min: Float = 0f,
    val max: Float = 100f
) : Component(properties, at, size, false, true) {
    override val properties
        get() = super.properties as SliderProperties
    private var barThickness = 0f
    private var bitX = 0f
    private var barY = 0f
    lateinit var barColor: Color.Mutable
    lateinit var usedBarColor: Color.Mutable

    var value: Float = min
        set(value) {
            field = value
            bitX = (value - min) / (max - min) * (width - height)
        }
    private var dragging = false
    private var mx = 0f

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        barColor = properties.barColor.normal.toMutable()
        usedBarColor = properties.usedBarColor.normal.toMutable()
    }

    override fun accept(event: Events): Boolean {
        if (event is Events.MousePressed) {
            if (event.button == 0) {
                if (!isOnBit(event.x - trueX, event.y - trueY)) {
                    set(event.x - height)
                }
                dragging = true
                mx = event.x - bitX
            }
        }
        return super.accept(event)
    }

    private fun isOnBit(x: Float, y: Float): Boolean {
        return x in bitX..bitX + height && y in y..y + height
    }

    override fun render() {
        if (dragging) {
            if (!polyui.mouseDown) dragging = false
            set(polyui.mouseX - mx)
        }
        val height = height
        val hHeight = height / 2f
        val barRadius = barThickness / 2f
        if (bitX != x) renderer.rect(x + hHeight, barY, bitX, barThickness, usedBarColor, barRadius)
        renderer.rect(x + bitX + hHeight, barY, width - bitX - height, barThickness, barColor, barRadius)
        renderer.rect(bitX, y, height, height, color, height)
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        barColor = properties.barColor.normal.toMutable()
        usedBarColor = properties.usedBarColor.normal.toMutable()
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        // set the value back so it updates position stuff
        value = value
    }

    private fun set(value: Float) {
        var v = value
        if (value > width - height) v = width - height
        if (value < 0f) v = 0f
        this.value = (v / (width - height)) * (max - min) + min
    }

    override fun calculateBounds() {
        super.calculateBounds()
        barThickness = height / properties.thicknessRatio
        barY = y + height / 2f - barThickness / 2f
    }
}
