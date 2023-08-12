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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Focusable
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.property.impl.TextInputProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.Line
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.*

open class TextInput(
    properties: TextInputProperties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>,
    vararg events: Event.Handler
) : Component(properties, at, size, false, true, *events), Focusable {
    override val properties: TextInputProperties
        get() = super.properties as TextInputProperties
    lateinit var text: Text
    var txt
        inline get() = text.text.string
        set(value) {
            text.str.text.string = value
            text.str.calculate(renderer)
        }
    private var caret = 0
        set(value) {
            field = value
            if (!selecting) select = value
        }
    private val selectBoxes = ArrayList<Pair<Pair<Float, Float>, Pair<Float, Float>>>(5)
    private var select: Int = 0
    var focused = false
        private set
    private var cposx = 0f
    private var cposy = 0f
    private var mouseDown = false
    private var selecting = false
    val selection get() = txt.substringSafe(caret, select)

    override fun render() {
        if (!mouseOver) mouseDown = false
        renderer.rect(x, y, width, height, properties.backgroundColor, properties.cornerRadii)
        renderer.hollowRect(x, y, width, height, properties.outlineColor, properties.outlineThickness, properties.cornerRadii)
        if (focused) {
            renderer.rect(cposx, cposy, 2f, text.fontSize, layout.colors.text.primary.normal)
            selectBoxes.fastEach {
                val (x, y) = it.first
                val (w, h) = it.second
                renderer.rect(x, y, w, h, layout.colors.page.border20)
            }
        } else {
            renderer.globalAlpha(0.8f)
        }
        text.render()
        renderer.resetGlobalAlpha()
    }

    override fun accept(event: Event): Boolean {
        if (event is MouseEntered) {
            polyUI.cursor = Cursor.Text
        }
        if (event is MouseExited) {
            polyUI.cursor = Cursor.Pointer
        }
        if (event is MousePressed) {
            mouseDown = true
            return true
        }
        if (event is MouseReleased) {
            mouseDown = false
            return true
        }
        if (event is MouseClicked) {
            clearSelection()
            if (event.clicks == 1) {
                posFromMouse(event.mouseX, event.mouseY)
                return true
            } else if (event.clicks == 2) {
                selectWordAroundCaret()
                return true
            }
        }
        if (event is MouseMoved) {
            if (mouseDown) {
                if (!polyUI.mouseDown) {
                    selecting = false
                    mouseDown = false
                } else {
                    mouseInput(polyUI.mouseX, polyUI.mouseY)
                }
            }
        }
        wantRedraw()
        return super<Component>.accept(event)
    }

    override fun accept(event: FocusedEvent) {
        if (event is FocusedEvent.Gained) {
            focused = true
        }
        if (event is FocusedEvent.Lost) {
            clearSelection()
            focused = false
        }
        if (event is FocusedEvent.KeyTyped) {
            if (event.mods < 2) {
                if (caret != select) {
                    txt = txt.replace(selection, "")
                    caret = if (select > caret) caret else select
                    clearSelection()
                }
                txt = txt.substring(0, caret) + event.key + txt.substring(caret)
                caret++
            } else if (event.hasModifier(KeyModifiers.LCONTROL) || event.hasModifier(KeyModifiers.RCONTROL)) {
                when (event.key) {
                    'V' -> {
                        txt = txt.substring(0, caret) + (polyUI.clipboard ?: "") + txt.substring(caret)
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
                        txt = txt.replace(selection, "")
                        clearSelection()
                    }

                    'A' -> {
                        caret = txt.lastIndex + 1
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
                        txt = txt.substring(0, f) + txt.substring(t)
                        caret = f
                        clearSelection()
                    } else if (!hasControl) {
                        txt = txt.dropAt(caret, 1)
                        if (caret != 0) caret--
                    } else {
                        dropToLastSpace()
                    }
                }

                Keys.TAB -> {
                    txt += "    "
                }

                Keys.DELETE -> {
                    if (caret + 1 > txt.length) return
                    txt = txt.dropAt(caret + 1, 1)
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
        wantRedraw()
    }

    fun caretPos() {
        val (line, idx, lni) = text.getLineByIndex(caret)
        cposx = renderer.textBounds(
            properties.text.font,
            line.text.substring(0, idx),
            text.str.fontSize,
            properties.text.alignment
        ).width + text.x
        cposy = lni * text.str.fontSize + text.y + text.str.textOffsetY
    }

    private fun mouseInput(mouseX: Float, mouseY: Float) {
        if (!selecting) {
            posFromMouse(mouseX, mouseY)
            select = caret
            selecting = true
        }
        posFromMouse(mouseX, mouseY)
        caretPos()
        selections()
    }

    fun selectWordAroundCaret() {
        var start = txt.lastIndexOf(' ', caret - 1) + 1
        var end = txt.indexOf(' ', caret)
        if (start == -1) start = 0
        if (end == -1) end = txt.length
        selecting = true
        select = start
        caret = end
        selections()
        caretPos()
    }

    private fun selections() {
        selectBoxes.clear()
        if (select == caret) return
        val (sl, si, sli) = if (caret < select) text.getLineByIndex(caret) else text.getLineByIndex(select)
        val (el, ei, eli) = if (caret < select) text.getLineByIndex(select) else text.getLineByIndex(caret)
        if (sl === el) {
            val endIndex = if (caret < select) select else caret
            val startIndex = if (caret < select) caret else select
            line(sl, si, endIndex - startIndex + si, sli)
            return
        }
        for (i in sli + 1 until eli) {
            val line = text[i]
            selectBoxes.add((text.x - 1f to text.y + (i.toFloat() * text.fontSize) + text.str.textOffsetY) to (line.width to line.height))
        }
        line(sl, si, sl.text.length, sli)
        line(el, 0, ei, eli)
    }

    private fun line(line: Line, startIndex: Int, endIndex: Int, lineIndex: Int) {
        val start = renderer.textBounds(text.font, line.text.substring(0, startIndex), text.fontSize, text.textAlign).a.px
        val width = renderer.textBounds(text.font, line.text.substring(startIndex, endIndex), text.fontSize, text.textAlign).a.px
        selectBoxes.add((text.x + start - 1f to text.y + (lineIndex * text.fontSize) + text.str.textOffsetY) to (width to line.height))
    }

    private fun posFromMouse(x: Float, y: Float) {
        val mouseY = y - text.trueY - text.str.textOffsetY
        val mouseX = x - text.trueX

        var i = 0f
        var idx = 0
        var l: Line
        if (text.lines.size == 1) {
            l = text.lines[0]
            if (mouseY > l.height) {
                caret = l.text.length
                caretPos()
                return
            }
        } else {
            run {
                text.lines.fastEach {
                    i += it.height
                    if (mouseY < i) {
                        l = it
                        if (mouseY > i + it.height) {
                            caret = idx + it.text.length
                            caretPos()
                            return
                        }
                        return@run
                    } else {
                        idx += it.text.length
                    }
                }
                // best to be safe
                l = text.lines.last()
            }
        }

        caret = if (mouseX < 0) {
            idx
        } else if (mouseX > l.width) {
            idx + l.text.length
        } else {
            val index = l.text.closestToPoint(renderer, properties.text.font, text.str.fontSize, properties.text.alignment, mouseX)
            if (index == -1) {
                idx + l.text.length
            } else {
                idx + index
            }
        }
        caretPos()
    }

    private fun moveLine(down: Boolean) {
        val h = text.getLineByIndex(caret).first.height
        posFromMouse(
            text.trueX + (cposx - text.x),
            text.trueY + (cposy - text.y) +
                if (down) h else -h
        )
        if (text.full) {
            if (!down && text.str.textOffsetY >= 0f) return
            if (down && text.str.textOffsetY <= -text.height) return
            text.str.textOffsetY += if (down) -h else h
            cposy += if (down) -h else h
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        text.rescale(scaleX, scaleY)
        cposx *= scaleX
        cposy *= scaleY
    }

    fun toLastSpace() {
        while (caret > 0 && txt[caret - 1] == ' ') caret--
        txt.trimEnd().lastIndexOf(' ', caret - 1).let {
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
        txt.trimEnd().lastIndexOf(' ', caret).let {
            caret = if (it != -1) {
                it
            } else {
                0
            }
        }
        txt = txt.substring(0, caret) + txt.substring(c)
    }

    fun toNextSpace() {
        while (caret < txt.length - 1 && txt[caret] == ' ') caret++
        txt.indexOf(' ', caret).let {
            if (selecting && select == caret) select = caret
            caret = if (it != -1) {
                it
            } else {
                txt.length
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
        caret = if (caret < txt.length - 1) {
            caret + 1
        } else {
            txt.length
        }
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        text.onColorsChanged(colors)
    }

    override fun reset() {
        properties.defaultText.reset()
        text.text = properties.defaultText.clone()
    }

    fun clearSelection() {
        selecting = false
        select = caret
        selectBoxes.clear()
    }

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        text = Text(
            properties.text,
            properties.defaultText.clone(),
            at.clone(),
            size?.clone(),
            properties.text.fontSize,
            properties.text.alignment,
            false
        )
        text.layout = this.layout
        text.setup(renderer, polyUI)
    }

    override fun calculateBounds() {
        text.calculateBounds()
        super.calculateBounds()
        caretPos()
    }

    override fun onInitComplete() {
        super.onInitComplete()
        text.x += properties.paddingFromTextLateral
        text.y += properties.paddingFromTextVertical
        text.width -= properties.paddingFromTextLateral * 2f
        text.height -= properties.paddingFromTextVertical * 2f
    }

    override fun calculateSize(): Vec2<Unit> {
        return text.size!!.clone().also {
            it.a.px += properties.paddingFromTextLateral * 2f
            it.b.px += properties.paddingFromTextVertical * 2f
        }
    }
}
