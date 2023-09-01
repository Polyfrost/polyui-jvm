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

import org.jetbrains.skija.*
import org.polyfrost.polyui.color.PolyColor as Color
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.data.Framebuffer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.TextAlign
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.utils.toByteArray

class SkijaRenderer(width: Float, height: Float) : Renderer(width, height) {
    override var height: Float
        get() = super.height
        set(value) {
            super.height = value
            target = BackendRenderTarget.makeGL(width.toInt(), height.toInt(), 0, 8, 0, FramebufferFormat.GR_GL_RGBA8)
            surface = Surface.makeFromBackendRenderTarget(context, target, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB())
        }
    override var width: Float
        get() = super.width
        set(value) {
            super.width = value
            target = BackendRenderTarget.makeGL(width.toInt(), height.toInt(), 0, 8, 0, FramebufferFormat.GR_GL_RGBA8)
            surface = Surface.makeFromBackendRenderTarget(context, target, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB())
        }
    private val context = DirectContext.makeGL()
    private var target = BackendRenderTarget.makeGL(width.toInt(), height.toInt(), 0, 8, 0, FramebufferFormat.GR_GL_RGBA8)
    private var surface = Surface.makeFromBackendRenderTarget(context, target, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB())
    private val paint = Paint()
    private val fonts = HashMap<Font, Typeface>()
    private val images = HashMap<PolyImage, Image>()
    private var font = Font()
    private val radii = FloatArray(4)

    override fun init() {
    }

    override fun beginFrame() {
    }

    override fun endFrame() {
        context.flush()
    }

    override fun gblAlpha(alpha: Float) {
    }

    override fun translate(x: Float, y: Float) {
        surface.canvas.translate(x, y)
    }

    override fun scale(x: Float, y: Float) {
        surface.canvas.scale(x, y)
    }

    override fun rotate(angleRadians: Double) {
        surface.canvas.rotate((Math.toDegrees(angleRadians)).toFloat())
    }

    override fun skewX(angleRadians: Double) {
        surface.canvas.skew(Math.toDegrees(angleRadians).toFloat(), 0f)
    }

    override fun skewY(angleRadians: Double) {
        surface.canvas.skew(0f, Math.toDegrees(angleRadians).toFloat())
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
        surface.canvas.save()
        surface.canvas.clipRect(Rect.makeXYWH(x, y, width, height), ClipMode.INTERSECT)
    }

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) {
    }

    override fun popScissor() {
        surface.canvas.restore()
    }

    override fun push() {
        surface.canvas.save()
    }

    override fun pop() {
        surface.canvas.restore()
    }

    override fun text(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float, textAlign: TextAlign) {
        paint.apply {
            this.color = color.argb
            mode = PaintMode.FILL
        }
        set(font, fontSize)
        surface.canvas.drawString(text, x, y - this.font.metrics.ascent, this.font, paint)
    }

    override fun textBounds(font: Font, text: String, fontSize: Float, textAlign: TextAlign): Vec2<Unit.Pixel> {
        set(font, fontSize)
        val r = this.font.measureText(text)
        return Vec2(r.width.px, r.height.px)
    }

    override fun initImage(image: PolyImage) {
        get(image)
    }

    override fun image(image: PolyImage, x: Float, y: Float, width: Float, height: Float, colorMask: Int, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {
        paint.apply {
            color = colorMask
        }
        surface.canvas.drawImage(get(image), x, y, paint)
    }

    override fun rect(x: Float, y: Float, width: Float, height: Float, color: Color, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {
        set(color, x, y, width, height)
        _rect(x, y, width, height, topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius)
    }

    private fun set(color: Color, x: Float, y: Float, w: Float, h: Float) {
        if (color is Color.Gradient) {
            val colors = intArrayOf(color.argb1, color.argb2)
            val shader = when (color.type) {
                is Color.Gradient.Type.LeftToRight -> {
                    Shader.makeLinearGradient(x, y, x + w, y, colors, floatArrayOf(0f, 1f), GradientStyle.DEFAULT)
                }
                is Color.Gradient.Type.TopToBottom -> {
                    Shader.makeLinearGradient(x, y, x, y + h, colors, floatArrayOf(0f, 1f), GradientStyle.DEFAULT)
                }
                is Color.Gradient.Type.BottomLeftToTopRight -> {
                    Shader.makeLinearGradient(x, y + h, x + w, y, colors, floatArrayOf(0f, 1f), GradientStyle.DEFAULT)
                }
                is Color.Gradient.Type.TopLeftToBottomRight -> {
                    Shader.makeLinearGradient(x + w, y + h, x, y, colors, floatArrayOf(0f, 1f), GradientStyle.DEFAULT)
                }
                is Color.Gradient.Type.Box -> {
                    Shader.makeBlend(
                        BlendMode.SRC_OVER,
                        Shader.makeLinearGradient(x, y, x + w, y, colors, floatArrayOf(0f, 1f), GradientStyle.DEFAULT),
                        Shader.makeLinearGradient(x, y, x, y + h, colors, floatArrayOf(0f, 1f), GradientStyle.DEFAULT),
                    )
                }
                is Color.Gradient.Type.Radial -> {
                    Shader.makeRadialGradient(x + w / 2f, y + h / 2f, w / 2f, colors, floatArrayOf(0f, 1f), GradientStyle.DEFAULT)
                }
            }
            paint.shader = shader
        } else {
            paint.apply {
                this.color = color.argb
                mode = PaintMode.FILL
            }
        }
    }

    private fun set(font: Font, fontSize: Float) {
        this.font.apply {
            this.typeface = get(font)
            this.isSubpixel = true
            this.size = fontSize
        }
    }

    private fun get(image: PolyImage) = images[image] ?: run {
        Image.makeFromEncoded(image.get().toByteArray()).also {
            images[image] = it
        }
    }

    private fun get(font: Font) = fonts[font] ?: run {
        Typeface.makeFromData(Data.makeFromBytes(font.get().toByteArray())).also {
            fonts[font] = it
        }
    }

    override fun hollowRect(x: Float, y: Float, width: Float, height: Float, color: Color, lineWidth: Float, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {
        paint.apply {
            this.color = color.argb
            mode = PaintMode.STROKE
            strokeWidth = lineWidth
        }
        _rect(x, y, width, height, topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius)
    }

    private fun _rect(x: Float, y: Float, width: Float, height: Float, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {
        radii.apply {
            this[0] = topLeftRadius
            this[1] = topRightRadius
            this[2] = bottomLeftRadius
            this[3] = bottomRightRadius
        }
        surface.canvas.drawRRect(RRect.makeComplexXYWH(x, y, width, height, radii), paint)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        paint.apply {
            this.color = color.argb
            mode = PaintMode.STROKE
            strokeWidth = width
        }
        surface.canvas.drawLine(x1, y1, x2, y2, paint)
    }

    override fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float) {
        paint.apply {
            color = 0xFF000000.toInt()
            mode = PaintMode.STROKE
            strokeWidth = 1f
            maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blur, true)
        }
        _rect(x, y, width, height, radius, radius, radius, radius)
    }

    override fun createFramebuffer(width: Float, height: Float): Framebuffer {
        return Framebuffer(width, height)
    }

    override fun delete(fbo: Framebuffer?) {
    }

    override fun delete(font: Font?) {
    }

    override fun delete(image: PolyImage?) {
    }

    override fun bindFramebuffer(fbo: Framebuffer?) {
    }

    override fun unbindFramebuffer(fbo: Framebuffer?) {
    }

    override fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float, width: Float, height: Float) {
    }

    override fun cleanup() {
        surface.close()
    }
}
