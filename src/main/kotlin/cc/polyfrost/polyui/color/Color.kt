package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.animate.Animation
import kotlin.math.sin

/** an immutable color used by PolyUI.
 * @see [Color.Mutable] */
open class Color(open val r: Int, open val g: Int, open val b: Int, open val a: Int) : Cloneable {

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

    open fun getARGB(): Int {
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
        val GRAYf = Color(0.5f, 0.5f, 0.5f, 0.5f)
    }

    /** A mutable version of [Color], that supports [recoloring][recolor] with animations.*/
    open class Mutable(override var r: Int, override var g: Int, override var b: Int, override var a: Int) :
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

        open fun recolor(target: Color, type: Animation.Type? = null, durationMillis: Long = 1000) {
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
        open fun update(deltaTimeMillis: Long): Boolean {
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

        open fun isRecoloring(): Boolean {
            return animation != null
        }

        override fun clone(): Any {
            return Mutable(r, g, b, a)
        }
    }

    class Gradient(color1: Color, val color2: Color, val type: Type) : Mutable(color1.r, color1.g, color1.b, color1.a) {
        sealed class Type {
            object TopToBottom : Type()
            object LeftToRight : Type()
            object TopLeftToBottomRight : Type()
            object BottomLeftToTopRight : Type()
            data class Radial(val innerRadius: Float, val outerRadius: Float) : Type()
        }

        @Deprecated(
            "Use [getARGB1] or [getARGB2] instead for gradient colors, as to not to confuse the end user.",
            ReplaceWith("getARGB1()")
        )
        override fun getARGB(): Int {
            return super.getARGB()
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other === this) return true
            if (other is Gradient) {
                return this.r == other.r && this.g == other.g && this.b == other.b && this.a == other.a && this.color2 == other.color2 && this.type == other.type
            }
            return false
        }

        fun getARGB1(): Int {
            return super.getARGB()
        }

        fun getARGB2(): Int {
            return color2.getARGB()
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + color2.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }
    }

    class Chroma(var speed: Long, alpha: Int) : Mutable(0, 0, 0, alpha) {
        @Deprecated("Chroma colors cannot be animated.", level = DeprecationLevel.ERROR)
        override fun recolor(target: Color, type: Animation.Type?, durationMillis: Long) =
            throw UnsupportedOperationException("Chroma colors cannot be animated.")

        override fun update(deltaTimeMillis: Long): Boolean {
            val time = System.currentTimeMillis()
            r = (sin((time * speed).toDouble()) * 127.0 + 128.0).toInt()
            g = (sin(time * speed + 2.0) * 127.0 + 128.0).toInt()
            b = (sin(time * speed + 4.0) * 127.0 + 128.0).toInt()
            return false
        }

        // although this technically should be true, we don't want a chroma color preventing an element from being deleted.
        override fun isRecoloring(): Boolean {
            return false
        }
    }

}

typealias Gradients = Color.Gradient.Type
