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
import org.polyfrost.polyui.component.Focusable
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.*

class TextInput(
    text: String = "Write something...",
    font: Font = PolyUI.defaultFonts.regular,
    fontSize: Float = 12f,
    at: Vec2? = null,
    alignment: Align = AlignDefault,
    size: Vec2? = null,
    wrap: Float = 120f,
    vararg children: Drawable?,
) : Text(text, font, fontSize, at, alignment, size, wrap, *children), Focusable {
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
        super.render()
    }

    override fun accept(event: Event): Boolean {
        if (event is MouseEntered) {
            polyUI.cursor = Cursor.Text
        }
        if (event is MouseExited) {
            polyUI.cursor = Cursor.Pointer
        }
        if (event is MouseClicked) {
            clearSelection()
            if (event.clicks == 1) {
                caretFromMouse(event.mouseX, event.mouseY)
                return true
            } else if (event.clicks == 2) {
                selectWordAroundCaret()
                return true
            }
        }
        return super<Text>.accept(event)
    }

    override fun accept(event: FocusedEvent): Boolean {
        if (event is FocusedEvent.Gained) {
            focused = true
            caretPos()
        }
        if (event is FocusedEvent.Lost) {
            clearSelection()
            focused = false
        }
        if (event is FocusedEvent.KeyTyped) {
            if (event.mods < 2) {
                if (caret != select) {
                    text = text.replace(selection, "")
                    caret = if (select > caret) caret else select
                    clearSelection()
                }
                text = text.substring(0, caret) + event.key + text.substring(caret)
                caret++
            } else if (event.hasModifier(KeyModifiers.LCONTROL) || event.hasModifier(KeyModifiers.RCONTROL)) {
                when (event.key) {
                    'V' -> {
                        text = text.substring(0, caret) + (polyUI.clipboard ?: "") + text.substring(caret)
                        caret += polyUI.clipboard?.length ?: 0
                        clearSelection()
                    }

                    'C' -> {
                        if (caret != select) {
                            polyUI.clipboard = selection
                        }
                    }

                    'X' -> {
                        polyUI.clipboard = null
                        text = text.replace(selection, "")
                        clearSelection()
                    }

                    'A' -> {
                        caret = text.lastIndex + 1
                        select = 0
                    }
                }
            }
        }
        if (event is FocusedEvent.KeyPressed) {
            val hasControl = event.hasModifier(KeyModifiers.LCONTROL) || event.hasModifier(KeyModifiers.RCONTROL)
            val hasShift = event.hasModifier(KeyModifiers.LSHIFT) || event.hasModifier(KeyModifiers.RSHIFT)
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
                        text = text.substring(0, f) + text.substring(t)
                        caret = f
                        clearSelection()
                    } else if (!hasControl) {
                        text = text.dropAt(caret, 1)
                        if (caret != 0) caret--
                    } else {
                        dropToLastSpace()
                    }
                }

                Keys.TAB -> {
                    text += "    "
                }

                Keys.DELETE -> {
                    if (caret + 1 > text.length) return true
                    text = text.dropAt(caret + 1, 1)
                }

                Keys.LEFT -> {
                    selecting = hasShift
                    if (hasControl) {
                        toLastSpace()
                    } else {
                        back()
                    }
                }

                Keys.RIGHT -> {
                    selecting = hasShift
                    if (hasControl) {
                        toNextSpace()
                    } else {
                        forward()
                    }
                }

                Keys.UP -> {
                    selecting = hasShift
                    moveLine(false)
                }

                Keys.DOWN -> {
                    selecting = hasShift
                    moveLine(true)
                }

                Keys.ESCAPE -> {
                    polyUI.unfocus()
                }

                else -> {}
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
        val p = lines[line].closestToPoint(renderer, font, fontSize, mouseX - x)
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
        val c = caret
        text.trimEnd().lastIndexOf(' ', caret).let {
            caret = if (it != -1) {
                it
            } else {
                0
            }
        }
        text = text.substring(0, caret) + text.substring(c)
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

    override fun calculateSize(): Vec2 {
        val out = super.calculateSize()
        linesData.clear()
        lines.fastEach {
            val size = renderer.textBounds(font, it, fontSize)
            linesData.add(size.x)
        }
        return out
    }
}
