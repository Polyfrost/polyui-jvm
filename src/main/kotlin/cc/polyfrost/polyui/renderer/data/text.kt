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

package cc.polyfrost.polyui.renderer.data

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.input.PolyText
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.TextAlign
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.origin
import cc.polyfrost.polyui.utils.*

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
    val size: Vec2<Unit>
) {
    protected lateinit var renderer: Renderer
    protected val autoSized = size == origin
    var textOffsetX = 0f
    var textOffsetY = 0f

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

    /**
     * @return the [Line] that encapsulates this character, the index of the character relative to the start of the line, and the index of this line
     * @since 0.18.5
     */
    abstract fun getLineByIndex(index: Int): Triple<Line, Int, Int>

    open fun calculate(renderer: Renderer) {
        this.renderer = renderer
    }

    open operator fun get(index: Int): Line = lines[index]

    abstract fun rescale(scaleX: Float, scaleY: Float): Float
}

/**
 * represents internally a collection of lines of text.
 * They are wrapped to fit the [size], and will not overflow when [full].
 *
 * @see SingleText
 * @see Text
 */
internal class MultilineText(
    text: PolyText,
    font: Font,
    fontSize: Float,
    textAlign: TextAlign = TextAlign.Left,
    size: Vec2<Unit>
) : Text(text, font, fontSize, textAlign, size) {
    override lateinit var lines: ArrayList<Line>

    override fun render(x: Float, y: Float, color: Color) {
        var y = y + textOffsetY
        lines.fastEach { (text, _, h) ->
            renderer.drawText(font, x, y, text, color, fontSize, textAlign)
            y += h
        }
    }

    override fun calculate(renderer: Renderer) {
        super.calculate(renderer)
        if (autoSized) {
            renderer.textBounds(font, text.string, fontSize, textAlign).also {
                size.a.px = it.width
                size.b.px = it.height
            }
            lines = arrayListOf(Line(text.string, size.width, size.height))
            return
        }
        lines = text.string.wrap(size.width, size.height, renderer, font, fontSize, textAlign).map {
            Line(it, renderer.textBounds(font, it, fontSize, textAlign) as Vec2<Unit>)
        }.toArrayList().also {
            if (it.isEmpty()) {
                it.add(Line("", 0f, fontSize))
            }
        }
        // todo this
        textOffsetY = if (lines.size * fontSize > size.height) {
            full = true
            size.height - lines.size * fontSize
        } else {
            full = false
            0f
        }
    }

    override fun getLineByIndex(index: Int): Triple<Line, Int, Int> {
        var i = 0
        lines.fastEachIndexed { lineIndex, line ->
            if (index <= i + line.text.length) {
                return Triple(line, index - i - lineIndex, lineIndex)
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
}

/**
 * Represents a single line of text, with its width. The height parameter is just shadowed from the [Text][cc.polyfrost.polyui.component.impl.Text] that contains it.
 * This text will instead of overflowing just render offset from the beginning, so it will always be able to be shown.
 * @see MultilineText
 * @see Text
 */
internal class SingleText(
    text: PolyText,
    font: Font,
    fontSize: Float,
    textAlign: TextAlign = TextAlign.Left,
    size: Vec2<Unit>
) : Text(text, font, fontSize, textAlign, size) {
    override var lines: ArrayList<Line> = arrayListOf(Line(text.string, size.width, size.height))
    var init = false
    override fun render(x: Float, y: Float, color: Color) {
        renderer.drawText(font, x + textOffsetX, y + textOffsetY, text.string, color, fontSize, textAlign)
    }

    override fun calculate(renderer: Renderer) {
        super.calculate(renderer)
        if (autoSized) {
            renderer.textBounds(font, text.string, fontSize, textAlign).also {
                size.a.px = it.width
                size.b.px = it.height
            }
            lines[0] = Line(text.string, size.width, size.height)
            return
        }
        lines[0] = Line(
            text.string,
            renderer.textBounds(font, text.string, fontSize, textAlign).also {
                if (!init && renderer.settings.debug && it.width > size.width) PolyUI.LOGGER.warn("Single line text overflow with initial bounds, is this intended? (text: $text.string, bounds: $size)")
            } as Vec2<Unit>
        )

        textOffsetX = if (lines[0].width > size.width) size.width - lines[0].width else 0f
        init = true
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
}

/** stores a line of text, its width, and its height. The height in 98% of times is just the font size. */
data class Line(val text: String, val width: Float, val height: Float) {
    constructor(text: String, bounds: Vec2<Unit>) : this(text, bounds.width, bounds.height)
}
