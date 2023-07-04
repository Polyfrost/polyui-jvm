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
import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.cl1
import cc.polyfrost.polyui.utils.fastRemoveIf
import cc.polyfrost.polyui.utils.toRadians

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
    var acceptsInput: Boolean = true
) : Cloneable {
    val eventHandlers = HashMap<Events, ((Events, Drawable) -> Boolean)>()

    /**
     * This is the name of this drawable, and it will be consistent over reboots of the program, so you can use it to get drawables from a layout by ID, e.g:
     *
     * `val text = myLayout["Text@4cf777e8"] as Text`
     */
    open var simpleName = "${this::class.simpleName}@${Integer.toHexString(this.hashCode())}"

    /** size of this drawable. */
    abstract var size: Size<Unit>?
    lateinit var renderer: Renderer
    lateinit var polyui: PolyUI

    /**
     * This is the initialization stage of this drawable.
     * @see PolyUI.INIT_COMPLETE
     * @see PolyUI.INIT_NOT_STARTED
     * @see PolyUI.INIT_SETUP
     * @since 0.19.0
     */
    var initStage = INIT_NOT_STARTED
        internal set

    /** weather or not the mouse is currently over this drawable. DO NOT modify this value. It is managed automatically by [cc.polyfrost.polyui.event.EventManager]. */
    var mouseOver = false
        internal set

    /**
     * Reference to the layout encapsulating this drawable.
     * For components, this is never null, but for layout, it can be null (meaning its parent is the polyui).
     *
     * In pretty much every situation it is safe to `!!` this, as any layout you create will have parent of [PolyUI.master]. If you are doing `this.layout.layout` or deeper, you should null-check.
     */
    abstract val layout: Layout?

    inline var x get() = at.a.px
        set(value) {
            at.a.px = value
            trueX = trueX()
        }

    inline var y get() = at.b.px
        set(value) {
            at.b.px = value
            trueY = trueY()
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
    var trueX = 0f

    /** true Y value (i.e. not relative to the layout) */
    var trueY = 0f

    /**
     * Calculate the true X value (i.e. not relative to the layout)
     *
     * This method will NOT assign the value to the [trueX] field.
     * @since 0.19.0
     */
    open fun trueX(): Float {
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
     *
     * This method will NOT assign the value to the [trueY] field.
     * @since 0.19.0
     */
    open fun trueY(): Float {
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

    protected val operations: ArrayList<Pair<DrawableOp, (Drawable.() -> kotlin.Unit)?>> = ArrayList(5)

    /** current rotation of this drawable (radians). */
    var rotation: Double = 0.0

    /** current skew in x dimension of this drawable (radians). */
    var skewX: Double = 0.0

    /** current skew in y dimension of this drawable (radians). */
    var skewY: Double = 0.0

    /** current scale in x dimension of this drawable. */
    var scaleX: Float = 1f

    /** current scale in y dimension of this drawable. */
    var scaleY: Float = 1f

    /** **a**t **c**ache **x** for transformations. */
    var acx = 0f

    /** **a**t **c**ache **y** for transformations. */
    var acy = 0f

    /**
     * pre-render functions, such as applying transforms.
     * In this method, you should set needsRedraw to true if you have something to redraw for the **next frame**.
     * @param deltaTimeNanos the time in nanoseconds since the last frame. Use this for animations. It is the same as [PolyUI.delta].
     *
     * **make sure to call super [Drawable.preRender]!**
     */
    open fun preRender(deltaTimeNanos: Long) {
        if (initStage != INIT_COMPLETE) throw IllegalStateException("${this.simpleName} was attempted to be rendered before it was fully initialized (parent ${this.layout?.simpleName}; stage $initStage)")
        renderer.push()
        operations.fastRemoveIf { (it, func) ->
            it.update(deltaTimeNanos)
            return@fastRemoveIf if (!it.isFinished) {
                wantRedraw()
                it.apply(renderer)
                false
            } else {
                if (it is DrawableOp.Rotate) {
                    if (rotation > 6.28 && rotation < 6.29) { // roughly 360 degrees, resets the rotation
                        rotation = 0.0
                    }
                }
                func?.invoke(this)
                true
            }
        }
        if (rotation != 0.0) {
            renderer.translate(x + width / 2f, y + height / 2f)
            renderer.rotate(rotation)
            renderer.translate(-(width / 2f), -(height / 2f))
            acx = x
            acy = y
            x = 0f
            y = 0f
        }
        if (skewX != 0.0) renderer.skewX(skewX)
        if (skewY != 0.0) renderer.skewY(skewY)
        if (scaleX != 1f || scaleY != 1f) renderer.scale(scaleX, scaleY)
    }

    /** draw script for this drawable. */
    abstract fun render()

    /**
     * Called after rendering, for functions such as removing transformations.
     *
     * **make sure to call super [Drawable.postRender]!**
     */
    open fun postRender() {
        renderer.pop()
        if (rotation != 0.0) {
            x = acx
            y = acy
        }
    }

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
        trueX = trueX()
        trueY = trueY()
    }

    /** function that should return true if it is ready to be removed from its parent.
     *
     * This is used for drawables that need to wait for an animation to finish before being removed.
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
        return eventHandlers[event]?.let { it(event, this) } ?: false
    }

    @OverloadResolutionByLambdaReturnType
    protected fun addHandler(event: Events, handler: Drawable.() -> Boolean) {
        val lambda = { _: Events, drawable: Drawable ->
            drawable.handler()
            true
        }
        eventHandlers[event] = lambda
    }

    @JvmName("addhandler")
    @OverloadResolutionByLambdaReturnType
    protected fun addHandler(event: Events, handler: Drawable.() -> kotlin.Unit) {
        val lambda = { _: Events, drawable: Drawable ->
            drawable.handler()
            true
        }
        eventHandlers[event] = lambda
    }

    @OverloadResolutionByLambdaReturnType
    protected fun addEventHandler(event: Events, handler: (Events, Drawable) -> Boolean) {
        eventHandlers[event] = handler
    }

    @JvmName("addEventhandler")
    @OverloadResolutionByLambdaReturnType
    protected fun addEventHandler(event: Events, handler: (Events, Drawable) -> kotlin.Unit) {
        val lambda = { e: Events, drawable: Drawable ->
            handler(e, drawable)
            true
        }
        eventHandlers[event] = lambda
    }

    /** Use this function to reset your drawable's [PolyText][cc.polyfrost.polyui.input.PolyTranslator] if it is using one. */
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
     * This function is called when initialization of a drawable is complete, so it has been fully calculated.
     *
     * This function is called once, and only once.
     * @see onParentInitComplete
     * @see INIT_COMPLETE
     * @since 0.19.0
     */
    open fun onInitComplete() {
        trueX = trueX()
        trueY = trueY()
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
    public override fun clone(): Drawable = throw CloneNotSupportedException("Cloning is not supported for ${this.simpleName}!")

    /**
     * Implement this function to return the size of this drawable, if no size is specified during construction.
     *
     * This should be so that if the drawable can determine its own size (for example, it is an image), then the size parameter in the constructor can be omitted using:
     *
     * `size: Vec2<cc.polyfrost.polyui.unit.Unit>? = null;` and this method **needs** to be implemented!
     *
     * Otherwise, the size parameter in the constructor must be specified.
     * @throws UnsupportedOperationException if this method is not implemented, and the size parameter in the constructor is not specified. */
    open fun calculateSize(): Size<Unit>? = null

    /**
     * Function that is called when the colors attached to this drawable change.
     *
     * If this is a layout, the colors will change, and all its children and components will be notified and updated accordingly.
     *
     * **Make sure to call** [super.onColorsChanged()][onColorsChanged] if you override this!
     * @see Colors
     * @see Component.recolor
     * @since 0.17.5
     */
    abstract fun onColorsChanged(colors: Colors)

    /** Add a [DrawableOp] to this drawable. */
    open fun addOperation(drawableOp: DrawableOp, onFinish: (Drawable.() -> kotlin.Unit)? = null) {
        wantRedraw()
        operations.add(drawableOp to onFinish)
    }

    /**
     * Scale this drawable by the given amount, in the X and Y dimensions.
     *
     * Please note that this ignores all bounds, and will simply scale this drawable, meaning that it can be clipped by its layout, and overlap nearby drawable.
     *
     * @since 0.17.4
     */
    fun scaleBy(
        xFactor: Float,
        yFactor: Float,
        animation: Animation.Type? = null,
        durationNanos: Long = 1.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null
    ) {
        addOperation(DrawableOp.Scale(xFactor, yFactor, true, this, animation, durationNanos), onFinish)
    }

    /**
     * Scale this drawable to the given amount, in the X and Y dimensions.
     *
     * Please note that this ignores all bounds, and will simply scale this drawable, meaning that it can be clipped by its layout, and overlap nearby drawable.
     *
     * @since 0.17.4
     */
    fun scaleTo(
        xFactor: Float,
        yFactor: Float,
        animation: Animation.Type? = null,
        durationNanos: Long = 1.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null
    ) {
        addOperation(DrawableOp.Scale(xFactor, yFactor, false, this, animation, durationNanos), onFinish)
    }

    /**
     * Rotate this drawable to the given amount, in degrees. The amount is MOD 360 to return a value between 0-360 always.
     *
     * Please note that this ignores all bounds, and will simply scale this drawable, meaning that it can be clipped by its layout, and overlap nearby drawable.
     * @see rotateBy
     */
    fun rotateTo(
        degrees: Double,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null
    ) {
        addOperation(
            DrawableOp.Rotate(degrees.toRadians(), false, this, animation, durationNanos),
            onFinish
        )
    }

    /**
     * Rotate this drawable by the given amount, in degrees.
     *
     * Please note that this ignores all bounds, and will simply scale this drawable, meaning that it can be clipped by its layout, and overlap nearby drawable.
     * @see rotateTo
     */
    fun rotateBy(
        degrees: Double,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null
    ) {
        addOperation(
            DrawableOp.Rotate(degrees.toRadians(), true, this, animation, durationNanos),
            onFinish
        )
    }

    /**
     * Skew this drawable to the given amount, in degrees.
     *
     * Please note that this ignores all bounds, and will simply scale this drawable, meaning that it can be clipped by its layout, and overlap nearby drawable.
     * @see skewBy
     */
    fun skewTo(skewX: Double, skewY: Double, animation: Animation.Type?, durationNanos: Long) {
        addOperation(DrawableOp.Skew(skewX.toRadians(), skewY.toRadians(), false, this, animation, durationNanos))
    }

    /**
     * Skew this drawable by the given amount, in degrees.
     *
     * Please note that this ignores all bounds, and will simply scale this drawable, meaning that it can be clipped by its layout, and overlap nearby drawable.
     * @see skewTo
     */
    fun skewBy(skewX: Double, skewY: Double, animation: Animation.Type?, durationNanos: Long) {
        addOperation(DrawableOp.Skew(skewX.toRadians(), skewY.toRadians(), true, this, animation, durationNanos))
    }

    /**
     * move this drawable to the provided [point][to].
     *
     * Please note that this ignores all bounds, and will simply scale this drawable, meaning that it can be clipped by its layout, and overlap nearby drawable.
     *
     * @see moveBy
     */
// todo this might change ^
    fun moveTo(
        to: Vec2<Unit>,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null
    ) {
        doDynamicSize(to)
        addOperation(DrawableOp.Move(to, false, this, animation, durationNanos), onFinish)
    }

    /**
     * move this drawable by the given amount.
     *
     * Please note that this ignores all bounds, and will simply scale this drawable, meaning that it can be clipped by its layout, and overlap nearby drawable.
     *
     * @see moveTo
     */
    fun moveBy(
        by: Vec2<Unit>,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null
    ) {
        doDynamicSize(by)
        addOperation(DrawableOp.Move(by, true, this, animation, durationNanos), onFinish)
    }

    /**
     * Bulk-add drawable operations to this drawable.
     * @see moveTo
     * @see resize
     * @see rotateTo
     * @see skewTo
     * @see animateBy
     * @see DrawableOp
     */
    fun animateTo(
        to: Vec2<Unit>? = null,
        size: Vec2<Unit>? = null,
        degrees: Double = 0.0,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        skewX: Double = 0.0,
        skewY: Double = 0.0,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds
    ) {
        if (to != null) moveTo(to, animation, durationNanos)
        if (size != null) resize(size, animation, durationNanos)
        if (scaleX != 1f || scaleY != 1f) scaleTo(scaleX, scaleY, animation, durationNanos)
        rotateTo(degrees, animation, durationNanos)
        skewTo(skewX, skewY, animation, durationNanos)
    }

    /**
     * Bulk-add drawable operations to this drawable.
     * @see moveBy
     * @see resize
     * @see rotateBy
     * @see skewBy
     * @see animateTo
     * @see DrawableOp
     */
    fun animateBy(
        to: Vec2<Unit>? = null,
        size: Vec2<Unit>? = null,
        degrees: Double = 0.0,
        scaleX: Float = 1f,
        scaleY: Float = 1f,
        skewX: Double = 0.0,
        skewY: Double = 0.0,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds
    ) {
        if (to != null) moveBy(to, animation, durationNanos)
        if (size != null) resize(size, animation, durationNanos)
        if (scaleX != 1f || scaleY != 1f) scaleBy(scaleX, scaleY, animation, durationNanos)
        rotateBy(degrees, animation, durationNanos)
        skewBy(skewX, skewY, animation, durationNanos)
    }

    /** resize this drawable to the given size. */
    fun resize(
        toSize: Size<Unit>,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null
    ) {
        doDynamicSize(toSize)
        addOperation(DrawableOp.Resize(toSize, this, animation, durationNanos), onFinish)
    }

    fun wantRedraw() {
        layout?.needsRedraw = true
    }
}
