package cc.polyfrost.polyui.renderer

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.properties.Settings
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.units.Box
import cc.polyfrost.polyui.units.Unit

abstract class Renderer() {
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

    abstract fun drawText(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float)

    abstract fun drawWrappedText(
        font: Font,
        x: Float,
        y: Float,
        width: Float,
        text: String,
        color: Color,
        fontSize: Float
    ): Box<Unit.Pixel>

    abstract fun getTextWidth(font: Font, text: String, fontSize: Float): Float

    abstract fun drawImage(image: Image, x: Float, y: Float, colorMask: Color = Color.NONE)

    /** Create a new image. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary. */
    abstract fun createImage(fileName: String): Image

    /** Create a new font. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary. */
    abstract fun createFont(fileName: String): Font

    /** Create a new framebuffer. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary. */
    abstract fun createFramebuffer(width: Int, height: Int, type: Settings.BufferType): Framebuffer

    abstract fun deleteFramebuffer(fbo: Framebuffer)

    abstract fun bindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode = Framebuffer.Mode.ReadWrite)

    abstract fun unbindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode = Framebuffer.Mode.ReadWrite)
    abstract fun supportsRenderbuffer(): Boolean

    abstract fun drawRectangle(x: Float, y: Float, width: Float, height: Float, color: Color)

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
}