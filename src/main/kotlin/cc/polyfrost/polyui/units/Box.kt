package cc.polyfrost.polyui.units

import cc.polyfrost.polyui.utils.compareTo

data class Box<T : Unit>(val x: T, val y: T, val w: T, val h: T) {
    constructor(point: Vec2<T>, dimensions: Vec2<T>) : this(point.a, point.b, dimensions.a, dimensions.b)

    fun isInside(x: Float, y: Float): Boolean {
        return x >= this.x && x <= this.x + this.w && y >= this.y && y <= this.y + this.h
    }

    /** add the given amount of pixels to each edge of this box */
    fun expand(amount: Float): Box<T> {
        this.x.pixels - amount
        this.y.pixels - amount
        this.w.pixels + amount
        this.h.pixels + amount
        return this
    }
}