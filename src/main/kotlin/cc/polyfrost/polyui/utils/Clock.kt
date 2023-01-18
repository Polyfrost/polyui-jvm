package cc.polyfrost.polyui.utils

/** a simple class for timing of animations and things. Literally a delta function. */
class Clock {
    private var lastTime: Long = System.currentTimeMillis()

    fun getDelta(): Long {
        val currentTime = System.currentTimeMillis()
        val delta = currentTime - lastTime
        lastTime = currentTime
        return delta
    }

}