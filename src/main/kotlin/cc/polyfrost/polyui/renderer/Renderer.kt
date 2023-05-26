/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
 * for these functions, such as [drawImage] and [drawText], an initialized [Font] or [PolyImage] instance will be given. This class simply contains a filepath to the resource. You will need to load it, and cache it for future use (ideally).
 */
abstract class Renderer(width: Float, height: Float) : AutoCloseable {
    var width: Float = width
        internal set
    var height: Float = height
        internal set
    var alphaCap: Float = 1f

    /** settings instance for this renderer. */
    @Suppress("LeakingThis")
    val settings = Settings(this)

    /** the pixel ratio of the screen, used mainly on Apple screens which use high-dpi. */
    var pixelRatio: Float = 1f
        internal set

    /** hook into this renderer. */
    @Suppress("UNUSED_EXPRESSION")
    internal inline fun alsoRender(block: Renderer.() -> kotlin.Unit) = block()

    abstract fun beginFrame()
    abstract fun endFrame()

    /** @see globalAlpha */
    protected abstract fun gblAlpha(alpha: Float)

    /** Set the alpha for all future draw calls, in the range (0-1), until [reset][resetGlobalAlpha].
     *
     * Note that this call is capped by [capAlpha], so if a value higher than [capAlpha]'s value is set, it will just set it to that.
     * @see capAlpha
     * */
    fun globalAlpha(alpha: Float) = gblAlpha(min(alphaCap, alpha))

    /** set a maximum alpha value for all future draw calls, in the range (0-1), until [reset][resetAlphaCap]. This is useful for fading in/out all of PolyUI, for example.
     *
     * **Note that this itself will not** set the global alpha, so use [gblAlpha] to do that. */
    fun capAlpha(alpha: Float) {
        alphaCap = alpha
    }

    /** reset the alpha cap. */
    fun resetAlphaCap() = capAlpha(1f)

    /** reset the global alpha to normal. */
    fun resetGlobalAlpha() = gblAlpha(1f)

    /**
     * translate the origin of all future draw calls by the given amount.
     *
     * **you must** call the inverse of this function ([translate(-x,-y)][translate]) when you are done with this transform!
     */
    abstract fun translate(x: Float, y: Float)

    /**
     * scales all future draw calls by the given amount.
     *
     * **you must** call the inverse of this function ([scale(1f/x,1f/y)][scale]) when you are done with this transform!
     */
    abstract fun scale(x: Float, y: Float)

    /**
     * rotate all future draw calls by the given amount.
     *
     * **you must** call the inverse of this function ([rotate(-angleRadians)][rotate]) when you are done with this transform!
     */
    abstract fun rotate(angleRadians: Double)

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
     * draw text to the screen, per the given parameters. The string will already be wrapped to the given width, and will be aligned according to the given [textAlign].
     */
    abstract fun drawText(
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

    abstract fun drawImage(
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
    abstract fun drawRect(
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

    abstract fun drawHollowRect(
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

    fun drawImage(image: PolyImage, x: Float, y: Float, width: Float = image.width, height: Float = image.height, radius: Float = 0f, colorMask: Int = 0) =
        drawImage(image, x, y, width, height, colorMask, radius, radius, radius, radius)

    fun drawImage(image: PolyImage, x: Float, y: Float, width: Float = image.width, height: Float = image.height, radii: FloatArray, colorMask: Int = 0) =
        drawImage(image, x, y, width, height, colorMask, radii[0], radii[1], radii[2], radii[3])

    fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float = 0f) =
        drawRect(x, y, width, height, color, radius, radius, radius, radius)

    fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Color, radii: FloatArray) =
        drawRect(x, y, width, height, color, radii[0], radii[1], radii[2], radii[3])

    fun drawHollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        radius: Float = 0f
    ) =
        drawHollowRect(x, y, width, height, color, lineWidth, radius, radius, radius, radius)

    fun drawHollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        radii: FloatArray
    ) =
        drawHollowRect(x, y, width, height, color, lineWidth, radii[0], radii[1], radii[2], radii[3])

    abstract fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float)

    abstract fun drawDropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float)

    /** Create a new framebuffer. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary. */
    abstract fun createFramebuffer(width: Float, height: Float): Framebuffer

    abstract fun deleteFramebuffer(fbo: Framebuffer)

    abstract fun bindFramebuffer(fbo: Framebuffer)

    abstract fun unbindFramebuffer(fbo: Framebuffer)

    /** draw the given framebuffer to the screen. */
    abstract fun drawFramebuffer(
        fbo: Framebuffer,
        x: Float,
        y: Float,
        width: Float = fbo.width,
        height: Float = fbo.height
    )

    /**
     * Cleanup the PolyUI instance.
     * Use this to free any native resources.
     */
    abstract fun cleanup()

    override fun close() {
        cleanup()
    }

    companion object {
        @JvmField
        var DefaultImage = PolyImage("err.png")
        @JvmField
        var DefaultFont = Font("Inter-Regular.ttf")
    }
}
