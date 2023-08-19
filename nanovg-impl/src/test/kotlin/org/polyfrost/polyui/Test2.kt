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

package org.polyfrost.polyui

import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.LightTheme
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.Modifiers
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.property.PropertyManager
import org.polyfrost.polyui.property.impl.*
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.renderer.impl.GLWindow
import org.polyfrost.polyui.renderer.impl.NVGRenderer
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.radii
import kotlin.random.Random

val moon = PolyImage("moon.svg")
val sun = PolyImage("sun.svg")
val brand = BlockProperties.brand(4f.radii())
val success = BlockProperties.success(4f.radii())
val warning = BlockProperties.warning(4f.radii())
val danger = BlockProperties.danger(4f.radii())
val text = TextProperties { fonts.medium }

fun main() {
    // todo implement designs for textbox etc

    val window = GLWindow("Test", 800, 500)

    val polyUI = PolyUI(
        renderer = NVGRenderer(window.width.toFloat(), window.height.toFloat()),
        colors = LightTheme(),
        drawables = drawables(
            create(origin, false).also {
                it.addComponents(
                    Text(
                        text = "polyfrost.copyright".localised(),
                        at = 24.px * 475.px,
                        fontSize = 10.px
                    ),
                    Image(
                        image = PolyImage("polyfrost.png"),
                        at = 24.px * 24.px
                    )
                )
            },
            create(400.px * 0.px, true).also {
                it.propertyManager = PropertyManager(DarkTheme())
                it.getComponent<Button>(2).leftImage!!.image = sun
                it.getComponent<Text>(1).initialText = "text.dark".localised()
            }
        )
    )

    polyUI.keyBinder.add(
        KeyBinder.Bind('P', mods = Modifiers.mods(Modifiers.LCONTROL)) {
            polyUI.debugPrint()
            true
        }
    )
    polyUI.settings.debug = false
    window.open(polyUI)
}

fun blocks(amount: Int = 40): Array<Block> {
    return Array(amount) {
        block()
    }
}

fun block() = Block(
    properties = prop(),
    at = flex(),
    size = (Random.nextFloat() * 100f + 32f).px * 32.px
)

fun prop() = when (Random.Default.nextInt(4)) {
    0 -> brand
    1 -> success
    2 -> warning
    else -> danger
}

fun create(at: Point<Unit>, default: Boolean): PixelLayout {
    var t = default
    return PixelLayout(
        acceptInput = false,
        at = at,
        drawables = drawables(
            Block(
                properties = BlockProperties.backgroundBlock,
                at = origin,
                size = 400.px * 500.px,
                acceptInput = false
            ),
            Text(
                properties = text,
                text = "text.light".localised(),
                fontSize = 20.px,
                at = 24.px * 65.px
            ),
            Button(
                left = moon,
                at = 24.px * 111.px,
                events = {
                    MouseClicked(0) to {
                        if (t) {
                            this.layout.changeColors(LightTheme())
                            this.layout.getComponent<Text>(1).text = "text.light".localised()
                            this.leftImage!!.image = moon
                        } else {
                            this.layout.changeColors(DarkTheme())
                            this.layout.getComponent<Text>(1).text = "text.dark".localised()
                            this.leftImage!!.image = sun
                        }
                        t = !t
                    }
                }
            ),
            Button(
                text = "button.text".localised("simple"),
                fontSize = 13.px,
                left = PolyImage("face-wink.svg"),
                at = 68.px * 111.px
            ),
            Switch(
                at = 266.px * 113.px,
                switchSize = 64.px * 32.px
            ),
            Slider(
                at = 336.px * 10.px,
                size = 32.px * 140.px
            ),
            Dropdown(
                at = 24.px * 159.px,
                size = 352.px * 32.px,
                entries = Dropdown.from(SlideDirection.entries.toTypedArray())
            ),
            TextInput(
                at = 24.px * 203.px,
                size = 352.px * 32.px,
                image = PolyImage("search.svg"),
                title = "Title:".localised(),
                hint = "px".localised()
            ),
            FlexLayout(
                at = 24.px * 247.px,
                drawables = blocks(),
                wrap = 348.px
            ).scrolling(348.px * 117.px),
            Button(
                left = PolyImage("shuffle.svg"),
                text = "button.randomize".localised(),
                at = 24.px * 380.px,
                events = {
                    MouseClicked(0) to {
                        this.layout.getLayout<FlexLayout>(0).shuffle()
                    }
                }
            ),
            Button(
                left = PolyImage("plus.svg"),
                at = 320.px * 380.px,
                events = {
                    MouseClicked(0) to {
                        this.layout.getLayout<FlexLayout>(0).addComponent(block())
                    }
                }
            ),
            Button(
                left = PolyImage("minus.svg"),
                at = 355.px * 380.px,
                events = {
                    MouseClicked(0) to {
                        val l = this.layout.getLayout<FlexLayout>(0)
                        l.removeComponentNow(l.flexDrawables.last())
                    }
                }
            ),
            Block(
                properties = BlockProperties.brandBlock,
                at = 24.px * 430.px,
                size = 85.px * 32.px
            ).draggable(),
            Block(
                at = 113.px * 430.px,
                size = 85.px * 32.px
            ),
            Block(
                at = 202.px * 430.px,
                size = 85.px * 32.px
            ),
            Block(
                at = 291.px * 430.px,
                size = 85.px * 32.px
            )
        )
    )
}
