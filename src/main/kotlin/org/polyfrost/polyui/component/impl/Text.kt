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
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.LinkedList
import org.polyfrost.polyui.utils.cl1
import org.polyfrost.polyui.utils.splitTo
import org.polyfrost.polyui.utils.wrap
import kotlin.math.max

open class Text(text: Translator.Text, font: Font = PolyUI.defaultFonts.regular, fontSize: Float = 12f, at: Vec2? = null, alignment: Align = AlignDefault, wrap: Float = 0f, visibleSize: Vec2? = null, focusable: Boolean = false, vararg children: Drawable?) :
    Drawable(at, alignment, visibleSize = visibleSize, focusable = focusable, children = children) {

    constructor(text: String, font: Font = PolyUI.defaultFonts.regular, fontSize: Float = 12f, at: Vec2? = null, alignment: Align = AlignDefault, wrap: Float = 0f, visibleSize: Vec2? = null, focusable: Boolean = false, vararg children: Drawable?) :
            this(Translator.Text.Simple(text), font, fontSize, at, alignment, wrap, visibleSize, focusable, *children)

    private val hasVisibleSize = visibleSize != null

    init {
        require(fontSize > 0f) { "Font size must be greater than 0" }
    }

    var wrap: Float = wrap
        set(value) {
            field = value
            updateTextBounds()
        }

    // asm: initially it is a dummy object to save need for a field
    // it is immediately overwritten by setup()
    // this is public so it can be inlined
    @Deprecated("Internal object, use text instead", replaceWith = ReplaceWith("text"), level = DeprecationLevel.ERROR)
    @ApiStatus.Internal
    var _translated = text
        set(value) {
            field = value
            if (initialized) updateTextBounds()
        }

    @Suppress("deprecation_error")
    var text: String
        inline get() = _translated.string
        set(value) {
            if (_translated.string == value) return
            val ev = Event.Change.Text(value)
            accept(ev)
            if (ev.cancelled) return
            _translated.string = value
            if (initialized) updateTextBounds()
        }

    @Transient
    protected val lines = LinkedList<String>()

    @Transient
    var font: Font = font
        set(value) {
            field = value
            updateTextBounds()
        }

    @Transient
    var fontSize = fontSize
        set(value) {
            field = value
            spacing = (font.lineSpacing - 1f) * value
            updateTextBounds()
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

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        val scale = cl1(scaleX, scaleY)
        fontSize *= scale
    }

    @Suppress("deprecation_error")
    override fun setup(polyUI: PolyUI): Boolean {
        if (initialized) return false
        palette = polyUI.colors.text.primary
        _translated = if (_translated is Translator.Text.Formatted) {
            polyUI.translator.translate(_translated.string, *(_translated as Translator.Text.Formatted).args)
        } else {
            polyUI.translator.translate(_translated.string)
        }
        // asm: in translation files \\n is used for new line for some reason
        text = text.replace("\\n", "\n")
        updateTextBounds(polyUI.renderer)
        super.setup(polyUI)
        return true
    }

    fun updateTextBounds(renderer: Renderer = this.renderer) {
        val wrap = if (!hasVisibleSize) wrap else visibleSize.x
        lines.clear()
        text.splitTo('\n', dest = lines)
        if (lines.isEmpty() || (lines.size == 1 && lines[0].isEmpty())) {
            size.x = 1f
            size.y = fontSize
            return
        }
        if (wrap == 0f) {
            var mx = 0f
            var ty = -spacing
            lines.fastEach {
                val bounds = renderer.textBounds(font, it, fontSize)
                mx = max(mx, bounds.x)
                ty += bounds.y + spacing
            }
            size.x = mx
            size.y = ty
            if (visibleSize.y == 0f) visibleSize.y = ty
            return
        }
        val cp = lines.copy()
        lines.clear()
        cp.fastEach {
            it.wrap(wrap, renderer, font, fontSize, lines)
        }
        val new = lines.size * (fontSize + spacing) - spacing
        if (visibleSize.y != 0f && new > visibleSize.y) {
            // asm: text is larger than its box, cut off the last lines, but a minimum of 1 line
            lines.cut(0, max((visibleSize.y / (fontSize + spacing)).toInt(), 1))
            size.y = visibleSize.y
        } else size.y = new
        size.x = wrap
        // asm: wrap was specified, but no height so just set it
        if (visibleSize.y == 0f) {
            visibleSize.y = size.y
        }
    }

    override fun calculateSize(): Vec2 {
        updateTextBounds(renderer)
        return size
    }
}
