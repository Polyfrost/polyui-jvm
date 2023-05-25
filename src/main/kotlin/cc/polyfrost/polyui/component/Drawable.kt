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
abstract class Drawable(open var acceptsInput: Boolean = true) {
    open val eventHandlers = mutableMapOf<Events, Drawable.() -> Boolean>()
    open var simpleName = "${this::class.simpleName}@${Integer.toHexString(this.hashCode())}"
    /** position **relative** to the [parents][layout] position. */
    abstract val at: Point<Unit>
    /** size of this drawable. */
    abstract var size: Size<Unit>?
    internal open lateinit var renderer: Renderer
    internal open lateinit var polyui: PolyUI

    /** weather or not the mouse is currently over this component. DO NOT modify this value. It is managed automatically by [cc.polyfrost.polyui.event.EventManager]. */
    var mouseOver = false
        internal set

    /**
     * Reference to the layout encapsulating this drawable.
     * For components, this is never null, but for layout, it can be null (meaning its parent is the polyui)
     */
    @PublishedApi
    internal abstract val layout: Layout?

    inline val x get() = at.a.px
    inline val y get() = at.b.px
    inline val width
        get() = size!!.a.px // ?: throw IllegalStateException("Drawable $simpleName has no size, but should have a size initialized by this point")
    inline val height
        get() = size!!.b.px // ?: throw IllegalStateException("Drawable $simpleName has no size, but should have a size initialized by this point") // note: this hot method kinda needs this not to be here

    /** true X value (i.e. not relative to the layout) */
    inline val trueX get() = this.at.a.px + (layout?.at?.a?.px ?: 0f)

    /** true Y value (i.e. not relative to the layout) */
    inline val trueY get() = this.at.b.px + (layout?.at?.b?.px ?: 0f)

    inline val atType: Unit.Type
        get() = at.type
    inline val sizeType: Unit.Type
        get() = size!!.type

    /** draw script for this drawable. */
    abstract fun render()

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
        size!!.scale(scaleX, scaleY)
    }

    /** function that should return true if it is ready to be removed from its parent.
     *
     * This is used for component that need to wait for an animation to finish before being removed.
     */
    abstract fun canBeRemoved(): Boolean

    /** add a debug render overlay for this drawable. This is always rendered regardless of the layout re-rendering if debug mode is on. */
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

    /** Use this function to reset your component's [PolyText][cc.polyfrost.polyui.input.PolyTranslator] if it is using one. */
    open fun reset() {
        // no-op
    }

    /** give this a renderer reference and a PolyUI reference.
     *
     * You can also use this method to do some calculations (such as text widths) that are not dependent on other sizes.
     *
     * If you need a method that has access to component's sizes, see [here][calculateBounds] or [here][calculateSize] (if you are operating on yourself only).
     */
    internal open fun setup(renderer: Renderer, polyui: PolyUI) {
        this.renderer = renderer
        this.polyui = polyui
    }

    /** debug print for this drawable.*/
    open fun debugPrint() {
        // noop
    }

    /**
     * returns true if the given coordinates are inside this drawable.
     */
    open fun isInside(x: Float, y: Float): Boolean {
        val tx = trueX
        val ty = trueY
        return x >= tx && x <= tx + this.size!!.a.px && y >= ty && y <= ty + this.size!!.b.px
    }


    protected fun doDynamicSize() {
        doDynamicSize(at)
        if (size != null) doDynamicSize(size!!)
    }

    protected fun doDynamicSize(upon: Vec2<Unit>) {
        if (upon.a is Unit.Dynamic) {
            upon.a.set(
                layout?.size?.a
                    ?: throw IllegalStateException("Dynamic unit only work on parents with a set size! ($this)")
            )
        }
        if (upon.b is Unit.Dynamic) {
            upon.b.set(
                layout?.size?.b
                    ?: throw IllegalStateException("Dynamic unit only work on parents with a set size! ($this)")
            )
        }
    }

    /**
     * Implement this function to return the size of this drawable, if no size is specified during construction.
     *
     * This should be so that if the component can determine its own size (for example, it is an image), then the size parameter in the constructor can be omitted using:
     *
     * `size: Vec2<cc.polyfrost.polyui.unit.Unit>? = null;` and this method **needs** to be implemented!
     *
     * Otherwise, the size parameter in the constructor must be specified.
     * @throws UnsupportedOperationException if this method is not implemented, and the size parameter in the constructor is not specified. */
    open fun calculateSize(): Vec2<Unit>? = null
}
