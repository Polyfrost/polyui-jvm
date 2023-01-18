package cc.polyfrost.polyui.animate

abstract class Animation(val durationMillis: Long, val from: Float = 0f, val to: Float = 100f) {
    private var value: Float = from
    var finished: Boolean = false
        private set

    fun update(deltaTimeMillis: Long): Float {
        if (finished) return to
        value += getValue(deltaTimeMillis) / durationMillis * (to - from)
        if (value >= to) {
            value = to
            finished = true
        }
        return value
    }

    fun getPercentComplete(): Float {
        return value / to
    }

    protected abstract fun getValue(deltaTimeMillis: Long): Float

}