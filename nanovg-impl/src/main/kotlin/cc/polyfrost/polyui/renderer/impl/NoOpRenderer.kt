/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.unit.TextAlign
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.origin

class NoOpRenderer(width: Float, height: Float) : Renderer(width, height) {
    override fun beginFrame() {
    }

    override fun endFrame() {
    }

    override fun gblAlpha(alpha: Float) {
    }

    override fun translate(x: Float, y: Float) {
    }

    override fun scale(x: Float, y: Float) {
    }

    override fun rotate(angleRadians: Double) {
    }

    override fun skewX(angleRadians: Double) {
    }

    override fun skewY(angleRadians: Double) {
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
    }

    override fun popScissor() {
    }

    override fun push() {
    }

    override fun pop() {
    }

    override fun text(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
        textAlign: TextAlign
    ) {
    }

    @Suppress("UNCHECKED_CAST")
    override fun textBounds(font: Font, text: String, fontSize: Float, textAlign: TextAlign): Vec2<Unit.Pixel> {
        return origin as Vec2<Unit.Pixel>
    }

    override fun initImage(image: PolyImage) {
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
        bottomRightRadius: Float
    ) {
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
        bottomRightRadius: Float
    ) {
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
        bottomRightRadius: Float
    ) {
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
    }

    override fun dropShadow(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blur: Float,
        spread: Float,
        radius: Float
    ) {
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
    }
}
