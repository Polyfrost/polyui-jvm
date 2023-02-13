/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl.extension

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2

/** layout that points to another layout. */
open class PointerLayout(
    layout: Layout
) : Layout(layout.at, layout.sized, layout.onAdded, layout.onRemoved, layout.acceptsInput, layout) {
    protected val ptr = layout
    override val at: Point<Unit> = ptr.at
    override var sized: Vec2<Unit>? = ptr.sized
    override var needsRedraw = ptr.needsRedraw
    override var needsRecalculation = ptr.needsRecalculation
    override val children = ptr.children
    override val components = ptr.components

    override fun reRenderIfNecessary() = ptr.reRenderIfNecessary()
    override fun preRender() = ptr.preRender()

    override fun render() = ptr.preRender()
    override fun postRender() = ptr.postRender()
    override fun addComponent(drawable: Drawable) = ptr.addComponent(drawable)
    override fun removeComponentNow(drawable: Drawable) = ptr.removeComponentNow(drawable)
    override fun removeComponent(drawable: Drawable) = ptr.removeComponent(drawable)
    override fun calculateBounds() {
        ptr.calculateBounds()
        if (sized == null) {
            sized = ptr.sized
        }
        needsRecalculation = false
    }

    override fun canBeRemoved(): Boolean = ptr.canBeRemoved()
    override fun onAll(onChildLayouts: Boolean, function: Component.() -> kotlin.Unit) =
        ptr.onAll(onChildLayouts) { function() }

    override fun isInside(x: Float, y: Float): Boolean = ptr.isInside(x, y)
    override fun getSize(): Vec2<Unit>? = ptr.getSize()

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        ptr.setup(renderer, polyUI)
    }
}
