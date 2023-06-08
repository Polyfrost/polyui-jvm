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

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Focusable
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.event.FocusedEvents
import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.input.Keys
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.property.impl.TextInputProperties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.utils.dropAt
import cc.polyfrost.polyui.utils.stdout
import cc.polyfrost.polyui.utils.substringSafe
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

class TextInput(
    properties: Properties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>,
    vararg events: Events.Handler
) : Component(properties, at, size, true, *events), Focusable {
    override val properties: TextInputProperties
        get() = super.properties as TextInputProperties
    lateinit var text: Text
    var txt
        get() = text.text.string
        set(value) {
            text.text.string = value
            text.str.calculate(renderer)
        }
    private var init = false
    private var caret = 0
        set(value) {
            field = value
            if (!selecting) select = value
        }
    private var select: Int = 0
    private var caretPos = x to y
    var selecting = false
    val selection get() = txt.substringSafe(caret, select).stdout()
    override fun render() {
        renderer.drawRect(
            at.a.px,
            at.b.px,
            size!!.a.px,
            size!!.b.px,
            properties.backgroundColor,
            properties.cornerRadii
        )
        renderer.drawHollowRect(
            at.a.px,
            at.b.px,
            size!!.a.px,
            size!!.b.px,
            properties.outlineColor,
            properties.outlineThickness,
            properties.cornerRadii
        )
        renderer.drawRect(caretPos.first, caretPos.second, 2f, properties.text.fontSize.px, polyui.colors.text.primary)
        text.render()
    }

    override fun accept(event: FocusedEvents) {
        if (event is FocusedEvents.KeyTyped) {
            if (event.mods < 2 && !text.full) {
                txt = txt.substring(0, caret) + event.key + txt.substring(caret)
                caret++
            } else if (event.hasModifier(KeyModifiers.LCONTROL) || event.hasModifier(KeyModifiers.RCONTROL)) {
                when (event.key) {
                    'V' -> {
                        try {
                            txt += Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
                                ?.getTransferData(DataFlavor.stringFlavor) as? String ?: ""
                        } catch (e: Exception) {
                            PolyUI.LOGGER.error("Failed to read clipboard data!", e)
                        }
                    }

                    'C' -> {
                        try {
                            if (caret != select) {
                                Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                    StringSelection(
                                        selection
                                    ),
                                    null
                                )
                            }
                        } catch (e: Exception) {
                            PolyUI.LOGGER.error("Failed tole write clipboard data!", e)
                        }
                    }

                    'X' -> {
                        try {
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(selection), null)
                            txt = txt.replace(selection, "")
                        } catch (e: Exception) {
                            PolyUI.LOGGER.error("Failed to write clipboard data!", e)
                        }
                        clearSelection()
                    }

                    'A' -> {
                        caret = txt.length
                        select = 0
                    }
                }
            }
        }
        if (event is FocusedEvents.KeyPressed) {
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
                        txt = txt.substring(0, f) + txt.substring(t) // todo finish
                    }
                    if (!hasControl) {
                        txt = txt.dropAt(caret, 1)
                        if (caret != 0) caret--
                    } else {
                        dropToLastSpace()
                    }
                }

                Keys.ENTER -> {
                    println("enter")
                }

                Keys.TAB -> {
                    txt += "    "
                }

                Keys.DELETE -> {
                    txt = txt.drop(1)
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
                    println("up")
                }

                Keys.DOWN -> {
                    println("down")
                }

                Keys.ESCAPE -> {
                    polyui.unfocus()
                }

                else -> {}
            }
        }
        caretPos = caretPos()
    }

    fun caretPos(): Pair<Float, Float> {
        val (line, idx, lni) = text.getByCharIndex(caret)
        return (
            renderer.textBounds(
                properties.text.font,
                txt.substring(0, idx),
                properties.text.fontSize.px,
                properties.text.alignment
            ).width + text.x + text.textOffset to lni * properties.text.fontSize.px + text.y
            )
    }

    fun toLastSpace() {
        while (caret > 0 && txt[caret - 1] == ' ') caret--
        txt.lastIndexOf(' ', caret - 1).let {
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
        txt.lastIndexOf(' ', caret).let {
            caret = if (it != -1) {
                it
            } else {
                0
            }
        }
        txt = txt.substring(0, caret) + txt.substring(c)
    }

    fun toNextSpace() {
        while (caret < txt.length && txt[caret] == ' ') caret++
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
        caret = if (caret < txt.length) {
            caret + 1
        } else {
            txt.length
        }
    }

    override fun reset() {
        properties.defaultText.reset()
        text.text = properties.defaultText.clone()
    }

    fun clearSelection() {
        select = caret
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        text = Text(
            properties.text,
            properties.defaultText.clone(),
            at.clone(),
            size?.clone(),
            properties.text.fontSize,
            properties.text.alignment,
            false
        )
        text.setup(renderer, polyui)
    }

    override fun calculateBounds() {
        text.layout = layout
        text.calculateBounds()
        super.calculateBounds()
        if (!init) {
            text.at.a.px += properties.paddingFromTextLateral
            text.at.b.px += properties.paddingFromTextVertical
            text.size!!.a.px -= properties.paddingFromTextLateral
            text.size!!.b.px -= properties.paddingFromTextVertical
            init = true
        }
    }

    override fun calculateSize(): Vec2<Unit> {
        return text.size!!.clone().also {
            it.a.px += properties.paddingFromTextLateral * 2
            it.b.px += properties.paddingFromTextVertical * 2
        }
    }
}
