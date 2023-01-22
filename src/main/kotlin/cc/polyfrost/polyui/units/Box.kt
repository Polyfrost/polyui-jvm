package cc.polyfrost.polyui.units

data class Box<T : Unit>(val point: Point<T>, val sized: Size<T>) {

    fun isInside(x: Float, y: Float): Boolean {
        return x >= this.x() && x <= this.x() + this.width() && y >= this.y() && y <= this.y() + this.height()
    }

    /** add the given amount of pixels to each edge of this box */
    fun expand(amount: Float): Box<T> {
        this.x() - amount
        this.y() - amount
        this.width() + amount
        this.height() + amount
        return this
    }

    fun x(): Float {
        return point[0].px
    }

    fun y(): Float {
        return point[1].px
    }

    fun width(): Float {
        return sized[0].px
    }

    fun height(): Float {
        return sized[1].px
    }
}