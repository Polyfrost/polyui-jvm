/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.animate

import cc.polyfrost.polyui.animate.animations.*

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
        EaseInBack,
        EaseOutBump,
        EaseOutQuad,
        EaseOutExpo,
        EaseOutElastic,
        EaseInOutCubic,
        EaseInOutQuad,
        EaseInOutQuart;

        /** create an animation based on the type.
         * @see Animations */
        fun create(durationNanos: Long, start: Float, end: Float): Animation {
            if (start == end) return Linear(1L, 0f, 0f) // prevent empty animations
            return when (this) {
                Linear -> Linear(durationNanos, start, end)
                EaseInBack -> EaseInBack(durationNanos, start, end)
                EaseOutBump -> EaseOutBump(durationNanos, start, end)
                EaseOutQuad -> EaseOutQuad(durationNanos, start, end)
                EaseOutExpo -> EaseOutExpo(durationNanos, start, end)
                EaseInOutCubic -> EaseInOutCubic(durationNanos, start, end)
                EaseInOutQuad -> EaseInOutQuad(durationNanos, start, end)
                EaseInOutQuart -> EaseInOutQuart(durationNanos, start, end)
                EaseOutElastic -> EaseOutElastic(durationNanos, start, end)
            }
        }
    }
}

typealias Animations = Animation.Type
