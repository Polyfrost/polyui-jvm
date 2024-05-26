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

package org.polyfrost.polyui.component.impl

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.*

open class Text(text: Translator.Text, font: Font? = null, fontSize: Float = 12f, at: Vec2? = null, alignment: Align = AlignDefault, wrap: Float = 0f, visibleSize: Vec2? = null, focusable: Boolean = false, vararg children: Drawable?) :
    Drawable(children = children, at, alignment, visibleSize = visibleSize, focusable = focusable) {
    constructor(text: String, font: Font? = null, fontSize: Float = 12f, at: Vec2? = null, alignment: Align = AlignDefault, wrap: Float = 0f, visibleSize: Vec2? = null, focusable: Boolean = false, vararg children: Drawable?) :
            this(Translator.Text.Simple(text), font, fontSize, at, alignment, wrap, visibleSize, focusable, children = children)

    init {
        require(fontSize > 0f) { "Font size must be greater than 0" }
    }

    /**
     * @since 1.0.6
     */
    var strikethrough = false

    /**
     * @since 1.0.6
     */
    var underline = false

    var wrap: Float = wrap
        set(value) {
            if (field == value) return
            field = value
            if (initialized) updateTextBounds()
        }

    // asm: initially it is a dummy object to save need for a field
    // it is immediately overwritten by setup()
    // this is public so it can be inlined
    @ApiStatus.Internal
    var _translated = text
        set(value) {
            if (field == value) return
            field = value
            if (initialized) updateTextBounds()
        }

    @Suppress("deprecation_error")
    var text: String
        inline get() = _translated.string
        set(value) {
            if (_translated.string == value) return
            if (hasListenersFor(Event.Change.Text::class.java)) {
                val ev = Event.Change.Text(value)
                accept(ev)
                if (ev.cancelled) return
            }
            _translated.string = value
            if (initialized) updateTextBounds()
        }

    /**
     * A list of the lines of this text, and their corresponding width.
     */
    protected val lines = ArrayList<Line>()

    @ApiStatus.Internal
    var _font: Font? = font
        set(value) {
            if (field == value) return
            field = value
            if (initialized) updateTextBounds()
        }

    var font: Font
        inline get() = _font ?: throw UninitializedPropertyAccessException("font")
        set(value) {
            _font = value
            spacing = (font.lineSpacing - 1f) * fontSize
        }

    /**
     * The weight of the [font].
     *
     * Setting of this value only works if this font is a member of a family.
     * @since 1.0.7
     */
    var fontWeight: Font.Weight
        inline get() = font.weight
        set(value) {
            val fam = font.family
            if (fam == null) {
                PolyUI.LOGGER.error("cannot set font weight on $this: Font was not created in a family")
                return
            }
            font = fam.get(value, italic)
        }

    /**
     * `true` if [font] is italic
     *
     * Setting of this value only works if this font is a member of a family.
     * @since 1.0.7
     */
    var italic: Boolean
        inline get() = font.italic
        set(value) {
            val fam = font.family
            if (fam == null) {
                PolyUI.LOGGER.error("cannot set italic on $this: Font was not created in a family")
                return
            }
            font = fam.get(fontWeight, value)
        }

    /**
     * Tracker for the unscaled [fontSize]. You should set this instead of font size in most cases.
     * @since 1.2.0
     */
    var uFontSize = fontSize
        set(value) {
            if (field == value) return
            field = value
            fontSize = if (initialized) value * (polyUI.size.y / polyUI.iSize.y)
            else value
        }

    /**
     * Internal, scaled font size. You probably should be using [uFontSize] instead, as this is an internal object.
     */
    @ApiStatus.Internal
    var fontSize = fontSize
        set(value) {
            if (field == value) return
            field = value
            if (_font != null) spacing = (font.lineSpacing - 1f) * value
            if (initialized) updateTextBounds()
        }

    protected var spacing = 0f
        private set

    override fun render() {
        var y = this.y
        val strikethrough = strikethrough
        val underline = underline
        lines.fastEach { (it, bounds) ->
            val (width, height) = bounds
            renderer.text(font, x, y, it, color, fontSize)
            if (strikethrough) {
                val hf = y + height / 2f
                renderer.line(x, hf, x + width, hf, color, 1f)
            }
            if (underline) {
                val ff = y + height - spacing + 1f
                renderer.line(x, ff, x + width, ff, color, 1f)
            }
            y += height + spacing
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float, position: Boolean) {
        super.rescale(scaleX, scaleY, position)
        val scale = cl1(scaleX, scaleY)
        fontSize *= scale
        wrap *= scale
    }

    @Suppress("deprecation_error")
    override fun setup(polyUI: PolyUI): Boolean {
        if (initialized) return false
        palette = polyUI.colors.text.primary
        if (_translated !is Translator.Text.Dont) {
            _translated = if (_translated is Translator.Text.Formatted) {
                polyUI.translator.translate(_translated.string, *(_translated as Translator.Text.Formatted).args)
            } else {
                polyUI.translator.translate(_translated.string)
            }
            // asm: in translation files \\n is used for new line for some reason
            text = text.replace("\\n", "\n")
        }
        if (_font == null) _font = polyUI.fonts.regular
        updateTextBounds(polyUI.renderer)
        super.setup(polyUI)
        return true
    }

    open fun updateTextBounds(renderer: Renderer = this.renderer) {
        lines.clear()
        if (text.isEmpty()) {
            lines.add("" to (1f by fontSize))
            size.x = 1f
            size.y = fontSize
            return
        }
        val wrap = if (!hasVisibleSize) wrap else visibleSize.x
        val hasNewLn = '\n' in text
        if (wrap == 0f) {
            if (!hasNewLn) {
                val bounds = renderer.textBounds(font, text, fontSize)
                lines.add(text to bounds)
                size = bounds
                if (hasVisibleSize) visibleSize.y = bounds.y
                return
            }
            var mx = 0f
            var ty = -spacing

            val txt = text
            var start = 0
            var end = txt.indexOf('\n')
            while (end != -1) {
                val line = txt.substring(start, end)
                val bounds = renderer.textBounds(font, line, fontSize)
                lines.add(line to bounds)
                mx = mx.coerceAtLeast(bounds.x)
                ty += bounds.y + spacing
                start = end + 1
                end = txt.indexOf('\n', start)
            }
            val line = txt.substring(start)
            val bounds = renderer.textBounds(font, line, fontSize)
            lines.add(line to bounds)
            mx = mx.coerceAtLeast(bounds.x)
            ty += bounds.y + spacing
            size.x = mx
            size.y = ty
            if (hasVisibleSize) visibleSize.y = ty
            return
        }

        text.wrap(wrap, renderer, font, fontSize, lines)

        val new = lines.size * (fontSize + spacing) - spacing
        if (lines.size > 1 && hasVisibleSize && new > visibleSize.y) {
            // asm: text is larger than its box, cut off the last lines, but a minimum of 1 line
            lines.cut(0, ((visibleSize.y / (fontSize + spacing)).toInt() - 1).coerceIn(0, lines.lastIndex))
            size.y = visibleSize.y
        } else size.y = new
        size.x = wrap

    }

    override fun calculateSize(): Vec2 {
        updateTextBounds()
        // asm: wrap was specified, but no height so just set it
        if (hasVisibleSize && visibleSize.y == 0f) {
            visibleSize.y = size.y
        }
        return size
    }

    override fun debugString() =
        """
lines: ${lines.size}
underline=$underline;  strike=$strikethrough;  italic=$italic
font: ${font.resourcePath.substringAfterLast('/')}; size: $fontSize;  weight: $fontWeight
        """
}
