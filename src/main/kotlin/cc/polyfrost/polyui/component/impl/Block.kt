/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - simple, easy to use and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.event.ComponentEvent
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
open class Block(
    val props: BlockProperties = Properties.get<_, Block>(),
    at: Vec2<Unit>, size: Size<Unit>,
    acceptInput: Boolean = true,
    vararg events: ComponentEvent.Handler,
) : Component(props, at, size, acceptInput, *events) {

    override fun render() {
        renderer.drawRoundRect(x, y, width, height, color, props.cornerRadius)
    }
}