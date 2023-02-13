/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property.impl

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.property.Properties

/**
 * @param cornerRadii The corner radii of the block. The order is top-left, top-right, bottom-right, bottom-left.
 */
open class BlockProperties @JvmOverloads constructor(
    override val color: Color = Color.BLACK,
    open val cornerRadii: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)
) : Properties() {
    open val hoverColor = Color(12, 48, 255)
    override val padding: Float = 0F

//    init {
//        addEventHandlers(
//            Events.MouseEntered to {
//                recolor(hoverColor, Animations.EaseInOutQuad, 0.2.seconds)
//            },
//            Events.MouseExited to {
//                recolor(properties.color, Animations.EaseInOutQuad, 0.4.seconds)
//            }
//        )
//    }
}
