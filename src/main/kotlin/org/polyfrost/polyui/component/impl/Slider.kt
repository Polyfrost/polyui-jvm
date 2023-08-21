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
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.event.MousePressed
import org.polyfrost.polyui.property.impl.SliderProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.Size
import org.polyfrost.polyui.unit.Unit

/**
 * A slider component.
 * @since 0.19.0
 */
@Suppress("UNCHECKED_CAST")
open class Slider(
    properties: SliderProperties? = null,
    at: Point<Unit>,
    size: Size<Unit>,
    val min: Float = 0f,
    val max: Float = 100f,
    events: EventDSL<Slider>.() -> kotlin.Unit = {},
) : Component(properties, at, size, false, true, events as EventDSL<Component>.() -> kotlin.Unit) {
    override val properties
        get() = super.properties as SliderProperties

    @Transient
    protected var barThickness = 0f

    @Transient
    protected var bitMain = 0f

    @Transient
    protected var barCross = 0f

    @Transient
    lateinit var barColor: Color.Animated

    @Transient
    lateinit var usedBarColor: Color.Animated

    @Transient
    val horizontal = size.width > size.height
    var main
        get() = if (horizontal) width else height
        set(value) {
            if (horizontal) width = value else height = value
        }
    var cross
        get() = if (horizontal) height else width
        set(value) {
            if (horizontal) height = value else width = value
        }

    var value: Float = min
        set(value) {
            bitMain = (value - min) / (max - min) * (main - cross)
            if ((p as? SliderProperties)?.setInstantly == true && field != value) {
                accept(ChangedEvent(value))
            }
            field = value
        }

    @Transient
    protected var dragging = false

    @Transient
    private var mp = 0f

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        barColor = properties.barColor.normal.toAnimatable()
        usedBarColor = properties.usedBarColor.normal.toAnimatable()
    }

    override fun accept(event: Event): Boolean {
        if (event is MousePressed) {
            if (event.button == 0) {
                val pos = if (horizontal) event.x else event.y
                if (!isOnBit(event.x - trueX, event.y - trueY)) {
                    val truePos = if (horizontal) trueX else trueY
                    set(event.x - truePos - cross / 2f)
                }
                dragging = true
                mp = pos - bitMain
            }
        }
        return super.accept(event)
    }

    protected fun isOnBit(x: Float, y: Float): Boolean {
        val main = if (horizontal) x else y
        val crs = if (horizontal) y else main
        return main in bitMain..bitMain + cross && crs in crs..crs + cross
    }

    protected fun doDrag() {
        if (dragging) {
            if (!polyUI.mouseDown) {
                if (!properties.setInstantly) {
                    accept(ChangedEvent(value))
                }
                dragging = false
            }
            wantRedraw()
            if (horizontal) set(polyUI.mouseX - mp) else set(polyUI.mouseY - mp)
        }
    }

    override fun render() {
        doDrag()
        val cross = cross
        val hCross = cross / 2f
        val barRadius = barThickness / 2f
        if (horizontal) {
            if (bitMain + x != x) renderer.rect(x + hCross, barCross, bitMain, barThickness, usedBarColor, barRadius)
            renderer.rect(x + bitMain + hCross, barCross, main - bitMain - cross, barThickness, barColor, barRadius)
            renderer.rect(bitMain + x, y, cross, cross, color, cross)
        } else {
            if (bitMain + y != y) renderer.rect(barCross, y + hCross, barThickness, bitMain, usedBarColor, barRadius)
            renderer.rect(barCross, y + bitMain + hCross, barThickness, main - bitMain - cross, barColor, barRadius)
            renderer.rect(x, bitMain + y, cross, cross, color, cross)
        }
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        barColor.recolor(properties.barColor.normal)
        usedBarColor.recolor(properties.usedBarColor.normal)
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        calculateBounds()
        // set the value back so it updates position stuff
        value = value
    }

    protected fun set(value: Float) {
        var v = value
        if (value > main - cross) v = main - cross
        if (value < 0f) v = 0f
        this.value = (v / (main - cross)) * (max - min) + min
    }

    override fun calculateBounds() {
        super.calculateBounds()
        barThickness = cross / properties.thicknessRatio
        barCross = cross / 2f - barThickness / 2f
        barCross += if (horizontal) y else x
    }

    class ChangedEvent internal constructor(val value: Float) : Event {
        constructor() : this(0f)

        // uhh, i'd say about 20 feet
        override fun hashCode() = 29083213
        override fun equals(other: Any?) = other is ChangedEvent
    }
}
