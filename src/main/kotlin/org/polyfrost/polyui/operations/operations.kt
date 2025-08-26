/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
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

@file:Suppress("EqualsOrHashCode")

package org.polyfrost.polyui.operations

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.color.asAnimatable
import org.polyfrost.polyui.color.asAnimatableGradient
import org.polyfrost.polyui.color.asMutable
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Scrollable
import org.polyfrost.polyui.component.extensions.getOperationValueOrElse
import org.polyfrost.polyui.component.extensions.getTargetPosition
import org.polyfrost.polyui.component.extensions.getTargetScale
import org.polyfrost.polyui.component.extensions.getTargetSize
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.Vec4
import kotlin.math.PI
import kotlin.math.sin

class Move<S : Component>(
    drawable: S,
    x: Float = drawable.x,
    y: Float = drawable.y,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : ComponentOp.Animatable<S>(drawable, animation, onFinish) {
    constructor(drawable: S, to: Vec2, add: Boolean = true, animation: Animation? = null, onFinish: (S.() -> Unit)? = null) :
            this(drawable, to.x, to.y, add, animation, onFinish)

    private val ox: Float
    private val oy: Float

    init {
        val pos = drawable.getTargetPosition()
        ox = pos.x
        oy = pos.y
    }

    private val tx = if (add) x else x - ox
    private val ty = if (add) y else y - oy

    @get:JvmName("getTarget")
    val target get() = Vec2(ox + tx, oy + ty)

    @get:JvmName("getOrigin")
    val origin get() = Vec2(ox, oy)

    override fun apply(value: Float) {
        if (tx != 0f) self.x = ox + (tx * value)
        if (ty != 0f) self.y = oy + (ty * value)
        if (!self.polyUI.inputManager.mouseDown) self.polyUI.inputManager.recalculate()
    }

    override fun verify() = tx != 0f || ty != 0f
}

class Fade<S : Drawable>(
    drawable: S,
    alpha: Float,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : ComponentOp.Animatable<S>(drawable, animation, onFinish) {
    private val ia: Float = drawable.getOperationValueOrElse<Fade<*>, Float>(drawable.alpha) { it.targetAlpha }

    private val ta = if (add) alpha else alpha - ia
    val targetAlpha get() = ia + ta

    override fun apply(value: Float) {
        self.alpha = ia + (ta * value)
    }

    override fun verify() = ta != 0f
}

class Rotate<S : Drawable>(
    drawable: S,
    angleRad: Double,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : ComponentOp.Animatable<S>(drawable, animation, onFinish) {
    private val ir: Double = drawable.getOperationValueOrElse<Rotate<*>, Double>(drawable.rotation) { it.targetRotation }


    private val tr = if (add) angleRad else angleRad - ir
    val targetRotation get() = ir + tr

    override fun apply(value: Float) {
        self.rotation = ir + (tr * value)
    }

    override fun verify() = tr != 0.0
}

class Resize<S : Component>(
    drawable: S,
    width: Float = drawable.width,
    height: Float = drawable.height,
    add: Boolean = true,
    withVisible: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : ComponentOp.Animatable<S>(drawable, animation, onFinish) {
    constructor(drawable: S, size: Vec2, add: Boolean = true, withVisible: Boolean = true, animation: Animation? = null, onFinish: (S.() -> Unit)? = null) :
            this(drawable, size.x, size.y, add, withVisible, animation, onFinish)

    private val ow: Float
    private val oh: Float

    init {
        val size = drawable.getTargetSize()
        ow = size.x
        oh = size.y
    }

    private val tw = if (add) width else width - ow
    private val th = if (add) height else height - oh

    @get:JvmName("getTarget")
    val target get() = Vec2(ow + tw, oh + th)
    private val withVisible = withVisible && self is Scrollable

    override fun apply(value: Float) {
        val oldW = self.width
        val oldH = self.height
        self.width = ow + (tw * value)
        self.height = oh + (th * value)
        if (withVisible) {
            self as Scrollable
            if (self.hasVisibleSize) {
                val delta = Vec2(self.width - oldW, self.height - oldH)
                self.visibleSize += delta
            }
        }
        self.clipChildren()
    }

    override fun verify() = tw != 0f || th != 0f
}

class Scale<S : Drawable>(
    drawable: S,
    scaleX: Float,
    scaleY: Float,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : ComponentOp.Animatable<S>(drawable, animation, onFinish) {
    private val sx: Float
    private val sy: Float

    init {
        val scale = drawable.getTargetScale()
        sx = scale.x
        sy = scale.y
    }

    private val tx = if (add) scaleX else scaleX - sx
    private val ty = if (add) scaleY else scaleY - sy
    val targetScaleX get() = sx + tx
    val targetScaleY get() = sy + ty

    override fun apply(value: Float) {
        self.scaleX = sx + (tx * value)
        self.scaleY = sy + (ty * value)
    }

    override fun verify() = tx != 0f || ty != 0f
}

class Skew<S : Drawable>(
    drawable: S,
    skewX: Double,
    skewY: Double,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : ComponentOp.Animatable<S>(drawable, animation, onFinish) {
    private val sx: Double
    private val sy: Double

    init {
        sx = drawable.getOperationValueOrElse<Skew<*>, Double>(drawable.skewX) { it.targetSkewX }
        sy = drawable.getOperationValueOrElse<Skew<*>, Double>(drawable.skewY) { it.targetSkewY }
    }

    private val tx = if (add) skewX else skewX - sx
    private val ty = if (add) skewY else skewY - sy
    val targetSkewX get() = sx + tx
    val targetSkewY get() = sy + ty

    override fun apply(value: Float) {
        self.skewX = sx + (tx * value)
        self.skewY = sy + (ty * value)
    }

    override fun verify() = tx != 0.0 || ty != 0.0
}

class ShakeOp<S : Component>(
    drawable: S,
    durationNanos: Long,
    oscillations: Int,
    private val range: Float = 4f,
    onFinish: (S.() -> Unit)? = null
) : ComponentOp.Animatable<S>(drawable, Animations.Linear.create(durationNanos), onFinish) {
    private val start = drawable.x
    private val oscillations = (oscillations * (PI * 2.0)).toFloat()

    override fun apply(value: Float) {
        self.x = start + sin(value * oscillations) * range
    }

    override fun exclusive() = true

    override fun equals(other: Any?) = other is ShakeOp<*> && other.self === self
}

/**
 * Changes the color of the component to the specified color.
 * Supports animation and callback function on finish.
 *
 * If the color is a [PolyColor.Gradient], this component's color will be changed to a gradient between the current color and the specified color.
 *
 *
 * @param toColor The color to change the component to.
 * @param animation The animation to use.
 * @param onFinish The callback function to execute when the color change animation finishes.
 * @since 0.19.1
 */
class Recolor<S : Drawable>(
    drawable: S,
    private val toColor: PolyColor,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : ComponentOp.Animatable<S>(drawable, animation, onFinish) {
    private val hueToReturnTo = self.color.hue
    private val reset: Boolean

    init {
        var shouldReset = false
        when (toColor) {
            is PolyColor.Gradient -> {
                val color = self.color
                val grad = if (color is PolyColor.Gradient) {
                    if (color.type != toColor.type) {
                        PolyColor.Gradient.Animated(color.asAnimatable(), color.color2.asAnimatable(), toColor.type)
                    } else color.asAnimatableGradient()
                } else {
                    val copy = PolyColor.Animated(color.hue, color.saturation, color.brightness, color.alpha)
                    PolyColor.Gradient.Animated(color.asAnimatable(), copy, toColor.type)
                }
                // asm: double duration as animation is used twice
                self.color = grad
                animation?.let { it.durationNanos *= 2L }
                grad.recolor(0, toColor[0], animation)
                grad.recolor(1, toColor[1], animation)
            }

            else -> {
                val color = self.color
                if (color is PolyColor.Gradient) {
                    val grad = color.asAnimatableGradient()
                    self.color = grad
                    animation?.let { it.durationNanos *= 2L }
                    grad.recolor(0, toColor, animation)
                    grad.recolor(1, toColor, animation)
                    shouldReset = true
                } else {
                    self.color = self.color.asAnimatable().recolor(toColor, animation)
                }
            }
        }
        reset = shouldReset
    }

    override fun verify() = toColor != self.color

    override fun apply() {}

    override fun unapply(): Boolean {
        val clr = self.color
        if (clr !is PolyColor.Dynamic) {
            PolyUI.LOGGER.error("cannot recolor a color which does not implement PolyColor.Dynamic. shouldn't have ended up here. this operation will be skipped.")
            return true
        }
        return if (clr.update(self.polyUI.delta)) {
            if (reset) self.color = toColor.asMutable().also { it.hue = hueToReturnTo }
            onFinish?.invoke(self)
            true
        } else {
            self.needsRedraw = true
            false
        }
    }

    override fun apply(value: Float) {}
}

/**
 * Simple scissor operation that scissors all rendering into the given [rectangle].
 * This is completely separate from a component, so a [renderer] needs to be provided.
 *
 * @since 1.11.3
 */
class Scissor(private val rectangle: Vec4, private val renderer: Renderer) : ComponentOp {
    constructor(x: Float, y: Float, width: Float, height: Float, renderer: Renderer) :
            this(Vec4.of(x, y, width, height), renderer)
    override fun apply() {
        val (x, y, w, h) = rectangle
        renderer.pushScissor(x, y, w, h)
    }

    override fun unapply(): Boolean {
        renderer.popScissor()
        return false
    }
}
