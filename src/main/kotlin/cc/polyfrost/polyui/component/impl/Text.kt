/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.event.ComponentEvent
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.property.impl.TextProperties
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.px

open class Text @JvmOverloads constructor(
    properties: Properties = Properties.get("cc.polyfrost.polyui.component.impl.Text"),
    acceptInput: Boolean = false,
    var text: String, val fontSize: Unit.Pixel = 12.px,
    at: Vec2<Unit>, size: Size<Unit>? = null,
    vararg events: ComponentEvent.Handler,
) : Component(properties, at, size, acceptInput, *events) {
    constructor(text: String, fontSize: Unit.Pixel, at: Vec2<Unit>, vararg events: ComponentEvent.Handler) : this(text = text, fontSize = fontSize, at = at, size = null, events = events)

    val props: TextProperties = properties as TextProperties
    var autoSized = false

    override fun render() {
        renderer.drawText(props.font, x, y, if (autoSized) 0f else width, text, props.color, fontSize.get())
    }

    @Suppress("UNCHECKED_CAST")
    override fun getSize(): Vec2<Unit> {
        autoSized = true
        return renderer.textBounds(props.font, text, fontSize.get()) as Vec2<Unit>
    }
}