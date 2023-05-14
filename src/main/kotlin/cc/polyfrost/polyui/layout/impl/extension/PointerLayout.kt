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
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2

/** layout that points to another layout. */
open class PointerLayout(
    layout: Layout
) : Layout(
    layout.at,
    layout.sized,
    layout.onAdded,
    layout.onRemoved,
    layout.acceptsInput,
    layout.resizesChildren,
    layout
) {
    protected val ptr = layout

    override var polyui: PolyUI
        get() = ptr.polyui
        set(value) { ptr.polyui = value }
    override var renderer: Renderer
        get() = ptr.renderer
        set(value) { ptr.renderer = value }
    override var simpleName: String
        get() = super.simpleName
        set(value) {
            super.simpleName = value
        }
    override var refuseFramebuffer: Boolean
        get() = ptr.refuseFramebuffer
        set(value) { ptr.refuseFramebuffer = value }

    override var fbo: Framebuffer?
        get() = ptr.fbo
        set(value) {
            if (refuseFramebuffer) ptr.fbo = value
        }

    override val removeQueue: ArrayList<Drawable> get() = ptr.removeQueue

    override fun debugPrint() = ptr.debugPrint()
    override val eventHandlers: MutableMap<Events, Drawable.() -> Boolean>
        get() = ptr.eventHandlers

    override fun debugRender() = ptr.debugRender()

    override fun rescale(scaleX: Float, scaleY: Float) = ptr.rescale(scaleX, scaleY)

    override fun accept(event: Events): Boolean = ptr.accept(event)
    override val at: Point<Unit> get() = ptr.at
    override var sized: Vec2<Unit>?
        get() = ptr.sized
        set(value) {
            ptr.sized = value
        }
    override var needsRedraw: Boolean
        get() = ptr.needsRedraw
        set(value) {
            ptr.needsRedraw = value
        }
    override var needsRecalculation
        get() = ptr.needsRecalculation
        set(value) {
            ptr.needsRecalculation = value
        }
    override val children get() = ptr.children
    override val components get() = ptr.components
    override var layout: Layout?
        get() = ptr.layout
        set(value) {
            ptr.layout = value
        }

    override fun reRenderIfNecessary() = ptr.reRenderIfNecessary()
    override fun preRender() = ptr.preRender()
    override fun render() = ptr.preRender()
    override fun postRender() = ptr.postRender()

    override fun addComponent(drawable: Drawable) = ptr.addComponent(drawable)
    override fun removeComponentNow(drawable: Drawable) = ptr.removeComponentNow(drawable)

    override fun removeComponent(drawable: Drawable) = ptr.removeComponent(drawable)
    override fun calculateBounds() = ptr.calculateBounds()

    override fun canBeRemoved(): Boolean = ptr.canBeRemoved()
    override fun onAll(onChildLayouts: Boolean, function: Component.() -> kotlin.Unit) =
        ptr.onAll(onChildLayouts) { function() }

    override fun isInside(x: Float, y: Float): Boolean = ptr.isInside(x, y)
    override fun getSize(): Vec2<Unit>? = ptr.getSize()

    override fun setup(renderer: Renderer, polyui: PolyUI) = ptr.setup(renderer, polyui)
}
