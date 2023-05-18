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

@Suppress("OVERRIDE_BY_INLINE")
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
    val ptr = layout

    final override inline var polyui: PolyUI
        get() = ptr.polyui
        set(value) { ptr.polyui = value }
    final override inline var renderer: Renderer
        get() = ptr.renderer
        set(value) { ptr.renderer = value }
    final override inline var simpleName: String
        get() = ptr.simpleName
        set(value) {
            ptr.simpleName = value
        }
    override var refuseFramebuffer: Boolean
        get() = ptr.refuseFramebuffer
        set(value) { ptr.refuseFramebuffer = value }

    final override inline var fbo: Framebuffer?
        get() = ptr.fbo
        set(value) {
            if (refuseFramebuffer) ptr.fbo = value
        }
    final override inline val removeQueue: ArrayList<Drawable> get() = ptr.removeQueue
    final override inline val eventHandlers: MutableMap<Events, Drawable.() -> Boolean>
        get() = ptr.eventHandlers
    final override inline val at: Point<Unit> get() = ptr.at
    final override inline var sized: Vec2<Unit>?
        get() = ptr.sized
        set(value) {
            ptr.sized = value
        }
    final override inline var needsRedraw: Boolean
        get() = ptr.needsRedraw
        set(value) {
            ptr.needsRedraw = value
        }

    final override inline var fboTracker: Int
        get() = ptr.fboTracker
        set(value) { ptr.fboTracker = value }
    final override inline val children get() = ptr.children
    final override inline val components get() = ptr.components
    final override var layout: Layout?
        get() = ptr.layout
        set(value) {
            ptr.layout = value
        }

    override fun reRenderIfNecessary() = ptr.reRenderIfNecessary()
    override fun render() = ptr.render()

    override fun addComponent(drawable: Drawable) = ptr.addComponent(drawable)
    override fun removeComponentNow(drawable: Drawable) = ptr.removeComponentNow(drawable)
    override fun removeComponent(drawable: Drawable) = ptr.removeComponent(drawable)

    override fun calculateBounds() = ptr.calculateBounds()
    override fun debugRender() = ptr.debugRender()
    override fun rescale(scaleX: Float, scaleY: Float) = ptr.rescale(scaleX, scaleY)
    override fun accept(event: Events): Boolean = ptr.accept(event)
    override fun canBeRemoved() = ptr.canBeRemoved()
    override fun debugPrint() = ptr.debugPrint()
    override fun onAll(onChildLayouts: Boolean, function: Component.() -> kotlin.Unit) =
        ptr.onAll(onChildLayouts) { function() }

    override fun isInside(x: Float, y: Float): Boolean = ptr.isInside(x, y)
    override fun getSize(): Vec2<Unit>? = ptr.getSize()

    override fun setup(renderer: Renderer, polyui: PolyUI) = ptr.setup(renderer, polyui)
}
