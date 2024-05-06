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

package org.polyfrost.polyui.renderer

import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.data.Framebuffer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Vec2

/**
 * # Renderer
 * The renderer is responsible for drawing all components to the screen, handling frame buffers, and more.
 * Please make sure to implement all the functions in this class, and you may want to familiarize yourself with how [org.polyfrost.polyui.PolyUI] works.
 *
 * It is also responsible for loading and caching all images and fonts, but this is down to you as a rendering implementation to implement.
 * for these functions, such as [image] and [text], an initialized [Font] or [PolyImage] instance will be given.
 * You can access the data using [Resource.stream][org.polyfrost.polyui.renderer.data.Resource.stream], and cache it for future use (ideally).
 */
interface Renderer : AutoCloseable {
    /**
     * This function is called during very early initialization, and should be used to prepare the renderer for use. This is always the first function called.
     * @since 0.21.2
     */
    fun init()

    /**
     * Begin a frame. this is called before all drawing calls.
     */
    fun beginFrame(width: Float, height: Float, pixelRatio: Float)

    /**
     * End a frame. this is called after all drawing calls.
     */
    fun endFrame()

    /** Set the alpha for all future draw calls, in the range (0-1), until [reset][resetGlobalAlpha].
     *
     */
    fun globalAlpha(alpha: Float)

    /**
     * Set a maximum alpha for all future draw calls, in the range (0-1).
     *
     * If this is set, calls to [globalAlpha] will not exceed this value.
     */
    fun setAlphaCap(cap: Float)

    /** reset the global alpha to normal.
     *
     * Respects the [setAlphaCap], so may not actually reset fully if cap is set to a value lower than 1.
     * @see globalAlpha
     */
    fun resetGlobalAlpha() = globalAlpha(1f)

    /**
     * translate the origin of all future draw calls by the given amount.
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     */
    fun translate(x: Float, y: Float)

    /**
     * scales all future draw calls by the given amount, around the given point, [px] and [py] (if [supported][transformsWithPoint])
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     */
    fun scale(sx: Float, sy: Float, px: Float, py: Float)

    /**
     * rotate all future draw calls by the given amount, around the given point, [px] and [py] (if [supported][transformsWithPoint])
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     */
    fun rotate(angleRadians: Double, px: Float, py: Float)

    /**
     * Skew all future draw calls by the given amount, around the given point, [px] and [py] (if [supported][transformsWithPoint])
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     *
     * @since 0.16.1
     */
    fun skewX(angleRadians: Double, px: Float, py: Float)

    /**
     * Skew all future draw calls by the given amount, around the given point, [px] and [py] (if [supported][transformsWithPoint])
     *
     * **you must** do all your transforms inside a [push], [pop] pair!
     *
     * @since 0.16.1
     */
    fun skewY(angleRadians: Double, px: Float, py: Float)

    /**
     * begin a scissor rectangle, that will clip rendering to the given rectangle.
     *
     * The scissor **is affected** by [translate], [scale], and [rotate], and is intersected with the previous scissor.
     *
     * **you must** call [popScissor] after you are done with this scissor!
     */
    fun pushScissor(x: Float, y: Float, width: Float, height: Float)

    /**
     * begin a scissor rectangle, that will clip rendering to the given rectangle. The rectangle is intersected with the previous scissor.
     *
     * The scissor **is affected** by [translate], [scale], and [rotate], and is intersected with the previous scissor.
     *
     * **you must** call [popScissor] after you are done with this scissor!
     */
    fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float)

    /** end a scissor.
     * @see pushScissor
     */
    fun popScissor()

    /**
     * Push the current state, saving all the current transforms before creating a new one with the same parameters as the current one.
     *
     * This is also called `saveState` or `pushMatrix` in many frameworks.
     *
     * [pop] - **you must** call this after you are finished!
     *
     * @since 0.17.2
     */
    fun push()

    /**
     * pop the current state, reverting all transforms to the previous values.
     *
     * This is also called `restoreState` or `popMatrix` in many frameworks.
     *
     * [push] - **you must** call this before you are begin!
     *
     * @since 0.17.2
     */
    fun pop()

    /**
     * draw text to the screen, per the given parameters. The string will already be wrapped to the given width.
     */
    fun text(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
    )

    /** calculate the bounds of this text, per the given parameters.
     * @return a Vec2 containing the width and height of the given string. If your API does not support returning string heights, just return the font size. The discrepancy should be negligible.
     */
    fun textBounds(font: Font, text: String, fontSize: Float): Vec2

    /** Function that can be called to explicitly initialize an image. This is used mainly for getting the size of an image, or to ensure an SVG has been rasterized. */
    fun initImage(image: PolyImage)

    /**
     * Draw an image to the screen, per the given parameters.
     */
    fun image(
        image: PolyImage,
        x: Float,
        y: Float,
        width: Float = image.width,
        height: Float = image.height,
        colorMask: Int = 0,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
    )

    /**
     * draw a rectangle to the screen, per the given parameters.
     *
     * If the radii are 0, this will just draw a normal rectangle. If they are not 0, it will draw a rounded rectangle.
     */
    fun rect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
    )

    /**
     * draw a hollow rectangle to the screen, per the given parameters.
     */
    fun hollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
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

    /** @see hollowRect */
    fun hollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        radius: Float = 0f,
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
        radii: FloatArray,
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
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float)

    /**
     * Draw a drop shadow to the screen, per the given parameters.
     */
    fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float)

    /**
     * Return true if your rendering implementation supports usage of framebuffers.
     *
     * If this method returns `false`, you can be safe that [createFramebuffer] and [bindFramebuffer], etc. will never be called.
     * @since 0.25.1
     */
    fun supportsFramebuffers(): Boolean

    /**
     * Return `true` if your rendering implementation supports usage of `x` and `y` parameters on [scale], [rotate], [skewX], and [skewY].
     *
     * If you return `false`, PolyUI will automatically using [translate] to mimic this behavior.
     *
     * @since 1.0.8
     */
    fun transformsWithPoint(): Boolean

    /** Create a new framebuffer. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary.
     * @return a PolyUI framebuffer object using the width and height passed to this method. This is used by PolyUI to identify it.
     */
    fun createFramebuffer(width: Float, height: Float): Framebuffer

    /** Bind the given framebuffer. */
    fun bindFramebuffer(fbo: Framebuffer)

    /** Unbind the currently bound framebuffer. */
    fun unbindFramebuffer()

    /** draw the given framebuffer to the screen. */
    fun drawFramebuffer(
        fbo: Framebuffer,
        x: Float,
        y: Float,
        width: Float = fbo.width,
        height: Float = fbo.height,
    )

    /** Delete the given framebuffer. Ignore if null. */
    fun delete(fbo: Framebuffer?)

    /** Delete the given font. Use this to free any native resources.
     * @since 0.20.1
     */
    fun delete(font: Font?)

    /** Delete the given image. Use this to free any native resources.
     * @since 0.20.1
     */
    fun delete(image: PolyImage?)

    /**
     * Cleanup this renderer.
     * Use this to free any native resources.
     */
    fun cleanup()

    override fun close() {
        cleanup()
    }
}
