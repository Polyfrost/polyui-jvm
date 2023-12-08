/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.LinkedList
import org.polyfrost.polyui.utils.truncate
import org.polyfrost.polyui.utils.wrap

open class Text(text: String, font: Font = PolyUI.defaultFonts.regular, fontSize: Float = 12f, at: Vec2? = null, alignment: Align = AlignDefault, size: Vec2? = null, wrap: Float = fontSize * 15f, focusable: Boolean = false, vararg children: Drawable?) :
    Drawable(at, alignment, size, focusable = focusable, children = children) {
    @Transient
    protected val fixed = !this.size.isZero
    val isSingleLine get() = lines.size == 1

    @Transient
    var wrap = wrap
        set(value) {
            field = value
            size = calculateSize()
        }

    // asm: initially it is a dummy object to save need for a field
    // it is immediately overwritten by setup()
    // this is public so it can be inlined
    @Deprecated("Internal object, use text instead", replaceWith = ReplaceWith("text"), level = DeprecationLevel.ERROR)
    @ApiStatus.Internal
    var _translated = Translator.Text(text)
        private set

    @Suppress("deprecation_error")
    var text: String
        inline get() = _translated.string
        set(value) {
            if (_translated.string == value) return
            _translated.string = value
            size = calculateSize()
            accept(Event.Change.Text(value))
        }

    @Transient
    protected val lines = LinkedList<String>()

    @Transient
    var font: Font = font
        set(value) {
            field = value
            size = calculateSize()
        }

    @Transient
    var fontSize = fontSize
        set(value) {
            field = value
            spacing = (font.lineSpacing - 1f) * value
            size = calculateSize()
        }

    @Transient
    protected var spacing = (font.lineSpacing - 1f) * fontSize
        private set

    override fun render() {
        var y = this.y
        lines.fastEach {
            renderer.text(font, x, y, it, color, fontSize)
            y += fontSize + spacing
        }
    }

    @Suppress("deprecation_error")
    override fun setup(polyUI: PolyUI) {
        palette = polyUI.colors.text.primary
        _translated = polyUI.translator.translate(_translated.string)
        super.setup(polyUI)
        calculateSize()
    }

    override fun calculateSize(): Vec2 {
        needsRedraw = true
        var width = if (fixed) size.x else wrap
        var height = 0f
        val bounds = renderer.textBounds(font, text, fontSize)
        if (bounds.x < width) {
            // asm: fast path: string shorter than wrap
            width = bounds.x
            height = bounds.y
            lines.clear()
            lines.add(text)
        } else {
            if (fixed) text = text.truncate(renderer, font, fontSize, this.width * (height / fontSize).toInt())
            text.wrap(width, renderer, font, fontSize, lines)
            height = (lines.size - 1) * (fontSize + spacing)
        }
        return if (fixed) size else Vec2(width, height)
    }
}
