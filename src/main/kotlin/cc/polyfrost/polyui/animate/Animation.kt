package cc.polyfrost.polyui.animate

import cc.polyfrost.polyui.animate.animations.*

abstract class Animation(val durationMillis: Long, val from: Float, val to: Float) {
    val range = to - from
    var passedTime = 0F
        private set
    var finished: Boolean = false
        private set

    val value: Float
        get() {
            return getValue(passedTime / durationMillis) * range + from
        }

    fun update(deltaTimeMillis: Long): Float {
        if (finished) return to
        passedTime += deltaTimeMillis
        finished = passedTime + deltaTimeMillis >= durationMillis
        return value
    }

    protected abstract fun getValue(percent: Float): Float


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