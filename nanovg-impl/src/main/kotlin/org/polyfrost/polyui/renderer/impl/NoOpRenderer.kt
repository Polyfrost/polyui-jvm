/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
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

import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.unit.Vec2

object NoOpRenderer : Renderer {
    override fun init() {
        println("renderer is a no-op implementation")
    }

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float) {
    }

    override fun endFrame() {
    }

    override fun globalAlpha(alpha: Float) {
    }

    override fun translate(x: Float, y: Float) {
    }

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) {
    }

    override fun rotate(angleRadians: Double, px: Float, py: Float) {
    }

    override fun skewX(angleRadians: Double, px: Float, py: Float) {
    }

    override fun skewY(angleRadians: Double, px: Float, py: Float) {
    }

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
    }

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) {
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
    ) {
    }

    override fun textBounds(font: Font, text: String, fontSize: Float) = Vec2.ONE

    override fun initImage(image: PolyImage) {
        if (!image.size.isPositive) image.size = Vec2(18f, 18f)
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
        radius: Float,
    ) {
    }

    override fun delete(font: Font?) {
    }

    override fun delete(image: PolyImage?) {
    }

    override fun cleanup() {
    }


    override fun transformsWithPoint() = false
}
