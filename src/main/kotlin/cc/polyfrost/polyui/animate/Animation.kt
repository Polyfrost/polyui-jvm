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

package cc.polyfrost.polyui.animate

/**
 * # Animation
 *
 * An animation used by the PolyUI system.
 *
 * PolyUI comes with many default [animations][cc.polyfrost.polyui.animate.animations], which you can use. To give the user some choice, you can also use the [Animations.create()][Animation.Type.create] to dynamically create them.
 *
 * check out [DrawableOperation][cc.polyfrost.polyui.component.DrawableOp] for more information on how to use animations dynamically; and [transitions][cc.polyfrost.polyui.animate.transitions] for more information on how to use animations with components.
 */
abstract class Animation(val durationNanos: Long, val from: Float, val to: Float) : Cloneable {
    val range = to - from
    var passedTime = 0F
        protected set
    var isFinished: Boolean = false
        protected set

    val value: Float
        get() {
            return getValue(passedTime / durationNanos) * range + from
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
        fun create(durationNanos: Long, start: Float, end: Float): Animation {
            if (start == end) return Linear(1L, start, start) // prevent empty animations
            return when (this) {
                Linear -> Linear(durationNanos, start, end)
                EaseOutBack -> Easing.Back(Easing.Type.Out, durationNanos, start, end)
                EaseOutBump -> Easing.Bump(Easing.Type.Out, durationNanos, start, end)
                EaseOutQuad -> Easing.Quad(Easing.Type.Out, durationNanos, start, end)
                EaseOutQuart -> Easing.Quart(Easing.Type.Out, durationNanos, start, end)
                EaseOutQuint -> Easing.Quint(Easing.Type.Out, durationNanos, start, end)
                EaseOutCirc -> Easing.Circ(Easing.Type.Out, durationNanos, start, end)
                EaseOutExpo -> Easing.Expo(Easing.Type.Out, durationNanos, start, end)
                EaseOutSine -> Easing.Sine(Easing.Type.Out, durationNanos, start, end)
                EaseOutCubic -> Easing.Cubic(Easing.Type.Out, durationNanos, start, end)
                EaseOutElastic -> Easing.Elastic(Easing.Type.Out, durationNanos, start, end)

                EaseInBack -> Easing.Back(Easing.Type.In, durationNanos, start, end)
                EaseInBump -> Easing.Bump(Easing.Type.In, durationNanos, start, end)
                EaseInQuad -> Easing.Quad(Easing.Type.In, durationNanos, start, end)
                EaseInQuart -> Easing.Quart(Easing.Type.In, durationNanos, start, end)
                EaseInQuint -> Easing.Quint(Easing.Type.In, durationNanos, start, end)
                EaseInCirc -> Easing.Circ(Easing.Type.In, durationNanos, start, end)
                EaseInExpo -> Easing.Expo(Easing.Type.In, durationNanos, start, end)
                EaseInSine -> Easing.Sine(Easing.Type.In, durationNanos, start, end)
                EaseInCubic -> Easing.Cubic(Easing.Type.In, durationNanos, start, end)
                EaseInElastic -> Easing.Elastic(Easing.Type.In, durationNanos, start, end)

                EaseInOutBack -> Easing.Back(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutBump -> Easing.Bump(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutQuad -> Easing.Quad(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutQuart -> Easing.Quart(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutQuint -> Easing.Quint(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutCirc -> Easing.Circ(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutExpo -> Easing.Expo(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutSine -> Easing.Sine(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutCubic -> Easing.Cubic(Easing.Type.InOut, durationNanos, start, end)
                EaseInOutElastic -> Easing.Elastic(Easing.Type.InOut, durationNanos, start, end)
            }
        }
    }
}

typealias Animations = Animation.Type
