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
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2

/**
 * # Block
 *
 * A simple block component, supporting the full PolyUI API.
 */
open class Block @JvmOverloads constructor(
    properties: Properties = Properties.get<_, Block>(),
    at: Vec2<Unit>,
    sized: Size<Unit>,
    acceptInput: Boolean = true,
    vararg events: Events.Handler
) : Component(properties, at, sized, acceptInput, *events) {
    val props = properties as BlockProperties

    override fun render() {
        if (props.lineThickness == 0f) {
            renderer.drawRect(x, y, width, height, color, props.cornerRadii)
        } else {
            renderer.drawHollowRect(x, y, width, height, color, props.lineThickness, props.cornerRadii)
        }
    }
}
