package cc.polyfrost.polyui.units

import cc.polyfrost.polyui.utils.compareTo

data class Box<T : Unit>(val x: T, val y: T, val width: T, val height: T) {
    constructor(point: Vec2<T>, dimensions: Vec2<T>) : this(point.a, point.b, dimensions.a, dimensions.b)

    fun isInside(x: Float, y: Float): Boolean {
        return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height
    }

    /** add the given amount of pixels to each edge of this box */
    fun expand(amount: Float): Box<T> {
        this.x.v - amount
        this.y.v - amount
        this.width.v + amount
        this.height.v + amount
        return this
    }

    fun x(): Float {
        return x.v
    }

    fun y(): Float {
        return y.v
    }

    fun width(): Float {
        return width.v
    }

    fun height(): Float {
        return height.v
    }
}