package cc.polyfrost.polyui.renderer

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.properties.Settings
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2

/**
 * # Renderer
 * The renderer is responsible for drawing all components to the screen, handling framebuffers, and more.
 * Please make sure to implement all the functions in this class, and you may want to familiarize yourself with how [cc.polyfrost.polyui.PolyUI] works.
 *
 *
 * It is also responsible for loading and caching all images and fonts, but this is down to you as a rendering implementation to implement.
 * In the with these, such as [drawImage] and [drawText], an initialized [Font] or [Image] instance will be given. This class simply contains a filepath to the resource. You will need to load it, and cache it for future use (ideally).
 *
 *
 */
abstract class Renderer {
    val settings: Settings = Settings(this)

    internal inline fun alsoRender(block: Renderer.() -> kotlin.Unit) {
        block()
    }

    abstract fun beginFrame(width: Int, height: Int)
    abstract fun endFrame()
    abstract fun cancelFrame()

    abstract fun translate(x: Float, y: Float)
    abstract fun scale(x: Float, y: Float)
    abstract fun rotate(angleRadians: Double)

    abstract fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float, width: Int = fbo.width, height: Int = fbo.height)

    abstract fun drawText(
        font: Font,
        x: Float,
        y: Float,
        width: Float? = null,
        text: String,
        color: Color,
        fontSize: Float
    )

    abstract fun getTextWidth(font: Font, text: String, fontSize: Float): Float

    abstract fun drawImage(image: Image, x: Float, y: Float, colorMask: Color = Color.NONE)

    /** Create a new framebuffer. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary. */
    abstract fun createFramebuffer(width: Int, height: Int, type: Settings.BufferType): Framebuffer

    abstract fun deleteFramebuffer(fbo: Framebuffer)

    abstract fun bindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode = Framebuffer.Mode.ReadWrite)

    abstract fun unbindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode = Framebuffer.Mode.ReadWrite)
    abstract fun supportsRenderbuffer(): Boolean

    abstract fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Color)

    abstract fun drawRectangleVaried(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float
    )

    fun drawRoundRectangle(x: Float, y: Float, width: Float, height: Float, color: Color, radius: Float) =
        drawRectangleVaried(x, y, width, height, color, radius, radius, radius, radius)

    abstract fun textBounds(font: Font, text: String, fontSize: Float, wrapWidth: Float): Vec2<Unit.Pixel>
}