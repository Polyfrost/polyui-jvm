/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - simple, easy to use and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.animate.Animation

/**
 * An immutable color used by PolyUI.
 *
 * @see [Color.Mutable]
 */
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

    /**
     * @return a new, [mutable][Mutable] version of this color
     */
    open fun toMutable() = Mutable(r, g, b, a)

    public override fun clone() = Color(r, g, b, a)

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
        val TRANSPARENT = Color(0f, 0f, 0f, 0f)
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
        val GRAYf = Color(0.5f, 0.5f, 0.5f, 0.5f)
    }

    /**
     * A mutable version of [Color], that supports [recoloring][recolor] with animations.
     */
    open class Mutable(
        override var r: Int,
        override var g: Int,
        override var b: Int,
        override var a: Int,
    ) : Color(r, g, b, a) {
        private var animation: Array<Animation>? = null

        fun toImmutable() = Color(r, g, b, a)

        @Deprecated("This would convert a mutable color to a mutable one.", replaceWith = ReplaceWith("clone()"))
        override fun toMutable(): Mutable {
            return clone()
        }

        /** recolor this color to the target color, with the given animation type and duration.
         *
         * **make sure to check if the color is a gradient, as this method may not have the expected result!**
         * @param type animation type. if it is null, the color will be set to the target color immediately.
         * @see [Gradient]
         * */
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

        override fun clone(): Mutable {
            return Mutable(r, g, b, a)
        }
    }

    class Gradient(color1: Color, color2: Color, val type: Type = Type.TopLeftToBottomRight) :
        Mutable(color1.r, color1.g, color1.b, color1.a) {
        val color2 = if (color2 !is Mutable) color2.toMutable() else color2

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

        override fun clone(): Gradient {
            return Gradient(this, color2, type)
        }

        /**
         * [Mutable.recolor] this gradient color.
         * @param whichColor which color to recolor. 1 for the first color, 2 for the second color.
         */
        fun recolor(whichColor: Int, target: Color, type: Animation.Type? = null, durationMillis: Long = 1000) {
            if (whichColor == 1) {
                super.recolor(target, type, durationMillis)
            } else if (whichColor == 2) {
                color2.recolor(target, type, durationMillis)
            } else {
                throw IllegalArgumentException("Invalid color index")
            }
        }

        /** merge the colors of this gradient into one color.
         * @param colorToMergeTo which color to merge to. 1 for the first color, 2 for the second. */
        fun mergeColors(colorToMergeTo: Int, type: Animation.Type? = null, durationMillis: Long = 1000L) {
            if (colorToMergeTo == 1) {
                color2.recolor(this, type, durationMillis)
            } else if (colorToMergeTo == 2) {
                super.recolor(color2, type, durationMillis)
            } else {
                throw IllegalArgumentException("Invalid color index")
            }
        }

        @Deprecated(
            "Gradient colors cannot be animated in this way.",
            ReplaceWith("recolor(1, target, type, durationMillis"),
            DeprecationLevel.ERROR
        )
        override fun recolor(target: Color, type: Animation.Type?, durationMillis: Long) {
            recolor(1, target, type, durationMillis)
        }
    }

    class Chroma(val speed: Long = 5000L, alpha: Int = 255) : Mutable(0, 0, 0, alpha) {
        @Deprecated("Chroma colors cannot be animated.", level = DeprecationLevel.ERROR)
        override fun recolor(target: Color, type: Animation.Type?, durationMillis: Long) {
            // no-op
        }

        override fun clone(): Chroma {
            return Chroma(speed, a)
        }

        override fun update(deltaTimeMillis: Long): Boolean {
            java.awt.Color.HSBtoRGB(System.currentTimeMillis() % speed / speed.toFloat(), 1f, 1f).let {
                r = (it shr 16) and 0xFF
                g = (it shr 8) and 0xFF
                b = it and 0xFF
            }
            return false
        }

        // although this technically should be true, we don't want a chroma color preventing an element from being deleted.
        override fun isRecoloring(): Boolean {
            return false
        }
    }

}
