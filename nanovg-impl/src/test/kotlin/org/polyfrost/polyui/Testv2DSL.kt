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

import org.polyfrost.polyui.animate.SetAnimation
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.asMutable
import org.polyfrost.polyui.color.rgba
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.FontFamily
import org.polyfrost.polyui.dsl.polyUI
import org.polyfrost.polyui.event.State
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.PolyBind
import org.polyfrost.polyui.renderer.impl.GLFWWindow
import org.polyfrost.polyui.renderer.impl.GLRenderer
import org.polyfrost.polyui.renderer.impl.NoOpRenderer
import org.polyfrost.polyui.renderer.impl.NoOpWindow
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.open
import kotlin.random.Random

fun main() {
    val noop = false
    val window = if (noop) NoOpWindow(800, 500) else GLFWWindow("PolyUI Test v2 (DSL)", 800, 500, gl2 = false)
    val theme = DarkTheme()
    PolyUI.registeredFonts["comic"] = FontFamily("ComicNeue", "polyui/fonts/comic/")

    polyUI {
        size = 800f by 500f
        renderer = if (noop) NoOpRenderer else GLRenderer
        if (true) {
            settings.debug = true
            settings.printPerfInfo = true
        }
        colors = theme
        backgroundColor = theme.page.bg.normal
        val bind = PolyBind(key = Keys.P) {
            println("you pressed the bind! $it")
            true
        }
        keyBinder?.add(bind)
        image("polyfrost.png")
        text("text.dark") {
            fontSize = 20f
        }
        Checkbox(size = 25f).add()
        Checkbox(size = 16f).add()
        Checkbox(size = 40f).add()
        val state = State(67f)
        val slider = Slider(length = 200f, min = 50f, max = 120f, instant = true, state = state)
        val boxedNumericInput = BoxedNumericInput(min = 50f, max = 120f, size = 80f by 32f, post = "Hi", state = state, arrows = false)

        group {
            Button("moon.svg".image()).add().onClick {
                shake()
                false
            }
            Button("face-wink.svg".image(), "button.text").onClick {
                color = color.asMutable()
                ColorPicker(State(color.asMutable()), polyUI, attachedDrawable = this)
                false
            }.add()
            Switch(size = 28f, state = State(true)).add()
            Checkbox(size = 28f).add()
        }
        Dropdown("monkey blur", "phosphor blur", "moulberry blur").add()
        BoxedTextInput(pre = "Title:", post = "px").add()
        var theBox = group {
            repeat(30) {
                val len = 32f + (Math.random().toFloat() * 100f)
                block(size = len by 32f) {
                    text("hello my name is jeff")
                }.withHoverStates()
            }
            configure {
                visibleSize = 350f by 120f
            }
        }.makeRearrangeableGrid()
        group {
            Button("shuffle.svg".image(), "button.randomize").onClick {
                theBox.children?.shuffle()
                theBox.position()
            }.add()
            Button("minus.svg".image()).onClick {
                theBox.removeChild(theBox.children!!.lastIndex)
            }.add()
            Button("plus.svg".image()).onClick {
                theBox.addChild(Block(size = 32f + (Math.random().toFloat() * 100f) by 32f).withHoverStates())
            }.add()
            Button(text = "reset the box").onClick {
                val boxParent = theBox.parent
                val idx = theBox.getMyIndex()
                theBox = Group(
                    *Array(30) {
                        val len = 32f + (Math.random().toFloat() * 100f)
                        Block(
                            Text("he"), size = len by 32f
                        ).withHoverStates()
                    }, visibleSize = 350f by 120f
                ).makeRearrangeableGrid()
                val anim = if (Random.nextBoolean()) SetAnimation.SlideRight else SetAnimation.SlideLeft
                boxParent.set(boxParent[idx], theBox, anim)
//                boxParent[idx] = theBox
            }.add()
            group {
                val state = State(0)
                Radiobutton("hello", "goodbye", "yes", "no", state = state).add()
                slider.add()
                text("blink three times when u feel it kicking in")
                Button(text = "select 3").onClick { state.value = 2; true }.add()
            }
        }
        group(alignment = Align(mode = Align.Mode.Vertical)) {
            text("wrapping text") {
                setFont(FontFamily::boldItalic)
                onRightClick {
                    popup(Text("right click menu"))
                }
            }
            text(
                "when am i gonna stop being wise for my age and just start being wise when am i gonna gonna stop being a pretty younger things to guys\n\n" +
                        "when will i stop being??\n" +
                        "they all say that it gets better, it gets better but what if i dont :(", visibleSize = 420f by 0f
            )
            text("i am some text that has been limited, so at some point i will stop showing up and i will just be cut off, which is a pretty handy feature.", limited = true, visibleSize = 400f by 12f)
            BoxedTextInput(post = "px").add()
            Button(text = "rec").onClick { keyBinder?.record(bind) { (this[0] as Text).text = bind.keysToString { window.getKeyName(it, -1) } }; false }.add().also {
                it.radii(0f, 12f, 6f, 2f)
            }
            group(size = Vec2(300f, 80f), alignment = Align(padEdges = Vec2(4f, 4f), main = Align.Content.SpaceEvenly, cross = Align.Content.SpaceEvenly)) {
                block(60f by 30f, color = PolyColor.Gradient(rgba(255, 255, 255), rgba(0, 0, 0), PolyColor.Gradient.Type.LeftToRight))
                block(40f by 30f, color = PolyColor.Gradient(rgba(255, 255, 255), rgba(0, 0, 0), PolyColor.Gradient.Type.TopLeftToBottomRight))
                block(40f by 30f, color = PolyColor.Gradient(rgba(255, 255, 255), rgba(0, 0, 0), PolyColor.Gradient.Type.TopToBottom))
                block(60f by 30f, color = PolyColor.Gradient(rgba(255, 255, 255), rgba(0, 0, 0), PolyColor.Gradient.Type.BottomLeftToTopRight))
                block(60f by 30f, color = PolyColor.Gradient(rgba(255, 255, 255), rgba(0, 0, 0), PolyColor.Gradient.Type.Box(12f, 20f)))
                block(40f by 30f)
                block(40f by 30f)
                block(60f by 30f)
            }
        }
        DraggingNumericTextInput(pre = "Width", suffix = "px").add()
        boxedNumericInput.add()
        Image("https://upload.wikimedia.org/wikipedia/commons/4/47/PNG_transparency_demonstration_1.png".image(), size = Vec2(280f, 210f)).add()
    }.also { it.master.rawRescaleSize = true }.open(window)
}
