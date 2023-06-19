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
import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.animate.KeyFrames
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastRemoveIf
import cc.polyfrost.polyui.utils.toRadians

/**
 * A component is a drawable object that can be interacted with. <br>
 *
 * It has a [properties] attached to it, which contains various pieces of
 * information about how this component should look, and its default responses
 * to event.
 */
abstract class Component @JvmOverloads constructor(
    properties: Properties? = null,
    /** position relative to this layout. */
    override val at: Point<Unit>,
    override var size: Size<Unit>? = null,
    acceptInput: Boolean = true,
    vararg events: Events.Handler
) : Drawable(acceptInput) {
    protected val animations: ArrayList<Pair<Animation, (Component.() -> kotlin.Unit)?>> = ArrayList(5)
    protected val operations: ArrayList<Pair<DrawableOp, (Component.() -> kotlin.Unit)?>> = ArrayList(5)

    /** current rotation of this component (radians). */
    var rotation: Double = 0.0

    /** current skew in x dimension of this component (radians). */
    var skewX: Double = 0.0

    /** current skew in y dimension of this component (radians). */
    var skewY: Double = 0.0

    /** current scale in x dimension of this component. */
    var scaleX: Float = 1f

    /** current scale in y dimension of this component. */
    var scaleY: Float = 1f

    /** **a**t **c**ache **x** for transformations. */
    var acx = 0f

    /** **a**t **c**ache **y** for transformations. */
    var acy = 0f

    @PublishedApi
    internal var p: Properties? = properties
        private set

    /** properties for the component. This is `open` so you can cast it, like so:
     * ```
     * final override val properties: YourProperties
     *     get() = super.properties as YourProperties
     * ```
     * @see Properties
     */
    open val properties get() = p!!

    /** the color of this component. */
    lateinit var color: Color.Mutable
    protected var autoSized = false
    protected var finishColorFunc: (Component.() -> kotlin.Unit)? = null
    final override lateinit var layout: Layout
    protected open lateinit var boundingBox: Box<Unit>
    protected var keyframes: KeyFrames? = null

    init {
        events.forEach {
            addEventHandler(it.event, it.handler)
        }
    }

    override fun accept(event: Events): Boolean {
        if (super.accept(event)) return true
        return properties.eventHandlers[event]?.let { it(this) } == true
    }

    /** Add a [DrawableOp] to this component. */
    open fun addOperation(drawableOp: DrawableOp, onFinish: (Component.() -> kotlin.Unit)? = null) {
        wantRedraw()
        operations.add(drawableOp to onFinish)
    }

    /**
     * Scale this component by the given amount, in the X and Y dimensions.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     *
     * @since 0.17.4
     */
    fun scaleBy(
        xFactor: Float,
        yFactor: Float,
        animation: Animation.Type? = null,
        durationNanos: Long = 1.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        addOperation(DrawableOp.Scale(xFactor, yFactor, true, this, animation, durationNanos), onFinish)
    }

    /**
     * Scale this component to the given amount, in the X and Y dimensions.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     *
     * @since 0.17.4
     */
    fun scaleTo(
        xFactor: Float,
        yFactor: Float,
        animation: Animation.Type? = null,
        durationNanos: Long = 1.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        addOperation(DrawableOp.Scale(xFactor, yFactor, false, this, animation, durationNanos), onFinish)
    }

    /**
     * Rotate this component to the given amount, in degrees. The amount is MOD 360 to return a value between 0-360 always.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     * @see rotateBy
     */
    fun rotateTo(
        degrees: Double,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        addOperation(
            DrawableOp.Rotate(degrees.toRadians(), false, this, animation, durationNanos),
            onFinish
        )
        acx = at.a.px
        acy = at.b.px
    }

    /**
     * Rotate this component by the given amount, in degrees.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     * @see rotateTo
     */
    fun rotateBy(
        degrees: Double,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        addOperation(
            DrawableOp.Rotate(degrees.toRadians(), true, this, animation, durationNanos),
            onFinish
        )
        acx = at.a.px
        acy = at.b.px
    }

    /**
     * Skew this component to the given amount, in degrees.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     * @see skewBy
     */
    fun skewTo(skewX: Double, skewY: Double, animation: Animation.Type?, durationNanos: Long) {
        addOperation(DrawableOp.Skew(skewX.toRadians(), skewY.toRadians(), false, this, animation, durationNanos))
    }

    /**
     * Skew this component by the given amount, in degrees.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     * @see skewTo
     */
    fun skewBy(skewX: Double, skewY: Double, animation: Animation.Type?, durationNanos: Long) {
        addOperation(DrawableOp.Skew(skewX.toRadians(), skewY.toRadians(), true, this, animation, durationNanos))
    }

    /**
     * move this component to the provided [point][to].
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     *
     * @see moveBy
     */
// todo this might change ^
    fun moveTo(
        to: Vec2<Unit>,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        doDynamicSize(to)
        addOperation(DrawableOp.Move(to, false, this, animation, durationNanos), onFinish)
    }

    /**
     * move this component by the given amount.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     *
     * @see moveTo
     */
    fun moveBy(
        by: Vec2<Unit>,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        doDynamicSize(by)
        addOperation(DrawableOp.Move(by, true, this, animation, durationNanos), onFinish)
    }

    /**
     * Bulk-add drawable operations to this component.
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
        color: Color? = null,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds
    ) {
        if (to != null) moveTo(to, animation, durationNanos)
        if (size != null) resize(size, animation, durationNanos)
        if (scaleX != 1f || scaleY != 1f) scaleTo(scaleX, scaleY, animation, durationNanos)
        rotateTo(degrees, animation, durationNanos)
        skewTo(skewX, skewY, animation, durationNanos)
        if (color != null) recolor(color, animation, durationNanos)
    }

    /**
     * Bulk-add drawable operations to this component.
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
        color: Color? = null,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds
    ) {
        if (to != null) moveBy(to, animation, durationNanos)
        if (size != null) resize(size, animation, durationNanos)
        if (scaleX != 1f || scaleY != 1f) scaleBy(scaleX, scaleY, animation, durationNanos)
        rotateBy(degrees, animation, durationNanos)
        skewBy(skewX, skewY, animation, durationNanos)
        if (color != null) recolor(color, animation, durationNanos)
    }

    /** resize this component to the given size. */
    fun resize(
        toSize: Size<Unit>,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        doDynamicSize(toSize)
        addOperation(DrawableOp.Resize(toSize, this, animation, durationNanos), onFinish)
    }

    open fun addAnimation(animation: Animation, onFinish: (Component.() -> kotlin.Unit)? = null) {
        animations.add(animation to onFinish)
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        acx = at.a.px
        acy = at.b.px
    }

    /**
     * recolor this component's color.
     */
    open fun recolor(
        toColor: Color,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        color.recolor(toColor, animation, durationNanos)
        wantRedraw()
        finishColorFunc = onFinish
    }

    /**
     * Function that is called when the colors attached to this component change.
     *
     * **Make sure to call** [super.onColorsChanged()][onColorsChanged] if you override this!
     * @see Colors
     * @see recolor
     * @since 0.17.5
     */
    open fun onColorsChanged(colors: Colors) {
        properties.colors = colors
        recolor(properties.color)
    }

    override fun calculateBounds() {
        if (size == null) {
            size = if (properties.size != null) {
                properties.size!!.clone()
            } else {
                autoSized = true
                calculateSize()
                    ?: throw UnsupportedOperationException("calculateSize() not implemented for ${this::class.simpleName}!")
            }
        }
        doDynamicSize()
        boundingBox = Box(at, size!!).expand(properties.padding)
    }

    fun wantRedraw() {
        layout.needsRedraw = true
    }

    /** change the properties attached to this component.
     * @see Properties
     */
    fun setProperties(properties: Properties) {
        if (polyui.settings.debug) PolyUI.LOGGER.info("{}'s properties set to {}", this.simpleName, properties)
        properties.colors = p!!.colors
        p = properties
        recolor(properties.color, Animations.Linear, 150L.milliseconds)
        wantRedraw()
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        if (p == null) {
            p = polyui.property.get(this)
        } else {
            p!!.colors = polyui.colors
        }
        color = properties.color.toMutable()
    }

    /**
     * pre-render functions, such as applying transforms.
     * In this method, you should set needsRedraw to true if you have something to redraw for the **next frame**.
     * @param deltaTimeNanos the time in nanoseconds since the last frame. Use this for animations. It is the same as [PolyUI.delta].
     *
     * **make sure to call super [Component.preRender]!**
     */
    open fun preRender(deltaTimeNanos: Long) {
        renderer.push()
        if (keyframes != null) {
            if (keyframes!!.update(deltaTimeNanos)) {
                keyframes = null
            } else {
                wantRedraw()
            }
        }
        animations.fastRemoveIf { (it, func) ->
            it.update(deltaTimeNanos)
            return@fastRemoveIf if (!it.isFinished) {
                wantRedraw()
                false
            } else {
                func?.invoke(this)
                true
            }
        }
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
        if (color.update(deltaTimeNanos)) {
            finishColorFunc?.invoke(this)
            finishColorFunc = null
        }
        if (color.updating || color.alwaysUpdates) wantRedraw()
        if (rotation != 0.0) {
            renderer.translate(at.a.px + size!!.a.px / 2f, at.b.px + size!!.b.px / 2f)
            renderer.rotate(rotation)
            renderer.translate(-(size!!.a.px / 2f), -(size!!.b.px / 2f))
            at.a.px = 0f
            at.b.px = 0f
        }
        if (skewX != 0.0) renderer.skewX(skewX)
        if (skewY != 0.0) renderer.skewY(skewY)
        if (scaleX != 1f || scaleY != 1f) renderer.scale(scaleX, scaleY)
    }

    /**
     * Called after rendering, for functions such as removing transformations.
     *
     * **make sure to call super [Component.postRender]!**
     */
    open fun postRender() {
        renderer.pop()
        if (rotation != 0.0) {
            at.a.px = acx
            at.b.px = acy
        }
    }

    override fun canBeRemoved(): Boolean = animations.size == 0 && operations.size == 0 && !color.updating

    override fun toString(): String =
        "$simpleName(${trueX}x$trueY, ${size!!.a.px}x${size!!.b.px}${if (autoSized) " (auto)" else ""}${if (animations.isNotEmpty()) ", animating" else ""}${if (operations.isNotEmpty()) ", operating" else ""})"

    fun addKeyframes(k: KeyFrames) {
        keyframes = k
        wantRedraw()
    }

    /**
     * add a function that is called every [nanos] nanoseconds.
     * @since 0.17.1
     */
    fun every(nanos: Long, repeats: Int = 0, func: Component.() -> kotlin.Unit): Component {
        polyui.every(nanos, repeats) {
            func(this)
        }
        return this
    }
}
