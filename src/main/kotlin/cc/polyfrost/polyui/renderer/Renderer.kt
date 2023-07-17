/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

package cc.polyfrost.polyui.renderer

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.property.Settings
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.unit.TextAlign
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import kotlin.math.min

/**
 * # Renderer
 * The renderer is responsible for drawing all components to the screen, handling frame buffers, and more.
 * Please make sure to implement all the functions in this class, and you may want to familiarize yourself with how [cc.polyfrost.polyui.PolyUI] works.
 *
 * It is also responsible for loading and caching all images and fonts, but this is down to you as a rendering implementation to implement.
 * for these functions, such as [image] and [text], an initialized [Font] or [PolyImage] instance will be given.
 * This class simply contains a path to the resource. You will need to load it using [getResourceStream][cc.polyfrost.polyui.utils.getResourceStream], and cache it for future use (ideally).
 */
abstract class Renderer(open var width: Float, open var height: Float) : AutoCloseable {
    /**
     * set a maximum alpha value for all future draw calls, in the range (0-1), until [reset][resetAlphaCap]. This is useful for fading in/out all of PolyUI, for example.
     *
     * **Note that this itself will not** set the global alpha, so use [globalAlpha] to do that.
     */
    var alphaCap: Float = 1f

    /** settings instance for this renderer. */
    @Suppress("LeakingThis")
    val settings = Settings(this)

    /** the pixel ratio of the screen, used mainly on Apple screens which use high-dpi. */
    var pixelRatio: Float = 1f
        internal set

    /** hook into this renderer. */
    @Suppress("UNUSED_EXPRESSION")
    inline fun render(block: Renderer.() -> kotlin.Unit) = block()

    /**
     * Begin a frame. this is called before all drawing calls.
     */
    abstract fun beginFrame()

    /**
     * End a frame. this is called after all drawing calls.
     */
    abstract fun endFrame()

    /** implementation for [globalAlpha] */
    protected abstract fun gblAlpha(alpha: Float)

    /** Set the alpha for all future draw calls, in the range (0-1), until [reset][resetGlobalAlpha].
     *
     * Note that this call is capped by [alphaCap], so if a value higher than [alphaCap]'s value is set, it will just set it to that.
     * @see alphaCap
     * */
    fun globalAlpha(alpha: Float) = gblAlpha(min(alphaCap, alpha))

    /** reset the [alphaCap]. This is the same as doing [alphaCap]` = 1f`. */
    fun resetAlphaCap() {
        alphaCap = 1f
    }

    /** reset the global alpha to normal.
     *
     * Respects the [alphaCap], so may not actually reset fully if the [alphaCap] is set to a value lower than 1.
     * @see globalAlpha
     * @see resetAlphaCap
     */
    fun resetGlobalAlpha() = gblAlpha(1f)

    /**
     * translate the origin of all future draw calls by the given amount.
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     */
    abstract fun translate(x: Float, y: Float)

    /**
     * scales all future draw calls by the given amount.
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     */
    abstract fun scale(x: Float, y: Float)

    /**
     * rotate all future draw calls by the given amount.
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     */
    abstract fun rotate(angleRadians: Double)

    /**
     * Skew all future draw calls by the given amount.
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     *
     * @since 0.16.1
     */
    abstract fun skewX(angleRadians: Double)

    /**
     * Skew all future draw calls by the given amount.
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     *
     * @since 0.16.1
     */
    abstract fun skewY(angleRadians: Double)

    /**
     * begin a scissor rectangle, that will clip rendering to the given rectangle. The scissor is affected by [translate], [scale], and [rotate].
     *
     * **you must** call [popScissor] after you are done with this scissor!
     */
    abstract fun pushScissor(x: Float, y: Float, width: Float, height: Float)

    /** end a scissor.
     * @see pushScissor
     */
    abstract fun popScissor()

    /**
     * Push the current state, saving all the current transforms before creating a new one with the same parameters as the current one.
     *
     * This is also called `saveState` or `pushMatrix` in many frameworks.
     *
     * [pop] - **you must** call this after you are finished!
     *
     * @since 0.17.2
     */
    abstract fun push()

    /**
     * pop the current state, reverting all transforms to the previous values.
     *
     * This is also called `restoreState` or `popMatrix` in many frameworks.
     *
     * [push] - **you must** call this before you are begin!
     *
     * @since 0.17.2
     */
    abstract fun pop()

    /** @see push */
    fun save() = push()

    /** @see pop */
    fun restore() = pop()

    /**
     * draw text to the screen, per the given parameters. The string will already be wrapped to the given width, and will be aligned according to the given [textAlign].
     */
    abstract fun text(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
        textAlign: TextAlign = TextAlign.Left
    )

    /** calculate the bounds of this text, per the given parameters.
     * @return a Vec2 containing the width and height of the given string. If your API does not support returning string heights, just return the font size. The discrepancy should be negligible.
     */
    abstract fun textBounds(font: Font, text: String, fontSize: Float, textAlign: TextAlign): Vec2<Unit.Pixel>

    /** Function that can be called to explicitly initialize an image. This is used mainly for getting the size of an image, or to ensure an SVG has been rasterized. */
    abstract fun initImage(image: PolyImage)

    /**
     * Draw an image to the screen, per the given parameters.
     */
    abstract fun image(
        image: PolyImage,
        x: Float,
        y: Float,
        width: Float = image.width,
        height: Float = image.height,
        colorMask: Int = 0,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    )

    /**
     * draw a rectangle to the screen, per the given parameters.
     *
     * If the radii are 0, this will just draw a normal rectangle. If they are not 0, it will draw a rounded rectangle.
     */
    abstract fun rect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    )

    /**
     * draw a hollow rectangle to the screen, per the given parameters.
     */
    abstract fun hollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    )

    /** @see image */
    fun image(image: PolyImage, x: Float, y: Float, width: Float = image.width, height: Float = image.height, radius: Float = 0f, colorMask: Int = 0) =
        image(image, x, y, width, height, colorMask, radius, radius, radius, radius)

    /** @see image */
    fun image(image: PolyImage, x: Float, y: Float, width: Float = image.width, height: Float = image.height, radii: FloatArray, colorMask: Int = 0) =
        image(image, x, y, width, height, colorMask, radii[0], radii[1], radii[2], radii[3])

    /** @see rect */
    fun rect(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float = 0f) =
        rect(x, y, width, height, color, radius, radius, radius, radius)

    /** @see rect */
    fun rect(x: Float, y: Float, width: Float, height: Float, color: Color, radii: FloatArray) =
        rect(x, y, width, height, color, radii[0], radii[1], radii[2], radii[3])

    /**
     * Draw a circle to the screen, as per the given parameters. Note that the x,y is the top left of the circle box, and this is intentional; so it is in-line with the rest of the PolyUI rendering methods.
     */
    open fun circle(x: Float, y: Float, radius: Float, color: Color) =
        rect(x, y, radius + radius, radius + radius, color, radius)

    /** @see hollowRect */
    fun hollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        radius: Float = 0f
    ) =
        hollowRect(x, y, width, height, color, lineWidth, radius, radius, radius, radius)

    /** @see hollowRect */
    fun hollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        radii: FloatArray
    ) =
        hollowRect(x, y, width, height, color, lineWidth, radii[0], radii[1], radii[2], radii[3])

    /**
     * Draws a line between two points with the specified color and width.
     *
     * @param x1 The x-coordinate of the starting point.
     * @param y1 The y-coordinate of the starting point.
     * @param x2 The x-coordinate of the ending point.
     * @param y2 The y-coordinate of the ending point.
     * @param color The color of the line.
     * @param width The width of the line.
     */
    abstract fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float)

    /**
     * Draw a drop shadow to the screen, per the given parameters.
     */
    abstract fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float)

    /** Create a new framebuffer. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary.
     * @return a PolyUI framebuffer object using the width and height passed to this method. This is used by PolyUI to identify it.
     */
    abstract fun createFramebuffer(width: Float, height: Float): Framebuffer

    /** Bind the given framebuffer. Ignore if null. */
    abstract fun bindFramebuffer(fbo: Framebuffer?)

    /** Unbind the given framebuffer. if it is null, unbind everything. */
    abstract fun unbindFramebuffer(fbo: Framebuffer?)

    /** draw the given framebuffer to the screen. */
    abstract fun drawFramebuffer(
        fbo: Framebuffer,
        x: Float,
        y: Float,
        width: Float = fbo.width,
        height: Float = fbo.height
    )

    /** Delete the given framebuffer. Ignore if null. */
    abstract fun delete(fbo: Framebuffer?)

    /** Delete the given font. Use this to free any native resources.
     * @since 0.20.1
     */
    abstract fun delete(font: Font?)

    /** Delete the given image. Use this to free any native resources.
     * @since 0.20.1
     */
    abstract fun delete(image: PolyImage?)

    /**
     * Cleanup the PolyUI instance.
     * Use this to free any native resources.
     */
    abstract fun cleanup()

    /** @see cleanup */
    override fun close() {
        cleanup()
    }

    companion object {
        @JvmField
        var DefaultImage = PolyImage("err.png")

        @JvmField
        var DefaultFont = Font("Poppins-Regular.ttf")
    }
}
