package cc.polyfrost.polyui.utils

import cc.polyfrost.polyui.units.Unit

operator fun Float.compareTo(x: Unit): Int {
    return this.compareTo(x.pixels)
}

fun Float.pixels(): Unit.Pixel {
    return Unit.Pixel(this)
}

fun Int.pixels(): Unit.Pixel {
    return Unit.Pixel(this.toFloat())
}

fun Double.pixels(): Unit.Pixel {
    return Unit.Pixel(this.toFloat())
}