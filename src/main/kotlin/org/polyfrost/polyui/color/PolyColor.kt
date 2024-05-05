/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.polyui.color

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.utils.HSBtoRGB
import org.polyfrost.polyui.utils.RGBtoHSB
import org.polyfrost.polyui.utils.rgba

/**
 * # Color
 *
 * The color used by PolyUI. It stores the color in the HSBA format, and can be converted to ARGB.
 *
 * @see [PolyColor.Animated]
 * @see [PolyColor.Gradient]
 */
open class PolyColor @JvmOverloads constructor(hue: Float, saturation: Float, brightness: Float, alpha: Float = 1f) : Cloneable {
    /**
     * The hue of this color. Can be any value, but is `mod 360`, so values are always between 0 and 360.
     */
    var hue = hue
        set(value) {
            if (field == value) return
            field = value
            dirty = true
        }

    /**
     * The saturation of this color. Clamped between `0f..1f`.
     */
    var saturation = saturation
        set(value) {
            if (field == value) return
            field = value.coerceIn(0f, 1f)
            dirty = true
        }

    /**
     * The brightness of this color. Clamped between `0f..1f`.
     */
    var brightness = brightness
        set(value) {
            if (field == value) return
            field = value.coerceIn(0f, 1f)
            dirty = true
        }

    /**
     * The alpha of this color. Clamped between `0f..1f`.
     */
    var alpha = alpha
        set(value) {
            if (field == value) return
            field = value.coerceIn(0f, 1f)
            dirty = true
        }

    /** Set this to true to update the [argb] value. */
    @Transient
    var dirty = true

    /** return an integer representation of this color.
     * Utilizes bit-shifts to store the color as one 32-bit integer, like so:
     *
     * `0bAAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB`
     * aka `0xAARRGGBB`
     *
     * @see org.polyfrost.polyui.utils.toColor
     */
    @get:JvmName("getARGB")
    @Transient
    var argb: Int = 0
        protected set
        get() {
            return if (dirty) {
                HSBtoRGB(hue, saturation, brightness).let { rgb ->
                    (rgb and 0x00FFFFFF) or ((alpha * 255f).toInt() shl 24)
                }.also {
                    dirty = false
                    field = it
                }
            } else {
                field
            }
        }

    open val transparent get() = this.argb == 0

    @JvmOverloads
    constructor(hsb: FloatArray, alpha: Float = 1f) : this(hsb[0], hsb[1], hsb[2], alpha)

    constructor(rgba: IntArray) : this(rgba[0], rgba[1], rgba[2], rgba[3] / 255f)

    @JvmOverloads
    constructor(r: Int, g: Int, b: Int, alpha: Float = 1f) : this(RGBtoHSB(r, g, b), alpha)

    init {
        require(saturation in 0f..1f) { "Saturation must be between 0 and 1" }
        require(brightness in 0f..1f) { "Brightness must be between 0 and 1" }
        require(alpha in 0f..1f) { "Alpha must be between 0 and 1" }
    }

    /** red value of this color, from 0 to 255 */
    @get:JvmName("red")
    inline val r get() = argb shr 16 and 0xFF

    /** green value of this color, from 0 to 255 */
    @get:JvmName("green")
    inline val g get() = argb shr 8 and 0xFF

    /** blue value of this color, from 0 to 255 */
    @get:JvmName("blue")
    inline val b get() = argb and 0xFF

    /** alpha value of this color, from 0 to 255 */
    @get:JvmName("alpha")
    inline val a get() = argb shr 24 and 0xFF

    /**
     * @return a new, [animatable][Animated] version of this color
     */
    open fun toAnimatable() = Animated(hue, saturation, brightness, alpha)

    public override fun clone() = Color(hue, saturation, brightness, alpha)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other is Color) {
            return argb == other.argb
        }
        return false
    }

    override fun toString(): String =
        "Color(h=$hue, s=$saturation, b=$brightness, a=$alpha, hex=#${Integer.toHexString(argb).uppercase()})"

    override fun hashCode(): Int {
        var result = hue
        result = 31f * result + saturation
        result = 31f * result + brightness
        result = 31f * result + alpha
        return result.toInt()
    }

    /**
     * Return a static java [Color][java.awt.Color] object of this color at the instant this method is called.
     *
     * Future changes to this color will not be reflected in the returned object.
     *
     * @since 1.1.51
     */
    fun toJavaColor() = java.awt.Color(argb, true)

    fun take(color: Color): Color {
        this.hue = color.hue
        this.saturation = color.saturation
        this.brightness = color.brightness
        this.alpha = color.alpha
        return this
    }

    companion object {
        /** Transparent color. This should be used for checking in draw calls to prevent drawing of empty objects, e.g.
         *
         * `if (color == TRANSPARENT) return`
         */
        @JvmField
        val TRANSPARENT = rgba(0, 0, 0, 0f)

        @JvmField
        val WHITE = rgba(255, 255, 255, 1f)

        @JvmField
        val BLACK = rgba(0, 0, 0, 1f)

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
            var hexColor = hex.removePrefix("#")
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
            return Color(r, g, b, a / 255f)
        }

        /** @see from(hex) */
        @JvmStatic
        fun from(hex: String, alpha: Int = 255) = from(hex + alpha.toString(16))

        /** @see from(hex) */
        @JvmStatic
        fun from(hex: String, alpha: Float = 1f) = from(hex, (alpha * 255).toInt())

        @JvmStatic
        fun hexOf(color: Color, alpha: Boolean = true): String {
            if (alpha) {
                return "#${Integer.toHexString(color.argb).uppercase()}"
            }
            return "#${Integer.toHexString(color.argb and 0x00FFFFFF).uppercase()}"
        }
    }

    /**
     * An animatable version of [Color], that supports [recoloring][recolor] with animations.
     */
    open class Animated(
        hue: Float,
        saturation: Float,
        brightness: Float,
        alpha: Float,
    ) : Color(hue, saturation, brightness, alpha) {
        /** return if the color is still updating (i.e. it has an animation to finish) */
        open val updating get() = animation != null

        @Transient
        protected var animation: Animation? = null

        @Transient
        protected var to: FloatArray? = null

        @Transient
        protected var from: FloatArray? = null

        @Transient
        protected var current: FloatArray? = null

        @Deprecated("This would convert an animatable color to an animatable one.", replaceWith = ReplaceWith("clone()"))
        override fun toAnimatable() = clone()

        /**
         * recolor this color to the target color, with the given animation type and duration.
         *
         * **make sure to check if the color is a gradient, as this method may not have the expected result!**
         * @param animation animation to use. if it is null, the color will be set to the target color immediately.
         * @see [Gradient]
         */
        open fun recolor(target: Color, animation: Animation? = null) {
            if (target == this) return
            // clear old animation
            this.animation = null
            if (animation != null) {
                this.animation = animation
                val from = floatArrayOf(
                    this.r.toFloat(),
                    this.g.toFloat(),
                    this.b.toFloat(),
                    this.alpha,
                )
                this.from = from
                current = FloatArray(4)
                to = floatArrayOf(
                    target.r.toFloat() - from[0],
                    target.g.toFloat() - from[1],
                    target.b.toFloat() - from[2],
                    target.alpha - from[3],
                )
            } else {
                this.hue = target.hue
                this.saturation = target.saturation
                this.brightness = target.brightness
                this.alpha = target.alpha
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
                dirty = true
                val animation = this.animation ?: return false
                val from = this.from ?: return false
                val to = this.to ?: return false
                val current = this.current ?: return false

                val progress = animation.update(deltaTimeNanos)
                RGBtoHSB(
                    (from[0] + to[0] * progress).toInt(),
                    (from[1] + to[1] * progress).toInt(),
                    (from[2] + to[2] * progress).toInt(),
                    current,
                )
                this.hue = current[0]
                this.saturation = current[1]
                this.brightness = current[2]
                this.alpha = (from[3] + to[3] * progress)

                if (animation.isFinished) {
                    this.animation = null
                    this.from = null
                    this.to = null
                    this.current = null
                    return true
                }
                return false
            }
            return false
        }

        override fun clone() = Animated(hue, saturation, brightness, alpha)
    }

    /** A gradient color. */
    class Gradient @JvmOverloads constructor(color1: Color, color2: Color, val type: Type = Type.TopLeftToBottomRight) :
        Animated(color1.hue, color1.saturation, color1.brightness, color1.alpha) {
        /**
         * **WARNING:** the following code will NOT behave as expected:
         * ```
         * val colorToSet = rgba(...)
         * myGradient.color1 = colorToSet
         * assert(myGradient.color1 === colorToSet) // this will fail!
         * ```
         * this is due to how gradient colors are implemented. use the equals operator instead (`==`) to compare colors.
         */
        var color1: Animated
            inline get() = this
            set(value) {
                hue = value.hue
                saturation = value.saturation
                brightness = value.brightness
                alpha = value.alpha
            }

        var color2 = if (color2 !is Animated) color2.toAnimatable() else color2

        @get:JvmName("getARGB1")
        val argb1 get() = super.argb

        @get:JvmName("getARGB2")
        val argb2 get() = color2.argb

        override val transparent: Boolean
            get() = super.transparent && color2.transparent

        override val updating: Boolean
            get() = super.updating || color2.updating

        init {
            require(this.color2 !== this) { "color1 and color2 must be different objects" }
        }

        @Suppress("ConvertObjectToDataObject")
        sealed interface Type {
            object TopToBottom : Type

            object LeftToRight : Type

            object TopLeftToBottomRight : Type

            object BottomLeftToTopRight : Type

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
            class Radial @JvmOverloads constructor(
                val innerRadius: Float,
                val outerRadius: Float,
                val centerX: Float = -1f,
                val centerY: Float = -1f,
            ) : Type {
                init {
                    require(innerRadius < outerRadius) { "innerRadius must be smaller than outerRadius! ($innerRadius < $outerRadius)" }
                    if (innerRadius + 5f > outerRadius) PolyUI.LOGGER.warn("[Gradient] innerRadius and outerRadius are very close together, you may just get a circle in a box.")
                }
            }

            class Box(val radius: Float, val feather: Float) : Type
        }

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other is Gradient) {
                return this.argb1 == other.argb1 && this.argb2 == other.argb2 && this.type == other.type
            }
            return false
        }

        operator fun get(index: Int): Color {
            return when (index) {
                0 -> this
                1 -> color2
                else -> throw IndexOutOfBoundsException("index must be 0 or 1")
            }
        }

        operator fun set(index: Int, color: Color) = recolor(index, color)

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + color2.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }

        override fun clone() = Gradient(this, color2, type)

        /**
         * [Animated.recolor] this gradient color.
         * @param whichColor which color to recolor. 0 for the first color, 1 for the second color.
         */
        fun recolor(whichColor: Int, target: Color, animation: Animation? = null) {
            when (whichColor) {
                0 -> super.recolor(target, animation)
                1 -> color2.recolor(target, animation)
                else -> throw IndexOutOfBoundsException("Invalid color index $whichColor: must be 0 or 1")
            }
        }

        override fun update(deltaTimeNanos: Long): Boolean {
            return super.update(deltaTimeNanos) or color2.update(deltaTimeNanos)
        }

        /**
         * Deprecated, see [Component.recolor][org.polyfrost.polyui.operations.Recolor] for how to animate gradients.
         */
        @Deprecated(
            "Gradient colors cannot be animated in this way. They can be animated separately using the given method.",
            ReplaceWith("recolor(0, target, type, durationNanos)"),
            DeprecationLevel.ERROR,
        )
        override fun recolor(target: Color, animation: Animation?) {
            // nop
        }
    }
}

typealias Color = PolyColor
