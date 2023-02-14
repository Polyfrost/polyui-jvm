/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.property.impl.DividerProperties
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit

/**
 * A static divider component.
 */
class Divider @JvmOverloads constructor(
    properties: Properties = Properties.get<_, Divider>(),
    at: Vec2<Unit>,
    val length: Unit,
    val direction: Direction = Direction.Horizontal
) : Component(properties, at, null, false) {
    override fun render() {
        renderer.drawLine(at.x, at.y, at.x + sized!!.width, at.y + sized!!.height, properties.color, (properties as DividerProperties).thickness)
    }

    override fun calculateBounds() {
        sized = getSize()
    }

    override fun getSize(): Vec2<Unit> {
        return when (direction) {
            Direction.Horizontal -> Vec2(length, 0.px)
            Direction.Vertical -> Vec2(0.px, length)
        }
    }
}
