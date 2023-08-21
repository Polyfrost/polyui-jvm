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

import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.DrawableOp.*
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds

/**
 * Class to represent an operation that can be applied on a component that modifies it in some way.
 *
 * It is applied before and after rendering, for example a [Move], [Resize], or [Rotate] operation, or a transition.
 */
abstract class DrawableOp(protected open val drawable: Drawable) {
    open val animation: Animation? = null

    /** apply this drawable operation.
     *
     * the renderer is provided in case you want to do some transformations. A state will already be [pushed][org.polyfrost.polyui.renderer.Renderer.push] for you.
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
     * Note: if you are making a UI, you should probably be using [scale][org.polyfrost.polyui.component.Component.resize], [rotate][org.polyfrost.polyui.component.Component.rotateBy], or [translate][org.polyfrost.polyui.component.Component.moveBy] instead of this class.
     */
    class Move(
        to: Vec2<Unit>,
        add: Boolean = true,
        drawable: Drawable,
        type: Animations? = null,
        durationNanos: Long = 1L.seconds,
    ) : DrawableOp(drawable) {
        override val animation = type?.create(durationNanos, 0f, 1f)
        private val origin = drawable.at.clone()
        private val to = if (add) to else to - origin

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                drawable.x = origin.x + (to.x * animation.value)
                drawable.y = origin.y + (to.y * animation.value)
            } else {
                drawable.x = origin.x + to.x
                drawable.y = origin.y + to.y
            }
            if (drawable is Layout) {
                drawable.ox = drawable.x
                drawable.oy = drawable.y
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

    class Fade(
        private val amount: Float,
        add: Boolean = true,
        drawable: Drawable,
        type: Animations? = null,
        durationNanos: Long = 1L.seconds,
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
     * Note: if you are making a UI, you should probably be using [scale][org.polyfrost.polyui.component.Component.resize], [rotate][org.polyfrost.polyui.component.Component.rotateBy], or [translate][org.polyfrost.polyui.component.Component.moveTo] instead of this class.
     */
    class Rotate(
        private val angle: Double,
        add: Boolean = true,
        drawable: Drawable,
        type: Animations? = null,
        durationNanos: Long = 1L.seconds,
    ) :
        DrawableOp(drawable) {
        override val animation =
            type?.create(
                durationNanos,
                drawable.rotation.toFloat(),
                if (add) (drawable.rotation + angle).toFloat() else angle.toFloat(),
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
        toSize: Vec2<Unit>,
        drawable: Drawable,
        animation: Animation.Type?,
        durationNanos: Long = 1L.seconds,
    ) :
        DrawableOp(drawable) {
        override val animation = animation?.create(durationNanos, 0f, 1f)
        private val origin = drawable.size!!.clone()
        private val to = toSize - origin

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                drawable.width = origin.width + (to.width * animation.value)
                drawable.height = origin.height + (to.height * animation.value)
            } else {
                drawable.width = origin.width + to.width
                drawable.height = origin.height + to.height
            }
        }
    }

    class Scale(
        scaleX: Float,
        scaleY: Float,
        add: Boolean = true,
        drawable: Drawable,
        animation: Animations? = null,
        durationNanos: Long = 1L.seconds,
    ) : DrawableOp(drawable) {
        override val animation = animation?.create(durationNanos, 0f, 1f)
        private val ox = drawable.scaleX
        private val oy = drawable.scaleY
        private val tx = if (add) scaleX else scaleX - ox
        private val ty = if (add) scaleY else scaleY - oy

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                val p = animation.value
                drawable.scaleX = ox + (tx * p)
                drawable.scaleY = oy + (ty * p)
            } else {
                drawable.scaleX = ox + tx
                drawable.scaleY = oy + ty
            }
        }
    }

    class Skew(
        private val skewX: Double,
        private val skewY: Double,
        add: Boolean = true,
        drawable: Drawable,
        animation: Animations? = null,
        durationNanos: Long = 1L.seconds,
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
