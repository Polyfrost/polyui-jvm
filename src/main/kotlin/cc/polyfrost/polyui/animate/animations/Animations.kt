/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

/** default animations packaged with PolyUI. */
package cc.polyfrost.polyui.animate.animations

import cc.polyfrost.polyui.animate.Animation
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class Linear(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    override fun getValue(percent: Float): Float {
        return percent
    }

    override fun clone(): Animation {
        return Linear(durationNanos, from, to)
    }
}

class EaseInBack(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    private val overshoot = 1.70158f
    private val overshoot2 = overshoot + 1f
    override fun getValue(percent: Float): Float {
        return overshoot * percent * percent * percent - overshoot2 * percent * percent
    }

    override fun clone(): Animation {
        return EaseInBack(durationNanos, from, to)
    }
}

class EaseOutBump(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    override fun getValue(percent: Float): Float {
        return (3.7 * (percent - 1.0).pow(3.0) + 1.7 * 1.2 * (percent - 1.0).pow(2.0)).toFloat()
    }

    override fun clone(): Animation {
        return EaseOutBump(durationNanos, from, to)
    }
}

class EaseOutQuad(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    override fun getValue(percent: Float): Float {
        return 1f - (1f - percent) * (1f - percent)
    }

    override fun clone(): Animation {
        return EaseOutQuad(durationNanos, from, to)
    }
}

class EaseOutExpo(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    override fun getValue(percent: Float): Float {
        return if (percent == 1f) {
            1f
        } else {
            1f - 2.0.pow((-10.0 * percent.toDouble())).toFloat()
        }
    }

    override fun clone(): Animation {
        return EaseOutExpo(durationNanos, from, to)
    }
}

class EaseOutElastic(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    private val v = (2.0 * PI) / 3.0
    override fun getValue(percent: Float): Float {
        return (2.0.pow(-10.0 * percent.toDouble()) * sin((percent.toDouble() * 10.0 - 0.75) * v) + 1.0).toFloat()
            .clamped(0f, 1f)
    }

    override fun clone(): Animation {
        return EaseOutExpo(durationNanos, from, to)
    }
}

// -- easeInOuts --
class EaseInOutCubic(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    override fun getValue(percent: Float): Float {
        return if (percent < 0.5) {
            4 * percent * percent * percent
        } else {
            (1.0 - (-2.0 * percent + 2.0).pow(3.0) / 2.0).toFloat()
        }
    }

    override fun clone(): Animation {
        return EaseInOutCubic(durationNanos, from, to)
    }
}

class EaseInOutQuad(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    override fun getValue(percent: Float): Float {
        return if (percent < 0.5) {
            2f * percent * percent
        } else {
            (1.0 - (-2.0 * percent + 2.0).pow(2.0) / 2.0).toFloat()
        }
    }

    override fun clone(): Animation {
        return EaseInOutQuad(durationNanos, from, to)
    }
}

class EaseInOutQuart(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    override fun getValue(percent: Float): Float {
        return if (percent < 0.5) {
            8f * percent * percent * percent * percent
        } else {
            (1.0 - (-2.0 * percent + 2.0).pow(4.0) / 2.0).toFloat()
        }
    }

    override fun clone(): Animation {
        return EaseInOutQuart(durationNanos, from, to)
    }
}

fun Float.clamped(min: Float, max: Float): Float {
    return if (this < min) {
        min
    } else if (this > max) {
        max
    } else {
        this
    }
}
