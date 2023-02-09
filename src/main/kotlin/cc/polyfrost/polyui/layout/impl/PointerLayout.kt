/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

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
    protected val self = layout
    override val at: Point<Unit> = self.at
    override var sized: Vec2<Unit>? = self.sized
    override var needsRedraw = self.needsRedraw
    override var needsRecalculation = self.needsRecalculation
    override val children = self.children
    override val components = self.components

    override fun reRenderIfNecessary() = self.reRenderIfNecessary()
    override fun preRender() = self.preRender()

    override fun render() = self.preRender()
    override fun postRender() = self.postRender()
    override fun addComponent(drawable: Drawable) = self.addComponent(drawable)
    override fun removeComponentNow(drawable: Drawable) = self.removeComponentNow(drawable)
    override fun removeComponent(drawable: Drawable) = self.removeComponent(drawable)
    override fun calculateBounds() {
        self.calculateBounds()
        if (sized == null) {
            sized = self.sized
        }
        needsRecalculation = false
    }

    override fun canBeRemoved(): Boolean = self.canBeRemoved()
    override fun onAll(onChildLayouts: Boolean, function: Component.() -> kotlin.Unit) =
        self.onAll(onChildLayouts) { function() }

    override fun isInside(x: Float, y: Float): Boolean = self.isInside(x, y)
    override fun getSize(): Vec2<Unit>? = self.getSize()

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        self.setup(renderer, polyUI)
    }
}
