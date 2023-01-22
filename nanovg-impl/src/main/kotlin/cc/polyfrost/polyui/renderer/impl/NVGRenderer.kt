package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.properties.Settings
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.units.Box
import cc.polyfrost.polyui.units.Unit
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGLUFramebuffer
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3
import org.lwjgl.opengl.GL30.*


class NVGRenderer : Renderer() {
    /** permanently allocated paint for a framebuffer. This is because so many are allocated so much that it is better just to permanently allocate one. */
    private val fboPaint: NVGPaint = NVGPaint.create()
    private val fbos: MutableMap<Framebuffer, NVGLUFramebuffer> = mutableMapOf()
    private var vg: Long = -1

    init {
        vg = NanoVGGL3.nvgCreate(if (settings.useAntialiasing) NanoVGGL3.NVG_ANTIALIAS else 0)
        if (vg == -1L) {
            throw ExceptionInInitializerError("Could not initialize NanoVG")
        }
    }

    fun checkInit() {
        if (vg == -1L) {
            throw ExceptionInInitializerError("NanoVG not initialized!")
        }
    }

    override fun beginFrame(width: Int, height: Int) {
        checkInit()
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glDisable(GL_ALPHA_TEST)
        nvgBeginFrame(vg, width.toFloat(), height.toFloat(), 1F)
    }

    override fun endFrame() = nvgEndFrame(vg)


    override fun cancelFrame() = nvgCancelFrame(vg)

    override fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    override fun scale(x: Float, y: Float) = nvgScale(vg, x, y)

    override fun rotate(angleRadians: Double) = nvgRotate(vg, angleRadians.toFloat())

    override fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float, width: Int, height: Int) {
        val framebuffer = fbos[fbo] ?: throw IllegalStateException("unknown framebuffer!")
        bindFramebuffer(fbo)
        nvgBeginPath(vg)
        nvgRect(vg, x, y, width.toFloat(), height.toFloat())
        nvgImagePattern(vg, x, y, width.toFloat(), height.toFloat(), 0F, framebuffer.image(), 1F, fboPaint)
        nvgFill(vg)
        unbindFramebuffer(fbo)
    }

    override fun drawText(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float) {

    }

    override fun drawWrappedText(
        font: Font,
        x: Float,
        y: Float,
        width: Float,
        text: String,
        color: Color,
        fontSize: Float
    ): Box<Unit.Pixel> {
        TODO("Not yet implemented")
    }

    override fun getTextWidth(font: Font, text: String, fontSize: Float): Float {
        TODO("Not yet implemented")
    }

    override fun drawImage(image: Image, x: Float, y: Float, colorMask: Color) {
        TODO("Not yet implemented")
    }

    override fun createImage(fileName: String): Image {
        TODO("Not yet implemented")
    }

    override fun createFont(fileName: String): Font {
        TODO("Not yet implemented")
    }

    override fun createFramebuffer(width: Int, height: Int, type: Settings.BufferType): Framebuffer {
        val f = Framebuffer(width, height, fbos.size + 69420, type)
        fbos[f] = NanoVGGL3.nvgluCreateFramebuffer(
            vg,
            width,
            height,
            NVG_IMAGE_REPEATX or NVG_IMAGE_REPEATY or NVG_IMAGE_GENERATE_MIPMAPS
        ) ?: throw ExceptionInInitializerError("Could not create: $f")
        return f
    }

    override fun deleteFramebuffer(fbo: Framebuffer) = NanoVGGL3.nvgluDeleteFramebuffer(
        vg,
        fbos[fbo] ?: throw IllegalStateException("Framebuffer not found when deleting it, already cleaned?")
    )

    override fun bindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode) =
        NanoVGGL3.nvgluBindFramebuffer(vg, fbos[fbo])

    override fun unbindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode) = NanoVGGL3.nvgluBindFramebuffer(vg, null)

    override fun supportsRenderbuffer(): Boolean {
        return false
    }

    override fun drawRectangle(x: Float, y: Float, width: Float, height: Float, color: Color) {
        nvgBeginPath(vg)
        nvgRect(vg, x, y, width, height)
        val nvgColor = color(color)
        nvgFill(vg)
        nvgColor.free()
    }

    override fun drawRectangleVaried(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float
    ) {
        nvgBeginPath(vg)
        nvgRoundedRectVarying(vg, x, y, width, height, topLeft, topRight, bottomLeft, bottomRight)
        val nvgColor = color(color)
        nvgFill(vg)
        nvgColor.free()
    }

    fun color(color: Color): NVGColor {
        val nvgColor = NVGColor.calloc()
        nvgRGBA(color.r, color.g, color.b, color.a, nvgColor)
        nvgFillColor(vg, nvgColor)
        return nvgColor
    }

}