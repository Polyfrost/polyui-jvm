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
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.unit.TextAlign
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2

/**
 * # Renderer
 * The renderer is responsible for drawing all components to the screen, handling frame buffers, and more.
 * Please make sure to implement all the functions in this class, and you may want to familiarize yourself with how [cc.polyfrost.polyui.PolyUI] works.
 *
 * It is also responsible for loading and caching all images and fonts, but this is down to you as a rendering implementation to implement.
 * for these functions, such as [drawImage] and [drawText], an initialized [Font] or [Image] instance will be given. This class simply contains a filepath to the resource. You will need to load it, and cache it for future use (ideally).
 */
abstract class Renderer : AutoCloseable {
    /**
     * Override this to set a default font if the font isn't found. It is
     * also used internally for PolyUI debug rendering.
     * @see [Settings.resourcePolicy]
     */
    abstract val defaultFont: Font

    /**
     * Override this to set a default image if the image isn't found.
     * @see [Settings.resourcePolicy]
     */
    abstract val defaultImage: Image

    /** settings instance for this renderer. */
    val settings = Settings(this)

    /** the pixel ratio of the screen, used mainly on Apple screens which use high-dpi. */
    var pixelRatio: Float = 1f
        internal set

    /** hook into this renderer. */
    internal inline fun alsoRender(block: Renderer.() -> kotlin.Unit) =
        block()

    abstract fun beginFrame(width: Int, height: Int)
    abstract fun endFrame()
    abstract fun cancelFrame()

    /** Set the alpha for all future draw calls, in the range (0-1), until [reset][resetGlobalAlpha].
     *
     * Note that this call is capped by [capAlpha], so if a value higher than [capAlpha]'s value is set, it will just set it to that.*/
    abstract fun globalAlpha(alpha: Float)

    /** set a maximum alpha value for all future draw calls, in the range (0-1), until [reset][resetAlphaCap]. This is useful for fading in/out all of PolyUI, for example.
     *
     * **Note that this itself will not** set the global alpha, so use [globalAlpha] to do that. */
    abstract fun capAlpha(alpha: Float)

    /** reset the alpha cap. */
    fun resetAlphaCap() = capAlpha(1f)

    /** reset the global alpha to normal. */
    fun resetGlobalAlpha() = globalAlpha(1f)
    abstract fun translate(x: Float, y: Float)
    abstract fun scale(x: Float, y: Float)
    abstract fun rotate(angleRadians: Double)

    /**
     * draw text to the screen, per the given parameters.
     * @param width the wrap width for this text. If 0, you can just draw the string (it won't wrap). If it is not 0, wrap the string to the given width, strictly.
     * This means that the string will NEVER go over the given width, and will be cut off to the next line if it does.
     */
    abstract fun drawText(
        font: Font,
        x: Float,
        y: Float,
        width: Float = 0f,
        text: String,
        color: Color,
        fontSize: Float,
        textAlign: TextAlign = TextAlign.Left
    )

    /** calculate the bounds of this text, per the given parameters.
     * @return a Vec2 containing the width and height of the given string.
     */
    abstract fun textBounds(font: Font, text: String, fontSize: Float, textAlign: TextAlign): Vec2<Unit.Pixel>

    /** Function that can be called to explicitly initialize an image. This is used mainly for getting the size of an image, or to ensure an SVG has been rasterized. */
    abstract fun initImage(image: Image)

    abstract fun drawImage(image: Image, x: Float, y: Float, colorMask: Int = 0)

    abstract fun drawRoundImage(image: Image, x: Float, y: Float, radius: Float, colorMask: Int = 0)

    abstract fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Color)

    abstract fun drawRoundRectVaried(
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

    fun drawRoundRect(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float) =
        drawRoundRectVaried(x, y, width, height, color, radius, radius, radius, radius)

    abstract fun drawHollowRect(x: Float, y: Float, width: Float, height: Float, color: Color, lineWidth: Int)

    abstract fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float)

    /** Create a new framebuffer. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary. */
    abstract fun createFramebuffer(width: Int, height: Int, type: Settings.BufferType): Framebuffer

    abstract fun deleteFramebuffer(fbo: Framebuffer)

    abstract fun bindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode = Framebuffer.Mode.ReadWrite)

    abstract fun unbindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode = Framebuffer.Mode.ReadWrite)

    /** draw the given framebuffer to the screen. */
    abstract fun drawFramebuffer(
        fbo: Framebuffer,
        x: Float,
        y: Float,
        width: Float = fbo.width,
        height: Float = fbo.height
    )

    abstract fun supportsRenderbuffer(): Boolean

    /**
     * Cleanup the PolyUI instance.
     * Use this to free any native resources.
     */
    abstract fun cleanup()

    override fun close() {
        cleanup()
    }
}
