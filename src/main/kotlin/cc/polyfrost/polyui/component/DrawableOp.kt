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
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.component.DrawableOp.*
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import org.jetbrains.annotations.ApiStatus

/**
 * Class to represent an operation that can be applied on a component that modifies it in some way.
 *
 * It is applied before and after rendering, for example a [Translate], [Scale], or [Rotate] operation, or a transition.
 */
abstract class DrawableOp(protected open val drawable: Drawable) {
    abstract val animation: Animation?

    /** apply this drawable operation.
     *
     * the renderer is provided in case you want to do some transformations.
     *
     * **please note that this is NOT intended** to be used directly for rendering of objects, and only for transformations.
     */
    abstract fun apply(renderer: Renderer)

    /** optionally de-apply this operation, if required.
     *
     * The renderer is provided in case you want to revert any transformations that you did.
     *
     * **It is extremely important that any transformations, such as scale, are undone in this method.**
     */
    open fun unapply(renderer: Renderer) {
        // no-op
    }

    /** update the underlying animation, if present. */
    fun update(deltaTimeMillis: Long) {
        animation?.update(deltaTimeMillis)
    }

    open val isFinished
        get() = animation?.isFinished ?: true

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.component.Component.scale], [rotate][cc.polyfrost.polyui.component.Component.rotate], or [translate][cc.polyfrost.polyui.component.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Translate(
        private val x: Float, private val y: Float, drawable: Drawable,
        type: Animations? = null, durationMillis: Long = 1000L,
    ) : DrawableOp(drawable) {
        override val animation = type?.create(durationMillis, drawable.x, drawable.x + x)
        private val yAnim = type?.create(durationMillis, drawable.y, drawable.y + y)
        override fun apply(renderer: Renderer) {
            if (animation != null) {
                drawable.at.a.px += animation.value
                drawable.at.b.px += yAnim!!.value
            } else {
                drawable.at.a.px += x
                drawable.at.b.px += y
            }
        }
    }

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.component.Component.scale], [rotate][cc.polyfrost.polyui.component.Component.rotate], or [translate][cc.polyfrost.polyui.component.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Scale(
        private val x: Float, private val y: Float, override val drawable: Component,
        type: Animations? = null, durationMillis: Long = 1000L,
    ) : DrawableOp(drawable) {
        override val animation = type?.create(durationMillis, drawable.scaleX, drawable.scaleX + x)
        private val yAnim = type?.create(durationMillis, drawable.scaleY, drawable.scaleY + y)
        override fun apply(renderer: Renderer) {
            if (animation != null) {
                drawable.scaleX = animation.value
                drawable.scaleY = yAnim!!.value
            } else {
                drawable.scaleX = x
                drawable.scaleY = y
            }
        }
    }

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.component.Component.scale], [rotate][cc.polyfrost.polyui.component.Component.rotate], or [translate][cc.polyfrost.polyui.component.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Rotate(
        private val angle: Double,
        override val drawable: Component,
        type: Animations? = null,
        durationMillis: Long = 1000L
    ) :
        DrawableOp(drawable) {
        override val animation =
            type?.create(durationMillis, drawable.rotation.toFloat(), (drawable.rotation + angle).toFloat())

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                drawable.rotation = animation.value.toDouble()
            } else {
                drawable.rotation += angle
            }
        }
    }

    @ApiStatus.Internal
    class Resize(
        private val toSize: Vec2<Unit>,
        drawable: Drawable,
        animation: Animation.Type?,
        durationMillis: Long = 1000L
    ) :
        DrawableOp(drawable) {
        override val animation = animation?.create(durationMillis, drawable.sized!!.a.px, toSize.a.px)
        private val animation2 = animation?.create(durationMillis, drawable.sized!!.b.px, toSize.b.px)

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                drawable.sized!!.a.px = animation.value
                drawable.sized!!.b.px = animation2!!.value
            } else {
                drawable.sized!!.a.px = toSize.a.px
                drawable.sized!!.b.px = toSize.b.px
            }
        }
    }
}

