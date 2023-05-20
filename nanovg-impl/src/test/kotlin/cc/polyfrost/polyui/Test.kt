/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.component.impl.Image
import cc.polyfrost.polyui.component.impl.Text
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.event.Events.Companion.events
import cc.polyfrost.polyui.input.Modifiers
import cc.polyfrost.polyui.input.Mouse
import cc.polyfrost.polyui.input.PolyTranslator.Companion.localised
import cc.polyfrost.polyui.layout.Layout.Companion.items
import cc.polyfrost.polyui.layout.impl.FlexLayout
import cc.polyfrost.polyui.layout.impl.PixelLayout
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.renderer.impl.NVGRenderer
import cc.polyfrost.polyui.renderer.impl.NoOpRenderer
import cc.polyfrost.polyui.renderer.impl.NoOpWindow
import cc.polyfrost.polyui.unit.*
import java.lang.Math.random

fun main() {
    // use the no-op rendering implementation (for profiling of the system code)
    val useNoOp = false

    val window = if (!useNoOp) GLWindow("Test", 800, 800) else NoOpWindow("Test", 800, 800)
    val things = Array<Drawable>(50) { // creates 50 rectangles with random sizes
        Block(
            at = flex(),
            sized = Size((random() * 40 + 40).px, (random() * 40 + 40).px),
            events = events(
                Events.MouseClicked(0) to {
                    println("Mouse clicked! $it")
                    rotate(120.0, Animations.EaseInOutCubic)
                },
                Events.MouseClicked(0, 2) to {
                    println("Mouse double-clicked!")
                },
                Events.MouseClicked(1) to {
                    println("Mouse right-clicked!")
                    wantRedraw()
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
        items = items(
            PixelLayout(
                at = 20.px * 570.px,
                items = items(
                    Text(
                        text = "polyui.test".localised("rainbow"),
                        fontSize = 32.px,
                        at = 20.px * 0.px
                    ),
                    Block(
                        properties = BlockProperties(
                            Color.Gradient(
                                Color(1f, 0f, 1f, 1f),
                                Color(0f, 1f, 1f, 1f)
                            )
                        ),
                        at = 20.px * 30.px,
                        sized = 120.px * 120.px,
                        events = events(
                            Events.MouseClicked(0) to {
                                rotate(120.0, Animations.EaseOutExpo)
                            }
                        )
                    ),
                    Block(
                        properties = BlockProperties(Color.Chroma(5.seconds)),
                        at = 200.px * 30.px,
                        sized = 120.px * 120.px,
                        events = events(
                            Events.MouseClicked(0) to {
                                rotate(120.0, Animations.EaseInOutCubic)
                            }
                        )
                    ),
                    Image(
                        PolyImage("/a.png", 120f, 120f),
                        at = 380.px * 30.px,
                        events = events(
                            Events.MouseClicked(0) to {
                                rotate(120.0, Animations.EaseOutBump)
                            }
                        )
                    )
                )
            ).draggable(),
            FlexLayout(
                at = 20.px * 30.px,
                wrap = 80.percent,
                items = things
            ).scrolling(0.px * 300.px)
        )
    )
    polyUI.keyBinder.add(Mouse.LEFT_MOUSE, Modifiers.LCONTROL) {
        println("${polyUI.eventManager.mouseX} x ${polyUI.eventManager.mouseY}")
    }

    window.open(polyUI)
}
