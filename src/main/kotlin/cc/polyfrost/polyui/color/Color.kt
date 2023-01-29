package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.animate.Animation

/** an immutable color used by PolyUI.
 * @see [Color.Mutable] */
data class Color(val r: Int, val g: Int, val b: Int, val a: Int) {
    constructor(r: Float, g: Float, b: Float, a: Float) : this(
        (r * 255).toInt(),
        (g * 255).toInt(),
        (b * 255).toInt(),
        (a * 255).toInt()
    )

    fun getARGB(): Int {
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** return a new, [mutable][Mutable] version of this color */
    fun toMutable(): Mutable {
        return Mutable(r, g, b, a)
    }

    override operator fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is Color) {
            return this.r == other.r && this.g == other.g && this.b == other.b && this.a == other.a
        }
        if (other is Mutable) {
            return this.r == other.r && this.g == other.g && this.b == other.b && this.a == other.a
        }
        return false
    }

    companion object {
        val NONE = Color(0f, 0f, 0f, 0f)
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
    }

    /** A mutable version of [Color], that supports [recoloring][recolor] with animations.*/
    data class Mutable(var r: Int, var g: Int, var b: Int, var a: Int) {
        private var animation: Array<Animation>? = null

        fun toImmutable(): Color {
            return Color(r, g, b, a)
        }

        fun recolor(target: Color, type: Animation.Type?, durationMillis: Long = 1000) {
            if (type != null) {
                this.animation = Array(4) {
                    when (it) {
                        0 -> type.create(durationMillis, r.toFloat(), target.r.toFloat())
                        1 -> type.create(durationMillis, g.toFloat(), target.g.toFloat())
                        2 -> type.create(durationMillis, b.toFloat(), target.b.toFloat())
                        3 -> type.create(durationMillis, a.toFloat(), target.a.toFloat())
                        else -> throw Exception("Invalid index")
                    }
                }

            } else {
                this.r = target.r
                this.g = target.g
                this.b = target.b
                this.a = target.a
            }
        }

        fun update(deltaTimeMillis: Long) {
            if (animation != null) {
                if (animation!![0].finished) {
                    animation = null
                    return
                }
                this.r = animation!![0].update(deltaTimeMillis).toInt()
                this.g = animation!![1].update(deltaTimeMillis).toInt()
                this.b = animation!![2].update(deltaTimeMillis).toInt()
                this.a = animation!![3].update(deltaTimeMillis).toInt()
            }
        }

        fun getARGB(): Int {
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        fun isRecoloring(): Boolean {
            return animation != null
        }
    }
}

