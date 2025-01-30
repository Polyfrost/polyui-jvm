/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2024 Polyfrost and its contributors.
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

package org.polyfrost.polyui

import org.polyfrost.polyui.color.mutable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.FontFamily
import org.polyfrost.polyui.dsl.polyUI
import org.polyfrost.polyui.renderer.impl.GLFWWindow
import org.polyfrost.polyui.renderer.impl.NVGRenderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.unit.fix
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.open
import org.polyfrost.polyui.utils.ref

fun main() {
    val window = GLFWWindow("PolyUI Test v2 (DSL)", 800, 500)
    polyUI {
        size = 800f by 500f
        renderer = NVGRenderer
        image("polyfrost.png")
        text("text.dark") {
            fontSize = 20f
        }
        group {
            Button("moon.svg".image()).add().onClick {
                shake(); false
            }
            Button("face-wink.svg".image(), "button.text").onClick {
                color = color.mutable()
                ColorPicker(color.mutable().ref(), mutableListOf(), mutableListOf(), polyUI)
                true
            }.add()
            Switch(size = 28f).add()
            Checkbox(size = 28f).add()
        }
        Dropdown("monkey blur", "phosphor blur", "moulberry blur").add()
        BoxedTextInput(pre = "Title:", post = "px").add()
        group {
            repeat(30) {
                val len = 32f + (Math.random().toFloat() * 100f)
                block(size = len by 32f) {
                    text("hello my name is jeff", visibleSize = Vec2(len - 8f, 14f))
                }.withStates()
            }
            it.visibleSize = 350f by 120f
        }.makeRearrangeableGrid()
        group(Vec2(800f, 120f)) {
            Button("shuffle.svg".image(), "button.randomize").onClick {
                val box = parent.parent[5]
                box.children?.shuffle()
                box.position()
            }.add()
            Button("minus.svg".image()).onClick {
                val box = parent.parent[5]
                box.removeChild(box.children!!.lastIndex)
            }.add()
            Button("plus.svg".image()).onClick {
                parent.parent[5].addChild(Block(size = 32f + (Math.random().toFloat() * 100f) by 32f).withStates())
            }.add()
            group {
                Radiobutton("hello", "goodbye", "yes", "no").add()
                Slider(length = 200f, min = 50f, max = 120f, instant = true, initialValue = 67f).add().onChange { value: Float ->
                    (parent.parent.parent[8][0][0][0] as Text).text = value.fix(2).toString()
                    false
                }
                text("blink three times when u feel it kicking in")
                Button(text = "select 3").onClick { (parent[0] as Inputtable).setRadiobuttonEntry(2); true }.add()
            }
        }
        group(alignment = Align(mode = Align.Mode.Vertical)) {
            text("wrapping text") { setFont(FontFamily::boldItalic) }
            text(
                "when am i gonna stop being wise for my age and just start being wise when am i gonna gonna stop being a pretty younger things to guys\n\n" +
                        "when will i stop being??\n" +
                        "they all say that it gets better, it gets better but what if i dont :(", visibleSize = 420f by 0f
            )
            text("i am some text that has been limited, so at some point i will stop showing up and i will just be cut off, which is a pretty handy feature.", limited = true, visibleSize = 400f by 12f)
            textInput(visibleSize = 150f by 12f)
        }
        BoxedNumericInput(min = 50f, max = 120f, size = 40f by 32f, initialValue = 67f).add().also {
            it[0].onChange { value: Float ->
                (polyUI.master[6][3][1]).setSliderValue(value, 50f, 120f)
                false
            }
        }
        Image("https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png".image(), size = Vec2(280f, 210f)).add()
    }.open(window)
}
