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

import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.events
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.component.withStates
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Recolor
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.impl.GLFWWindow
import org.polyfrost.polyui.renderer.impl.NVGRenderer
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.unit.ms
import org.polyfrost.polyui.utils.image
import kotlin.random.Random

fun main() {
    val window = GLFWWindow("PolyUI Test v2", 800, 500)
    val renderer = NVGRenderer
    val polyUI = PolyUI(
        Image("polyfrost.png"),
        Text("text.dark", font = Font("https://raw.githubusercontent.com/coreyhu/Urbanist/main/fonts/ttf/Urbanist-BlackItalic.ttf"), fontSize = 20f),
        Group(
            Button("moon.svg".image()),
            Button("face-wink.svg".image(), "button.text").events {
                Event.Mouse.Clicked(0, amountClicks = 2) then {
                    Recolor(
                        this,
                        PolyColor(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f),
                        Animations.EaseInOutQuad.create(0.8.ms)
                    ).add()
                }
            },
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
            Button("shuffle.svg".image(), "button.randomize").events {
                Event.Mouse.Clicked then { _ ->
                    val it = parent.parent[5]
                    it.children?.shuffle()
                    it.repositionChildren()
                }
            },
            Button("minus.svg".image()),
            Button("plus.svg".image()).events {
                Event.Mouse.Clicked then {
                    parent.parent[5] = Group(
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

    if (!PolyUI.isOnMac) window.setIcon("icon.png")
    window.open(polyUI)
}
