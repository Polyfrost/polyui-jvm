/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.unit.seconds
import cc.polyfrost.polyui.utils.HSBtoRGB

/**
 * # Color
 *
 * The color used by PolyUI. It stores the color in the ARGB format.
 *
 * @see [Color.Mutable]
 * @see [Color.Gradient]
 * @see [Color.Chroma]
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

    /** return an integer representation of this color.
     * Utilizes bit-shifts to store the color as one 32-bit integer, like so:
     *
     * `0bAAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB`
     * aka `0xAARRGGBB`
     *
     * @see cc.polyfrost.polyui.utils.toColor
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getARGB")
    open val argb: Int
        get() {
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
            return argb == other.argb
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
        /** Transparent color. This should be used for checking in draw calls to prevent drawing of empty objects, e.g.
         *
         * `if (color == TRANSPARENT) return`
         */
        @JvmField
        val TRANSPARENT = Color(0f, 0f, 0f, 0f)

        /**
         * Turn the given hex string into a color.
         *
         * - If it is 8 characters long, it is assumed to be in the format `#RRGGBBAA` (alpha optional)
         * - If there is a leading `#`, it will be removed.
         * - If it is 1 character long, the character is repeated e.g. `#f` -> `#ffffff`
         * - If it is 2 characters long, the character is repeated e.g. `#0f` -> `#0f0f0f`
         * - If it is 3 characters long, the character is repeated e.g. `#0fe` -> `#00ffee`
         *
         * @throws IllegalArgumentException if the hex string is not in a valid format
         * @throws NumberFormatException if the hex string is not a valid hex string
         */
        @JvmStatic
        fun from(hex: String): Color {
            var hexColor = hex.replace("#", "")
            when (hexColor.length) {
                1 -> hexColor = hexColor.repeat(6)
                2 -> hexColor = hexColor.repeat(3)
                3 -> hexColor = run {
                    var newHex = ""
                    hexColor.forEach {
                        newHex += it.toString().repeat(2)
                    }
                    newHex
                }

                6 -> {}
                8 -> {}
                else -> throw IllegalArgumentException("Invalid hex color: $hex")
            }
            val r = hexColor.substring(0, 2).toInt(16)
            val g = hexColor.substring(2, 4).toInt(16)
            val b = hexColor.substring(4, 6).toInt(16)
            val a = if (hexColor.length == 8) hexColor.substring(6, 8).toInt(16) else 255
            return Color(r, g, b, a)
        }

        /** @see from(hex) */
        @JvmStatic
        fun from(hex: String, alpha: Int = 255) = from(hex + alpha.toString(16))

        /** @see from(hex) */
        @JvmStatic
        fun from(hex: String, alpha: Float = 1f) = from(hex, (alpha * 255).toInt())
    }

    /**
     * A mutable version of [Color], that supports [recoloring][recolor] with animations.
     */
    open class Mutable(
        override var r: Int,
        override var g: Int,
        override var b: Int,
        override var a: Int
    ) : Color(r, g, b, a) {

        /** Set this in your Color class if it's always updating, but still can be removed while updating, for
         * example a chroma color, which always needs to update, but still can be removed any time.
         * @see Chroma
         * @see updating
         */
        open val alwaysUpdates get() = false

        /** return if the color is still updating (i.e. it has an animation to finish)
         * @see alwaysUpdates
         */
        open val updating get() = animation != null
        private var animation: Animation? = null
        private var to = Array(4) { 0f }
        private var from = Array(4) { 0f }

        /** turn this mutable color into an immutable one. */
        fun toImmutable() = Color(r, g, b, a)

        @Deprecated("This would convert a mutable color to a mutable one.", replaceWith = ReplaceWith("clone()"))
        override fun toMutable() = clone()

        /**
         * recolor this color to the target color, with the given animation type and duration.
         *
         * **make sure to check if the color is a gradient, as this method may not have the expected result!**
         * @param type animation type. if it is null, the color will be set to the target color immediately.
         * @see [Gradient]
         */
        open fun recolor(target: Color, type: Animation.Type? = null, durationNanos: Long = 1L.seconds) {
            // clear old animation
            animation = null
            if (type != null) {
                this.animation = type.create(durationNanos, 0f, 1f)
                from[0] = this.r.toFloat()
                from[1] = this.g.toFloat()
                from[2] = this.b.toFloat()
                from[3] = this.a.toFloat()
                to[0] = target.r.toFloat() - from[0]
                to[1] = target.g.toFloat() - from[1]
                to[2] = target.b.toFloat() - from[2]
                to[3] = target.a.toFloat() - from[3]
            } else {
                this.r = target.r
                this.g = target.g
                this.b = target.b
                this.a = target.a
            }
        }

        /**
         * update the color animation, if present.
         * After, the animation is cleared, and the color becomes static again.
         *
         * @return true if the animation finished on this tick, false if otherwise
         * */
        open fun update(deltaTimeNanos: Long): Boolean {
            if (animation != null) {
                val from = this.from
                val to = this.to
                if (animation!!.isFinished) {
                    animation = null
                    return true
                }
                val progress = animation!!.update(deltaTimeNanos)
                this.r = (from[0] + to[0] * progress).toInt()
                this.g = (from[1] + to[1] * progress).toInt()
                this.b = (from[2] + to[2] * progress).toInt()
                this.a = (from[3] + to[3] * progress).toInt()
                return false
            }
            return false
        }

        override fun clone() = Mutable(r, g, b, a)
    }

    /** A gradient color. */
    class Gradient @JvmOverloads constructor(color1: Color, color2: Color, val type: Type = Type.TopLeftToBottomRight) :
        Mutable(color1.r, color1.g, color1.b, color1.a) {
        val color2 = if (color2 !is Mutable) color2.toMutable() else color2

        sealed class Type {
            data object TopToBottom : Type()
            data object LeftToRight : Type()
            data object TopLeftToBottomRight : Type()
            data object BottomLeftToTopRight : Type()

            /**
             * Radial gradient.
             *
             * Note that if your radii are very close together, you may just get a circle in a box.
             * @param innerRadius how much of the shape should be filled entirely with color1
             * @param outerRadius how much of the shape should be filled entirely with color2
             * @param centerX the center of the gradient, in the x-axis. if it is -1, it will be the center of the shape
             * @param centerY the center of the gradient, in the y-axis. if it is -1, it will be the center of the shape
             *
             * @throws IllegalArgumentException if innerRadius1 is larger than outerRadius2
             */
            data class Radial @JvmOverloads constructor(
                val innerRadius: Float,
                val outerRadius: Float,
                val centerX: Float = -1f,
                val centerY: Float = -1f
            ) : Type() {
                init {
                    require(innerRadius < outerRadius) { "innerRadius must be smaller than outerRadius! ($innerRadius < $outerRadius)" }
                    if (innerRadius + 5 > outerRadius) PolyUI.LOGGER.warn("[Gradient] innerRadius and outerRadius are very close together, you may just get a circle in a box.")
                }
            }

            data class Box(val radius: Float, val feather: Float) : Type()
        }

        @Deprecated(
            "Use [getARGB1] or [getARGB2] instead for gradient colors, as to not to confuse the end user.",
            ReplaceWith("getARGB1()")
        )
        @Suppress("INAPPLICABLE_JVM_NAME")
        @get:JvmName("getARGB")
        override val argb: Int
            get() {
                return super.argb
            }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other === this) return true
            if (other is Gradient) {
                return this.r == other.r && this.g == other.g && this.b == other.b && this.a == other.a && this.color2 == other.color2 && this.type == other.type
            }
            return false
        }

        @get:JvmName("getARGB1")
        val argb1 get() = super.argb

        @get:JvmName("getARGB2")
        val argb2 get() = color2.argb

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + color2.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }

        override fun clone() = Gradient(this, color2, type)

        /**
         * [Mutable.recolor] this gradient color.
         * @param whichColor which color to recolor. 1 for the first color, 2 for the second color.
         */
        fun recolor(whichColor: Int, target: Color, type: Animation.Type? = null, durationNanos: Long = 1L.seconds) {
            when (whichColor) {
                1 -> super.recolor(target, type, durationNanos)
                2 -> color2.recolor(target, type, durationNanos)
                else -> throw IllegalArgumentException("Invalid color index")
            }
        }

        /** merge the colors of this gradient into one color.
         * @param colorToMergeTo which color to merge to. 1 for the first color, 2 for the second.
         * */
        fun mergeColors(colorToMergeTo: Int, type: Animation.Type? = null, durationNanos: Long = 1L.seconds) {
            when (colorToMergeTo) {
                1 -> color2.recolor(this, type, durationNanos)
                2 -> super.recolor(color2, type, durationNanos)
                else -> throw IllegalArgumentException("Invalid color index")
            }
        }

        @Deprecated(
            "Gradient colors cannot be animated in this way.",
            ReplaceWith("recolor(1, target, type, durationNanos"),
            DeprecationLevel.ERROR
        )
        override fun recolor(target: Color, type: Animation.Type?, durationNanos: Long) {
            // noop
        }
    }

    /**
     * # Chroma Color
     *
     * A color that changes over [time][speedNanos], in an endless cycle. You can use the [saturation] and [brightness] fields to set the tone of the chroma. Some examples:
     *
     * `brightness = 0.2f, saturation = 0.8f` -> dark chroma
     *
     * `brightness = 0.8f, saturation = 0.8f` -> less offensive chroma tone
     */
    class Chroma @JvmOverloads constructor(
        /**
         * the speed of this color, in nanoseconds.
         *
         * The speed refers to the amount of time it takes for this color to complete one cycle, e.g. from red, through to blue, through to green, then back to red.
         * */
        private val speedNanos: Long = 5000L,
        /** brightness of the color range (0.0 - 1.0) */
        private val brightness: Float = 1f,
        /** saturation of the color range (0.0 - 1.0) */
        private val saturation: Float = 1f,
        alpha: Int = 255
    ) : Mutable(0, 0, 0, alpha) {
        private var time = 0L

        @Deprecated("Chroma colors cannot be animated.", level = DeprecationLevel.ERROR)
        override fun recolor(target: Color, type: Animation.Type?, durationNanos: Long) {
            // no-op
        }

        override fun clone() = Chroma(speedNanos, brightness, saturation, a)

        override fun update(deltaTimeNanos: Long): Boolean {
            time += deltaTimeNanos
            HSBtoRGB(
                time % speedNanos / speedNanos.toFloat(),
                saturation,
                brightness
            ).let {
                r = (it shr 16) and 0xFF
                g = (it shr 8) and 0xFF
                b = it and 0xFF
            }
            return false
        }

        // although this technically should be true, we don't want a chroma color preventing an element from being deleted.
        override val updating get() = false
        override val alwaysUpdates get() = true
    }
}
