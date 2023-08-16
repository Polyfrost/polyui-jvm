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

package org.polyfrost.polyui

import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.animate.keyframed
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.LightTheme
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.Event.Companion.events
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.Modifiers
import org.polyfrost.polyui.input.Modifiers.Companion.mods
import org.polyfrost.polyui.input.Mouse
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.renderer.impl.GLWindow
import org.polyfrost.polyui.renderer.impl.NVGRenderer
import org.polyfrost.polyui.renderer.impl.NoOpRenderer
import org.polyfrost.polyui.renderer.impl.NoOpWindow
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.rgba
import kotlin.random.Random

fun main() {
    // use the no-op rendering implementation (for profiling of the system code)
    val useNoOp = false

    val window = if (!useNoOp) GLWindow("Test", 800, 800) else NoOpWindow("Test", 800, 800)
    val things = Array<Drawable>(50) { // creates 50 rectangles with random sizes
        val block = Block(
            properties = Properties.brandBlock,
            at = flex(),
            size = Size((Random.Default.nextFloat() * 40f + 40f).px, (Random.Default.nextFloat() * 40f + 40f).px),
            events = events(
                MouseClicked(0) to {
                    println("Mouse clicked! $it")
                    setProperties(Properties.successBlock)
                    rotateBy(120.0, Animations.EaseInOutCubic, .5.seconds)
                },
                MouseClicked(0, 2) to {
                    println("Mouse double-clicked!")
                    setProperties(Properties.warningBlock)
                },
                MouseClicked(1) to {
                    println("Mouse right-clicked!")
                    setProperties(Properties.dangerBlock)
                    true
                }
            )
        )
        block
    }
    val polyUI = PolyUI(
        renderer = if (!useNoOp) {
            NVGRenderer(
                window.width.toFloat(),
                window.height.toFloat()
            )
        } else {
            NoOpRenderer(window.width.toFloat(), window.height.toFloat())
        },
        drawables = drawables(
            PixelLayout(
                at = 20.px * 50.percent,
                drawables = drawables(
                    PixelLayout(
                        at = 500.px * 40.px,
                        drawables = drawables(
                            PixelLayout(
                                at = 50.percent * 10.percent,
                                drawables = drawables(
                                    PixelLayout(
                                        at = 50.percent * 10.percent,
                                        drawables = drawables(
                                            Block(
                                                Properties.successBlock,
                                                at = origin,
                                                size = 60.px * 60.px,
                                                events = events(
                                                    MouseClicked(0) to {
                                                        recolor(
                                                            Color.Gradient(
                                                                rgba(1f, 0f, 1f, 1f),
                                                                rgba(0f, 1f, 1f, 1f)
                                                            ),
                                                            Animations.EaseOutExpo,
                                                            2.seconds
                                                        )
                                                    },
                                                    MouseClicked(1) to {
                                                        recolor(rgba(1f, 0f, 1f, 1f), Animations.EaseOutExpo, 2.seconds)
                                                    }
                                                )
                                            )
                                        )
                                    ),
                                    Block(
                                        at = origin,
                                        size = 120.px * 120.px
                                    )
                                )
                            ),
                            Block(
                                Properties.brandBlock,
                                at = origin,
                                size = 240.px * 240.px
                            )
                        )
                    ),
                    Text(
                        text = "polyui.test".localised("rainbow"),
                        fontSize = 32.px,
                        at = origin
                    ),
                    Block(
                        at = 0.px * 30.px,
                        size = 120.px * 120.px,
                        events = events(
                            MouseClicked(0) to {
                                keyframed(2.seconds, Animations.EaseOutExpo) {
                                    20 {
                                        rotation = 20.0
                                        skewX = 30.0
                                    }
                                    32 {
                                        rotation = 35.0
                                    }
                                    50 {
                                        rotation = 180.0
                                        skewY = 20.0
                                        skewX = 20.0
                                    }
                                    100 {
                                        rotation = 0.0
                                        skewX = 0.0
                                        skewY = 0.0
                                    }
                                }
                            }
                        )
                    ),
                    Block(
                        at = 180.px * 30.px,
                        size = 18.percent * 120.px,
                        events = events(
                            MouseClicked(0) to {
                                rotateBy(120.0, Animations.EaseInOutCubic, .5.seconds)
                            }
                        )
                    ),
                    Image(
                        image = PolyImage("/a.png", 120f, 120f),
                        at = 360.px * 30.px,
                        events = events(
                            MouseClicked(0) to {
                                rotateBy(120.0, Animations.EaseOutBump)
                            }
                        )
                    ),
                    Button(
                        at = 0.px * 160.px,
                        left = PolyImage("/ta.png", 15f, 15f),
                        text = "polyui.button".localised(),
                        right = PolyImage("/test.jpg", 15f, 15f),
                        events = events(
                            MouseClicked(0) to {
                                rotateBy(120.0, Animations.EaseInOutCubic)
                                polyUI.master.getLayout<FlexLayout>(0).shuffle()
                            }
                        )
                    ),
                    TextInput(
                        at = 200.px * 160.px,
                        size = 270.px * 40.px
                    ),
                    Slider(
                        at = 0.px * 220.px,
                        size = 50.percent * 30.px
                    ),
                    Checkbox(
                        at = 0.px * 260.px,
                        size = 30.px * 30.px,
                        onCheck = {
                            if (checked) {
                                layout.changeColors(LightTheme())
                            } else {
                                layout.changeColors(DarkTheme())
                            }
                        }
                    )
                )
            ).draggable().background(),
            FlexLayout(
                at = 2.percent * 30.px,
                wrap = 80.percent,
                drawables = things
            ).scrolling(620.px * 300.px)
        )
    )
    polyUI.keyBinder.add(
        KeyBinder.Bind('P', mods = mods(Modifiers.LCONTROL)) {
            polyUI.debugPrint()
            true
        }
    )
    polyUI.keyBinder.add(
        KeyBinder.Bind(mouse = Mouse.LEFT_MOUSE, mods = mods(Modifiers.LCONTROL), durationNanos = 1.seconds) {
            println("${polyUI.mouseX} x ${polyUI.mouseY}")
            polyUI.getComponentsIn(polyUI.mouseX - 25f, polyUI.mouseY - 25f, 50f, 50f).fastEach {
                it.recolor(Color(Random.Default.nextFloat(), Random.Default.nextFloat(), Random.Default.nextFloat(), 1f))
            }
            true
        }
    )
    var light = false
    polyUI.keyBinder.add(
        KeyBinder.Bind(key = Keys.F1) {
            if (!light) {
                polyUI.colors = LightTheme()
            } else {
                polyUI.colors = DarkTheme()
            }
            light = !light
            true
        }
    )

    window.open(polyUI)
}
