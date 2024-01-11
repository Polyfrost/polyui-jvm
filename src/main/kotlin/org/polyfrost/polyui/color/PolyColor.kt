/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

@file:Suppress("invisible_member", "invisible_reference")

package org.polyfrost.polyui.color

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.unit.seconds
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
 * @see [PolyColor.Chroma]
 */
open class PolyColor @JvmOverloads constructor(hue: Float, saturation: Float, brightness: Float, alpha: Float = 1f) :
    Cloneable {

    var hue = hue
        set(value) {
            if (field == value) return
            field = value
            dirty = true
        }
    var saturation = saturation
        set(value) {
            if (field == value) return
            field = value.coerceIn(0f, 1f)
            dirty = true
        }
    var brightness = brightness
        set(value) {
            if (field == value) return
            field = value.coerceIn(0f, 1f)
            dirty = true
        }

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

    @JvmOverloads
    constructor(hsb: FloatArray, alpha: Int = 255) : this(hsb[0], hsb[1], hsb[2], alpha)
    constructor(hsb: FloatArray, alpha: Float = 1f) : this(hsb[0], hsb[1], hsb[2], alpha)

    @JvmOverloads
    constructor(r: Int, g: Int, b: Int, alpha: Int = 255) : this(RGBtoHSB(r, g, b), alpha)
    constructor(r: Int, g: Int, b: Int, alpha: Float = 1f) : this(RGBtoHSB(r, g, b), alpha)

    constructor(hue: Float, saturation: Float, brightness: Float, alpha: Int = 255) : this(
        hue,
        saturation,
        brightness,
        alpha.toFloat() / 255f
    )

    init {
        require(saturation in 0f..1f) { "Saturation must be between 0 and 1" }
        require(brightness in 0f..1f) { "Brightness must be between 0 and 1" }
        require(alpha in 0f..1f) { "Alpha must be between 0 and 1" }
    }

    /** red value of this color, from 0 to 255 */
    @kotlin.internal.InlineOnly
    inline val r get() = argb shr 16 and 0xFF

    /** green value of this color, from 0 to 255 */
    @kotlin.internal.InlineOnly
    inline val g get() = argb shr 8 and 0xFF

    /** blue value of this color, from 0 to 255 */
    @kotlin.internal.InlineOnly
    inline val b get() = argb and 0xFF

    /** alpha value of this color, from 0 to 255 */
    @kotlin.internal.InlineOnly
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
        "Color(hue=$hue, saturation=$saturation, brightness=$brightness, alpha=$alpha, argb=#${Integer.toHexString(argb)})"

    override fun hashCode(): Int {
        var result = hue.toInt()
        result = 31 * result + saturation.toInt()
        result = 31 * result + brightness.toInt()
        result = 31 * result + alpha.toInt()
        return result
    }

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
         * `if (color === TRANSPARENT) return`
         */
        @JvmField
        @Transient
        val TRANSPARENT = rgba(0f, 0f, 0f, 0f)

        @JvmField
        @Transient
        val WHITE = rgba(1f, 1f, 1f, 1f)

        @JvmField
        @Transient
        val BLACK = rgba(0f, 0f, 0f, 1f)

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
            return Color(r, g, b, a)
        }

        /** @see from(hex) */
        @JvmStatic
        fun from(hex: String, alpha: Int = 255) = from(hex + alpha.toString(16))

        /** @see from(hex) */
        @JvmStatic
        fun from(hex: String, alpha: Float = 1f) = from(hex, (alpha * 255).toInt())

        /** Utility function to convert Java.awt.Color to PolyColor **/
        @JvmStatic
        fun from(color: java.awt.Color) = Color(color.red, color.green, color.blue, color.alpha)

        @JvmStatic
        fun hexOf(color: Color): String {
            val r = color.r.toString(16).padStart(2, '0')
            val g = color.g.toString(16).padStart(2, '0')
            val b = color.b.toString(16).padStart(2, '0')
            val a = color.a.toString(16).padStart(2, '0')
            return "#$r$g$b$a"
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

        @Transient
        private var animation: Animation? = null

        @Transient
        private var to: FloatArray? = null

        @Transient
        private var from: FloatArray? = null

        @Transient
        private var current: FloatArray? = null

        @Deprecated(
            "This would convert an animatable color to an animatable one.",
            replaceWith = ReplaceWith("clone()")
        )
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

                val progress = animation.update(deltaTimeNanos)
                RGBtoHSB(
                    (from[0] + to[0] * progress).toInt(),
                    (from[1] + to[1] * progress).toInt(),
                    (from[2] + to[2] * progress).toInt(),
                    this.current,
                )
                val current = this.current ?: return false
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
        Animated(color2.hue, color2.saturation, color2.brightness, color2.alpha) {
        val color1 = if (color1 !is Animated) color1.toAnimatable() else color1

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
                val centerY: Float = -1f,
            ) : Type() {
                init {
                    require(innerRadius < outerRadius) { "innerRadius must be smaller than outerRadius! ($innerRadius < $outerRadius)" }
                    if (innerRadius + 5f > outerRadius) PolyUI.LOGGER.warn("[Gradient] innerRadius and outerRadius are very close together, you may just get a circle in a box.")
                }
            }

            data class Box(val radius: Float, val feather: Float) : Type()
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
                0 -> color1
                1 -> this
                else -> throw IndexOutOfBoundsException("index must be 0 or 1")
            }
        }

        operator fun set(index: Int, color: Color) = recolor(index, color)

        @get:JvmName("getARGB1")
        val argb1 get() = color1.argb

        @get:JvmName("getARGB2")
        val argb2 get() = super.argb

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + color1.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }

        override fun clone() = Gradient(color1, this, type)

        /**
         * [Animated.recolor] this gradient color.
         * @param whichColor which color to recolor. 0 for the first color, 1 for the second color.
         */
        fun recolor(whichColor: Int, target: Color, animation: Animation? = null) {
            when (whichColor) {
                0 -> color1.recolor(target, animation)
                1 -> super.recolor(target, animation)
                else -> throw IllegalArgumentException("Invalid color index")
            }
        }

        override fun update(deltaTimeNanos: Long): Boolean {
            color1.update(deltaTimeNanos)
            return super.update(deltaTimeNanos)
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

    /**
     * # Chroma Color
     *
     * A color that changes over [time][speedNanos], in an endless cycle. You can use the [saturation] and [brightness] fields to set the tone of the chroma. Some examples:
     *
     * `brightness = 0.2f, saturation = 0.8f` -> dark chroma
     *
     * `brightness = 0.8f, saturation = 0.8f` -> less offensive chroma tone
     *
     * @param initialHue the initial hue of the color, being any float value. the value is mod 360, so 360 is the same as 0.
     */
    class Chroma @JvmOverloads constructor(
        /**
         * the speed of this color, in nanoseconds.
         *
         * The speed refers to the amount of time it takes for this color to complete one cycle, e.g. from red, through to blue, through to green, then back to red.
         * */
        var speedNanos: Long = 5L.seconds,
        /** brightness of the color range (0.0 - 1.0) */
        brightness: Float = 1f,
        /** saturation of the color range (0.0 - 1.0) */
        saturation: Float = 1f,
        alpha: Float = 1f,
        initialHue: Float = 0f,
    ) : Animated(0f, saturation, brightness, alpha) {
        @Transient
        private var time: Long = ((initialHue % 360f) * speedNanos.toFloat()).toLong()

        @Deprecated("Chroma colors cannot be animated.", level = DeprecationLevel.ERROR)
        override fun recolor(target: Color, animation: Animation?) {
            // nop
        }

        override fun clone() = Chroma(speedNanos, brightness, saturation, alpha)

        /**
         * Convert this chroma color to a mutable color, which is a snapshot of this color at the time of calling this method.
         * @since 0.19.1
         */
        fun freeze() = Animated(hue, saturation, brightness, alpha)

        override fun update(deltaTimeNanos: Long): Boolean {
            time += deltaTimeNanos
            hue = (time % speedNanos) / speedNanos.toFloat()
            return false
        }

        // although this technically should be true, we don't want a chroma color preventing an element from being deleted.
        override val updating get() = false
        override val alwaysUpdates get() = true
    }
}

typealias Color = PolyColor
