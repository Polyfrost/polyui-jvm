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

package org.polyfrost.polyui.animate

/**
 * # Animation
 *
 * An animation used by the PolyUI system.
 *
 * PolyUI comes with many default [animations][org.polyfrost.polyui.animate.Easing], which you can use. To give the user some choice, you can also use the [Animations.create()][Animation.Type.create] to dynamically create them.
 *
 * check out [DrawableOperation][org.polyfrost.polyui.component.DrawableOp] for more information on how to use animations with components.
 */
abstract class Animation(var durationNanos: Long, var from: Float, var to: Float, var onFinish: (Animation.() -> Unit)? = null) : Cloneable {
    inline val range get() = to - from
    var passedTime = 0f
        protected set
    var isFinished: Boolean = false
        protected set(value) {
            if (field == value) return
            field = value
            if (value) onFinish?.invoke(this)
        }

    fun reset() {
        passedTime = 0f
        isFinished = false
    }

    fun reverse() {
        val temp = from
        from = to
        to = temp
        reset()
    }

    val value: Float
        get() {
            return if (isFinished) {
                to
            } else {
                getValue(passedTime / durationNanos) * range + from
            }
        }

    fun update(deltaTimeNanos: Long): Float {
        if (isFinished) return to
        passedTime += deltaTimeNanos
        isFinished = passedTime + deltaTimeNanos >= durationNanos
        return value
    }

    protected abstract fun getValue(percent: Float): Float

    public abstract override fun clone(): Animation

    enum class Type {
        Linear,
        EaseOutBack, EaseInBack, EaseInOutBack,
        EaseOutBump, EaseInBump, EaseInOutBump,
        EaseOutQuad, EaseInQuad, EaseInOutQuad,
        EaseOutQuart, EaseInQuart, EaseInOutQuart,
        EaseOutSine, EaseInSine, EaseInOutSine,
        EaseOutQuint, EaseInQuint, EaseInOutQuint,
        EaseOutCirc, EaseInCirc, EaseInOutCirc,
        EaseOutExpo, EaseInExpo, EaseInOutExpo,
        EaseOutCubic, EaseInCubic, EaseInOutCubic,
        EaseOutElastic, EaseInElastic, EaseInOutElastic;

        /** create an animation based on the type.
         * @see Animations */
        fun create(durationNanos: Long, start: Float, end: Float, onFinish: (Animation.() -> Unit)? = null): Animation {
            if (start == end) return Linear(1L, start, start, onFinish) // prevent empty animations
            return when (this) {
                Linear -> Linear(durationNanos, start, end)

                EaseInBack -> Easing.Back(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutBack -> Easing.Back(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutBack -> Easing.Back(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInBump -> Easing.Bump(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutBump -> Easing.Bump(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutBump -> Easing.Bump(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInQuad -> Easing.Quad(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutQuad -> Easing.Quad(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutQuad -> Easing.Quad(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInQuart -> Easing.Quart(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutQuart -> Easing.Quart(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutQuart -> Easing.Quart(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInQuint -> Easing.Quint(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutQuint -> Easing.Quint(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutQuint -> Easing.Quint(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInCirc -> Easing.Circ(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutCirc -> Easing.Circ(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutCirc -> Easing.Circ(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInExpo -> Easing.Expo(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutExpo -> Easing.Expo(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutExpo -> Easing.Expo(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInSine -> Easing.Sine(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutSine -> Easing.Sine(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutSine -> Easing.Sine(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInCubic -> Easing.Cubic(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutCubic -> Easing.Cubic(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutCubic -> Easing.Cubic(Easing.Type.InOut, durationNanos, start, end, onFinish)

                EaseInElastic -> Easing.Elastic(Easing.Type.In, durationNanos, start, end, onFinish)
                EaseOutElastic -> Easing.Elastic(Easing.Type.Out, durationNanos, start, end, onFinish)
                EaseInOutElastic -> Easing.Elastic(Easing.Type.InOut, durationNanos, start, end, onFinish)
            }
        }
    }
}

typealias Animations = Animation.Type
