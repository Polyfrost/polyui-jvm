package cc.polyfrost.polyui.units

data class Vec2<T : Unit>(var a: T, var b: T) : Cloneable {
    operator fun minus(value: Float): Vec2<T> {
        this.a.px - value
        this.b.px - value
        return this
    }

    operator fun get(index: Int): T {
        return when (index) {
            0 -> a
            1 -> b
            else -> throw IndexOutOfBoundsException()
        }
    }

    operator fun plus(value: Float): Vec2<T> {
        this.a.px + value
        this.b.px + value
        return this
    }

    fun move(x: Float, y: Float): Vec2<T> {
        this.a.px += x
        this.b.px += y
        return this
    }

    fun type(): Unit.Type {
        return a.type
    }

    fun x(): Float {
        return a.px
    }

    fun y(): Float {
        return b.px
    }

    fun width(): Float {
        return a.px
    }

    fun height(): Float {
        return b.px
    }

    override fun clone(): Vec2<T> {
        return Vec2(a.clone() as T, b.clone() as T)
    }

    fun scale(scaleX: Float, scaleY: Float) {
        this.a.px *= scaleX
        this.b.px *= scaleY
    }
}

typealias Point<T> = Vec2<T>
typealias Size<T> = Vec2<T>

