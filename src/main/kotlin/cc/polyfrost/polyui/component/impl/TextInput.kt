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

class TextInput @JvmOverloads constructor(
    properties: Properties = Properties.get<TextInput>(),
    at: Vec2<Unit>,
    sized: Vec2<Unit>? = null,
    vararg events: Events.Handler
) : Component(properties, at, sized, true, *events), Focusable {
    val props = properties as TextInputProperties
    val text = Text(props.textProperties, props.defaultText, at = at.clone(), size = sized, acceptInput = false)
    var init = false
    val autoSized = text.autoSized
    override fun render() {
        if (props.backgroundColor != null) {
            renderer.drawRect(x, y, width, height, props.backgroundColor, props.cornerRadii)
        }
        if (props.outlineColor != null) {
            renderer.drawHollowRect(x, y, width, height, props.outlineColor, props.outlineThickness, props.cornerRadii)
        }
        text.render()
    }

    override fun accept(event: FocusedEvents) {
        if (event is FocusedEvents.KeyTyped) {
            if (event.mods < 2) {
                text.text += event.key
            } else if (event.hasModifier(Keys.Modifiers.LCONTROL) || event.hasModifier(Keys.Modifiers.RCONTROL)) {
                when (event.key) {
                    'V' -> {
                        println("paste")
                    }

                    'C' -> {
                        println("copy")
                    }

                    'X' -> {
                        println("cut")
                    }

                    'A' -> {
                        println("select all")
                    }
                }
            }
        }
        if (event is FocusedEvents.KeyPressed) {
            when (event.key) {
                Keys.BACKSPACE -> {
                    text.text = text.text.dropLast(1)
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
                    println("left")
                }

                Keys.RIGHT -> {
                    println("right")
                }

                Keys.UP -> {
                    println("up")
                }

                Keys.DOWN -> {
                    println("down")
                }

                Keys.INSERT -> {
                    println("paste")
                }

                Keys.ESCAPE -> {
                    println("escape")
                    polyui.focused = null
                }

                else -> {}
            }
        }
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
