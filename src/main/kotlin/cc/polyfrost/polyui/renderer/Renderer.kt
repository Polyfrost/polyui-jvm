package cc.polyfrost.polyui.renderer

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.property.Settings
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2

/**
 * # Renderer
 * The renderer is responsible for drawing all component to the screen, handling framebuffers, and more.
 * Please make sure to implement all the functions in this class, and you may want to familiarize yourself with how [cc.polyfrost.polyui.PolyUI] works.
 *
 * It is also responsible for loading and caching all images and fonts, but this is down to you as a rendering implementation to implement.
 * In the with these, such as [drawImage] and [drawText], an initialized [Font] or [Image] instance will be given. This class simply contains a filepath to the resource. You will need to load it, and cache it for future use (ideally).
 */
abstract class Renderer : AutoCloseable {
    /**
     * Override this to set a default font if the font isn't found. It is
     * used internally for PolyUI property.
     */
    abstract val defaultFont: Font
    val settings = Settings(this)

    internal inline fun alsoRender(block: Renderer.() -> kotlin.Unit) =
        block()

    abstract fun beginFrame(width: Int, height: Int)
    abstract fun endFrame()
    abstract fun cancelFrame()

    abstract fun globalAlpha(alpha: Float)

    abstract fun translate(x: Float, y: Float)
    abstract fun scale(x: Float, y: Float)
    abstract fun rotate(angleRadians: Double)

    abstract fun drawFramebuffer(
        fbo: Framebuffer,
        x: Float,
        y: Float,
        width: Float = fbo.width,
        height: Float = fbo.height,
    )

    abstract fun drawText(
        font: Font,
        x: Float,
        y: Float,
        width: Float = 0f,
        text: String,
        color: Color,
        fontSize: Float,
    )

    abstract fun drawImage(image: Image, x: Float, y: Float, colorMask: Int = 0)

    abstract fun drawRoundImage(image: Image, x: Float, y: Float, radius: Float, colorMask: Int = 0)

    /** Create a new framebuffer. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary. */
    abstract fun createFramebuffer(width: Int, height: Int, type: Settings.BufferType): Framebuffer

    abstract fun deleteFramebuffer(fbo: Framebuffer)

    abstract fun bindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode = Framebuffer.Mode.ReadWrite)

    abstract fun unbindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode = Framebuffer.Mode.ReadWrite)
    abstract fun supportsRenderbuffer(): Boolean

    abstract fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Color)

    /** Function that can be called to explicitly initialize an image. This is used mainly for getting the size of an image, or to ensure an SVG has been rasterized. */
    abstract fun initImage(image: Image)

    abstract fun drawRoundRectVaried(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float,
    )

    abstract fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float)

    fun drawRoundRect(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float) =
        drawRoundRectVaried(x, y, width, height, color, radius, radius, radius, radius)

    abstract fun textBounds(font: Font, text: String, fontSize: Float): Vec2<Unit.Pixel>

    /**
     * Cleanup the PolyUI instance.
     * Use this to free any native resources.
     */
    abstract fun cleanup()
    abstract fun drawHollowRect(x: Float, y: Float, width: Float, height: Float, color: Color, lineWidth: Int)

    override fun close() {
        cleanup()
    }
}