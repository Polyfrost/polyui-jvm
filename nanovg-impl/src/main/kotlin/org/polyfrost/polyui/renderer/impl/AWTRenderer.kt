/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2024 Polyfrost and its contributors.
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

import org.lwjgl.nanovg.NanoSVG
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.data.Framebuffer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Vec2
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.IdentityHashMap
import javax.imageio.ImageIO

class AWTRenderer : Renderer {
    lateinit var g2d: Graphics2D
    var prev: AffineTransform? = null
    val colors = HashMap<Color, java.awt.Color>()
    val strokes = IdentityHashMap<Float, Stroke>()
    val images = IdentityHashMap<PolyImage, Image>()
    var prevC: Composite? = null
    val fonts = IdentityHashMap<Font, java.awt.Font>()

    fun begin(frame: Frame) {
        if (frame.bufferStrategy == null) frame.createBufferStrategy(2)
        g2d = frame.bufferStrategy.drawGraphics as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.translate(0, frame.insets.top)
        g2d.background = java.awt.Color.BLACK
    }

    override fun init() {}

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float) {
        g2d.clearRect(0, 0, width.toInt(), height.toInt())
        g2d.composite = AlphaComposite.SrcOver
    }

    override fun endFrame() {
    }

    override fun globalAlpha(alpha: Float) {
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
    }

    override fun setAlphaCap(cap: Float) {
        //ok
    }

    override fun translate(x: Float, y: Float) {
        g2d.translate(x.toDouble(), y.toDouble())
    }

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        g2d.scale(sx.toDouble(), sy.toDouble())
    }

    override fun rotate(angleRadians: Double, px: Float, py: Float) {
        g2d.rotate(angleRadians, px.toDouble(), py.toDouble())
    }

    override fun skewX(angleRadians: Double, px: Float, py: Float) {
        g2d.shear(angleRadians, 0.0)
    }

    override fun skewY(angleRadians: Double, px: Float, py: Float) {
        g2d.shear(0.0, angleRadians)
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
        g2d.clipRect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
    }

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) {
        pushScissor(x, y, width, height)
    }

    override fun popScissor() {
        g2d.clip = null
    }

    override fun push() {
        prev = g2d.transform
        prevC = g2d.composite
    }

    override fun pop() {
        g2d.transform = prev
        g2d.composite = prevC
    }

    override fun text(font: Font, x: Float, y: Float, text: String, color: Color, fontSize: Float) {
        g2d.font = (fonts.getOrPut(font) { java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, font.stream) }).deriveFont(fontSize)
        g2d.color = colors.getOrPut(color) { Color(color.argb, true) }
        g2d.drawString(text, x, y + fontSize)
    }

    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        g2d.font = (fonts.getOrPut(font) { java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, font.stream) }).deriveFont(fontSize)
        val bounds = g2d.fontMetrics.getStringBounds(text, g2d)
        return Vec2(bounds.width.toFloat(), fontSize)
    }

    override fun initImage(image: PolyImage) {
        getImage(image)
    }

    private fun getImage(image: PolyImage): Image {
        return images.getOrPut(image) {
            if (image.type == PolyImage.Type.Vector) {
                val d = InputStreamReader(image.stream!!).readText()
                val svg = NanoSVG.nsvgParse(d, "px", 96f) ?: throw Exception("Failed to open SVG: $image (invalid data?)")
                val raster = NanoSVG.nsvgCreateRasterizer()
                val w = svg.width().toInt()
                val h = svg.height().toInt()
                val buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
                NanoSVG.nsvgRasterize(raster, svg, 0f, 0f, 1f, buf, w, h, w * 4)
                val img = BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR)
                val a = ByteArray(buf.remaining())
                buf.get(a)
                img.raster.setDataElements(0, 0, w, h, a)
                image.size = Vec2.Immutable(w.toFloat(), h.toFloat())
                return@getOrPut img
            }
            val img = ImageIO.read(image.stream)
            image.size = Vec2.Immutable(img.width.toFloat(), img.height.toFloat())
            img
        }
    }

    private fun to(x: Float, y: Float) {
        g2d.translate(x.toDouble(), y.toDouble())
    }

    private fun uto(x: Float, y: Float) {
        g2d.translate(-x.toDouble(), -y.toDouble())
    }

    override fun image(image: PolyImage, x: Float, y: Float, width: Float, height: Float, colorMask: Int, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {
        to(x, y)
        g2d.drawImage(getImage(image), 0, 0, width.toInt(), height.toInt(), null)
        uto(x, y)
    }

    override fun rect(x: Float, y: Float, width: Float, height: Float, color: Color, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {
        g2d.color = colors.getOrPut(color) { Color(color.argb, true) }
        val aw = (topLeftRadius * 2f).toInt()
        to(x, y)
        g2d.fillRoundRect(0, 0, width.toInt(), height.toInt(), aw, aw)
        uto(x, y)
    }

    override fun hollowRect(x: Float, y: Float, width: Float, height: Float, color: Color, lineWidth: Float, topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float) {
        g2d.color = colors.getOrPut(color) { Color(color.argb, true) }
        g2d.stroke = strokes.getOrPut(lineWidth) { BasicStroke(lineWidth) }
        val aw = (topLeftRadius * 2f).toInt()
        to(x, y)
        g2d.drawRoundRect(0, 0, width.toInt(), height.toInt(), aw, aw)
        uto(x, y)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        g2d.color = colors.getOrPut(color) { Color(color.argb, true) }
        g2d.stroke = strokes.getOrPut(width) { BasicStroke(width) }
        g2d.drawLine(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
    }

    override fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, radius: Float) {}

    override fun supportsFramebuffers() = false

    override fun transformsWithPoint() = false

    override fun createFramebuffer(width: Float, height: Float) = throw NotImplementedError()

    override fun bindFramebuffer(fbo: Framebuffer) {}

    override fun unbindFramebuffer() {}

    override fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float, width: Float, height: Float) {}

    override fun delete(fbo: Framebuffer?) {}

    override fun delete(font: Font?) {
        fonts.remove(font)
    }

    override fun delete(image: PolyImage?) {
        images.remove(image)
    }

    override fun cleanup() {
        fonts.clear()
        colors.clear()
        strokes.clear()
        images.clear()
    }
}
