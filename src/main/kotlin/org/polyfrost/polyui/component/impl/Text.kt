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

open class Text(text: Translator.Text, font: Font? = null, fontSize: Float = 12f, at: Vec2? = null, alignment: Align = AlignDefault, visibleSize: Vec2? = null, focusable: Boolean = false, vararg children: Drawable?) :
    Drawable(children = children, at, alignment, visibleSize = visibleSize, focusable = focusable) {
    constructor(text: String, font: Font? = null, fontSize: Float = 12f, at: Vec2? = null, alignment: Align = AlignDefault, visibleSize: Vec2? = null, focusable: Boolean = false, vararg children: Drawable?) :
            this(Translator.Text.Simple(text), font, fontSize, at, alignment, visibleSize, focusable, children = children)

    init {
        require(fontSize > 0f) { "Font size must be greater than 0" }
    }

    /**
     * Mode that this text was created in. Must be one of [UNLIMITED], [WRAP], [SCROLLING_SINGLE_LINE], [LIMITED_WRAP].
     * @since 1.4.1
     */
    protected val mode = if (visibleSize == null) UNLIMITED else when (visibleSize.y) {
        0f -> WRAP
        fontSize -> SCROLLING_SINGLE_LINE
        else -> LIMITED_WRAP
    }

    /**
     * @since 1.0.6
     */
    var strikethrough = false

    /**
     * @since 1.0.6
     */
    var underline = false

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
                val ff = y + height - spacing - 2f
                renderer.line(x, ff, x + width, ff, color, 1f)
            }
            y += height + spacing
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float, position: Boolean) {
        super.rescale(scaleX, scaleY, position)
        val scale = cl1(scaleX, scaleY)
        fontSize *= scale
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
            size.set(1f, fontSize)
            return
        }
        val mode = mode
        val maxWidth = when (mode) {
            WRAP, LIMITED_WRAP -> visibleSize.x
            else -> 0f
        }
        text.wrap(maxWidth, renderer, font, fontSize, lines)
        var w = 0f
        var h = 0f
        lines.fastEach { (_, bounds) ->
            w = kotlin.math.max(w, bounds.x)
            h += bounds.y + spacing
        }
        size.set(w, h)
        when (mode) {
            WRAP -> visibleSize.y = h
            LIMITED_WRAP, SCROLLING_SINGLE_LINE -> tryMakeScrolling()
        }
    }

    override fun calculateSize(): Vec2 {
        updateTextBounds()
        return size
    }

    override fun debugString() =
        """
lines: ${lines.size}, mode=${getModeName(mode)}
underline=$underline;  strike=$strikethrough;  italic=$italic
font: ${font.resourcePath.substringAfterLast('/')}; size: $fontSize;  weight: $fontWeight
        """

    companion object Mode {
        /**
         * Text can expand without any limits.
         *
         * Specified by a `null` [Drawable.visibleSize].
         * @since 1.4.1
         */
        const val UNLIMITED: Byte = 0

        /**
         * Text can expand infinitely vertically, but has a horizontal (wrap) limit.
         *
         * Specified by a [Drawable.visibleSize] of `Vec2(wrapLimit, 0f)`
         * @since 1.4.1
         */
        const val WRAP: Byte = 1

        /**
         * [WRAP], but has a limited amount of vertical lines.
         *
         * Specified by a [Drawable.visibleSize] of `Vec2(wrapLimit, maxHeight)`
         * @since 1.4.1
         */
        const val LIMITED_WRAP: Byte = 2

        /**
         * A single line of text which will scroll indefinitely.
         *
         * Specified by a [Drawable.visibleSize] of `Vec2(width, fontSize)`
         * @since 1.4.1
         */
        const val SCROLLING_SINGLE_LINE: Byte = 3

        /**
         * Return the name of the given constant.
         */
        @JvmStatic
        fun getModeName(mode: Byte) = when (mode) {
            UNLIMITED -> "UNLIMITED"
            WRAP -> "WRAP"
            LIMITED_WRAP -> "LIMITED_WRAP"
            SCROLLING_SINGLE_LINE -> "SCROLLING_SINGLE_LINE"
            else -> throw IllegalArgumentException("invalid mode $mode")
        }
    }
}
