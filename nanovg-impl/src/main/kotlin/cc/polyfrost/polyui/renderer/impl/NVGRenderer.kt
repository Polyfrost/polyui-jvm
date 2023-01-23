package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.properties.Settings
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2
import cc.polyfrost.polyui.utils.IOUtils
import cc.polyfrost.polyui.utils.IOUtils.toByteBuffer
import cc.polyfrost.polyui.utils.px
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGLUFramebuffer
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3
import org.lwjgl.opengl.GL30.*
import java.nio.ByteBuffer


class NVGRenderer : Renderer() {
    /** permanently allocated paint for a framebuffer. This is because so many are allocated so much that it is better just to permanently allocate one. */
    private val fboPaint: NVGPaint = NVGPaint.create()
    private val fbos: MutableMap<Framebuffer, NVGLUFramebuffer> = mutableMapOf()
    private val images: MutableMap<Image, NVGImage> = mutableMapOf()
    private val fonts: MutableMap<Font, NVGFont> = mutableMapOf()
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

    override fun drawText(
        font: Font,
        x: Float,
        y: Float,
        width: Float?,
        text: String,
        color: Color,
        fontSize: Float
    ) {
        nvgBeginPath(vg)
        nvgFontSize(vg, fontSize)
        nvgFontFaceId(vg, getFont(font).id)
        nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
        val color = color(color)
        if (width != null) {
            nvgTextBox(vg, x, y, width, text)
        } else {
            nvgText(vg, x, y, text)
        }
        nvgFillColor(vg, color)
        color.free()
    }

    override fun getTextWidth(font: Font, text: String, fontSize: Float): Float {
        TODO("Not yet implemented")
    }

    override fun drawImage(image: Image, x: Float, y: Float, colorMask: Color) {
        val paint = NVGPaint.calloc()
        val img = getImage(image)
        nvgBeginPath(vg)
        nvgImagePattern(vg, x, y, img.width, img.height, 0F, img.id, 1F, paint)
        nvgRGBA(colorMask.r, colorMask.g, colorMask.b, colorMask.a, paint.innerColor())
        nvgRect(vg, x, y, img.width, img.height)
        nvgFillPaint(vg, paint)
        nvgFill(vg)
        paint.free()
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

    override fun drawRect(x: Float, y: Float, width: Float, height: Float, color: Color) {
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

    override fun textBounds(font: Font, text: String, fontSize: Float, wrapWidth: Float): Vec2<Unit.Pixel> {
        val out = FloatArray(4)
        nvgTextBounds(vg, 0F, 0F, text, out)
        return Vec2(out[2].px(), out[3].px())
    }

    private fun color(color: Color): NVGColor {
        val nvgColor = NVGColor.calloc()
        nvgRGBA(color.r, color.g, color.b, color.a, nvgColor)
        nvgFillColor(vg, nvgColor)
        return nvgColor
    }

    private fun getFont(font: Font): NVGFont {
        return fonts[font] ?: run {
            val data = IOUtils.getResourceAsStream(font.fileName).toByteBuffer()
            val ft = nvgCreateFontMem(vg, font.name, data, 0)
            NVGFont(ft, data).also { fonts[font] = it }
        }
    }

    private fun getImage(image: Image): NVGImage {
        return images[image] ?: run {
            val data = IOUtils.getResourceAsStream(image.fileName).toByteBuffer()
            val img = nvgCreateImageMem(vg, 0, data)
            NVGImage(img, image.width.toFloat(), image.height.toFloat(), data).also { images[image] = it }
        }
    }

    // used to ensure that the data is not discarded by the GC
    data class NVGImage(val id: Int, val width: Float, val height: Float, val data: ByteBuffer)
    data class NVGFont(val id: Int, val data: ByteBuffer)

}