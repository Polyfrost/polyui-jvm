/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property.impl

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.event.ComponentEvent
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.unit.seconds

open class BlockProperties(override val color: Color = Color.BLACK) : Properties() {
    open val hoverColor = Color(12, 48, 255)
    override val padding: Float = 0F
    open val cornerRadius: Float = 0F

    init {
        addEventHandlers(
            ComponentEvent.MouseEntered to {
                recolor(hoverColor, Animations.EaseInOutQuad, 0.2.seconds)
            },
            ComponentEvent.MouseExited to {
                recolor(properties.color, Animations.EaseInOutQuad, 0.4.seconds)
            },
        )
    }
}