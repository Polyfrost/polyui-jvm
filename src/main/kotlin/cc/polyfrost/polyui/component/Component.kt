/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.fastRemoveIf

/**
 * A component is a drawable object that can be interacted with. <br>
 *
 * It has a [properties] attached to it, which contains various pieces of
 * information about how this component should look, and its default responses
 * to event.
 */
abstract class Component @JvmOverloads constructor(
    val properties: Properties,
    /** position relative to this layout. */
    override val at: Point<Unit>,
    override var sized: Size<Unit>? = null,
    acceptInput: Boolean = true,
    vararg events: Events.Handler
) : Drawable(acceptInput) {
    protected val animations: ArrayList<Pair<Animation, (Component.() -> kotlin.Unit)?>> = ArrayList()
    protected val operations: ArrayList<Pair<DrawableOp, (Component.() -> kotlin.Unit)?>> = ArrayList()

    /** current rotation of this component (radians). */
    var rotation: Double = 0.0

    // needed for translation when rotating
    private var atCache: Pair<Float, Float>? = null
    val color: Color.Mutable = properties.color.toMutable()
    protected var sizedBySelf = false
    protected var finishColorFunc: (Component.() -> kotlin.Unit)? = null
    final override lateinit var layout: Layout
    open lateinit var boundingBox: Box<Unit>

    init {
        events.forEach {
            addEventHook(it.event, it.handler)
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
     */
    fun scale(
        xFactor: Float,
        yFactor: Float,
        animation: Animation.Type? = null,
        durationNanos: Long = 1.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) = resize((width * xFactor).px * (height * yFactor).px, animation, durationNanos, onFinish)

    /**
     * Rotate this component by the given amount, in degrees.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     */
    fun rotate(
        degrees: Double,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        addOperation(DrawableOp.Rotate(Math.toRadians(degrees), this, animation, durationNanos), onFinish)
        if (atCache != null) return
        atCache = at.a.px to at.b.px
        at.a.px = -width / 2f
        at.b.px = -height / 2f
    }

    /**
     * move this component by the given amount, in the X and Y dimensions.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     */
// todo this might change ^
    fun move(
        to: Vec2<Unit>,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        doDynamicSize(to)
        addOperation(DrawableOp.Move(to, this, animation, durationNanos), onFinish)
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

    open fun animate(animation: Animation, onFinish: (Component.() -> kotlin.Unit)? = null) {
        animations.add(animation to onFinish)
    }

    open fun recolor(
        toColor: Color,
        animation: Animation.Type,
        durationNanos: Long,
        onFinish: (Component.() -> kotlin.Unit)? = null
    ) {
        color.recolor(toColor, animation, durationNanos)
        finishColorFunc = onFinish
    }

    override fun calculateBounds() {
        if (sized == null) {
            sized = if (properties.size != null) {
                properties.size!!.clone()
            } else {
                sizedBySelf = true
                getSize()
                    ?: throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
            }
        }
        doDynamicSize()

        boundingBox = Box(at, sized!!).expand(properties.padding)
    }

    fun wantRedraw() {
        layout.needsRedraw = true
        layout.clock.getDelta()
    }

    /**
     * Called before rendering.
     *
     * **make sure to call super [Component.preRender]!**
     */
    override fun preRender(deltaTimeNanos: Long) {
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
                    if (rotation > 6.283 && rotation < 6.284) { // roughly 360 degrees, resets the rotation
                        at.a.px = atCache!!.first
                        at.b.px = atCache!!.second
                        atCache = null
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
            renderer.translate(atCache!!.first + width / 2f, atCache!!.second + height / 2f)
            renderer.rotate(rotation)
        }
    }

    /**
     * Called after rendering.
     *
     * **make sure to call super [Component.postRender]!**
     */
    override fun postRender() {
        if (rotation != 0.0) {
            renderer.rotate(-rotation)
            renderer.translate(-(atCache!!.first + width / 2f), -(atCache!!.second + height / 2f))
        }
        operations.fastEach { (it, _) ->
            it.unapply(renderer)
        }
    }

    override fun canBeRemoved(): Boolean {
        return animations.size == 0 && operations.size == 0 && !color.updating
    }

    override fun isInside(x: Float, y: Float): Boolean {
        return if (atCache == null) {
            super.isInside(x, y)
        } else {
            val tx = atCache!!.first + layout.at.a.px
            val ty = atCache!!.second + layout.at.b.px
            x >= tx && x <= tx + this.sized!!.a.px && y >= ty && y <= ty + this.sized!!.b.px
        }
    }
}
