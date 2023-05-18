/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit

/**
 * # Drawable
 * The most basic thing in the PolyUI rendering system.
 *
 * This class is implemented for both [Layout] and [Drawable], and you should use them as bases if you are creating a UI in most cases.
 */
abstract class Drawable(var acceptsInput: Boolean = true) {
    open val eventHandlers = mutableMapOf<Events, Drawable.() -> Boolean>()
    open var simpleName = this.toString().substringAfterLast(".")
    abstract val at: Point<Unit>
    abstract var sized: Size<Unit>?
    internal open lateinit var renderer: Renderer
    internal open lateinit var polyui: PolyUI

    /** weather or not the mouse is currently over this component. DO NOT modify this value. It is managed automatically by [cc.polyfrost.polyui.event.EventManager]. */
    var mouseOver = false
        internal set

    /**
     * Reference to the layout encapsulating this drawable.
     * For components, this is never null, but for layout, it can be null (meaning its parent is the polyui)
     */
    internal abstract val layout: Layout?

    inline val x get() = at.a.px
    inline val y get() = at.b.px
    inline val width
        get() = sized!!.a.px // ?: throw IllegalStateException("Drawable $simpleName has no size, but should have a size initialized by this point")
    inline val height
        get() = sized!!.b.px // ?: throw IllegalStateException("Drawable $simpleName has no size, but should have a size initialized by this point") // note: this hot method kinda needs this not to be here

    inline val atUnitType: Unit.Type
        get() = at.type
    inline val sizedUnitType: Unit.Type
        get() = sized!!.type

    /** pre-render functions, such as applying transforms.
     * In this method, you should set needsRedraw to true if you have something to redraw for the **next frame**.*/
    abstract fun preRender()

    /** draw script for this drawable. */
    abstract fun render()

    /** post-render functions, such as removing transforms. */
    abstract fun postRender()

    /**
     * Calculate the position and size of this drawable. Make sure to call [doDynamicSize] in this method to avoid issues with sizing.
     *
     * This method is called once the [layout] is populated for children and component, and when a recalculation is requested.
     *
     * The value of [layout]'s bounds will be updated after this method is called, so **do not** use [layout]'s bounds as an updated value in this method.
     */
    abstract fun calculateBounds()

    /**
     * Method that is called when the physical size of the total window area changes.
     */
    open fun rescale(scaleX: Float, scaleY: Float) {
        at.scale(scaleX, scaleY)
        sized!!.scale(scaleX, scaleY)
    }

    /** function that should return true if it is ready to be removed from its parent.
     *
     * This is used for component that need to wait for an animation to finish before being removed.
     */
    abstract fun canBeRemoved(): Boolean

    /** implement this method to add a debug render overlay for this drawable. */
    open fun debugRender() {
        // no-op
    }

    /** called when this drawable receives an event.
     *
     * **make sure to call [super.accept()][Drawable.accept]!**
     *
     * @return true if the event should be consumed (cancelled so no more handlers are called), false otherwise.
     * */
    open fun accept(event: Events): Boolean {
        return eventHandlers[event]?.let { it(this) } ?: false
    }

    protected fun addEventHook(event: Events, handler: Drawable.() -> Boolean) {
        this.eventHandlers[event] = handler
    }

    @JvmName("addEventhook")
    protected fun addEventHook(event: Events, handler: Drawable.() -> kotlin.Unit) {
        this.eventHandlers[event] = {
            handler(this)
            true
        }
    }

    /** Use this function to reset your component's [PolyText][cc.polyfrost.polyui.input.PolyTranslator] if it is using one */
    open fun resetText() {
        // no-op
    }

    internal open fun setup(renderer: Renderer, polyui: PolyUI) {
        this.renderer = renderer
        this.polyui = polyui
    }

    /** implement this method to add a debug print message for this drawable. */
    open fun debugPrint() {
        PolyUI.LOGGER.warn("Drawable {} has no debug print method implemented, defaulting to no-op.", this)
    }

    open fun isInside(x: Float, y: Float): Boolean {
        val tx = this.at.a.px + (layout?.at?.a?.px ?: 0f)
        val ty = this.at.b.px + (layout?.at?.b?.px ?: 0f)
        return x >= tx && x <= tx + this.sized!!.a.px && y >= ty && y <= ty + this.sized!!.b.px
    }

    protected fun doDynamicSize() {
        doDynamicSize(at)
        if (sized != null) doDynamicSize(sized!!)
    }

    protected fun doDynamicSize(upon: Vec2<Unit>) {
        if (upon.a is Unit.Dynamic) {
            upon.a.set(
                layout?.sized?.a
                    ?: throw IllegalStateException("Dynamic unit only work on parents with a set size! ($this)")
            )
        }
        if (upon.b is Unit.Dynamic) {
            upon.b.set(
                layout?.sized?.b
                    ?: throw IllegalStateException("Dynamic unit only work on parents with a set size! ($this)")
            )
        }
    }

    /**
     * Implement this function to return the size of this drawable, if no size is specified during construction.
     *
     * This should be so that if the component can determine its own size (for example, it is an image), then the size parameter in the constructor can be omitted using:
     *
     * `sized: Vec2<cc.polyfrost.polyui.unit.Unit>? = null;` and this method **needs** to be implemented!
     *
     * Otherwise, the size parameter in the constructor must be specified.
     * @throws UnsupportedOperationException if this method is not implemented, and the size parameter in the constructor is not specified. */
    open fun getSize(): Vec2<Unit>? = null
}
