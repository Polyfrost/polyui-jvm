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
 * check out [DrawableOperation][cc.polyfrost.polyui.component.DrawableOp] for more information on how to use animations dynamically; and [transistions][cc.polyfrost.polyui.animate.transitions] for more information on how to use animations with components.
 */
abstract class Animation(val durationMillis: Long, val from: Float, val to: Float) : Cloneable {
    val range = to - from
    var passedTime = 0F
        protected set
    var isFinished: Boolean = false
        protected set

    val value: Float
        get() {
            return getValue(passedTime / durationMillis) * range + from
        }

    fun update(deltaTimeMillis: Long): Float {
        if (isFinished) return to
        passedTime += deltaTimeMillis
        isFinished = passedTime + deltaTimeMillis >= durationMillis
        return value
    }

    protected abstract fun getValue(percent: Float): Float

    public abstract override fun clone(): Animation


    enum class Type {
        EaseInBack,
        EaseOutBump,
        EaseOutQuad,
        EaseOutExpo,
        EaseInOutCubic,
        EaseInOutQuad,
        EaseInOutQuart;

        fun create(durationMillis: Long, start: Float, end: Float): Animation {
            return when (this) {
                EaseInBack -> EaseInBack(durationMillis, start, end)
                EaseOutBump -> EaseOutBump(durationMillis, start, end)
                EaseOutQuad -> EaseOutQuad(durationMillis, start, end)
                EaseOutExpo -> EaseOutExpo(durationMillis, start, end)
                EaseInOutCubic -> EaseInOutCubic(durationMillis, start, end)
                EaseInOutQuad -> EaseInOutQuad(durationMillis, start, end)
                EaseInOutQuart -> EaseInOutQuart(durationMillis, start, end)
            }
        }
    }

}

typealias Animations = Animation.Type