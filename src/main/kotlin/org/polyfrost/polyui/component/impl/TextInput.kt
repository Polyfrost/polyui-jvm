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

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.*
import kotlin.math.max

open class TextInput(
    text: String = "",
    placeholder: String = "polyui.textinput.placeholder",
    font: Font? = null,
    fontSize: Float = 12f,
    at: Vec2? = null,
    alignment: Align = AlignDefault,
    visibleSize: Vec2? = null,
    vararg children: Drawable?,
) : Text(text.translated().dont(), font, fontSize, at, alignment, visibleSize, true, *children) {

    private val selectBoxes = ArrayList<Pair<Vec2, Vec2>>()

    // todo the old error stuff?

    var focused = false
        private set

    private val caretColor = PolyColor.WHITE

    private var cposx = 0f

    private var cposy = 0f

    private var caret = 0
        set(value) {
            field = value
            if (!selecting) select = value
        }

    private var select = 0

    private var selecting = false

    val selection get() = text.substringSafe(caret, select)

    private var _placeholder: Translator.Text = Translator.Text.Simple(placeholder)

    var placeholder: String
        get() = _placeholder.string
        set(value) {
            _placeholder.string = value
        }

    init {
        acceptsInput = true
    }

    override fun render() {
        if (focused) {
            alpha = 1f
            renderer.rect(cposx + this.x, cposy + this.y, 1.5f, fontSize, caretColor)
            selectBoxes.fastEach {
                val (x, y) = it.first
                val (w, h) = it.second
                renderer.rect(this.x + x, this.y + y, w, h, polyUI.colors.page.border20)
            }
        } else {
            alpha = 0.8f
        }
        if (text.isEmpty()) {
            alpha = 0.6f
            renderer.text(font, x, y, _placeholder.string, color, fontSize)
        } else super.render()
    }

    override fun accept(event: Event): Boolean {
        if (!enabled) return false
        needsRedraw = true
        val r = when (event) {
            is Event.Mouse.Entered -> {
                polyUI.cursor = Cursor.Text
                true
            }

            is Event.Mouse.Exited -> {
                polyUI.cursor = Cursor.Pointer
                true
            }

            is Event.Mouse.Clicked -> {
                clearSelection()
                return when (event.clicks) {
                    1 -> {
                        caretFromMouse(event.x, event.y)
                        focused
                    }

                    2 -> {
                        selectWordAroundCaret()
                        focused
                    }

                    else -> false
                }
            }

            is Event.Focused -> {
                accept(event)
            }

            else -> false
        }
        return if (!r) super.accept(event)
        else true
    }

    fun accept(event: Event.Focused): Boolean {
        when (event) {
            is Event.Focused.Gained -> {
                focused = true
                caretPos()
            }

            is Event.Focused.Lost -> {
                focused = false
                clearSelection()
            }

            is Event.Focused.KeyTyped -> {
                if (caret > text.length) caret = text.length
                if (event.mods.value < 2 /* mods == 0 || hasShift() */) {
                    if (caret != select) {
                        text = text.replace(selection, "")
                        caret = if (select > caret) caret else select
                        clearSelection()
                    }
                    val tl = text.length
                    text = text.substring(0, caret) + event.key + text.substring(caret)
                    if (text.length != tl) caret++
                } else if (event.mods.hasControl) {
                    when (event.key) {
                        'v', 'V' -> {
                            val tl = text.length
                            text = text.substring(0, caret) + (polyUI.clipboard ?: "") + text.substring(caret)
                            if (text.length != tl) caret += polyUI.clipboard?.length ?: 0
                            clearSelection()
                        }

                        'c', 'C' -> {
                            if (caret != select) {
                                polyUI.clipboard = selection
                            }
                        }

                        'x', 'X' -> {
                            text = text.replace(selection, "")
                            polyUI.clipboard = selection
                            clearSelection()
                        }

                        'a', 'A' -> {
                            caret = text.lastIndex + 1
                            select = 0
                        }
                    }
                }
                caretPos()
                selections()
            }

            is Event.Focused.KeyPressed -> {
                when (event.key) {
                    Keys.BACKSPACE -> {
                        if (select != caret) {
                            val f: Int
                            val t: Int
                            if (select > caret) {
                                f = caret
                                t = select
                            } else {
                                f = select
                                t = caret
                            }
                            val tl = text.length
                            text = text.substring(0, f) + text.substring(t)
                            if (tl != text.length) caret = f
                            clearSelection()
                        } else if (!event.mods.hasControl) {
                            val tl = text.length
                            text = text.dropAt(caret, 1)
                            if (caret != 0 && tl != text.length) caret--
                        } else {
                            dropToLastSpace()
                        }
                    }

                    Keys.TAB -> {
                        val tl = text.length
                        text += "    "
                        if (tl != text.length) caret += 4
                    }

                    Keys.DELETE -> {
                        if (caret + 1 > text.length) return true
                        text = text.dropAt(caret + 1, 1)
                    }

                    Keys.LEFT -> {
                        selecting = event.mods.hasShift
                        if (event.mods.hasControl) {
                            toLastSpace()
                        } else {
                            back()
                        }
                    }

                    Keys.RIGHT -> {
                        selecting = event.mods.hasShift
                        if (event.mods.hasControl) {
                            toNextSpace()
                        } else {
                            forward()
                        }
                    }

                    Keys.UP -> {
                        selecting = event.mods.hasShift
                        moveLine(false)
                    }

                    Keys.DOWN -> {
                        selecting = event.mods.hasShift
                        moveLine(true)
                    }

                    Keys.ESCAPE -> {
                        polyUI.unfocus()
                    }

                    else -> {}
                }
                caretPos()
                selections()
            }
        }
        return false
    }

    fun dropToLastSpace() {
        val tl = text.length
        val c: Int
        text.trimEnd().lastIndexOf(' ', caret).let {
            c = if (it != -1) {
                it
            } else {
                0
            }
        }
        text = text.substring(0, caret) + text.substring(c)
        if (tl != text.length) caret = c
    }

    private fun caretPos() {
        val (line, idx, lni) = getLineByIndex(caret)
        cposx = renderer.textBounds(
            font,
            line.substring(0, idx),
            fontSize,
        ).x
        cposy = lni * (fontSize + spacing)

        xScroll?.let {
            val s = it.from
            val e = s + visibleSize.x
            // todo make it autoscroll using this method !
        }
    }

    // todo make this work at some point
    fun mouseInput(mouseX: Float, mouseY: Float) {
        if (!selecting) {
            caretFromMouse(mouseX, mouseY)
            select = caret
            selecting = true
        }
        caretFromMouse(mouseX, mouseY)
        caretPos()
        selections()
    }

    fun selectWordAroundCaret() {
        var start = text.lastIndexOf(' ', caret - 1) + 1
        var end = text.indexOf(' ', caret)
        if (start == -1) start = 0
        if (end == -1) end = text.length
        selecting = true
        select = start
        caret = end
        selections()
        caretPos()
    }

    /**
     * `line to index to lineIndex to linePos`
     */
    private fun getLineByIndex(index: Int): Quad<String, Int, Int, Float> {
        require(index > -1) { "Index must not be negative" }
        var i = 0
        var y = spacing
        lines.fastEachIndexed { li, (it, bounds) ->
            if (index < i + it.length) {
                return Quad(it, index - i, li, y)
            }
            y += bounds.y + spacing
            i += it.length
        }
        val (ln, bounds) = lines.last()
        return Quad(ln, ln.length, lines.lastIndex, height - spacing - bounds.y)
    }

    private fun selections() {
        selectBoxes.clear()
        if (select == caret) return
        val start: Int
        val end: Int
        if (caret < select) {
            start = caret
            end = select
        } else {
            start = select
            end = caret
        }
        // startLine startIndex startLineIndex startLinePos
        val (sl, si, sli, slp) = getLineByIndex(start)
        val (el, ei, eli, elp) = getLineByIndex(end)
        if (sl === el) {
            selection(sl, si, end - start + si, slp)
            return
        }
        // funny - get it? realY and really are like the same !
        var really = slp + lines[si].second.y
        for (i in sli + 1 until eli) {
            val (_, bounds) = lines[i]
            selectBoxes.add((-1f by really) to bounds)
            really += bounds.y + spacing
        }
        selection(sl, si, sl.length, slp)
        selection(el, 0, ei, elp)
    }

    private fun selection(line: String, startIndex: Int, endIndex: Int, linePos: Float) {
        val start = renderer.textBounds(font, line.substring(0, startIndex), fontSize)
        val end = renderer.textBounds(font, line.substring(startIndex, endIndex), fontSize)
        val lh = max(start.y, end.y)
        selectBoxes.add((start.x - 1f by linePos) to (end.x by lh))
    }

    private fun caretFromMouse(mouseX: Float, mouseY: Float) {
        val line = ((mouseY - y) / (fontSize + spacing)).toInt()
        val p = lines[line.coerceAtMost(lines.lastIndex)].first.closestToPoint(renderer, font, fontSize, mouseX - x)
        caret = if (p == -1) text.length else p
        caretPos()
    }

    fun moveLine(down: Boolean) {
        caretFromMouse(cposx, cposy + if (down) spacing else -spacing)
    }

    fun toLastSpace() {
        while (caret > 0 && text[caret - 1] == ' ') caret--
        text.trimEnd().lastIndexOf(' ', caret - 1).let {
            if (selecting && select == caret) select = caret
            caret = if (it != -1) {
                it
            } else {
                0
            }
        }
    }

    fun toNextSpace() {
        while (caret < text.length - 1 && text[caret] == ' ') caret++
        text.indexOf(' ', caret).let {
            if (selecting && select == caret) select = caret
            caret = if (it != -1) {
                it
            } else {
                text.length
            }
        }
    }

    fun back() {
        if (selecting && select == caret) select = caret
        caret = if (caret > 0) {
            caret - 1
        } else {
            0
        }
    }

    fun forward() {
        if (selecting && select == caret) select = caret
        caret = if (caret < text.length - 1) {
            caret + 1
        } else {
            text.length
        }
    }

    fun clearSelection() {
        selecting = false
        select = caret
        selectBoxes.clear()
    }

    override fun setup(polyUI: PolyUI): Boolean {
        _placeholder = polyUI.translator.translate(_placeholder.string)
        return super.setup(polyUI)
    }

    override fun updateTextBounds(renderer: Renderer) {
        super.updateTextBounds(renderer)
        if (text.isEmpty()) {
            val bounds = renderer.textBounds(font, _placeholder.string, fontSize)
            size.smax(bounds)
            visibleSize.x = max(visibleSize.x, bounds.x)
        }
    }

    override fun debugString() = "placeholder: ${_placeholder.string}\ncaret: $caret;  select: $select;  selecting=$selecting\n${super.debugString()}"
}
