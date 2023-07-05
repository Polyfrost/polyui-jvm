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

package cc.polyfrost.polyui

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.animate.keyframed
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.color.DarkTheme
import cc.polyfrost.polyui.color.LightTheme
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.impl.*
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.event.Events.Companion.events
import cc.polyfrost.polyui.input.Keys
import cc.polyfrost.polyui.input.Modifiers
import cc.polyfrost.polyui.input.Modifiers.Companion.mods
import cc.polyfrost.polyui.input.Mouse
import cc.polyfrost.polyui.input.PolyTranslator.Companion.localised
import cc.polyfrost.polyui.layout.Layout.Companion.drawables
import cc.polyfrost.polyui.layout.impl.FlexLayout
import cc.polyfrost.polyui.layout.impl.PixelLayout
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.renderer.impl.NVGRenderer
import cc.polyfrost.polyui.renderer.impl.NoOpRenderer
import cc.polyfrost.polyui.renderer.impl.NoOpWindow
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.rgba
import kotlin.random.Random

fun main() {
    // use the no-op rendering implementation (for profiling of the system code)
    val useNoOp = false

    val window = if (!useNoOp) GLWindow("Test", 800, 800) else NoOpWindow("Test", 800, 800)
    val things = Array<Drawable>(50) { // creates 50 rectangles with random sizes
        Block(
            properties = Properties.brandProperties,
            at = flex(),
            size = Size((Random.Default.nextFloat() * 40f + 40f).px, (Random.Default.nextFloat() * 40f + 40f).px),
            events = events(
                Events.MouseClicked(0) to {
                    println("Mouse clicked! $it")
                    setProperties(Properties.successProperties)
                    rotateBy(120.0, Animations.EaseInOutCubic, .5.seconds)
                },
                Events.MouseClicked(0, 2) to {
                    println("Mouse double-clicked!")
                    setProperties(Properties.warningProperties)
                },
                Events.MouseClicked(1) to {
                    println("Mouse right-clicked!")
                    setProperties(Properties.dangerProperties)
                    true
                }
            )
        )
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
                                                Properties.successProperties,
                                                at = origin,
                                                size = 60.px * 60.px,
                                                events = events(
                                                    Events.MouseClicked(0) to {
                                                        recolor(
                                                            Color.Gradient(
                                                                rgba(1f, 0f, 1f, 1f),
                                                                rgba(0f, 1f, 1f, 1f)
                                                            ),
                                                            Animations.EaseOutExpo,
                                                            2.seconds
                                                        )
                                                    },
                                                    Events.MouseClicked(1) to {
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
                                Properties.brandProperties,
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
                        properties = BlockProperties(
                            Color.Gradient(
                                rgba(1f, 0f, 1f, 1f),
                                rgba(0f, 1f, 1f, 1f)
                            )
                        ),
                        at = 0.px * 30.px,
                        size = 120.px * 120.px,
                        events = events(
                            Events.MouseClicked(0) to {
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
                        properties = BlockProperties(Color.Chroma(5.seconds)),
                        at = 180.px * 30.px,
                        size = 18.percent * 120.px,
                        events = events(
                            Events.MouseClicked(0) to {
                                rotateBy(120.0, Animations.EaseInOutCubic, .5.seconds)
                            }
                        )
                    ),
                    Image(
                        image = PolyImage("/a.png", 120f, 120f),
                        at = 360.px * 30.px,
                        events = events(
                            Events.MouseClicked(0) to {
                                rotateBy(120.0, Animations.EaseOutBump)
                            }
                        )
                    ),
                    Button(
                        at = 0.px * 160.px,
                        leftIcon = PolyImage("/ta.png", 15f, 15f),
                        text = "polyui.button".localised(),
                        rightIcon = PolyImage("/test.jpg", 15f, 15f),
                        events = events(
                            Events.MouseClicked(0) to {
                                rotateBy(120.0, Animations.EaseInOutCubic)
                                polyui.master.getLayout<FlexLayout>(0).shuffle()
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
                        onCheck = { self, checked ->
                            if (checked) {
                                self.layout.changeColors(LightTheme())
                            } else {
                                self.layout.changeColors(DarkTheme())
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
    polyUI.keyBinder.add(key = 'P', mods = mods(Modifiers.LCONTROL)) {
        polyUI.debugPrint()
        return@add
    }
    polyUI.keyBinder.add(Mouse.LEFT_MOUSE, amountClicks = 2, mods = mods(Modifiers.LCONTROL)) {
        println("${polyUI.mouseX} x ${polyUI.mouseY}")
        polyUI.getComponentsIn(polyUI.mouseX - 25f, polyUI.mouseY - 25f, 50f, 50f).fastEach {
            it.recolor(Color(Random.Default.nextFloat(), Random.Default.nextFloat(), Random.Default.nextFloat(), 1f))
        }
    }
    var light = false
    polyUI.keyBinder.add(Keys.F1) {
        if (!light) {
            polyUI.colors = LightTheme()
        } else {
            polyUI.colors = DarkTheme()
        }
        light = !light
    }

    window.open(polyUI)
}
