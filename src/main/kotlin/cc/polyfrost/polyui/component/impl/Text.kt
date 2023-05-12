/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.property.impl.TextProperties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.RenderText
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit

open class Text @JvmOverloads constructor(
    properties: Properties = Properties.get<Text>(),
    text: String,
    fontSize: Unit.Pixel? = null,
    at: Vec2<Unit>,
    size: Size<Unit>? = null,
    textAlign: TextAlign? = null,
    acceptInput: Boolean = false,
    vararg events: Events.Handler
) : Component(properties, at, null, acceptInput, *events) {
    constructor(text: String, fontSize: Unit.Pixel, at: Vec2<Unit>) : this(Properties.get<Text>(), text, fontSize, at) // java accessor

    private val props: TextProperties = properties as TextProperties
    var autoSized = size == null
    private val str = RenderText(text, props.font, fontSize?.px ?: props.fontSize.px, textAlign ?: TextAlign.Left, size ?: origin)
    val lines get() = str.lines
    val full get() = str.full
    val font get() = str.font
    val fontSize get() = str.fontSize
    val textAlign get() = str.textAlign

    override var sized: Size<Unit>?
        get() = str.size
        set(value) {
            if (value != null) {
                str.size.a.px = value.a.px
                str.size.b.px = value.b.px
            }
        }
    var text get() = str.text
        set(value) {
            str.text = value
            if (autoSized) {
                str.calculate(renderer)
                sized = str.size
            }
        }

    override fun render() {
        str.render(at.x, at.y, color)
    }

    operator fun get(index: Int) = str[index]

    fun getByCharIndex(index: Int) = str.getByCharIndex(index)

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        str.calculate(renderer)
        sized = str.size
    }

    override fun getSize(): Vec2<Unit>? {
        str.calculate(renderer)
        return sized
    }
}
