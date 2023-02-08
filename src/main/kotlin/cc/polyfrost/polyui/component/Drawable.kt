/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import org.jetbrains.annotations.ApiStatus


/**
 * # Drawable
 * The most basic thing in the PolyUI rendering system.
 *
 * This class is implemented for both [Layout] and [Drawable], and you should use them as bases if you are creating a UI in most cases.
 */
@ApiStatus.Internal
interface Drawable {
    val at: Point<Unit>
    var sized: Size<Unit>?
    val renderer: Renderer
    var onAdded: (Drawable.() -> kotlin.Unit)?
    var onRemoved: (Drawable.() -> kotlin.Unit)?

    /** weather this component should receive mouse event, such as on click, hover, etc. */
    var acceptInput: Boolean

    /**
     * Reference to the layout encapsulating this drawable.
     * For component, this is never null, but for layout, it can be null (meaning its parent is the polyui)
     */
    val layout: Layout?

    val x get() = at.x
    val y get() = at.y
    val width
        get() = sized?.width
            ?: throw IllegalStateException("Drawable $this has no size, but should have a size initialized by this point")
    val height
        get() = sized?.height
            ?: throw IllegalStateException("Drawable $this has no size, but should have a size initialized by this point")

    /** pre-render functions, such as applying transforms. */
    fun preRender()

    /** draw script for this drawable. */
    fun render()

    /** post-render functions, such as removing transforms. */
    fun postRender()

    /**
     * Calculate the position and size of this drawable. Make sure to call [doDynamicSize] in this method to avoid issues with sizing.
     *
     * This method is called once the [layout] is populated for children and component, and when a recalculation is requested.
     *
     * The value of [layout]'s bounds will be updated after this method is called, so **do not** use [layout]'s bounds as an updated value in this method.
     */
    fun calculateBounds()

    /**
     * Method that is called when the physical size of the total window area changes.
     */
    fun rescale(scaleX: Float, scaleY: Float) {
//        at.scale(scaleX, scaleY)
        sized!!.scale(scaleX, scaleY)
    }

    /** function that should return true if it is ready to be removed from its parent.
     *
     * This is used for component that need to wait for an animation to finish before being removed.
     */
    fun canBeRemoved(): Boolean


    /** implement this method to add a debug render overlay for this drawable. */
    fun debugRender() {
        // no-op
    }

    /** implement this method to add a debug print message for this drawable. */
    fun debugPrint() {
        PolyUI.LOGGER.warn("Drawable $this has no debug print method implemented, defaulting to no-op.")
    }

    fun isInside(x: Float, y: Float): Boolean {
        return if (!acceptInput) false
        else x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height
    }

    fun atUnitType(): Unit.Type = at.type()
    fun sizedUnitType(): Unit.Type = sized!!.type()

    fun doDynamicSize() {
        doDynamicSize(at)
        doDynamicSize(sized!!)
    }

    fun doDynamicSize(upon: Vec2<Unit>) {
        if (upon.a is Unit.Dynamic) {
            upon.a.set(
                layout?.sized?.a ?: throw IllegalStateException("Dynamic unit only work on parents with a set size!")
            )
        }
        if (upon.b is Unit.Dynamic) {
            upon.b.set(
                layout?.sized?.b ?: throw IllegalStateException("Dynamic unit only work on parents with a set size!")
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
    fun getSize(): Vec2<Unit>? {
        return null
    }
}