package cc.polyfrost.polyui.units

data class Vec2<T : Unit>(val a: T, val b: T) {
    operator fun minus(value: Float): Vec2<T> {
        this.a.pixels - value
        this.b.pixels - value
        return this
    }

    operator fun plus(value: Float): Vec2<T> {
        this.a.pixels + value
        this.b.pixels + value
        return this
    }
}

typealias Point<T> = Vec2<T>
typealias Size<T> = Vec2<T>

