/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
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
    layout.size,
    layout.onAdded,
    layout.onRemoved,
    layout.rawResize,
    layout.resizesChildren,
    layout.acceptsInput,
    layout
) {
    val ptr = layout

    final override inline var polyui: PolyUI
        get() = ptr.polyui
        set(value) {
            ptr.polyui = value
        }
    final override inline var renderer: Renderer
        get() = ptr.renderer
        set(value) {
            ptr.renderer = value
        }
    final override inline var simpleName: String
        get() = ptr.simpleName
        set(value) {
            ptr.simpleName = value
        }
    final override inline var acceptsInput: Boolean
        get() = ptr.acceptsInput
        set(value) {
            ptr.acceptsInput = value
        }
    override var refuseFramebuffer: Boolean
        get() = ptr.refuseFramebuffer
        set(value) {
            ptr.refuseFramebuffer = value
        }

    final override inline var fbo: Framebuffer?
        get() = ptr.fbo
        set(value) {
            if (!refuseFramebuffer) ptr.fbo = value
        }
    final override inline val removeQueue: ArrayList<Drawable> get() = ptr.removeQueue
    final override inline val eventHandlers: HashMap<Events, Drawable.() -> Boolean>
        get() = ptr.eventHandlers
    final override inline val at: Point<Unit> get() = ptr.at
    final override inline var size: Vec2<Unit>?
        get() = ptr.size
        set(value) {
            ptr.size = value
        }
    final override inline var needsRedraw: Boolean
        get() = ptr.needsRedraw
        set(value) {
            ptr.needsRedraw = value
        }
    final override inline var fboTracker: Int
        get() = ptr.fboTracker
        set(value) {
            ptr.fboTracker = value
        }

    final override inline var initStage: Int
        get() = ptr.initStage
        set(value) {
            ptr.initStage = value
        }
    final override inline var preRender
        get() = ptr.preRender
        set(value) {
            ptr.preRender = value
        }
    final override inline var postRender
        get() = ptr.postRender
        set(value) {
            ptr.postRender = value
        }

    override var enabled: Boolean
        get() = ptr.enabled
        set(value) {
            ptr.enabled = value
        }
    final override inline val children get() = ptr.children
    final override inline val components get() = ptr.components
    final override var layout: Layout?
        get() = ptr.layout
        set(value) {
            ptr.layout = value
        }

    override fun onInitComplete() = ptr.onInitComplete()

    // asm: don't override reRenderIfNecessary as it is essentially a pointer method anyway
    override fun render() = ptr.render()
    override fun setup(renderer: Renderer, polyui: PolyUI) = ptr.setup(renderer, polyui)
    override fun addComponent(drawable: Drawable) = ptr.addComponent(drawable)
    override fun removeComponentNow(drawable: Drawable?) = ptr.removeComponentNow(drawable)
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
    override fun calculateSize(): Vec2<Unit>? = ptr.calculateSize()
}
