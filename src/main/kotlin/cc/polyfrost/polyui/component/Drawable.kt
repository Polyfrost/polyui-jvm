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

package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import cc.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import cc.polyfrost.polyui.PolyUI.Companion.INIT_SETUP
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.cl1

/**
 * # Drawable
 * The most basic thing in the PolyUI rendering system.
 *
 * This class is implemented for both [Layout] and [Drawable], and you should use them as bases if you are creating a UI in most cases.
 */
abstract class Drawable(
    /** position **relative** to the [parents][layout] position. */
    open val at: Point<Unit>,
    /**
     * This property controls weather this drawable resizes "raw", meaning it will **not** respect the aspect ratio.
     *
     * By default, it is `false`, so when resized, the smallest increase is used for both width and height. This means that it will stay at the same aspect ratio.
     *
     * @see cc.polyfrost.polyui.layout.impl.FlexLayout - It uses this functionality to make itself larger without respect to the aspect ratio, but its children will.
     * @since 0.19.0
     */
    val rawResize: Boolean = false,
    open var acceptsInput: Boolean = true
) : Cloneable {
    open val eventHandlers = HashMap<Events, Drawable.() -> Boolean>()

    /**
     * This is the name of this drawable, and it will be consistent over reboots of the program, so you can use it to get components from a layout by ID, e.g:
     *
     * `val text = myLayout["Text@4cf777e8"] as Text`
     */
    open var simpleName = "${this::class.simpleName}@${Integer.toHexString(this.hashCode())}"

    /** size of this drawable. */
    abstract var size: Size<Unit>?
    open lateinit var renderer: Renderer
    open lateinit var polyui: PolyUI

    /**
     * This is the initialization stage of this drawable.
     * @see PolyUI.INIT_COMPLETE
     * @see PolyUI.INIT_NOT_STARTED
     * @see PolyUI.INIT_SETUP
     * @since 0.19.0
     */
    open var initStage = INIT_NOT_STARTED
        internal set

    /** weather or not the mouse is currently over this component. DO NOT modify this value. It is managed automatically by [cc.polyfrost.polyui.event.EventManager]. */
    var mouseOver = false
        internal set

    /**
     * Reference to the layout encapsulating this drawable.
     * For components, this is never null, but for layout, it can be null (meaning its parent is the polyui)
     */
    @PublishedApi
    internal abstract val layout: Layout?

    inline var x get() = at.a.px
        set(value) {
            at.a.px = value
        }

    inline var y get() = at.b.px
        set(value) {
            at.b.px = value
        }

    inline var width get() = size!!.a.px
        set(value) {
            size!!.a.px = value
        }

    inline var height get() = size!!.b.px
        set(value) {
            size!!.b.px = value
        }

    /** true X value (i.e. not relative to the layout) */
    val trueX get() = trueX()

    /** true Y value (i.e. not relative to the layout) */
    val trueY get() = trueY()

    /**
     * Calculate the true X value (i.e. not relative to the layout)
     * @since 0.19.0
     */
    private fun trueX(): Float {
        var x = this.x
        var parent = this.layout
        while (parent != null) {
            x += parent.x
            parent = parent.layout
        }
        return x
    }

    /**
     * Calculate the true Y value (i.e. not relative to the layout)
     * @since 0.19.0
     */
    // the JVM should branch-predict this, so we can do it like this
    private fun trueY(): Float {
        var y = this.y
        var parent = this.layout
        while (parent != null) {
            y += parent.y
            parent = parent.layout
        }
        return y
    }

    inline val atType: Unit.Type
        get() = at.type
    inline val sizeType: Unit.Type
        get() = size!!.type

    /**
     * Returns `true` if this drawable has a dynamic size or position, or if the size is null.
     */
    inline val isDynamic get() = at.dynamic || size?.dynamic ?: true

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
        if (rawResize) {
            at.scale(scaleX, scaleY)
            size!!.scale(scaleX, scaleY)
        } else {
            val scale = cl1(scaleX, scaleY)
            at.scale(scale, scale)
            size!!.scale(scale, scale)
        }
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

    /**
     * Add event handlers to this drawable.
     * @since 0.18.5
     */
    fun addEventHandlers(vararg handlers: Events.Handler) {
        for (handler in handlers) {
            eventHandlers[handler.event] = handler.handler
        }
    }

    protected fun addEventHandler(event: Events, handler: Drawable.() -> Boolean) {
        eventHandlers[event] = handler
    }

    @JvmName("addEventhandler")
    protected fun addEventHandler(event: Events, handler: Drawable.() -> kotlin.Unit) {
        eventHandlers[event] = { handler(); true }
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
     *
     * this method is called once, and only once.
     *
     * @see INIT_SETUP
     */
    internal open fun setup(renderer: Renderer, polyui: PolyUI) {
        if (initStage != INIT_NOT_STARTED) throw IllegalStateException("${this.simpleName} has already been setup!")
        this.renderer = renderer
        this.polyui = polyui
    }

    /** debug print for this drawable.*/
    open fun debugPrint() {
        // noop
    }

    /**
     * This function is called when initialization of a component is complete, so it has been fully calculated.
     *
     * This function is called once, and only once.
     * @see onParentInitComplete
     * @see INIT_COMPLETE
     * @since 0.19.0
     */
    open fun onInitComplete() {
    }

    /**
     * This function is called when initialization of this parent is complete.
     *
     * This function is called once, and only once.
     * @see onInitComplete
     * @see INIT_COMPLETE
     * @since 0.19.0
     */
    open fun onParentInitComplete() {
    }

    /**
     * returns true if the given coordinates are inside this drawable.
     */
    open fun isInside(x: Float, y: Float): Boolean {
        val tx = trueX
        val ty = trueY
        return x in tx..tx + width && y in ty..ty + height
    }

    fun doDynamicSize() {
        doDynamicSize(at)
        if (size != null) doDynamicSize(size!!)
    }

    protected fun doDynamicSize(upon: Vec2<Unit>) {
        if (upon.a is Unit.Dynamic) {
            upon.a.set(
                layout?.size?.a
                    ?: throw IllegalStateException("Dynamic unit only work on parents with a set size! (${this.simpleName}; parent ${this.layout?.simpleName})")
            )
        }
        if (upon.b is Unit.Dynamic) {
            upon.b.set(
                layout?.size?.b
                    ?: throw IllegalStateException("Dynamic unit only work on parents with a set size! (${this.simpleName}; parent ${this.layout?.simpleName})")
            )
        }
    }

    /**
     * Implement this function to enable cloning of your Drawable.
     *
     * If this function is not implemented, attempts to clone the drawable will not compile due to type erasure, but if this is ignored a [CloneNotSupportedException] will be thrown.
     *
     * @since 0.19.0
     */
    public override fun clone(): Drawable = super.clone() as Drawable

    /**
     * Implement this function to return the size of this drawable, if no size is specified during construction.
     *
     * This should be so that if the component can determine its own size (for example, it is an image), then the size parameter in the constructor can be omitted using:
     *
     * `size: Vec2<cc.polyfrost.polyui.unit.Unit>? = null;` and this method **needs** to be implemented!
     *
     * Otherwise, the size parameter in the constructor must be specified.
     * @throws UnsupportedOperationException if this method is not implemented, and the size parameter in the constructor is not specified. */
    open fun calculateSize(): Size<Unit>? = null
}
