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

package cc.polyfrost.polyui

import cc.polyfrost.polyui.color.DarkTheme
import cc.polyfrost.polyui.color.LightTheme
import cc.polyfrost.polyui.component.impl.*
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.event.Events.Companion.events
import cc.polyfrost.polyui.input.Modifiers
import cc.polyfrost.polyui.input.PolyTranslator.Companion.localised
import cc.polyfrost.polyui.layout.Layout.Companion.drawables
import cc.polyfrost.polyui.layout.impl.FlexLayout
import cc.polyfrost.polyui.layout.impl.PixelLayout
import cc.polyfrost.polyui.property.PropertyManager
import cc.polyfrost.polyui.property.State
import cc.polyfrost.polyui.property.impl.*
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.renderer.impl.NVGRenderer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.radii
import kotlin.random.Random

val moon = PolyImage("moon.svg")
val sun = PolyImage("sun.svg")

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

    polyUI.keyBinder.add(key = 'P', mods = Modifiers.mods(Modifiers.LCONTROL)) {
        polyUI.debugPrint()
        return@add
    }
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
    0 -> BrandBlockProperties(4f.radii())
    1 -> StateBlockProperties(State.Success, 4f.radii())
    2 -> StateBlockProperties(State.Warning, 4f.radii())
    else -> StateBlockProperties(State.Danger, 4f.radii())
}

fun create(at: Point<Unit>, default: Boolean): PixelLayout {
    var t = default
    return PixelLayout(
        acceptInput = false,
        at = at,
        drawables = drawables(
            Block(
                properties = BackgroundProperties(),
                at = origin,
                size = 400.px * 500.px,
                acceptInput = false
            ),
            Text(
                properties = TextProperties(Font("Poppins-SemiBold.ttf")),
                text = "text.light".localised(),
                fontSize = 20.px,
                at = 24.px * 65.px
            ),
            Button(
                leftIcon = moon,
                at = 24.px * 111.px,
                events = events(
                    Events.MouseClicked(0) to {
                        if (t) {
                            this.layout.changeColors(LightTheme())
                            this.layout.getComponent<Text>(1).text = "text.light".localised()
                            (this as Button).leftImage!!.image = moon
                        } else {
                            this.layout.changeColors(DarkTheme())
                            this.layout.getComponent<Text>(1).text = "text.dark".localised()
                            (this as Button).leftImage!!.image = sun
                        }
                        t = !t
                    }
                )
            ),
            Button(
                text = "button.text".localised(),
                fontSize = 13.px,
                leftIcon = PolyImage("face-wink.svg"),
                at = 68.px * 111.px
            ),
            Dropdown(
                at = 24.px * 159.px,
                size = 352.px * 32.px,
                entries = Dropdown.from(SlideDirection.values())
            ),
            TextInput(
                at = 24.px * 203.px,
                size = 352.px * 32.px
            ),
            FlexLayout(
                at = 24.px * 247.px,
                drawables = blocks(),
                wrap = 348.px
            ).scrolling(348.px * 117.px),
            Button(
                leftIcon = PolyImage("shuffle.svg"),
                text = "button.randomize".localised(),
                at = 24.px * 380.px,
                events = events(
                    Events.MouseClicked(0) to {
                        this.layout.getLayout<FlexLayout>(0).shuffle()
                    }
                )
            ),
            Button(
                leftIcon = PolyImage("plus.svg"),
                at = 320.px * 380.px,
                events = events(
                    Events.MouseClicked(0) to {
                        this.layout.getLayout<FlexLayout>(0).addComponent(block())
                    }
                )
            ),
            Button(
                leftIcon = PolyImage("minus.svg"),
                at = 355.px * 380.px,
                events = events(
                    Events.MouseClicked(0) to {
                        val l = this.layout.getLayout<FlexLayout>(0)
                        l.removeComponentNow(l.components.last())
                    }
                )
            ),
            Block(
                properties = BrandBlockProperties(),
                at = 24.px * 430.px,
                size = 85.px * 32.px
            ),
            Block(
                at = 113.px * 430.px,
                size = 85.px * 32.px
            )
        )
    )
}
