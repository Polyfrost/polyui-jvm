/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer.data

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.TextAlign
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.origin
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.fastEachIndexed
import cc.polyfrost.polyui.utils.toArrayList
import cc.polyfrost.polyui.utils.wrap

internal class RenderText(
    text: String,
    val font: Font,
    fontSize: Float,
    val textAlign: TextAlign = TextAlign.Left,
    val size: Vec2<Unit>
) {
    internal lateinit var lines: ArrayList<Line>
    internal var full = false
    private lateinit var renderer: Renderer
    private val autoSized = size == origin
    var text = text
        set(value) {
            field = value
            calculate(renderer)
        }
    var fontSize = fontSize
        set(value) {
            field = value
            calculate(renderer)
        }

    fun render(x: Float, y: Float, color: Color) {
        // renderer.drawHollowRect(x, y, size.width, size.height, color, 1f)
        var y = y
        lines.fastEach { (text, _, h) ->
            renderer.drawText(font, x, y, text, color, fontSize, textAlign)
            y += h
        }
    }

    fun calculate(renderer: Renderer) {
        this.renderer = renderer
        if (autoSized) {
            renderer.textBounds(font, text, fontSize, textAlign).also {
                size.a.px = it.width
                size.b.px = it.height
            }
            lines = arrayListOf(Line(text, size.width, size.height))
            return
        }
        lines = text.wrap(size.width, size.height, renderer, font, fontSize, textAlign)
            .also { full = it.second }.first.map {
                Line(it, renderer.textBounds(font, it, fontSize, textAlign) as Vec2<Unit>)
            }.toArrayList()
    }

    operator fun get(index: Int): Line {
        return lines[index]
    }

    /** @return the [Line] that encapsulates this character, the index of the character relative to the start of the line, and the index of this line*/
    fun getByCharIndex(index: Int): Triple<Line, Int, Int> {
        var i = 0
        lines.fastEachIndexed { lineIndex, it ->
            if (index <= i + it.string.length) return Triple(it, index - i - lineIndex, lineIndex)
            i += it.string.length
        }
        return if (lines.isEmpty()) {
            Triple(Line("", 0f, 0f), 0, 0)
        } else {
            Triple(lines.last(), lines.last().string.length, lines.size - 1)
        }
    }
}

/** stores a line of text, its width, and its height. The height in 98% of times is just the font size. */
data class Line(val string: String, val width: Float, val height: Float) {
    constructor(text: String, bounds: Vec2<Unit>) : this(text, bounds.width, bounds.height)
}
