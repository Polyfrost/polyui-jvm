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

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.component.DrawableOp.*
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.seconds

/**
 * Class to represent an operation that can be applied on a component that modifies it in some way.
 *
 * It is applied before and after rendering, for example a [Move], [Resize], or [Rotate] operation, or a transition.
 */
abstract class DrawableOp(protected open val drawable: Drawable) {
    open val animation: Animation? = null

    /** apply this drawable operation.
     *
     * the renderer is provided in case you want to do some transformations. A state will already be [pushed][cc.polyfrost.polyui.renderer.Renderer.push] for you.
     *
     * **please note that this is NOT intended** to be used directly for rendering of objects, and only for transformations.
     */
    abstract fun apply(renderer: Renderer)

    /**
     * de-apply this operation, if required.
     */
    open fun unapply(renderer: Renderer) {
        // no-op
    }

    /** update the underlying animation, if present. */
    open fun update(deltaTimeNanos: Long) {
        animation?.update(deltaTimeNanos)
    }

    open val isFinished
        get() = animation?.isFinished ?: true

    /**
     * Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.component.Component.resize], [rotate][cc.polyfrost.polyui.component.Component.rotateBy], or [translate][cc.polyfrost.polyui.component.Component.moveBy] instead of this class.
     */
    class Move(
        to: Vec2<Unit>,
        private val add: Boolean = true,
        drawable: Drawable,
        type: Animations? = null,
        durationNanos: Long = 1L.seconds
    ) : DrawableOp(drawable) {
        override val animation = type?.create(durationNanos, 0f, 1f)

        // targets are the amounts to move by, so if add is false the resultant coord is `to`, if it is true then it is current + to. they are done using x = x + targetx
        private val targetx = if (add) to.a.px - drawable.x else to.a.px
        private val targety = if (add) to.b.px - drawable.y else to.b.px

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                if (add) {
                    drawable.x += animation.value * targetx
                    drawable.y += animation.value * targety
                } else {
                    drawable.x = animation.value * targetx
                    drawable.y = animation.value * targety
                }
            } else {
                drawable.x = targetx
                drawable.y = targety
            }
        }
    }

    /**
     * Abstract class representing a persistent drawable operation.
     *
     * A persistent operation is one that remains active indefinitely and does not finish.
     *
     * These are special as they are not taken into account during removal of a drawable.
     *
     * @param drawable The drawable object to apply the operation on.
     * @since 0.19.2
     */
    abstract class Persistent(drawable: Drawable) : DrawableOp(drawable) {
        override fun update(deltaTimeNanos: Long) {
            // nop
        }
        override val isFinished get() = false
    }

    /**
     * A scissor operation that can be applied to a Drawable.
     *
     * @param drawable The Drawable to which the scissor operation will be applied.
     * @param size The size of the scissor operation. If not provided, the size of the drawable will be used.
     * @since 0.19.2
     */
    class Scissor(
        drawable: Drawable,
        private val at: Vec2<Unit>,
        size: Vec2<Unit>? = null
    ) : Persistent(drawable) {
        private val size = size ?: drawable.size!!
        override fun apply(renderer: Renderer) = renderer.pushScissor(at.a.px, at.b.px, size.a.px, size.b.px)

        override fun unapply(renderer: Renderer) = renderer.popScissor()
    }

    class Fade(
        private val amount: Float,
        add: Boolean = true,
        drawable: Drawable,
        type: Animations? = null,
        durationNanos: Long = 1L.seconds
    ) : DrawableOp(drawable) {
        override val animation = type?.create(durationNanos, drawable.alpha, if (add) drawable.alpha + amount else amount)
        override fun apply(renderer: Renderer) {
            if (animation != null) {
                drawable.alpha = animation.value
            } else {
                drawable.alpha = amount
            }
        }
    }

    /**
     * Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.component.Component.resize], [rotate][cc.polyfrost.polyui.component.Component.rotateBy], or [translate][cc.polyfrost.polyui.component.Component.moveTo] instead of this class.
     */
    class Rotate(
        private val angle: Double,
        add: Boolean = true,
        drawable: Drawable,
        type: Animations? = null,
        durationNanos: Long = 1L.seconds
    ) :
        DrawableOp(drawable) {
        override val animation =
            type?.create(
                durationNanos,
                drawable.rotation.toFloat(),
                if (add) (drawable.rotation + angle).toFloat() else angle.toFloat()
            )

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                drawable.rotation = animation.value.toDouble()
            } else {
                drawable.rotation += angle
            }
        }
    }

    class Resize(
        private val toSize: Vec2<Unit>,
        drawable: Drawable,
        animation: Animation.Type?,
        durationNanos: Long = 1L.seconds
    ) :
        DrawableOp(drawable) {
        override val animation = animation?.create(durationNanos, 0f, 1f)

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                val p = animation.value
                drawable.width = toSize.a.px * p
                drawable.height = toSize.b.px * p
            } else {
                drawable.width = toSize.a.px
                drawable.height = toSize.b.px
            }
        }
    }

    class Scale(
        private val scaleX: Float,
        private val scaleY: Float,
        add: Boolean = true,
        drawable: Drawable,
        animation: Animations? = null,
        durationNanos: Long = 1L.seconds
    ) : DrawableOp(drawable) {
        override val animation = animation?.create(durationNanos, 0f, 1f)
        private val tx = if (add) drawable.scaleX + scaleX else scaleX
        private val ty = if (add) drawable.scaleY + scaleY else scaleY

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                val p = animation.value
                drawable.scaleX = tx * p
                drawable.scaleY = ty * p
            } else {
                drawable.scaleX = scaleX
                drawable.scaleY = scaleY
            }
        }
    }

    class Skew(
        private val skewX: Double,
        private val skewY: Double,
        add: Boolean = true,
        drawable: Drawable,
        animation: Animations? = null,
        durationNanos: Long = 1L.seconds
    ) : DrawableOp(drawable) {
        override val animation = animation?.create(durationNanos, 0f, 1f)
        private val tx = if (add) drawable.skewX + skewX else skewX
        private val ty = if (add) drawable.skewY + skewY else skewY

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                val p = animation.value.toDouble()
                drawable.skewX = tx * p
                drawable.skewY = ty * p
            } else {
                drawable.skewX = skewX
                drawable.skewY = skewY
            }
        }
    }
}
