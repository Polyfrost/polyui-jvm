package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.animate.Animation
import java.io.Serializable

/** an immutable color used by PolyUI.
 * @see [Color.Mutable] */
open class Color(open val r: Int, open val g: Int, open val b: Int, open val a: Int) : Cloneable, Serializable {

    constructor(r: Int, g: Int, b: Int) : this(r, g, b, 255)

    constructor(r: Float, g: Float, b: Float) : this(
        (r * 255).toInt(),
        (g * 255).toInt(),
        (b * 255).toInt()
    )

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
    open fun toMutable(): Mutable {
        return Mutable(r, g, b, a)
    }

    override fun clone(): Any {
        return Color(r, g, b, a)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other is Color) {
            return this.r == other.r && this.g == other.g && this.b == other.b && this.a == other.a
        }
        return false
    }

    override fun hashCode(): Int {
        var result = r
        result = 31 * result + g
        result = 31 * result + b
        result = 31 * result + a
        return result
    }

    companion object {
        val NONE = Color(0f, 0f, 0f, 0f)
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
    }

    /** A mutable version of [Color], that supports [recoloring][recolor] with animations.*/
    class Mutable(override var r: Int, override var g: Int, override var b: Int, override var a: Int) :
        Color(r, g, b, a) {
        private var animation: Array<Animation>? = null

        fun toImmutable(): Color {
            return Color(r, g, b, a)
        }

        @Deprecated("This would convert a mutable color to a mutable one. You don't need to do that. use [clone] instead.")
        override fun toMutable(): Mutable {
            PolyUI.LOGGER.warn("Tried to convert an already mutable color to mutable. Returning self.")
            return this
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

        /** update the color animation, if present.
         * After, the animation is cleared, and the color becomes static again.
         *
         * @return true if the animation finished on this tick, false if otherwise */
        fun update(deltaTimeMillis: Long): Boolean {
            if (animation != null) {
                if (animation!![0].finished) {
                    animation = null
                    return true
                }
                this.r = animation!![0].update(deltaTimeMillis).toInt()
                this.g = animation!![1].update(deltaTimeMillis).toInt()
                this.b = animation!![2].update(deltaTimeMillis).toInt()
                this.a = animation!![3].update(deltaTimeMillis).toInt()
                return false
            }
            return true
        }

        fun isRecoloring(): Boolean {
            return animation != null
        }

        override fun clone(): Any {
            return Mutable(r, g, b, a)
        }
    }
}

