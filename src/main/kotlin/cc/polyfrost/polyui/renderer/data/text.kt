/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer.data

import cc.polyfrost.polyui.PolyUI
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

/**
 * internal superclass that represents text for rendering.
 * @see MultilineText
 * @see SingleText
 */
internal abstract class Text(
    text: String,
    val font: Font,
    fontSize: Float,
    val textAlign: TextAlign = TextAlign.Left,
    val size: Vec2<Unit>
) {
    protected lateinit var renderer: Renderer
    protected val autoSized = size == origin

    /** weather the text is overflowing the [size] */
    var full = false

    /** storage of the lines of text */
    abstract var lines: ArrayList<Line>
        protected set
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

    abstract fun render(x: Float, y: Float, color: Color)

    /** @return the [Line] that encapsulates this character, the index of the character relative to the start of the line, and the index of this line */
    abstract fun getByCharIndex(index: Int): Triple<Line, Int, Int>

    open fun calculate(renderer: Renderer) {
        this.renderer = renderer
    }

    open operator fun get(index: Int): Line = lines[index]
}

/**
 * represents internally a collection of lines of text.
 * They are wrapped to fit the [size], and will not overflow when [full].
 *
 * @see SingleText
 * @see Text
 */
internal class MultilineText(
    text: String,
    font: Font,
    fontSize: Float,
    textAlign: TextAlign = TextAlign.Left,
    size: Vec2<Unit>
) : Text(text, font, fontSize, textAlign, size) {
    override lateinit var lines: ArrayList<Line>

    override fun render(x: Float, y: Float, color: Color) {
        // renderer.drawHollowRect(x, y, size.width, size.height, color, 1f)
        var y = y
        lines.fastEach { (text, _, h) ->
            renderer.drawText(font, x, y, text, color, fontSize, textAlign)
            y += h
        }
    }

    override fun calculate(renderer: Renderer) {
        super.calculate(renderer)
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

    override fun getByCharIndex(index: Int): Triple<Line, Int, Int> {
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

/**
 * Represents a single line of text, with its width. The height parameter is just shadowed from the [Text][cc.polyfrost.polyui.component.impl.Text] that contains it.
 * This text will instead of overflowing just render offset from the beginning, so it will always be able to be shown.
 * @see MultilineText
 * @see Text
 */
internal class SingleText(
    text: String,
    font: Font,
    fontSize: Float,
    textAlign: TextAlign = TextAlign.Left,
    size: Vec2<Unit>
) : Text(text, font, fontSize, textAlign, size) {
    override var lines: ArrayList<Line> = arrayListOf(Line(text, size.width, size.height))
    var init = false
    var textOffset: Float = 0f
    override fun render(x: Float, y: Float, color: Color) {
        if (textOffset != 0f) {
            renderer.pushScissor(x, y, size.width, size.height)
            renderer.drawText(font, x + textOffset, y, text, color, fontSize, textAlign)
            renderer.popScissor()
        } else {
            renderer.drawText(font, x, y, text, color, fontSize, textAlign)
        }
    }

    override fun calculate(renderer: Renderer) {
        super.calculate(renderer)
        if (autoSized) {
            renderer.textBounds(font, text, fontSize, textAlign).also {
                size.a.px = it.width
                size.b.px = it.height
            }
            lines[0] = Line(text, size.width, size.height)
            return
        }
        lines[0] = Line(
            text,
            renderer.textBounds(font, text, fontSize, textAlign).also {
                if (!init && renderer.settings.debug && it.width > size.width) PolyUI.LOGGER.warn("Single line text overflow with initial bounds, is this intended? (text: $text, bounds: $size)")
            } as Vec2<Unit>
        )

        textOffset = if (lines[0].width > size.width) size.width - lines[0].width else 0f
        init = true
    }

    override operator fun get(index: Int): Line {
        if (index != 0) throw IndexOutOfBoundsException("SingleText only has one line!")
        return lines[0]
    }

    override fun getByCharIndex(index: Int): Triple<Line, Int, Int> = Triple(lines[0], index, 0)
}

/** stores a line of text, its width, and its height. The height in 98% of times is just the font size. */
data class Line(val string: String, val width: Float, val height: Float) {
    constructor(text: String, bounds: Vec2<Unit>) : this(text, bounds.width, bounds.height)
}
