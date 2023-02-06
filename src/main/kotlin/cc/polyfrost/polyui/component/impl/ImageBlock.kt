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
import cc.polyfrost.polyui.property.impl.ImageBlockProperties
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.px

open class ImageBlock @JvmOverloads constructor(
    private val image: Image,
    properties: ImageBlockProperties = Properties.get<_, ImageBlock>(),
    acceptInput: Boolean = true,
    at: Vec2<Unit>,
    vararg events: ComponentEvent.Handler,
) : Component(properties, at, null, acceptInput, *events) {

    override fun render() {
        renderer.drawImage(image, x, y)
    }

    override fun getSize(): Vec2<Unit> {
        if (image.width == null) {
            renderer.initImage(image)
        }
        return Vec2(image.width!!.px, image.height!!.px)
    }
}