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
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.Text.Mode.LIMITED_WRAP
import org.polyfrost.polyui.component.impl.Text.Mode.SCROLLING_SINGLE_LINE
import org.polyfrost.polyui.component.impl.Text.Mode.UNLIMITED
import org.polyfrost.polyui.component.impl.Text.Mode.WRAP
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.*
import kotlin.math.roundToInt

open class Text(text: Translator.Text, font: Font? = null, fontSize: Float = 12f, at: Vec2 = Vec2.ZERO, alignment: Align = AlignDefault, visibleSize: Vec2 = Vec2.ZERO, focusable: Boolean = false, private val limited: Boolean = false, vararg children: Component?) :
    Drawable(children = children, at, alignment, visibleSize = visibleSize, focusable = focusable) {
    constructor(text: String, font: Font? = null, fontSize: Float = 12f, at: Vec2 = Vec2.ZERO, alignment: Align = AlignDefault, visibleSize: Vec2 = Vec2.ZERO, focusable: Boolean = false, limited: Boolean = false, vararg children: Component?) :
            this(Translator.Text.Simple(text), font, fontSize, at, alignment, visibleSize, focusable, limited, children = children)

    init {
        require(fontSize > 0f) { "Font size must be greater than 0" }
        if (limited) {
            require(visibleSize.isPositive && visibleSize.y >= fontSize) { "visibleSize must be set and have a height larger than the fontSize" }
            @Suppress("LeakingThis") // reason: this is safe as the only overrider is TextInput, and we don't do any unsafe operations
            shouldScroll = false
        }
    }

    /**
     * Mode that this text was created in. Must be one of [UNLIMITED], [WRAP], [SCROLLING_SINGLE_LINE], [LIMITED_WRAP].
     * @since 1.4.1
     */
    protected val mode get() = if (!visibleSize.isPositive) UNLIMITED else if (limited) LIMITED else when (visibleSize.y) {
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
    private var _text = text
        set(value) {
            if (field == value) return
            field = value
            if (initialized) updateTextBounds()
        }

    open var text: String
        get() = _text.string
        set(value) {
            if (_text.string == value) return
            val old = _text.string
            _text.string = value
            if (initialized) {
                // asm: in order for cancel to work, we need to update the text bounds first
                // this means that in the event we do cancel, we can restore the old text
                updateTextBounds()
                if (hasListenersFor(Event.Change.Text::class.java)) {
                    val ev = Event.Change.Text(value)
                    accept(ev)
                    if (ev.cancelled) {
                        // fuck. never mind!
                        _text.string = old
                        updateTextBounds()
                        return
                    }
                }
            }

        }

    /**
     * A list of the lines of this text, and their corresponding width.
     */
    protected val lines = ArrayList<Line>(/* initialCapacity = */ when (mode) {
        WRAP, LIMITED_WRAP, LIMITED -> (visibleSize.y / fontSize).toInt()
        else -> 1
    }
    )

    @ApiStatus.Internal
    @get:JvmName("getFontOrNull")
    @set:JvmName("setFontInternal")
    var _font: Font? = font
        set(value) {
            if (field == value) return
            field = value
            if (initialized) updateTextBounds()
        }

    var font: Font
        inline get() = _font ?: throw UninitializedPropertyAccessException("font must be initialized (see _font)")
        set(value) {
            _font = value
            spacing = (value.lineSpacing - 1f) * fontSize
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
            fontSize = if (initialized) value * (polyUI.size.y / polyUI.iSize.y) else value
            if (_font != null) spacing = (font.lineSpacing - 1f) * value
        }

    /**
     * Internal, scaled font size. You probably should be using [uFontSize] instead, as this is an internal object.
     */
    @ApiStatus.Internal
    var fontSize = fontSize
        set(value) {
            if (field == value) return
            field = value
            if (initialized) updateTextBounds()
        }

    protected var spacing = 0f
        private set

    override fun render() {
        var y = this.y
        val strikethrough = strikethrough
        val underline = underline
        val firstLine: Int
        val lastLine: Int
        // asm: don't render lines that are not visible
        if (mode == LIMITED_WRAP) {
            firstLine = ((screenAt.y - y) / (fontSize + spacing)).roundToInt().coerceAtLeast(0)
            val maxLines = (visibleSize.y / (fontSize + spacing)).roundToInt() + 1
            lastLine = maxLines + firstLine
        } else {
            firstLine = 0
            lastLine = 0
        }
        lines.fastEachIndexed { i, (it, bounds) ->
            val (width, height) = bounds
            if (it.isEmpty() || (mode == LIMITED_WRAP && (i !in firstLine..<lastLine))) {
                y += height + spacing
                return@fastEachIndexed
            }
            renderer.text(font, x, y, it, color, fontSize)
            if (strikethrough) {
                val hf = y + height / 2f
                renderer.line(x, hf, x + width, hf, color, 1f)
            }
            if (underline) {
                val ff = y + height - spacing
                renderer.line(x, ff, x + width, ff, color, 1f)
            }
            y += height + spacing
        }
    }

    override fun rescale0(scaleX: Float, scaleY: Float, withChildren: Boolean) {
        super.rescale0(scaleX, scaleY, withChildren)
        fontSize *= scaleY
    }

    @Suppress("deprecation_error")
    override fun setup(polyUI: PolyUI): Boolean {
        if (initialized) return false
        palette = polyUI.colors.text.primary
        // asm: replace the text with the translated text, if available
        if (_text !is Translator.Text.Dont) {
            _text = if (_text is Translator.Text.Formatted) {
                polyUI.translator.translate(_text.string, *(_text as Translator.Text.Formatted).args)
            } else {
                polyUI.translator.translate(_text.string)
            }
            // asm: in translation files \\n is used for new line for some reason
            text = text.replace("\\n", "\n")
        }
        if (_font == null) font = polyUI.fonts.regular
        updateTextBounds(polyUI.renderer)
        super.setup(polyUI)
        return true
    }

    open fun updateTextBounds(renderer: Renderer = this.renderer) {
        lines.clear()
        if (text.isEmpty()) {
            lines.add(Line("", (1f by fontSize)))
            width = 1f
            height = fontSize
            return
        }
        val maxWidth = when (mode) {
            LIMITED, WRAP, LIMITED_WRAP -> visibleSize.x
            else -> 0f
        }
        text.wrap(maxWidth, renderer, font, fontSize, lines)
        var w = 0f
        var h = 0f
        var i = 0
        lines.fastEach { (str, bounds) ->
            w = kotlin.math.max(w, bounds.x)
            h += bounds.y + spacing
            if (mode == LIMITED && h >= visibleSize.y - fontSize) {
                h -= spacing
                // safe to not re-measure as we know the bounds will contain it.
                // also won't co-mod thanks to fastEach
                lines[i] = Line(str.truncate(renderer, font, fontSize, w), bounds)
                lines.cut(0, i)
                width = w
                height = h
                visHeight = h
                return
            }
            i++
        }
        h -= spacing
        width = w
        height = h
        when (mode) {
            WRAP -> visHeight = h
            LIMITED_WRAP, SCROLLING_SINGLE_LINE -> tryMakeScrolling()
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("calculateSize")
    override fun calculateSize(): Vec2 {
        updateTextBounds()
        return size
    }

    override fun fixVisibleSize() {
        // due to how we use the scrolling mechanic, this method is not needed.
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
         * Some text which is **not allowed to scroll**. It is instead trimmed using [truncate].
         */
        const val LIMITED: Byte = 4

        /**
         * Return the name of the given constant.
         */
        @JvmStatic
        fun getModeName(mode: Byte) = when (mode) {
            UNLIMITED -> "UNLIMITED"
            WRAP -> "WRAP"
            LIMITED_WRAP -> "LIMITED_WRAP"
            SCROLLING_SINGLE_LINE -> "SCROLLING_SINGLE_LINE"
            LIMITED -> "LIMITED"
            else -> throw IllegalArgumentException("invalid mode $mode")
        }
    }
}
