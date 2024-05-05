/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
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
    val renderer = NVGRenderer
    val polyUI = PolyUI(
        Image(PolyImage("polyfrost.png")),
        Text("text.dark", font = PolyUI.defaultFonts.medium, fontSize = 20f),
        Group(
            Button(leftImage = PolyImage("moon.svg")),
            Button(leftImage = PolyImage("face-wink.svg"), text = "button.text"),
            Switch(size = 28f),
            Checkbox(size = 28f),
        ),
        Dropdown("tomato", "orange", "banana", "lime"),
        Block(
            Text("Title:"),
            TextInput(),
            Block(Text("px")),
        ),
        Group(
            *Array(30) {
                Block(size = Vec2(32f + (Random.nextFloat() * 100f), 32f)).withStates() // .onInit { color.makeChroma() }
            },
            visibleSize = Vec2(350f, 120f),
        ),
        Group(
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
        Group(
            Radiobutton("hello", "goodbye"),
            Slider(),
            Text("blink three times when u feel it kicking in"),
        ),
        size = 800f by 500f,
        renderer = renderer,
    )

    // window.setIcon("icon.png")
    window.open(polyUI)
}
