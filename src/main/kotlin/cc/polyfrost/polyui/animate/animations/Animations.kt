package cc.polyfrost.polyui.animate.animations

import cc.polyfrost.polyui.animate.Animation
import kotlin.math.pow

class EaseInBack(durationMillis: Long, start: Float, end: Float) : Animation(durationMillis, start, end) {
    private val overshoot = 1.70158F
    private val overshoot2 = overshoot + 1
    override fun getValue(percent: Float): Float {
        return overshoot * percent * percent * percent - overshoot2 * percent * percent
    }
}

class EaseOutBump(durationMillis: Long, start: Float, end: Float) :
    Animation(durationMillis, start, end) {
    private val a: Double = 1.7
    private val b: Double = 2.7
    override fun getValue(percent: Float): Float {
        return (1 + b * (percent - 1).pow(3) + a * 1.2 * (percent - 1).pow(2)).toFloat()
    }
}

class EaseOutQuad(durationMillis: Long, start: Float, end: Float) :
    Animation(durationMillis, start, end) {
    override fun getValue(percent: Float): Float {
        return 1 - (1 - percent) * (1 - percent)
    }
}

class EaseOutExpo(durationMillis: Long, start: Float, end: Float) :
    Animation(durationMillis, start, end) {
    override fun getValue(percent: Float): Float {
        return if (percent == 1F) 1F
        else 1 - 2.0.pow((-10 * percent).toDouble()).toFloat()
    }
}


// -- easeInOuts --
class EaseInOutCubic(durationMillis: Long, start: Float, end: Float) :
    Animation(durationMillis, start, end) {
    override fun getValue(percent: Float): Float {
        return if (percent < 0.5) 4 * percent * percent * percent
        else (1 - (-2 * percent + 2).toDouble().pow(3.0) / 2).toFloat()
    }
}

class EaseInOutQuad(durationMillis: Long, start: Float, end: Float) :
    Animation(durationMillis, start, end) {
    override fun getValue(percent: Float): Float {
        return if (percent < 0.5) 2 * percent * percent
        else 1 - (-2 * percent + 2).toDouble().pow(2.0).toFloat() / 2
    }
}

class EaseInOutQuart(durationMillis: Long, start: Float, end: Float) :
    Animation(durationMillis, start, end) {
    override fun getValue(percent: Float): Float {
        return if (percent < 0.5) 8 * percent * percent * percent * percent
        else 1 - (-2 * percent + 2).toDouble().pow(4.0).toFloat() / 2
    }
}