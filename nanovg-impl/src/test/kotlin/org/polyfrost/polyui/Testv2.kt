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

package org.polyfrost.polyui

import org.polyfrost.polyui.component.events
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.component.withStates
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.renderer.impl.GLFWWindow
import org.polyfrost.polyui.renderer.impl.NVGRenderer
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.image
import kotlin.random.Random

fun main() {
    val window = GLFWWindow("PolyUI Test v2", 800, 500)
    val renderer = NVGRenderer(800f by 500f)
    val polyUI =
        PolyUI(
            renderer = renderer,
            drawables =
            arrayOf(
                Image(PolyImage("polyfrost.png")),
                Text("text.dark", font = PolyUI.defaultFonts.medium, fontSize = 20f),
                Group(
                    children =
                    arrayOf(
                        Button(leftImage = PolyImage("moon.svg")),
                        Button(leftImage = PolyImage("face-wink.svg"), text = "button.text"),
                        Switch(size = 28f),
                        Checkbox(size = 28f)
                    ),
                ),
                Dropdown(
                    entries =
                    arrayOf(
                        null to "tomato",
                        null to "orange",
                        null to "banana",
                        null to "lime",
                    ),
                ),
                Block(
                    children =
                    arrayOf(
                        Text("Title:"),
                        TextInput(),
                        Block(
                            children =
                            arrayOf(
                                Text("px"),
                            ),
                        ),
                    ),
                ),
                Group(
                    visibleSize = Vec2(350f, 120f),
                    children =
                    Array(30) {
                        Block(size = Vec2(32f + (Random.nextFloat() * 100f), 32f)).withStates()//.onInit { color.makeChroma() }
                    },
                ),
                Group(
                    children =
                    arrayOf(
                        Button(leftImage = PolyImage("shuffle.svg"), text = "button.randomize").events {
                            Event.Mouse.Clicked(0) then { _ ->
                                val it = parent!!.parent!![5]
                                it.children?.shuffle()
                                it.recalculateChildren()
                            }
                        },
                        Button("minus.svg".image()),
                        Button("plus.svg".image()).events {
                            Event.Mouse.Clicked(0) then {
                                parent!!.parent!![5] = Group(
                                    *Array(30) {
                                        Block(size = Vec2(32f + (Random.nextFloat() * 100f), 32f)).withStates()
                                    },
                                    visibleSize = Vec2(350f, 120f),
                                )
                            }
                        },
                    ),
                ),
                Group(
                    children =
                    arrayOf(
//                        Radiobutton(
//                            entries = arrayOf(
//                                null to "hello",
//                                null to "goodbye"
//                            )
//                        ),
                        Slider(),
                        Text("blink three times when u feel it kicking in")
                    )
                ),
            ),
        )

    // window.setIcon("icon.png")
    window.open(polyUI)
}
