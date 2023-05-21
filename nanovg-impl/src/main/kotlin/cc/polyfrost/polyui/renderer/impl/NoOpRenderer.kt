/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) {
    }

    override fun popScissor() {
    }

    override fun drawText(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
        textAlign: TextAlign
    ) {
    }

    override fun textBounds(font: Font, text: String, fontSize: Float, textAlign: TextAlign): Vec2<Unit.Pixel> {
        return origin as Vec2<Unit.Pixel>
    }

    override fun initImage(image: PolyImage) {
    }

    override fun drawImage(
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
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
    }

    override fun drawDropShadow(
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

    override fun deleteFramebuffer(fbo: Framebuffer) {
    }

    override fun bindFramebuffer(fbo: Framebuffer) {
    }

    override fun unbindFramebuffer(fbo: Framebuffer) {
    }

    override fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float, width: Float, height: Float) {
    }

    override fun cleanup() {
    }
}
