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

package org.polyfrost.polyui.component

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import org.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import org.polyfrost.polyui.PolyUI.Companion.INIT_SETUP
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.event.EventDSLMarker
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.*

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
     * @see org.polyfrost.polyui.layout.impl.FlexLayout - It uses this functionality to make itself larger without respect to the aspect ratio, but its children will.
     * @since 0.19.0
     */
    val rawResize: Boolean = false,
    acceptsInput: Boolean = true,
) : Cloneable {
    @Transient
    val eventHandlers = HashMap<Event, (Drawable.(Event) -> Boolean)>()

    /**
     * This is the name of this drawable, and it will be consistent over reboots of the program, so you can use it to get drawables from a layout by ID, e.g:
     *
     * `val text = myLayout["Text@4cf777e8"] as Text`
     */
    @Transient
    open var simpleName = "${this::class.simpleName}@${Integer.toHexString(this.hashCode())}"

    /** size of this drawable. */
    abstract var size: Size<Unit>?

    @Transient
    lateinit var renderer: Renderer

    @Transient
    lateinit var polyUI: PolyUI

    var acceptsInput = acceptsInput
        get() = exists && field

    /**
     * This is the initialization stage of this drawable.
     * @see PolyUI.INIT_COMPLETE
     * @see PolyUI.INIT_NOT_STARTED
     * @see PolyUI.INIT_SETUP
     * @since 0.19.0
     */
    @Transient
    var initStage = INIT_NOT_STARTED
        internal set

    /** weather or not the mouse is currently over this drawable.
     *
     * **DO NOT** modify this value. It is managed automatically by [org.polyfrost.polyui.event.EventManager].
     * Changing this directly will not cause you to receive events.
     */
    @Transient
    var mouseOver = false
        internal set

    /**
     * weather or not this drawable should be rendered.
     *
     * This is controlled by [Layout.clipDrawables] to save resources by not drawing drawables which cannot be seen.
     *
     * It is only respected by [Layout] by default. This means that if you are calling [render] yourself (which you really shouldn't), you should check this value.
     * @see Layout.render
     * @since 0.21.4
     */
    open var renders = true
        get() = field && exists

    /**
     * Reference to the layout encapsulating this drawable.
     * For components, this is never null, but for layout, it can be null (meaning its parent is the polyui).
     *
     * In pretty much every situation it is safe to `!!` this, as any layout you create will have parent of [PolyUI.master]. If you are doing `this.layout.layout` or deeper, you should null-check.
     */
    abstract val layout: Layout?

    /**
     * If this is `true`, the drawable will be the only drawable that is allowed to be hovered at once.
     *
     * There are essentially two modes for hovering in PolyUI, and by default, components are in the "normal" (`false`) mode, where only one of them can be hovered as once
     * as would be expected, but layouts are in the "group" (`true`) mode, so will still receive the events even if another drawable is hovered, for example scrolling.
     * @since 0.21.3
     */
    abstract var consumesHover: Boolean

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

    /**
     * This flag controls weather this drawable exists, meaning if it accepts events and renders.
     *
     * Additionally:
     * - In layouts, they will display as empty (no drawables) if this is false.
     * - In components, they will be excluded from calculations.
     *
     * @since 0.19.0
     */
    var exists = true

    /** true X value (i.e. not relative to the layout) */
    @Transient
    var trueX = 0f

    /** true Y value (i.e. not relative to the layout) */
    @Transient
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

    @Transient
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

    /**
     * The alpha value of this drawable.
     * @since 0.20.0
     */
    var alpha = 1f

    /** **a**t **c**ache **x** for transformations. */
    @Transient
    @ApiStatus.Internal
    var acx = 0f

    /** **a**t **c**ache **y** for transformations. */
    @Transient
    @ApiStatus.Internal
    var acy = 0f

    /**
     * pre-render functions, such as applying transforms.
     * In this method, you should set needsRedraw to true if you have something to redraw for the **next frame**.
     * @param deltaTimeNanos the time in nanoseconds since the last frame. Use this for animations. It is the same as [PolyUI.delta].
     *
     * **make sure to call super [Drawable.preRender]!**
     */
    @MustBeInvokedByOverriders
    open fun preRender(deltaTimeNanos: Long) {
        if (initStage != INIT_COMPLETE) throw IllegalStateException("${this.simpleName} was attempted to be rendered before it was fully initialized (parent ${this.layout?.simpleName}; stage $initStage)")
        renderer.push()
        operations.fastRemoveIf { (it, func) ->
            it.update(deltaTimeNanos)
            it.apply(renderer)
            return@fastRemoveIf if (!it.isFinished) {
                if (it !is DrawableOp.Persistent) layout?.needsRedraw = true
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
        if (alpha != 1f) renderer.globalAlpha(alpha)
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
        operations.fastEachReversed {
            it.first.unapply(renderer)
        }
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
    // asm: don't check size == 0 as it checked in allAre
    open fun canBeRemoved(): Boolean = operations.allAre { it.first is DrawableOp.Persistent }

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
    @MustBeInvokedByOverriders
    open fun accept(event: Event): Boolean {
        return eventHandlers[event]?.let { it(this, event) } ?: false
    }

    @OverloadResolutionByLambdaReturnType
    @Suppress("UNCHECKED_CAST")
    fun <E : Event, S : Drawable> S.addEventHandler(event: E, handler: (S.(E) -> Boolean)) {
        eventHandlers[event] = handler as Drawable.(Event) -> Boolean
    }

    @JvmName("addEventhandler")
    @Suppress("UNCHECKED_CAST")
    @OverloadResolutionByLambdaReturnType
    fun <E : Event, S : Drawable> S.addEventHandler(event: E, handler: S.(E) -> kotlin.Unit) {
        eventHandlers[event] = {
            handler(this as S, it as E)
            true
        }
    }

    @EventDSLMarker
    fun <S : Drawable> S.events(dsl: EventDSL<S>.() -> kotlin.Unit) {
        EventDSL(this).apply(dsl)
    }

    /** Use this function to reset your drawable's [PolyText][org.polyfrost.polyui.input.Translator] if it is using one. */
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
    open fun setup(renderer: Renderer, polyUI: PolyUI) {
        require(initStage == INIT_NOT_STARTED) { "${this.simpleName} has already been setup!" }
        this.renderer = renderer
        this.polyUI = polyUI
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
     * @return `true` if the given point is inside this drawable.
     * @see intersects
     * @see isInside(Float, Float, Float, Float)
     */
    open fun isInside(x: Float, y: Float): Boolean {
        val tx = trueX
        val ty = trueY
        return x in tx..tx + width && y in ty..ty + height
    }

    /**
     * @return `true` if all the points of this drawable are inside the given box (x, y, width, height).
     * @since 0.21.4
     * @see intersects
     * @see isInside
     */
    open fun isInside(x: Float, y: Float, width: Float, height: Float): Boolean {
        val tx = trueX
        val ty = trueY
        return tx in x..x + width && ty in y..y + height && tx + this.width in x..x + width && ty + this.height in y..y + height
    }

    /**
     * @return `true` if the drawable has at least one point inside the given box (x, y, width, height).
     * @since 0.21.4
     * @see isInside
     */
    open fun intersects(x: Float, y: Float, width: Float, height: Float): Boolean {
        val tx = trueX
        val ty = trueY
        return (x < tx + this.width && tx < x + width) && (y < ty + this.height && ty < y + height)
    }

    fun doDynamicSize() {
        doDynamicSize(at)
        if (size != null) doDynamicSize(size!!)
    }

    /**
     * Return the layout of the highest order (the one inside the [master layout][PolyUI.master]) that contains this drawable.
     *
     * If this is the master layout, it is returned.
     *
     * @since 0.23.2
     */
    fun getContainingLayout(): Layout {
        if (this === polyUI.master) return this
        var layout = if (this is Layout) this else this.layout
        while (layout?.layout?.layout != null) {
            layout = layout.layout
        }
        return layout!!
    }

    protected fun doDynamicSize(upon: Vec2<Unit>) {
        if (upon.a is Unit.Dynamic) {
            upon.a.set(
                layout?.size?.a
                    ?: throw IllegalStateException("Dynamic unit only work on parents with a set size! (${this.simpleName}; parent ${this.layout?.simpleName})"),
            )
        }
        if (upon.b is Unit.Dynamic) {
            upon.b.set(
                layout?.size?.b
                    ?: throw IllegalStateException("Dynamic unit only work on parents with a set size! (${this.simpleName}; parent ${this.layout?.simpleName})"),
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
     * `size: Vec2<org.polyfrost.polyui.unit.Unit>? = null;` and this method **needs** to be implemented!
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
     * @see onFontsChanged
     * @since 0.17.5
     */
    @MustBeInvokedByOverriders
    abstract fun onColorsChanged(colors: Colors)

    /**
     * Function that is called when the fonts attached to this drawable change.
     *
     * If this is a layout, the fonts will change, and all its children and components will be notified and updated accordingly.
     *
     * **Make sure to call** [super.onFontsChanged()][onFontsChanged] if you override this!
     * @see FontFamily
     * @see onColorsChanged
     * @since 0.22.0
     */
    @MustBeInvokedByOverriders
    abstract fun onFontsChanged(fonts: FontFamily)

    /** Add a [DrawableOp] to this drawable. */
    open fun addOperation(drawableOp: DrawableOp, onFinish: (Drawable.() -> kotlin.Unit)? = null) {
        if (this is Layout) {
            needsRedraw = true
        } else {
            layout?.needsRedraw = true
        }
        operations.add(drawableOp to onFinish)
    }

    /**
     * Remove an operation from this drawable.
     */
    fun removeOperation(drawableOp: DrawableOp) {
        operations.fastRemoveIfReversed { it.first == drawableOp }
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
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
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
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
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
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
    ) {
        addOperation(
            DrawableOp.Rotate(degrees.toRadians(), false, this, animation, durationNanos),
            onFinish,
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
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
    ) {
        addOperation(
            DrawableOp.Rotate(degrees.toRadians(), true, this, animation, durationNanos),
            onFinish,
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
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
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
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
    ) {
        doDynamicSize(by)
        addOperation(DrawableOp.Move(by, true, this, animation, durationNanos), onFinish)
    }

    /**
     * Fade this drawable to the given alpha.
     * @see fadeBy
     */
    fun fadeTo(
        alpha: Float,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
    ) {
        addOperation(DrawableOp.Fade(alpha, false, this, animation, durationNanos), onFinish)
    }

    /**
     * Fade this drawable by the given amount.
     * @see fadeTo
     */
    fun fadeBy(
        alpha: Float,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
    ) {
        addOperation(DrawableOp.Fade(alpha, true, this, animation, durationNanos), onFinish)
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
        durationNanos: Long = 1L.seconds,
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
        durationNanos: Long = 1L.seconds,
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
        onFinish: (Drawable.() -> kotlin.Unit)? = null,
    ) {
        doDynamicSize(toSize)
        addOperation(DrawableOp.Resize(toSize, this, animation, durationNanos), onFinish)
    }
}
