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
import org.polyfrost.polyui.utils.LinkedList
import org.polyfrost.polyui.utils.closestToPoint
import org.polyfrost.polyui.utils.dropAt
import org.polyfrost.polyui.utils.substringSafe
import kotlin.math.min

class TextInput(
    text: String = "",
    placeholder: String = "polyui.textinput.placeholder",
    font: Font = PolyUI.defaultFonts.regular,
    fontSize: Float = 12f,
    at: Vec2? = null,
    alignment: Align = AlignDefault,
    visibleSize: Vec2? = null,
    wrap: Float = 0f,
    vararg children: Drawable?,
) : Text(text, font, fontSize, at, alignment, wrap, visibleSize, true, *children) {
    @Transient
    private val linesData = LinkedList<Float>()

    @Transient
    private val selectBoxes = LinkedList<Pair<Pair<Float, Float>, Pair<Float, Float>>>()

    // todo the old error stuff?

    @Transient
    var focused = false
        private set

    @Transient
    private val caretColor = PolyColor.WHITE

    @Transient
    private var cposx = 0f

    @Transient
    private var cposy = 0f

    @Transient
    private var caret = 0
        set(value) {
            field = value
            if (!selecting) select = value
        }

    @Transient
    private var select = 0

    @Transient
    private var selecting = false

    val selection get() = text.substringSafe(caret, select)

    private var _placeholder: Translator.Text = Translator.Text.Simple(placeholder)

    init {
        acceptsInput = true
    }

    override fun render() {
        if (focused) {
            alpha = 1f
            renderer.rect(cposx, cposy, 2f, fontSize, caretColor)
            selectBoxes.fastEach {
                val (x, y) = it.first
                val (w, h) = it.second
                renderer.rect(x, y, w, h, polyUI.colors.page.border20)
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
        return when (event) {
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
                        caretFromMouse(event.mouseX, event.mouseY)
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

            else -> super.accept(event)
        }
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
                if (event.mods < 2 /* mods == 0 || hasShift() */) {
                    if (caret != select) {
                        text = text.replace(selection, "")
                        caret = if (select > caret) caret else select
                        clearSelection()
                    }
                    val tl = text.length
                    text = text.substring(0, caret) + event.key + text.substring(caret)
                    if (text.length != tl) caret++
                } else if (event.hasControl()) {
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
                        } else if (!event.hasControl()) {
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
                        selecting = event.hasShift()
                        if (event.hasControl()) {
                            toLastSpace()
                        } else {
                            back()
                        }
                    }

                    Keys.RIGHT -> {
                        selecting = event.hasShift()
                        if (event.hasControl()) {
                            toNextSpace()
                        } else {
                            forward()
                        }
                    }

                    Keys.UP -> {
                        selecting = event.hasShift()
                        moveLine(false)
                    }

                    Keys.DOWN -> {
                        selecting = event.hasShift()
                        moveLine(true)
                    }

                    Keys.ESCAPE -> {
                        polyUI.unfocus()
                    }

                    else -> {}
                }
            }
        }
        caretPos()
        selections()
        return false
    }

    private fun caretPos() {
        val (line, idx, lni) = getLineByIndex(caret)
        cposx = renderer.textBounds(
            font,
            line.substring(0, idx),
            fontSize,
        ).width + x
        cposy = lni * (fontSize + spacing) + y
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

    private fun getLineByIndex(index: Int): Triple<String, Int, Int> {
        require(index > -1) { "Index must not be negative" }
        var i = 0
        lines.fastEachIndexed { li, it ->
            if (index < i + it.length) {
                return Triple(it, index - i, li)
            }
            i += it.length
        }
        val l = lines.last()
        return Triple(l, l.length, lines.lastIndex)
    }

    private fun selections() {
        selectBoxes.clear()
        if (select == caret) return
        val (sl, si, sli) = if (caret < select) getLineByIndex(caret) else getLineByIndex(select)
        val (el, ei, eli) = if (caret < select) getLineByIndex(select) else getLineByIndex(caret)
        if (sl === el) {
            val endIndex = if (caret < select) select else caret
            val startIndex = if (caret < select) caret else select
            selection(sl, si, endIndex - startIndex + si, sli)
            return
        }
        val lh = fontSize + spacing
        for (i in sli + 1 until eli) {
            selectBoxes.add((x - 1f to y + (i.toFloat() * lh)) to (linesData[i] to lh))
        }
        selection(sl, si, sl.length, sli)
        selection(el, 0, ei, eli)
    }

    private fun selection(line: String, startIndex: Int, endIndex: Int, lineIndex: Int) {
        val lh = fontSize + spacing
        val start = renderer.textBounds(font, line.substring(0, startIndex), fontSize).x
        val width = renderer.textBounds(font, line.substring(startIndex, endIndex), fontSize).x
        selectBoxes.add((x + start - 1f to y + (lineIndex * lh)) to (width to lh))
    }

    private fun caretFromMouse(mouseX: Float, mouseY: Float) {
        val line = ((mouseY - y) / (fontSize + spacing)).toInt()
        val p = lines[min(line, lines.lastIndex)].closestToPoint(renderer, font, fontSize, mouseX - x)
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
        if (!super.setup(polyUI)) return false
        _placeholder = polyUI.translator.translate(_placeholder.string)
        val bounds = renderer.textBounds(font, _placeholder.string, fontSize)
        size.ensureLargerThan(bounds)
        return true
    }

    override fun updateTextBounds(renderer: Renderer) {
        super.updateTextBounds(renderer)
        linesData.clear()
        lines.fastEach {
            val size = renderer.textBounds(font, it, fontSize)
            linesData.add(size.x)
        }
        if (text.isEmpty()) {
            val bounds = renderer.textBounds(font, _placeholder.string, fontSize)
            size.ensureLargerThan(bounds)
        }
    }
}
