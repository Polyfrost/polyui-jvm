/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

package org.polyfrost.polyui.operations

import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.unit.Vec2

class Move<S : Drawable>(
    drawable: S,
    x: Float = drawable.x,
    y: Float = drawable.y,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : DrawableOp.Animatable<S>(drawable, animation, onFinish) {
    constructor(drawable: S, at: Vec2, add: Boolean = true, animation: Animation? = null, onFinish: (S.() -> Unit)? = null) :
        this(drawable, at.x, at.y, add, animation, onFinish)

    private val ox = self.x
    private val oy = self.y
    private val tx = if (add) x else x - ox
    private val ty = if (add) y else y - oy

    override fun apply(value: Float) {
        self.x = ox + (tx * value)
        self.y = oy + (ty * value)
    }
}

class Fade<S : Drawable>(
    drawable: S,
    alpha: Float,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : DrawableOp.Animatable<S>(drawable, animation, onFinish) {
    private val ia = self.alpha
    private val ta = if (add) alpha else alpha - ia
    override fun apply(value: Float) {
        self.alpha = ia + (ta * value)
    }
}

class Rotate<S : Drawable>(
    drawable: S,
    angleRad: Double,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : DrawableOp.Animatable<S>(drawable, animation, onFinish) {
    private val ir = self.rotation
    private val tr = if (add) angleRad else angleRad - ir

    override fun apply(value: Float) {
        self.rotation = ir + (tr * value)
    }
}

class Resize<S : Drawable>(
    drawable: S,
    width: Float = drawable.size.x,
    height: Float = drawable.size.y,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : DrawableOp.Animatable<S>(drawable, animation, onFinish) {
    constructor(drawable: S, size: Vec2, add: Boolean = true, animation: Animation? = null, onFinish: (S.() -> Unit)? = null) :
        this(drawable, size.x, size.y, add, animation, onFinish)

    private val ow = self.size.x
    private val oh = self.size.y
    private val tw = if (add) width else width - ow
    private val th = if (add) height else height - oh

    override fun apply(value: Float) {
        self.size.x = ow + (tw * value)
        self.size.y = oh + (th * value)
        self.clipDrawables()
    }
}

class Scale<S : Drawable>(
    drawable: S,
    scaleX: Float,
    scaleY: Float,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : DrawableOp.Animatable<S>(drawable, animation, onFinish) {
    private val sx = self.scaleX
    private val sy = self.scaleY
    private val tx = if (add) scaleX else scaleX - sx
    private val ty = if (add) scaleY else scaleY - sy

    override fun apply(value: Float) {
        self.scaleX = sx + (tx * value)
        self.scaleY = sy + (ty * value)
    }
}

class Skew<S : Drawable>(
    drawable: S,
    skewX: Double,
    skewY: Double,
    add: Boolean = true,
    animation: Animation? = null,
    onFinish: (S.() -> Unit)? = null,
) : DrawableOp.Animatable<S>(drawable, animation, onFinish) {
    private val sx = self.skewX
    private val sy = self.skewY
    private val tx = if (add) skewX else skewX - sx
    private val ty = if (add) skewY else skewY - sy

    override fun apply(value: Float) {
        self.skewX = sx + (tx * value)
        self.skewY = sy + (ty * value)
    }
}

/**
 * Changes the color of the component to the specified color.
 * Supports animation and callback function on finish.
 *
 * If the color is a [PolyColor.Gradient], this component's color will be changed to a gradient between the current color and the specified color.
 *
 * If the color is a [PolyColor.Chroma], this component's color will be changed to a chroma color, keeping its current hue for consistency.
 * **Note:** This will look wierd if the current color is a gradient, and the brightness, saturation and alpha are NOT animated, so will change instantly.
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
    private val onFinish: (S.() -> Unit)? = null,
) : DrawableOp(drawable) {
    private val hueToReturnTo = self.color.hue
    private val reset: Boolean

    init {
        var shouldReset = false
        val color = self.color
        when (toColor) {
            is PolyColor.Gradient -> {
                if (color !is PolyColor.Gradient || color.type != toColor.type) {
                    self.color = PolyColor.Gradient(color, color.clone(), toColor.type)
                }
                color as PolyColor.Gradient
                color.recolor(0, toColor[0], animation)
                color.recolor(1, toColor[1], animation)
            }

            is PolyColor.Chroma -> {
                if (color !is PolyColor.Chroma) {
                    self.color = PolyColor.Chroma(toColor.speedNanos, toColor.brightness, toColor.saturation, toColor.alpha, color.hue)
                } else {
                    color.speedNanos = toColor.speedNanos
                    color.brightness = toColor.brightness
                    color.saturation = toColor.saturation
                    color.alpha = toColor.alpha
                }
            }

            else -> {
                if (color is PolyColor.Gradient) {
                    color.recolor(0, toColor, animation)
                    color.recolor(1, toColor, animation)
                    shouldReset = true
                } else {
                    if (color is PolyColor.Chroma) {
                        self.color = color.freeze()
                    }
                    color.recolor(toColor, animation)
                }
            }
        }
        reset = shouldReset
    }

    override fun apply() {
        self.color.update(self.polyUI.delta)
        self.needsRedraw = true
    }

    @Suppress("unchecked_cast")
    override fun unapply(): Boolean {
        return if (self.color.updating) {
            false
        } else {
            if (reset) {
                self.color = toColor.toAnimatable()
                self.color.hue = hueToReturnTo
            }
            onFinish?.invoke(this as S)
            true
        }
    }
}
