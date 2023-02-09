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
import cc.polyfrost.polyui.event.ComponentEvent
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Box
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.Clock
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.fastRemoveIf

/**
 * A component is a drawable object that can be interacted with. <br>
 *
 * It has a [properties] attached to it, which contains various pieces of
 * information about how this component should look, and its default responses
 * to event.
 */
abstract class Component @JvmOverloads constructor (
    val properties: Properties,
    /** position relative to this layout. */
    override val at: Point<Unit>,
    override var sized: Size<Unit>? = null,
    final override var acceptInput: Boolean = true,
    vararg events: ComponentEvent.Handler,
) : Drawable {
    override var onAdded: (Drawable.() -> kotlin.Unit)? = null
    override var onRemoved: (Drawable.() -> kotlin.Unit)? = null
    private val eventHandlers = mutableMapOf<ComponentEvent, Component.() -> kotlin.Unit>()
    private val animations: ArrayList<Pair<Animation, (Component.() -> kotlin.Unit)?>> = ArrayList()
    private val operations: ArrayList<Pair<DrawableOp, (Component.() -> kotlin.Unit)?>> = ArrayList()
    var scaleX: Float = 1F
    var scaleY: Float = 1F

    /** current rotation of this component (radians). */
    var rotation: Double = 0.0
    val color: Color.Mutable = properties.color.toMutable()
    private var finishColorFunc: (Component.() -> kotlin.Unit)? = null
    private val clock = Clock()
    final override lateinit var renderer: Renderer
    final override lateinit var layout: Layout
    open lateinit var boundingBox: Box<Unit>

    /** weather or not the mouse is currently over this component. DO NOT modify this value. It is managed automatically by [cc.polyfrost.polyui.event.EventManager]. */
    var mouseOver = false
        internal set

    init {
        events.forEach {
            addEventHandler(it.event, it.handler)
        }
    }


    /**
     * Called when an event is received by this component.
     *
     * **make sure to call super [Component.accept]!**
     */
    open fun accept(event: ComponentEvent) {
        properties.eventHandlers[event]?.let { it(this) }
        when (event) {
            is ComponentEvent.Added -> {
                onAdded?.invoke(this)
            }

            is ComponentEvent.Removed -> {
                onRemoved?.invoke(this)
            }

            else -> eventHandlers[event]?.let { it(this) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun addEventHandler(event: ComponentEvent, handler: Component.() -> kotlin.Unit) {
        when (event) {
            is ComponentEvent.Added -> {
                onAdded = handler as Drawable.() -> kotlin.Unit
            }

            is ComponentEvent.Removed -> {
                onRemoved = handler as Drawable.() -> kotlin.Unit
            }

            else -> this.eventHandlers[event] = handler
        }
    }


    /** Add a [DrawableOp] to this component. */
    open fun addOperation(drawableOp: DrawableOp, onFinish: (Component.() -> kotlin.Unit)? = null) {
        operations.add(drawableOp to onFinish)
    }

    /**
     * Scale this component by the given amount, in the X and Y dimensions.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     */
    fun scale(
        byX: Float,
        byY: Float,
        animation: Animation.Type? = null,
        durationMillis: Long = 0L,
        onFinish: (Component.() -> kotlin.Unit)? = null,
    ) {
        addOperation(DrawableOp.Scale(byX, byY, this, animation, durationMillis), onFinish)
    }

    /**
     * Rotate this component by the given amount, in degrees.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     */
    fun rotate(
        degrees: Double,
        animation: Animation.Type? = null,
        durationMillis: Long = 0L,
        onFinish: (Component.() -> kotlin.Unit)? = null,
    ) {
        addOperation(DrawableOp.Rotate(Math.toRadians(degrees), this, animation, durationMillis), onFinish)
    }

    /**
     * move this component by the given amount, in the X and Y dimensions.
     *
     * Please note that this ignores all bounds, and will simply scale this component, meaning that it can be clipped by its layout, and overlap nearby component.
     */
    // todo this might change ^
    fun move(
        byX: Float,
        byY: Float,
        animation: Animation.Type? = null,
        durationMillis: Long = 0L,
        onFinish: (Component.() -> kotlin.Unit)? = null,
    ) {
        addOperation(DrawableOp.Translate(byX, byY, this, animation, durationMillis), onFinish)
    }

    /** resize this component to the given size. */
    fun resize(
        toSize: Size<Unit>,
        animation: Animation.Type? = null,
        durationMillis: Long = 0L,
        onFinish: (Component.() -> kotlin.Unit)? = null,
    ) {
        doDynamicSize(toSize)
        addOperation(DrawableOp.Resize(toSize, this, animation, durationMillis), onFinish)
    }

    open fun animate(animation: Animation, onFinish: (Component.() -> kotlin.Unit)? = null) {
        animations.add(animation to onFinish)
    }

    open fun recolor(
        toColor: Color,
        animation: Animation.Type,
        durationMillis: Long,
        onFinish: (Component.() -> kotlin.Unit)? = null,
    ) {
        color.recolor(toColor, animation, durationMillis)
        finishColorFunc = onFinish
    }

    override fun calculateBounds() {
        if (sized == null) {
            sized = if (properties.size != null) properties.size!!.clone()
            else getSize()
                ?: throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
        }
        doDynamicSize()

        boundingBox = Box(at, sized!!).expand(properties.padding)
    }

    fun wantRedraw() {
        layout.needsRedraw = true
    }

    fun wantRecalculation() {
        layout.needsRecalculation = true
    }

    /**
     * Called before rendering.
     *
     * **make sure to call super [Component.preRender]!**
     */
    override fun preRender() {
        animations.fastRemoveIf { it.first.isFinished.also { b -> if (b) it.second?.invoke(this) } }
        operations.fastRemoveIf { it.first.isFinished.also { b -> if (b) it.second?.invoke(this) } }

        val delta = clock.getDelta()
        animations.fastEach { (it, _) ->
            it.update(delta)
            if (!it.isFinished) wantRedraw()
        }
        operations.fastEach { (it, _) ->
            it.update(delta)
            if (!it.isFinished) wantRedraw()
            it.apply(renderer)
        }
        if (scaleX != 1f && scaleY != 1f) renderer.scale(scaleX, scaleY)
        if (rotation != 0.0) renderer.rotate(rotation)
        if (color.update(delta)) {
            finishColorFunc?.invoke(this)
            finishColorFunc = null
        }
    }

    /**
     * Called after rendering.
     *
     * **make sure to call super [Component.postRender]!**
     */
    override fun postRender() {
        if (scaleX != 1f && scaleY != 1f) renderer.scale(-scaleX, -scaleY)
        if (rotation != 0.0) renderer.rotate(-rotation)
    }

    override fun canBeRemoved(): Boolean {
        return animations.size == 0 && operations.size == 0 && !color.isRecoloring()
    }
}
