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

@file:Suppress("UNCHECKED_CAST")

package org.polyfrost.polyui.renderer.data

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.input.PolyText
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.TextAlign
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.origin
import org.polyfrost.polyui.utils.*

/**
 * internal superclass that represents text for rendering.
 * @see MultilineText
 * @see SingleText
 */
internal abstract class Text(
    text: PolyText,
    val font: Font,
    fontSize: Float,
    val textAlign: TextAlign = TextAlign.Left,
    protected val renderer: Renderer,
) {
    var size: Vec2<Unit> = origin
        protected set
    val at: Vec2<Unit> = origin
    var textOffsetX = 0f
    var textOffsetY = 0f

    /** weather the text is overflowing the [size] */
    abstract val full: Boolean

    /** storage of the lines of text */
    abstract val lines: ArrayList<Line>
    var text = text
        set(value) {
            field = value
            calculate()
        }
    var fontSize = fontSize
        set(value) {
            field = value
            calculate()
        }

    abstract fun render(color: Color)

    /**
     * @return the [Line] that encapsulates this character, the index of the character relative to the start of the line, and the index of this line
     * @since 0.18.5
     */
    abstract fun getLineByIndex(index: Int): Triple<Line, Int, Int>

    abstract fun calculate()

    open operator fun get(index: Int): Line = lines[index]

    abstract fun rescale(scaleX: Float, scaleY: Float): Float

    override fun toString() = "('${text.string}', ${at.x} x ${at.y}, ${size.width} x ${size.height})"
}

/**
 * represents internally a collection of lines of text.
 * They are wrapped to fit the [maxSize], and will not overflow when [full].
 *
 *
 * @see SingleText
 * @see Text
 */
internal class MultilineText(
    text: PolyText,
    font: Font,
    fontSize: Float,
    textAlign: TextAlign = TextAlign.Left,
    renderer: Renderer,
    private val maxSize: Vec2<Unit>,
    private val truncates: Boolean = false,
) : Text(text, font, fontSize, textAlign, renderer) {
    override val lines = ArrayList<Line>(10)
    override val full: Boolean
        get() = textOffsetY != 0f

    override fun render(color: Color) {
        var yy = at.y + textOffsetY
        lines.fastEach { (text, w, h) ->
            when (textAlign) {
                TextAlign.Left -> renderer.text(font, at.x, yy, text, color, fontSize)
                TextAlign.Center -> renderer.text(font, at.x + (size.width - w) / 2f, yy, text, color, fontSize)
                TextAlign.Right -> renderer.text(font, at.x + (size.width - w), yy, text, color, fontSize)
            }
            yy += h
        }
    }

    override fun calculate() {
        if (truncates) {
            val max = maxSize.width * ((maxSize.height / fontSize).toInt() - 1)
            text.string = text.string.truncate(renderer, font, fontSize, max)
        }
        val split = text.string.split("\n").asArrayList()
        if (split.isEmpty()) {
            lines.add(Line("", 0f, fontSize))
        } else {
            split.fastEach { line ->
                val bounded = line.wrap(maxSize.width, renderer, font, fontSize).map {
                    Line(it, renderer.textBounds(font, it, fontSize) as Vec2<Unit>)
                }.asArrayList()
                lines.addAll(bounded)
            }
        }
        lines.fastEach { line ->
            size.a.px = maxOf(size.width, line.width)
            size.b.px += line.height
        }
        textOffsetY = if (maxSize.height != 0f && size.height > maxSize.height) {
            if (truncates) PolyUI.LOGGER.warn("should not have to offset a truncated text! Please report this!")
//            maxSize.height - size.height
            0f
        } else {
            0f
        }
    }

    override fun getLineByIndex(index: Int): Triple<Line, Int, Int> {
        var i = 0
        lines.fastEachIndexed { lineIndex, line ->
            if (index < i + line.text.length) {
                return Triple(line, index - i, lineIndex)
            }
            i += line.text.length
        }
        val l = lines.last()
        return Triple(l, l.text.length, lines.lastIndex)
    }

    override fun rescale(scaleX: Float, scaleY: Float): Float {
        fontSize *= scaleY
        return 1f
    }

    override fun toString() = "MultilineText${super.toString()}"
}

/**
 * Represents a single line of text, with its width. The height parameter is just shadowed from the [Text][org.polyfrost.polyui.component.impl.Text] that contains it.
 * This text will instead of overflowing just render offset from the beginning, so it will always be able to be shown.
 * @see MultilineText
 * @see Text
 */
internal class SingleText(
    text: PolyText,
    font: Font,
    fontSize: Float,
    textAlign: TextAlign = TextAlign.Left,
    renderer: Renderer,
    private val maxWidth: Float = -1f,
) : Text(text, font, fontSize, textAlign, renderer) {
    override val lines = ArrayList<Line>(1)

    init {
        // so set works later (dw bout it)
        lines.add(Line("", 0f, fontSize))
    }

    override val full get() = textOffsetX != 0f
    override fun render(color: Color) {
        val (text, w, _) = lines[0]
        when (textAlign) {
            TextAlign.Left -> renderer.text(font, at.x, at.y, text, color, fontSize)
            TextAlign.Center -> renderer.text(font, at.x + (size.width - w) / 2f, at.y, text, color, fontSize)
            TextAlign.Right -> renderer.text(font, at.x + (size.width - w), at.y, text, color, fontSize)
        }
    }

    override fun calculate() {
        size = renderer.textBounds(font, text.string, fontSize) as Vec2<Unit>
        lines[0] = Line(text.string, size.width, size.height)

        if (maxWidth != -1f) textOffsetX = if (lines[0].width > maxWidth) maxWidth - lines[0].width else 0f
    }

    override operator fun get(index: Int): Line {
        if (index != 0) throw IndexOutOfBoundsException("SingleText only has one line!")
        return lines[0]
    }

    override fun getLineByIndex(index: Int): Triple<Line, Int, Int> = Triple(lines[0], index, 0)

    override fun rescale(scaleX: Float, scaleY: Float): Float {
        // todo potentially something like this?
        // val old = if (autoSized) renderer.textBounds(font, text.string, fontSize, textAlign).a.px else size.a.px / scaleX
        // val new = renderer.textBounds(font, text.string, fontSize * scaleY, textAlign).a.px
        // val max = size.a.px
        fontSize *= scaleY
        return 1f
    }

    override fun toString() = "SingleText${super.toString()}"
}

/** stores a line of text, its width, and its height. The height in 98% of times is just the font size. */
data class Line(val text: String, val width: Float, val height: Float) {
    constructor(text: String, bounds: Vec2<Unit>) : this(text, bounds.width, bounds.height)
}
