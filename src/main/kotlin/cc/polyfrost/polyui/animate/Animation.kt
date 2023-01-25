package cc.polyfrost.polyui.animate

abstract class Animation(val durationMillis: Long, val from: Float = 0f, val to: Float = 100f) {
    var passedTime = 0L
        private set
    var finished: Boolean = false
        private set

    fun update(deltaTimeMillis: Long): Float {
        if (finished) return to
        passedTime += deltaTimeMillis
        finished = passedTime + deltaTimeMillis >= durationMillis
        return getValue(getPercentComplete()) * (to - from) + from
    }

    inline fun getPercentComplete(): Float {
        return (passedTime / durationMillis).toFloat()
    }

    protected abstract fun getValue(percent: Float): Float

}