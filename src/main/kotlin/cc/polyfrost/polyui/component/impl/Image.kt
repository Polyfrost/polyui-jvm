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
import cc.polyfrost.polyui.property.impl.ImageProperties
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit

open class Image @JvmOverloads constructor(
    private val image: PolyImage,
    properties: ImageProperties = Properties.get<Image>() as ImageProperties,
    acceptInput: Boolean = true,
    at: Vec2<Unit>,
    vararg events: Events.Handler
) : Component(properties, at, null, acceptInput, *events) {
    private val props get() = properties as ImageProperties

    override fun render() {
        renderer.drawImage(image, x, y, width, height, props.cornerRadii, props.color.getARGB())
    }

    override fun getSize(): Vec2<Unit> {
        if (image.width == -1f) {
            renderer.initImage(image)
        }
        return Vec2(image.width.px, image.height.px)
    }
}
