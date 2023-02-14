/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.component.impl.ImageBlock
import cc.polyfrost.polyui.component.impl.Text
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.event.Events.Companion.events
import cc.polyfrost.polyui.layout.Layout.Companion.items
import cc.polyfrost.polyui.layout.impl.FlexLayout
import cc.polyfrost.polyui.layout.impl.PixelLayout
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.renderer.impl.NVGRenderer
import cc.polyfrost.polyui.unit.*
import java.lang.Math.random

private var lastSecond = System.currentTimeMillis()

fun main() {
    val window = GLWindow("Test", 800, 800)
    val things = Array<Drawable>(50) { // creates 50 rectangles with random sizes
        Block(
            at = flex(),
            sized = Size((random() * 40 + 40).px, (random() * 40 + 40).px),
            events = events(
                Events.MouseClicked(0) to {
                    println("Mouse clicked! $it")
                },
                Events.MouseClicked(0, 2) to {
                    println("Mouse double-clicked!")
                },
                Events.MouseClicked(1) to {
                    println("Mouse right-clicked!")
                    true
                }
            )
        )
    }
    val polyUI = PolyUI(
        window.width,
        window.height,
        NVGRenderer(),
        items = items(
            PixelLayout(
                at = 20.px * 570.px,
                items = items(
                    Text(
                        text = "Kotlin...       rainbow!      and image",
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
                        sized = 120.px * 120.px
                    ),
                    Block(
                        properties = BlockProperties(Color.Chroma(5.seconds)),
                        at = 200.px * 30.px,
                        sized = 120.px * 120.px
                    ),
                    ImageBlock(
                        Image("/s.png", 120, 120),
                        at = 380.px * 30.px
                    )
                )
            ).draggable(),
            FlexLayout(
                at = 20.px * 30.px,
                wrap = 80.percent,
                items = things
            )
        )
    )

    window.open(polyUI)
}
