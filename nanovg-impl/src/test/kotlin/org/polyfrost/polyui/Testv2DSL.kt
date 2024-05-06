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

import org.polyfrost.polyui.component.events
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.component.withStates
import org.polyfrost.polyui.dsl.polyUI
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.renderer.impl.GLFWWindow
import org.polyfrost.polyui.renderer.impl.NVGRenderer
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.open

fun main() {
    val window = GLFWWindow("PolyUI Test v2 (DSL)", 800, 500)
    polyUI {
        size = 800f by 500f
        renderer = NVGRenderer
        Image("polyfrost.png".image())
        Text("text.dark") {
            fontSize = 20f
        }
        Group {
            Button("moon.svg".image()).add()
            Button("face-wink.svg".image(), "button.text").add()
            Switch(size = 28f).add()
            Checkbox(size = 28f).add()
        }
        Dropdown("tomato", "orange", "banana", "lime").add()
        Block {
            Text("Title:")
            TextInput().add()
            Block {
                Text("px")
            }
        }
        Group {
            repeat(30) {
                Block(size = (32f + (Math.random().toFloat() * 100f)) by 32f).withStates()
            }
            it.visibleSize = 350f by 120f
        }
        Group {
            Button("shuffle.svg".image(), "button.randomize").events {
                Event.Mouse.Clicked(0) then { _ ->
                    val box = parent.parent[5]
                    box.children?.shuffle()
                    box.recalculateChildren()
                }
            }.add()
            Button("minus.svg".image()).add()
            Button("plus.svg".image()).events {
                Event.Mouse.Clicked(0) then {
                    parent.parent[5] = Group(
                        *Array(30) {
                            Block(size = (32f + (Math.random().toFloat() * 100f)) by 32f).withStates()
                        },
                        visibleSize = 350f by 120f,
                    )
                }
            }.add()
            Group {
                Radiobutton("hello", "goodbye").add()
                Slider().add()
                Text("blink three times when u feel it kicking in")
            }
        }
    }.open(window)
}
