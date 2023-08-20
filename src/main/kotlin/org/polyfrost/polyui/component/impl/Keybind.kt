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
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.property.impl.BlockProperties.Companion.withStates
import org.polyfrost.polyui.property.impl.KeybindProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2

open class Keybind(
    properties: KeybindProperties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>? = null,
    withIcon: Boolean = true,
    bind: KeyBinder.Bind? = null,
) : Button(properties, at, size, left = if (withIcon) PolyImage("keyboard.svg", 18f, 18f) else null, text = bind?.keysToString()?.localised() ?: empty, right = PolyImage("close.svg", 10f, 10f)) {
    override val properties
        get() = super.properties as KeybindProperties
    private var recording = false
        set(value) {
            if (value == field) return
            field = value
            if (value) {
                if (bind != null) polyUI.keyBinder.remove(bind!!)
                text!!.recolor(properties.colors.state.danger.normal)
                polyUI.keyBinder.record(
                    bind?.durationNanos ?: 0L,
                    bind?.action ?: {
                        println("Unmapped keybind function")
                        true
                    },
                ).thenAccept {
                    if (it != null) {
                        bind = it
                    }
                    recording = false
                }
            } else {
                text!!.recolor(properties.colors.text.primary.normal)
                polyUI.keyBinder.cancelRecord()
            }
        }
    var bind = bind
        set(value) {
            field = value
            string = value?.keysToString() ?: empty.string
            if (value != null) polyUI.keyBinder.add(value)
        }
    protected open var animation: Animation? = null
    protected var out = false
    protected lateinit var color2: Color

    init {
        rightImage!!.acceptsInput = true
        addEventHandler(MouseClicked(0)) {
            if (mouseOver) {
                recording = true
            }
        }
        rightImage.addEventHandler(MouseClicked(0)) {
            mouseOver = false
            recording = false
        }
    }

    override fun render() {
        if (properties.hasBackground) {
            if (properties.outlineThickness != 0f) {
                renderer.hollowRect(0f, 0f, width, height, color, properties.outlineThickness, properties.cornerRadii)
            }
            renderer.rect(0f, 0f, width, height, color, properties.cornerRadii)
        }
        text!!.render()
        if (recording) {
            drawRecordingIcon(properties.lateralPadding, y)
            rightImage!!.render()
        } else {
            leftImage?.render()
        }
    }

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        empty.translator = polyUI.translator
        empty.string
        rightImage!!.properties.withStates()
        if (bind != null) polyUI.keyBinder.add(bind!!)
        color2 = properties.breatheColor.clone()
        color2.alpha = 0.6f
    }

    /**
     * Draw the recording icon at the given position.
     */
    open fun drawRecordingIcon(x: Float, y: Float) {
        wantRedraw()
        if (animation?.isFinished == true) {
            animation = null
            out = !out
        }
        if (animation == null || animation!!.isFinished) {
            animation = if (out) {
                properties.breatheAnimation.create(properties.breatheDuration, height / properties.begin, height / properties.extent)
            } else {
                properties.breatheAnimation.create(properties.breatheDuration, height / properties.extent, height / properties.begin)
            }
        }
        val posy = height / 2f - properties.extent
        animation!!.update(polyUI.delta)
        val v = animation!!.value
        val b = properties.begin
        renderer.rect(x + b - v / 2f, posy + b - v / 2f, b + v, b + v, color2, v * 2f)

        renderer.rect(x + b, posy + b, b, b, properties.breatheColor, b / 2f)
    }

    companion object {
        val empty = "polyui.keybind.empty".localised()
    }
}
