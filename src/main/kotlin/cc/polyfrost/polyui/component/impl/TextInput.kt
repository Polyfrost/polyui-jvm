/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Focusable
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.event.FocusedEvents
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

class TextInput @JvmOverloads constructor(
    properties: Properties = Properties.get<TextInput>(),
    at: Vec2<Unit>,
    sized: Vec2<Unit>,
    vararg events: Events.Handler
) : Component(properties, at, sized, true, *events), Focusable {
    private val props = properties as TextInputProperties
    val text = Text(props.text, props.defaultText, at = at.clone(), size = sized.clone(), acceptInput = false)
    private var init = false
    private var caret = 0
        set(value) {
            field = value
            if (!selecting) select = value
        }
    private var select: Int = 0
        set(value) {
            field = value
        }
    private var caretPos = x to y
    var selecting = false
    val selection get() = text.text.substringSafe(caret, select).stdout()
    val autoSized get() = text.autoSized
    override fun render() {
        if (props.backgroundColor != null) {
            renderer.drawRect(x, y, width, height, props.backgroundColor, props.cornerRadii)
        }
        if (props.outlineColor != null) {
            renderer.drawHollowRect(x, y, width, height, props.outlineColor, props.outlineThickness, props.cornerRadii)
        }
        renderer.drawRect(caretPos.first, caretPos.second, 2f, props.text.fontSize.px, Color.WHITE_90)
        text.render()
    }

    override fun accept(event: FocusedEvents) {
        if (event is FocusedEvents.KeyTyped) {
            if (event.mods < 2 && !text.full) {
                text.text = text.text.substring(0, caret) + event.key + text.text.substring(caret)
                caret++
            } else if (event.hasModifier(Keys.Modifiers.LCONTROL) || event.hasModifier(Keys.Modifiers.RCONTROL)) {
                when (event.key) {
                    'V' -> {
                        try {
                            text.text += Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
                                ?.getTransferData(DataFlavor.stringFlavor) as? String ?: ""
                        } catch (e: Exception) {
                            PolyUI.LOGGER.error("Failed to read clipboard data!", e)
                        }
                    }

                    'C' -> {
                        try {
                            if (caret != select) Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(selection), null)
                        } catch (e: Exception) {
                            PolyUI.LOGGER.error("Failed tole write clipboard data!", e)
                        }
                    }

                    'X' -> {
                        try {
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(selection), null)
                            text.text = text.text.replace(selection, "")
                        } catch (e: Exception) {
                            PolyUI.LOGGER.error("Failed to write clipboard data!", e)
                        }
                        clearSelection()
                    }

                    'A' -> {
                        println("select all")
                    }
                }
            }
        }
        if (event is FocusedEvents.KeyPressed) {
            val hasControl = event.hasModifier(Keys.Modifiers.LCONTROL) || event.hasModifier(Keys.Modifiers.RCONTROL)
            val hasShift = event.hasModifier(Keys.Modifiers.LSHIFT) || event.hasModifier(Keys.Modifiers.RSHIFT)
            when (event.key) {
                Keys.BACKSPACE -> {
                    if (!hasControl) {
                        text.text = text.text.dropAt(caret, 1)
                        if (caret != 0) caret--
                    } else {
                        dropToLastSpace()
                    }
                }

                Keys.ENTER -> {
                    println("enter")
                }

                Keys.TAB -> {
                    text.text += "    "
                }

                Keys.DELETE -> {
                    text.text = text.text.drop(1)
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
        return (renderer.textBounds(props.text.font, line.string.substring(0, idx), props.text.fontSize.px, props.text.textAlignment).width + text.x to lni * props.text.fontSize.px + text.y)
    }

    fun toLastSpace() {
        while (caret > 0 && text.text[caret - 1] == ' ') caret--
        text.text.lastIndexOf(' ', caret - 1).let {
            if (selecting && select != caret) select = caret
            caret = if (it != -1) {
                it
            } else {
                0
            }
        }
    }

    fun dropToLastSpace() {
        val c = caret
        text.text.lastIndexOf(' ', caret).let {
            caret = if (it != -1) {
                it
            } else {
                0
            }
        }
        text.text = text.text.substring(0, caret) + text.text.substring(c)
    }

    fun toNextSpace() {
        while (caret < text.text.length && text.text[caret] == ' ') caret++
        text.text.indexOf(' ', caret).let {
            if (selecting && select != caret) select = caret
            caret = if (it != -1) {
                it
            } else {
                text.text.length
            }
        }
    }

    fun back() {
        if (selecting && select != caret) select = caret
        caret = if (caret > 0) {
            caret - 1
        } else {
            0
        }
    }

    fun forward() {
        if (selecting && select != caret) select = caret
        caret = if (caret < text.text.length) {
            caret + 1
        } else {
            text.text.length
        }
    }

    fun clearSelection() {
        select = caret
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        text.setup(renderer, polyui)
    }

    override fun calculateBounds() {
        text.layout = layout
        text.calculateBounds()
        super.calculateBounds()
        if (!init) {
            text.at.a.px += props.paddingFromTextLateral
            text.at.b.px += props.paddingFromTextVertical
            text.sized!!.a.px -= props.paddingFromTextLateral * 2
            text.sized!!.b.px -= props.paddingFromTextVertical * 2
            init = true
        }
    }

    override fun getSize(): Vec2<Unit> {
        return text.sized!!.clone().also {
            it.a.px += props.paddingFromTextLateral * 2
            it.b.px += props.paddingFromTextVertical * 2
        }
    }
}
