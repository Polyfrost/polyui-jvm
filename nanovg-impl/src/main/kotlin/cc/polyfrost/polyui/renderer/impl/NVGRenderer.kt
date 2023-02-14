/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.property.Settings
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.unit.TextAlign
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.px
import cc.polyfrost.polyui.utils.getResourceStream
import cc.polyfrost.polyui.utils.getResourceStreamNullable
import cc.polyfrost.polyui.utils.toByteBuffer
import org.lwjgl.nanovg.*
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImageResize
import org.lwjgl.system.MemoryUtil
import java.io.InputStreamReader
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class NVGRenderer : Renderer() {
    private val nvgPaint: NVGPaint = NVGPaint.create()
    private val nvgColor: NVGColor = NVGColor.malloc()
    private val nvgColor2: NVGColor = NVGColor.malloc()
    private val fbos: MutableMap<Framebuffer, NVGLUFramebuffer> = mutableMapOf()
    private val images: MutableMap<Image, NVGImage> = mutableMapOf()
    private val fonts: MutableMap<Font, NVGFont> = mutableMapOf()
    private var alphaCap = 1f
    private var vg: Long = -1

    init {
        vg = NanoVGGL3.nvgCreate(if (settings.useAntialiasing) NanoVGGL3.NVG_ANTIALIAS else 0)
        if (vg == -1L) {
            throw ExceptionInInitializerError("Could not initialize NanoVG")
        }
    }

    private fun checkInit() {
        if (vg == -1L) {
            throw ExceptionInInitializerError("NanoVG not initialized!")
        }
    }

    override fun beginFrame(width: Int, height: Int) {
        checkInit()
        nvgBeginFrame(vg, width.toFloat(), height.toFloat(), pixelRatio)
    }

    override fun endFrame() = nvgEndFrame(vg)

    override fun cancelFrame() = nvgCancelFrame(vg)

    override fun globalAlpha(alpha: Float) {
        val a = min(alphaCap, alpha)
        nvgGlobalAlpha(vg, a)
    }

    override fun capAlpha(alpha: Float) {
        this.alphaCap = alpha
    }

    override fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    override fun scale(x: Float, y: Float) = nvgScale(vg, x, y)

    override fun rotate(angleRadians: Double) = nvgRotate(vg, angleRadians.toFloat())

    override fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float, width: Float, height: Float) {
        val framebuffer = fbos[fbo] ?: throw IllegalStateException("unknown framebuffer!")
        drawImage(framebuffer.image(), x, y, width, height, 0)
    }

    override fun drawText(
        font: Font,
        x: Float,
        y: Float,
        width: Float,
        text: String,
        color: Color,
        fontSize: Float,
        textAlign: TextAlign
    ) {
        nvgBeginPath(vg)
        nvgFontSize(vg, fontSize)
        nvgFontFaceId(vg, getFont(font).id)
        nvgTextAlign(vg, textAlign(textAlign))
        color(color)
        nvgFillColor(vg, nvgColor)
        if (width != 0f) {
            nvgTextBox(vg, x, y, width, text)
        } else {
            nvgText(vg, x, y, text)
        }
    }

    override fun drawImage(
        image: Image,
        x: Float,
        y: Float,
        colorMask: Int,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    ) {
        val img = getImage(image)
        nvgImagePattern(vg, x, y, img.width, img.height, 0F, img.id, 1F, nvgPaint)
        if (colorMask != 0) {
            nvgRGBA(
                (colorMask shr 16 and 0xFF).toByte(),
                (colorMask shr 8 and 0xFF).toByte(),
                (colorMask and 0xFF).toByte(),
                (colorMask shr 24 and 0xFF).toByte(),
                nvgPaint.innerColor()
            )
        }
        nvgBeginPath(vg)
        nvgRoundedRectVarying(
            vg,
            x,
            y,
            img.width,
            img.height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius
        )
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun drawImage(img: Int, x: Float, y: Float, width: Float, height: Float, colorMask: Int = 0) {
        nvgImagePattern(vg, x, y, width, height, 0F, img, 1F, nvgPaint)
        if (colorMask != 0) {
            nvgRGBA(
                (colorMask shr 16 and 0xFF).toByte(),
                (colorMask shr 8 and 0xFF).toByte(),
                (colorMask and 0xFF).toByte(),
                (colorMask shr 24 and 0xFF).toByte(),
                nvgPaint.innerColor()
            )
        }
        nvgBeginPath(vg)
        nvgRect(vg, x, y, width, height)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    override fun createFramebuffer(width: Int, height: Int, type: Settings.BufferType): Framebuffer {
        val f = Framebuffer(width.toFloat(), height.toFloat(), type)
        fbos[f] = NanoVGGL3.nvgluCreateFramebuffer(
            vg,
            width,
            height,
            NVG_IMAGE_REPEATX or NVG_IMAGE_REPEATY
        ) ?: throw ExceptionInInitializerError("Could not create: $f (possibly an invalid sized layout?)")
        return f
    }

    override fun deleteFramebuffer(fbo: Framebuffer) {
        fbos.remove(fbo).also {
            NanoVGGL3.nvgluDeleteFramebuffer(
                vg,
                it ?: throw IllegalStateException("Framebuffer not found when deleting it, already cleaned?")
            )
        }
    }

    override fun bindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode) {
        NanoVGGL3.nvgluBindFramebuffer(vg, fbos[fbo])
        glClearColor(0F, 0F, 0F, 0F)
        glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
    }

    override fun unbindFramebuffer(fbo: Framebuffer, mode: Framebuffer.Mode) = NanoVGGL3.nvgluBindFramebuffer(vg, null)

    override fun supportsRenderbuffer(): Boolean {
        return false
    }

    override fun initImage(image: Image) {
        getImage(image)
    }

    override fun drawRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float
    ) {
        // note: nvg checks params and draws class rec if 0, so we don't need to
        nvgBeginPath(vg)
        nvgRoundedRectVarying(
            vg,
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius
        )
        if (color(color, x, y, width, height)) {
            nvgFillPaint(vg, nvgPaint)
        } else {
            nvgFillColor(vg, nvgColor)
        }
        nvgFill(vg)
    }

    override fun drawHollowRect(
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
    ) {
        nvgBeginPath(vg)
        nvgRoundedRectVarying(
            vg,
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius
        )
        nvgStrokeWidth(vg, lineWidth)
        if (color(color, x, y, width, height)) {
            nvgStrokePaint(vg, nvgPaint)
        } else {
            nvgStrokeColor(vg, nvgColor)
        }
        nvgStroke(vg)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        nvgBeginPath(vg)
        nvgMoveTo(vg, x1, y1)
        nvgLineTo(vg, x2, y2)
        nvgStrokeWidth(vg, width)
        if (color(color, x1, y1, x2, y2)) {
            nvgStrokePaint(vg, nvgPaint)
        } else {
            nvgStrokeColor(vg, nvgColor)
        }
        nvgStroke(vg)
    }

    override fun textBounds(font: Font, text: String, fontSize: Float, textAlign: TextAlign): Vec2<Unit.Pixel> {
        val out = FloatArray(4)
        nvgFontFaceId(vg, getFont(font).id)
        nvgTextAlign(vg, textAlign(textAlign))
        nvgFontSize(vg, fontSize)
        nvgTextBounds(vg, 0F, 0F, text, out)
        return Vec2(out[2].px, out[3].px)
    }

    private fun textAlign(textAlign: TextAlign): Int {
        return when (textAlign) {
            TextAlign.Left -> NVG_ALIGN_LEFT or NVG_ALIGN_TOP
            TextAlign.Center -> NVG_ALIGN_CENTER or NVG_ALIGN_TOP
            TextAlign.Right -> NVG_ALIGN_RIGHT or NVG_ALIGN_TOP
        }
    }

    private fun color(color: Color) {
        if (color is Color.Gradient) {
            nvgRGBA(color.r.toByte(), color.g.toByte(), color.b.toByte(), color.a.toByte(), nvgColor)
            nvgRGBA(
                color.color2.r.toByte(),
                color.color2.g.toByte(),
                color.color2.b.toByte(),
                color.color2.a.toByte(),
                nvgColor2
            )
        } else {
            nvgRGBA(color.r.toByte(), color.g.toByte(), color.b.toByte(), color.a.toByte(), nvgColor)
        }
    }

    private fun color(
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ): Boolean {
        color(color)
        if (color !is Color.Gradient) return false
        when (color.type) {
            is Color.Gradient.Type.TopToBottom -> nvgLinearGradient(
                vg,
                x,
                y,
                x,
                y + height,
                nvgColor,
                nvgColor2,
                nvgPaint
            )

            is Color.Gradient.Type.TopLeftToBottomRight -> nvgLinearGradient(
                vg,
                x,
                y,
                x + width,
                y + height,
                nvgColor,
                nvgColor2,
                nvgPaint
            )

            is Color.Gradient.Type.LeftToRight -> nvgLinearGradient(
                vg,
                x,
                y,
                x + width,
                y,
                nvgColor,
                nvgColor2,
                nvgPaint
            )

            is Color.Gradient.Type.BottomLeftToTopRight -> nvgLinearGradient(
                vg,
                x,
                y + height,
                x + width,
                y,
                nvgColor,
                nvgColor2,
                nvgPaint
            )

            is Color.Gradient.Type.Radial -> {
                nvgRadialGradient(
                    vg,
                    // smart-cast impossible: [x] is a public API property declared in different module (bit cringe)
                    if ((color.type as Color.Gradient.Type.Radial).centerX == -1f) x + (width / 2f) else (color.type as Color.Gradient.Type.Radial).centerX,
                    if ((color.type as Color.Gradient.Type.Radial).centerY == -1f) y + (height / 2f) else (color.type as Color.Gradient.Type.Radial).centerY,
                    (color.type as Color.Gradient.Type.Radial).innerRadius,
                    (color.type as Color.Gradient.Type.Radial).outerRadius,
                    nvgColor,
                    nvgColor2,
                    nvgPaint
                )
            }

            is Color.Gradient.Type.Box -> nvgBoxGradient(
                vg,
                x,
                y,
                width,
                height,
                (color.type as Color.Gradient.Type.Box).radius,
                (color.type as Color.Gradient.Type.Box).feather,
                nvgColor,
                nvgColor2,
                nvgPaint
            )
        }
        return true
    }

    private fun getFont(font: Font): NVGFont {
        return fonts[font] ?: run {
            val data =
                getResourceStreamNullable(font.fileName)?.toByteBuffer()
                    ?: if (settings.resourcePolicy == Settings.ResourcePolicy.WARN) {
                        getResourceStream(
                            DefaultFont.fileName
                        ).also {
                            PolyUI.LOGGER.warn(
                                "Failed to get font: {}, falling back to default font!",
                                font.fileName
                            )
                        }
                            .toByteBuffer()
                    } else {
                        throw ExceptionInInitializerError("Failed to get font: ${font.fileName}")
                    }
            val ft = nvgCreateFontMem(vg, font.name, data, 0)
            NVGFont(ft, data).also { fonts[font] = it }
        }
    }

    private fun getImage(image: Image): NVGImage {
        return images[image] ?: run {
            if (image.width != null && image.height == null) throw ExceptionInInitializerError("$image width is set but height is not!")
            val stream = getResourceStreamNullable(image.fileName)
                ?: if (settings.resourcePolicy == Settings.ResourcePolicy.WARN) {
                    getResourceStream(DefaultImage.fileName)
                        .also {
                            PolyUI.LOGGER.warn(
                                "Failed to get image: {}, falling back to default image!",
                                image.fileName
                            )
                        }
                } else {
                    throw ExceptionInInitializerError("Failed to get image: ${image.fileName}")
                }
            val data: ByteBuffer
            when (image.type) {
                Image.Type.PNG -> {
                    val w = IntArray(1)
                    val h = IntArray(1)
                    data = STBImage.stbi_load_from_memory(
                        stream.toByteBuffer(),
                        w,
                        h,
                        IntArray(1),
                        4
                    ).also {
                        if (it == null) {
                            throw Exception("Failed to initialize image: $image")
                        }
                        if (image.width == null) {
                            image.width = w[0]
                            image.height = h[0]
                        } else {
                            PolyUI.LOGGER.info("resizing $image: ${w[0]}x${h[0]} -> ${image.width}x${image.height}")
                            STBImageResize.stbir_resize_uint8(
                                it, w[0], h[0],
                                0, it,
                                image.width!!, image.height!!,
                                0, 4
                            )
                        }
                    } ?: throw Exception("Failed to initialize image: $image")
                }

                Image.Type.SVG -> {
                    val d = InputStreamReader(stream).readText() as CharSequence
                    val svg =
                        NanoSVG.nsvgParse(d, "px", 96F) ?: throw Exception("Failed to open SVG: $image (invalid data?)")
                    val raster = NanoSVG.nsvgCreateRasterizer()
                    val scale = if (image.width != null) {
                        max(image.width!! / svg.width(), (image.height ?: 0) / svg.height())
                    } else {
                        1F
                    }
                    image.width = (svg.width() * scale).toInt()
                    image.height = (svg.height() * scale).toInt()
                    data = MemoryUtil.memAlloc(image.width!! * image.height!! * 4)
                    NanoSVG.nsvgRasterize(
                        raster, svg,
                        0F, 0F,
                        scale, data,
                        image.width!!, image.height!!,
                        image.width!! * 4
                    )
                    NanoSVG.nsvgDeleteRasterizer(raster)
                    NanoSVG.nsvgDelete(svg)
                }
            }
            val img = nvgCreateImageRGBA(vg, image.width!!, image.height!!, 0, data)
            NVGImage(img, image.width!!.toFloat(), image.height!!.toFloat(), data).also { images[image] = it }
        }
    }

    override fun cleanup() {
        fonts.clear()
        images.values.forEach { nvgDeleteImage(vg, it.id) }
        images.clear()
        fbos.values.forEach { NanoVGGL3.nvgluDeleteFramebuffer(vg, it) }
        fbos.clear()
        nvgColor.free()
        nvgPaint.free()
        vg = -1
    }

    // used to ensure that the data is not discarded by the GC
    data class NVGImage(val id: Int, val width: Float, val height: Float, val data: ByteBuffer)
    data class NVGFont(val id: Int, val data: ByteBuffer)
}
