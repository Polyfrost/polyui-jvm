/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastEach
import kotlin.math.max

/**
 * The most basic layout.
 *
 * Just a container basically, that can infer it's size.
 * */
open class PixelLayout(
    at: Point<Unit>,
    sized: Size<Unit>? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    acceptInput: Boolean = true,
    vararg items: Drawable
) : Layout(at, sized, onAdded, onRemoved, acceptInput, *items) {
    private var hasAdded = false
    private var dyn = false

    init {
        items.forEach {
            if (it.atUnitType() == Unit.Type.Flex || it.atUnitType() == Unit.Type.Grid) {
                // todo make special exceptions that can tell you more verbosely which component is at fault
                throw Exception("Unit type mismatch: Drawable $it does not have a valid unit type for layout: ${this.simpleName} (using ${it.atUnitType()})")
            }
        }
    }

    override fun calculateBounds() {
        if (layout != null) doDynamicSize()
        components.fastEach {
            it.layout = this
            it.calculateBounds()
            it.at.a.px += x
            it.at.b.px += y
        }
        children.fastEach {
            it.layout = this
            it.calculateBounds()
            it.at.a.px += x
            it.at.b.px += y
        }
        if (this.sized == null) {
            sized = getSize()
        }
        needsRecalculation = false
    }

    override fun getSize(): Vec2<Unit> {
        var width = children.maxOfOrNull { (it.x - x) + it.width } ?: 0f
        width = max(width, components.maxOfOrNull { (it.x - x) + it.width } ?: 0f)
        var height = children.maxOfOrNull { (it.y - y) + it.height } ?: 0f
        height = max(height, components.maxOfOrNull { (it.y - y) + it.height } ?: 0f)
        if (width == 0f) throw Exception("unable to infer width of ${this.simpleName}: no sized children or components, please specify a size")
        if (height == 0f) throw Exception("unable to infer height of ${this.simpleName}: no sized children or components, please specify a size")
        return width.px * height.px
    }
}
