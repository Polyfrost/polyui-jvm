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

@file:JvmName("Derivatives")

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Fade
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.operations.Rotate
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.LinkedList
import org.polyfrost.polyui.utils.asLinkedList
import org.polyfrost.polyui.utils.radii
import kotlin.math.PI

fun Button(leftImage: PolyImage? = null, text: String? = null, rightImage: PolyImage? = null, fontSize: Float = 12f, at: Vec2? = null, size: Vec2? = null): Drawable {
    return Block(
        at = at,
        size = size,
        children = arrayOf(
            if (leftImage != null) Image(leftImage) else null,
            if (text != null) Text(text, fontSize = fontSize) else null,
            if (rightImage != null) Image(rightImage) else null,
        ),
    ).onInit { _ ->
        this.children?.fastEach {
            (it as? Image)?.image?.size?.max(32f, 32f)
        }
    }.withStates().namedId("Button")
}

fun Switch(at: Vec2? = null, size: Float, padding: Float = 3f, state: Boolean = false, lateralStretch: Float = 1.8f): Drawable {
    val circleSize = size - (padding + padding)
    val adjSize = size * (lateralStretch - 1f)
    return Block(
        at = at,
        size = Vec2(size * lateralStretch, size),
        alignment = Align(main = Align.Main.Start, padding = Vec2(padding, padding)),
        radii = (size / 2f).radii(),
        children = arrayOf(
            Block(size = Vec2(circleSize, circleSize), radii = (circleSize / 2f).radii()).setPalette { text.primary },
        ),
    ).withStates().events {
        var switched = false
        Event.Mouse.Clicked(0) then {
            val circle = this[0]
            val target = Vec2(if (switched) -adjSize else adjSize, 0f)
            switched = !switched
            setPalette(if (switched) polyUI.colors.brand.fg else polyUI.colors.component.bg)
            Move(circle, target, true, Animations.EaseInOutQuad.create(0.2.seconds)).add()
            accept(Event.Change.State(switched))
            false
        }
    }.namedId("Switch").also {
        if (state) {
            it.setPalette { brand.fg }
            it.afterParentInit(2) {
                accept(Event.Mouse.Clicked(0))
            }
        }
    }
}

fun Radiobutton(at: Vec2? = null, entries: Array<Pair<PolyImage?, String?>>, initial: Int = 0, fontSize: Float = 12f): Drawable {
    val centerAlign = Align(Align.Main.Center)
    val maxImageSize = fontSize * 1.3f
    val buttons: LinkedList<Drawable> = entries.map { (img, text) ->
        Group(
            alignment = centerAlign,
            children = arrayOf(
                if (img != null) Image(img) else null,
                if (text != null) Text(text, fontSize = fontSize) else null,
            ),
        ).events {
            Event.Lifetime.Init then {
                (this[0] as? Image)?.image?.size?.max(maxImageSize, maxImageSize)
            }
            Event.Mouse.Clicked(0) then { _ ->
                val children = parent!!.children!!
                val it = children.first()
                Move(it, this.x, add = false, animation = Animations.EaseInOutQuad.create(0.15.seconds)).add()
                Resize(it, this.width, add = false, animation = Animations.EaseInOutQuad.create(0.15.seconds)).add()
                accept(Event.Change.Number(children.indexOf(this) - 1))
                true
            }
        }
    }.asLinkedList()
    return Block(
        at = at,
        children = buttons.apply {
            add(0, Block(ignored, ignored))
        }.toTypedArray(),
        alignment = centerAlign,
    ).afterInit { _ ->
        val it = this[0]
        val target = this[initial + 1]
        it.at.set(target.at)
        it.size.set(target.size)
        it.setPalette(polyUI.colors.brand.fg)
    }.namedId("Radiobutton")
}

fun Dropdown(at: Vec2? = null, size: Vec2? = null, entries: Array<Pair<PolyImage?, String>>, fontSize: Float = 12f, initial: Int = 0): Drawable {
    var heightTracker = 0f
    val it = Block(
        at,
        size,
        focusable = true,
        children = arrayOf(
            Text(entries[initial].second, fontSize = fontSize),
            Image(PolyImage("chevron-down.svg")),
        ),
    )
    val dropdown = Block(
        size = size,
        alignment = Align(mode = Align.Mode.Vertical),
        children = entries.map { (img, text) ->
            Group(
                children = arrayOf(
                    if (img != null) Image(img) else null,
                    Text(text, fontSize = fontSize),
                ),
            ).events {
                Event.Mouse.Clicked(0) then { _ ->
                    val title = (it[0] as Text)
                    val self = ((if (children!!.size == 2) this[1] else this[0]) as Text).text
                    if (title.text == self) return@then false
                    title.text = self
                    accept(Event.Change.Number(parent!!.children!!.indexOf(this)))
                }
            }
        }.toTypedArray(),
    ).disable()
    return it.events {
        Event.Focused.Gained then {
            if (dropdown.height != 0f) heightTracker = dropdown.height
            dropdown.height = 0f
            dropdown.enabled = true
            dropdown.y = this.y + this.size.y
            Resize(dropdown, height = heightTracker, add = false, animation = Animations.EaseInOutQuad.create(0.15.seconds)).add()
            Rotate(this[1], PI, add = false, animation = Animations.EaseInOutQuad.create(0.15.seconds)).add()
        }
        Event.Focused.Lost then {
            if (dropdown.height != 0f) heightTracker = dropdown.height
            Resize(dropdown, height = 0f, add = false, animation = Animations.EaseInOutQuad.create(0.15.seconds)) {
                dropdown.enabled = false
            }.add()
            Rotate(this[1], 0.0, add = false, animation = Animations.EaseInOutQuad.create(0.15.seconds)).add()
        }
        Event.Lifetime.PostInit then {
            parent?.addChild(dropdown)
        }
        Event.Mouse.Clicked(0) then {
            if (polyUI.eventManager.hasFocused) polyUI.unfocus()
        }
    }.namedId("Dropdown").afterParentInit {
        dropdown.x = this.x
        this.width = dropdown.width
        this[1].x = this.x + this.width - this[1].width - alignment.padding.x
    }
}

fun Slider(at: Vec2? = null, min: Float = 0f, max: Float = 100f, initialValue: Float = 0f, ptrSize: Float = 24f, scaleFactor: Float = 2f, floating: Boolean = true, instant: Boolean = false): Drawable {
    val barHeight = ptrSize / 2.8f
    val barWidth = (max - min) * scaleFactor
    val size = Vec2(barWidth + ptrSize, ptrSize)
    val rad = (barHeight / 2f).radii()

    return Group(
        at = at,
        size = size,
        alignment = Align(Align.Main.Start, padding = Vec2.ZERO),
        children = arrayOf(
            Block(
                size = Vec2(barWidth, barHeight),
                alignment = Align(Align.Main.Start, padding = Vec2.ZERO),
                radii = rad,
                children = arrayOf(
                    Block(
                        size = Vec2.Based(1f, barHeight),
                        radii = rad,
                    ).setPalette { brand.fg },
                ),
            ),
            Block(
                size = ptrSize.vec,
                radii = (ptrSize / 2f).radii(),
            ).setPalette { text.primary }.draggable(withY = false) {
                val ptr = this.parent!![1]
                val bar = this.parent!![0]
                val half = this.size.x / 2f
                this.x = this.x.coerceIn(bar.x - half, bar.x + bar.size.x - half)
                bar[0].width = ptr.x - bar.x + half
                if (instant) {
                    val progress = (this.x + half - bar.x) / size.x
                    var value = (max - min) * progress
                    if (!floating) value = value.toInt().toFloat()
                    accept(Event.Change.Number(value))
                }
            },
        ),
    ).apply {
        addEventHandler(Event.Mouse.Clicked(0)) {
            val bar = this[0]
            val ptr = this[1]
            val half = ptr.size.x / 2f
            ptr.x = it.mouseX - half
            ptr.x = ptr.x.coerceIn(bar.x - half, bar.x + bar.size.x - half)
            bar[0].width = ptr.x - bar.x + half

            val progress = (ptr.x + half - bar.x) / size.x
            var value = (max - min) * progress
            if (!floating) value = value.toInt().toFloat()
            accept(Event.Change.Number(value))
        }
        addEventHandler(Event.Lifetime.PostInit) {
            val bar = this[0]
            val ptr = this[1]
            ptr.x = bar.x + barWidth * (initialValue / (max - min))
            bar.x += ptrSize / 2f
            bar[0].width = ptr.x - bar.x + (ptrSize / 2f)
        }
    }.namedId("Slider")
}

fun Checkbox(at: Vec2? = null, size: Float, state: Boolean = false): Drawable {
    return Block(
        at = at,
        size = Vec2(size, size),
        alignment = Align(padding = ((size - size / 1.25f) / 2f).vec),
        children = arrayOf(
            Image(
                image = PolyImage("check.svg", size = (size / 1.25f).vec),
            ).disable(!state).also {
                if (!state) it.alpha = 0f
            },
        ),
    ).events {
        var checked = state
        Event.Mouse.Clicked(0) then {
            val check = this[0]
            checked = !checked
            setPalette(if (checked) polyUI.colors.brand.fg else polyUI.colors.component.bg)
            if (checked) {
                check.enabled = true
                Fade(check, 1f, false, Animations.EaseInOutQuad.create(0.1.seconds)).add()
            } else {
                Fade(check, 0f, false, Animations.EaseInOutQuad.create(0.1.seconds)) {
                    check.enabled = false
                }.add()
            }
            accept(Event.Change.State(checked))
        }
    }.withStates().namedId("Checkbox").also {
        if (state) it.setPalette { brand.fg }
    }
}
