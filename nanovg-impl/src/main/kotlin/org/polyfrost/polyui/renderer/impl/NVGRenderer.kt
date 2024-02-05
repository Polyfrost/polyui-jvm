/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

package org.polyfrost.polyui.renderer.impl

import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGLUFramebuffer
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3.*
import org.lwjgl.opengl.GL20C.*
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImageResize
import org.lwjgl.system.MemoryUtil
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.property.Settings
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.data.Framebuffer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.toByteBuffer
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.max
import org.polyfrost.polyui.color.PolyColor as Color

class NVGRenderer(size: Vec2.Mut) : Renderer(size) {
    private val nvgPaint: NVGPaint = NVGPaint.malloc()
    private val nvgColor: NVGColor = NVGColor.malloc()
    private val nvgColor2: NVGColor = NVGColor.malloc()
    private val fbos = IdentityHashMap<Framebuffer, NVGLUFramebuffer>()
    private val images = IdentityHashMap<PolyImage, NVGImage>()
    private val fonts = IdentityHashMap<Font, NVGFont>()
    private var vg: Long = -1

    override fun init() {
        vg = nvgCreate(if (settings.useAntialiasing) NVG_ANTIALIAS else 0)
        require(vg != -1L) { "Could not initialize NanoVG" }
    }

    override fun beginFrame() {
        nvgBeginFrame(vg, size.x, size.y, pixelRatio)
    }

    override fun endFrame() = nvgEndFrame(vg)

    override fun gblAlpha(alpha: Float) = nvgGlobalAlpha(vg, alpha)

    override fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) = nvgScale(vg, sx, sy)

    override fun rotate(angleRadians: Double, px: Float, py: Float) = nvgRotate(vg, angleRadians.toFloat())

    override fun skewX(angleRadians: Double, px: Float, py: Float) = nvgSkewX(vg, angleRadians.toFloat())

    override fun skewY(angleRadians: Double, px: Float, py: Float) = nvgSkewY(vg, angleRadians.toFloat())

    override fun transformsWithPoint() = false

    override fun push() = nvgSave(vg)

    override fun pop() = nvgRestore(vg)

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) = nvgScissor(vg, x, y, width, height)

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) = nvgIntersectScissor(vg, x, y, width, height)

    override fun popScissor() = nvgResetScissor(vg)

    override fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float, width: Float, height: Float) {
        val framebuffer = fbos[fbo] ?: throw IllegalStateException("cannot draw $fbo as it is not known by this renderer")
        nvgImagePattern(vg, x, y, fbo.width, fbo.height, 0f, framebuffer.image(), 1f, nvgPaint)
        nvgBeginPath(vg)
        nvgRect(vg, x, y, width, height)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    override fun text(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
    ) {
        if (color.transparent) return
        nvgBeginPath(vg)
        nvgFontSize(vg, fontSize)
        nvgFontFaceId(vg, getFont(font).id)
        nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, x, y, text)
    }

    override fun image(
        image: PolyImage,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        colorMask: Int,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
    ) {
        val img = getImage(image)
        nvgImagePattern(vg, x, y, width, height, 0f, img.id, 1f, nvgPaint)
        if (colorMask != 0) {
            nvgRGBA(
                (colorMask shr 16 and 0xFF).toByte(),
                (colorMask shr 8 and 0xFF).toByte(),
                (colorMask and 0xFF).toByte(),
                (colorMask shr 24 and 0xFF).toByte(),
                nvgPaint.innerColor(),
            )
        }
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
            bottomLeftRadius,
        )
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    override fun supportsFramebuffers() = true

    override fun createFramebuffer(width: Float, height: Float): Framebuffer {
        val f = Framebuffer(width, height)
        fbos[f] = nvgluCreateFramebuffer(
            vg,
            width.toInt(),
            height.toInt(),
            0,
        ) ?: throw ExceptionInInitializerError("Could not create: $f (possibly an invalid sized layout?)")
        return f
    }

    override fun delete(fbo: Framebuffer?) {
        fbos.remove(fbo).also {
            if (it == null) {
                PolyUI.LOGGER.error("Framebuffer not found when deleting it, already cleaned?")
                return
            }
            nvgluDeleteFramebuffer(vg, it)
        }
    }

    override fun delete(font: Font?) {
        fonts.remove(font)
    }

    override fun delete(image: PolyImage?) {
        images.remove(image).also {
            if (it != null) {
                nvgDeleteImage(vg, it.id)
                MemoryUtil.memFree(it.data)
            }
        }
    }

    override fun bindFramebuffer(fbo: Framebuffer?) {
        if (fbo == null) return
        nvgEndFrame(vg)
        nvgluBindFramebuffer(vg, fbos[fbo] ?: throw NullPointerException("Cannot bind: $fbo does not exist!"))
        glViewport(0, 0, fbo.width.toInt(), fbo.height.toInt())
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        nvgBeginFrame(vg, fbo.width, fbo.height, pixelRatio)
    }

    override fun unbindFramebuffer(fbo: Framebuffer?) {
        nvgEndFrame(vg)
        nvgluBindFramebuffer(vg, null)
        glViewport(0, 0, (size.x * pixelRatio).toInt(), (size.y * pixelRatio).toInt())
        nvgBeginFrame(vg, size.x, size.y, pixelRatio)
    }

    override fun initImage(image: PolyImage) {
        getImage(image)
    }

    override fun rect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
    ) {
        if (color.transparent) return
        // note: nvg checks params and draws classic rect if 0, so we don't need to
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
            bottomLeftRadius,
        )
        if (color(color, x, y, width, height)) {
            nvgFillPaint(vg, nvgPaint)
        } else {
            nvgFillColor(vg, nvgColor)
        }
        nvgFill(vg)
    }

    override fun hollowRect(
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
    ) {
        if (color.transparent) return
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
            bottomLeftRadius,
        )
        nvgStrokeWidth(vg, lineWidth)
        if (color(color, x, y, width, height)) {
            nvgStrokePaint(vg, nvgPaint)
        } else {
            nvgStrokeColor(vg, nvgColor)
        }
        nvgStroke(vg)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        if (color.transparent) return
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

    override fun dropShadow(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blur: Float,
        spread: Float,
        radius: Float,
    ) {
        nvgBoxGradient(vg, x - spread, y - spread, width + spread * 2f, height + spread * 2f, radius + spread, blur, nvgColor, nvgColor2, nvgPaint)
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x - spread, y - spread - blur, width + spread * 2f + blur * 2f, height + spread * 2f + blur * 2f, radius + spread)
        nvgRoundedRect(vg, x, y, width, height, radius)
        nvgPathWinding(vg, NVG_HOLE)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    @Suppress("NAME_SHADOWING")
    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        // nanovg trims single whitespace, so add an extra one (lol)
        var text = text
        if (text.endsWith(' ')) {
            text += ' '
        }
        val out = FloatArray(4)
        nvgFontFaceId(vg, getFont(font).id)
        nvgTextAlign(vg, NVG_ALIGN_TOP or NVG_ALIGN_LEFT)
        nvgFontSize(vg, fontSize)
        nvgTextBounds(vg, 0f, 0f, text, out)
        val w = out[2] - out[0]
        val h = out[3] - out[1]
        return Vec2(w, h)
    }

    private fun color(color: Color) {
        if (color is Color.Gradient) {
            nvgRGBA(color.r.toByte(), color.g.toByte(), color.b.toByte(), color.a.toByte(), nvgColor)
            nvgRGBA(
                color.color2.r.toByte(),
                color.color2.g.toByte(),
                color.color2.b.toByte(),
                color.color2.a.toByte(),
                nvgColor2,
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
        height: Float,
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
                nvgPaint,
            )

            is Color.Gradient.Type.TopLeftToBottomRight -> nvgLinearGradient(
                vg,
                x,
                y,
                x + width,
                y + height,
                nvgColor,
                nvgColor2,
                nvgPaint,
            )

            is Color.Gradient.Type.LeftToRight -> nvgLinearGradient(
                vg,
                x,
                y,
                x + width,
                y,
                nvgColor,
                nvgColor2,
                nvgPaint,
            )

            is Color.Gradient.Type.BottomLeftToTopRight -> nvgLinearGradient(
                vg,
                x,
                y + height,
                x + width,
                y,
                nvgColor,
                nvgColor2,
                nvgPaint,
            )

            is Color.Gradient.Type.Radial -> {
                val type = color.type as Color.Gradient.Type.Radial
                nvgRadialGradient(
                    vg,
                    // smart-cast impossible: [x] is a public API property declared in different module (bit cringe)
                    if (type.centerX == -1f) x + (width / 2f) else type.centerX,
                    if (type.centerY == -1f) y + (height / 2f) else type.centerY,
                    type.innerRadius,
                    type.outerRadius,
                    nvgColor,
                    nvgColor2,
                    nvgPaint,
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
                nvgPaint,
            )
        }
        return true
    }

    private fun getFont(font: Font): NVGFont {
        return fonts.getOrPut(font) {
            val data = font.stream?.toByteBuffer() ?: if (settings.resourcePolicy == Settings.ResourcePolicy.WARN) {
                PolyUI.LOGGER.warn(
                    "Failed to get font: {}, falling back to default font!",
                    font.resourcePath,
                )
                PolyUI.defaultFonts.regular.get().toByteBuffer(false)
            } else {
                throw ExceptionInInitializerError("Failed to get font: ${font.resourcePath}")
            }
            val ft = nvgCreateFontMem(vg, font.name, data, false)
            NVGFont(ft, data)
        }
    }

    private fun getImage(image: PolyImage): NVGImage {
        return images.getOrPut(image) {
            var def = true
            val stream = image.stream ?: if (settings.resourcePolicy != Settings.ResourcePolicy.CRASH) {
                PolyUI.LOGGER.warn(
                    "Failed to get image: {}, falling back to default image!",
                    image.resourcePath,
                )
                def = false
                PolyUI.defaultImage.stream ?: throw IllegalStateException("Default image not found!")
            } else {
                throw ExceptionInInitializerError("Failed to get image: ${image.resourcePath}")
            }
            val type = if (!def) PolyUI.defaultImage.type else image.type
            val data: ByteBuffer
            when (type) {
                // let stb figure it out
                PolyImage.Type.Unknown, PolyImage.Type.Raster -> {
                    val w = IntArray(1)
                    val h = IntArray(1)
                    data = STBImage.stbi_load_from_memory(
                        stream.toByteBuffer(def),
                        w,
                        h,
                        IntArray(1),
                        4,
                    ).also {
                        if (it == null) {
                            PolyUI.LOGGER.error("STB error: ${STBImage.stbi_failure_reason()}")
                            throw Exception("Failed to initialize $image")
                        }
                        if (image.width == -1f || image.height == -1f) {
                            val sh = image.height != -1f
                            val sw = image.width != -1f
                            if (!sw) {
                                if (!sh) {
                                    image.width = w[0].toFloat()
                                    image.height = h[0].toFloat()
                                    return@also
                                } else {
                                    // !sw, sh
                                    val ratio = image.height / h[0].toFloat()
                                    image.width = w[0].toFloat() * ratio
                                }
                            } else {
                                // !sh, sw
                                val ratio = image.width / w[0].toFloat()
                                image.height = h[0].toFloat() * ratio
                            }
                        }
                        PolyUI.LOGGER.info("resizing image ${image.resourcePath}: ${w[0]}x${h[0]} -> ${image.width}x${image.height}")
                        STBImageResize.stbir_resize_uint8(
                            it, w[0], h[0],
                            0, it,
                            image.width.toInt(), image.height.toInt(),
                            0, 4,
                        )
                    } ?: throw Exception("Failed to initialize $image")
                }

                PolyImage.Type.Vector -> {
                    val RASTER_SCALE = 2f
                    val d = InputStreamReader(stream).readText()
                    val svg = NanoSVG.nsvgParse(d, "px", 96f) ?: throw Exception("Failed to open SVG: $image (invalid data?)")
                    val raster = NanoSVG.nsvgCreateRasterizer()
                    val scale = if (image.width != -1f || image.height != -1f) {
                        max(image.width / svg.width(), image.height / svg.height()) * RASTER_SCALE
                    } else {
                        RASTER_SCALE
                    }
                    image.width = svg.width() * scale
                    image.height = svg.height() * scale
                    data = MemoryUtil.memAlloc(image.width.toInt() * image.height.toInt() * 4)
                    NanoSVG.nsvgRasterize(
                        raster, svg,
                        0f, 0f,
                        scale, data,
                        image.width.toInt(), image.height.toInt(),
                        image.width.toInt() * 4,
                    )
                    STBImageResize.stbir_resize_uint8(
                        data, image.width.toInt(), image.height.toInt(),
                        0, data,
                        (image.width / RASTER_SCALE).toInt(), (image.height / RASTER_SCALE).toInt(),
                        0, 4,
                    )
                    image.size /= RASTER_SCALE
                    NanoSVG.nsvgDeleteRasterizer(raster)
                    NanoSVG.nsvgDelete(svg)
                }
            }
            val img = nvgCreateImageRGBA(vg, image.width.toInt(), image.height.toInt(), 0, data)
            NVGImage(img, image.width, image.height, data).also { images[image] = it }
        }
    }

    override fun cleanup() {
        nvgColor.free()
        nvgPaint.free()
        vg = -1
    }

    // used to ensure that the data is not discarded by the GC
    data class NVGImage(val id: Int, val width: Float, val height: Float, val data: ByteBuffer)
    data class NVGFont(val id: Int, val data: ByteBuffer)
}
