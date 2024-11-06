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

@file:JvmName("Derivatives")
@file:Suppress("FunctionName")

package org.polyfrost.polyui.component.impl

import org.jetbrains.annotations.Contract
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.*
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.coerceWithin
import org.polyfrost.polyui.utils.mapToArray
import kotlin.experimental.or
import kotlin.math.PI

@JvmName("Button")
fun Button(leftImage: PolyImage? = null, text: String? = null, rightImage: PolyImage? = null, fontSize: Float = 12f, font: Font? = null, radii: FloatArray = floatArrayOf(8f), padding: Vec2 = Vec2(12f, 6f), at: Vec2 = Vec2.ZERO, size: Vec2 = Vec2.ZERO): Block {
    return Block(
        if (leftImage != null) Image(leftImage) else null,
        if (text != null) Text(text, fontSize = fontSize, font = font) else null,
        if (rightImage != null) Image(rightImage) else null,
        alignment = Align(main = Align.Main.Center, pad = padding),
        at = at,
        size = size,
        radii = radii,
    ).withStates().namedId("Button")
}

@JvmName("Switch")
fun Switch(at: Vec2 = Vec2.ZERO, size: Float, padding: Float = 3f, state: Boolean = false, lateralStretch: Float = 1.8f): Block {
    val circleSize = size - (padding + padding)
    return Block(
        Block(size = Vec2(circleSize, circleSize)).radius(circleSize / 2f).setPalette { text.primary },
        at = at,
        size = Vec2(size * lateralStretch, size),
        alignment = Align(main = Align.Main.Start, pad = Vec2(padding, 0f)),
    ).radius(size / 2f).toggleable(state).namedId("Switch").apply {
        if (state) afterParentInit {
            this[0].x = this.x + this.width - this[0].width - padding
        }
    }.onToggle {
        val circle = this[0]
        val target = this.x + if (it) this.width - circle.width - padding else padding
        Move(circle, target, add = false, animation = Animations.Default.create(0.2.seconds)).add()
    }
}

/**
 * For images, use `Image` for each entry.
 *
 * For both strings and images, use `Image to "string"`.
 */
@JvmName("Radiobutton")
fun Radiobutton(vararg entries: String, at: Vec2 = Vec2.ZERO, initial: Int = 0, fontSize: Float = 12f, optionLateralPadding: Float = 6f, optionVerticalPadding: Float = 6f) = Radiobutton(entries = entries.mapToArray { null to it }, at, initial, fontSize, optionLateralPadding, optionVerticalPadding)

/**
 * For strings, use `"string"` for each entry.
 *
 * For both strings and images, use `Image to "string"`.
 */
@JvmName("Radiobutton")
fun Radiobutton(vararg entries: PolyImage, at: Vec2 = Vec2.ZERO, initial: Int = 0, fontSize: Float = 12f, optionLateralPadding: Float = 6f, optionVerticalPadding: Float = 6f) = Radiobutton(entries = entries.mapToArray { it to null }, at, initial, fontSize, optionLateralPadding, optionVerticalPadding)

/**
 * For just strings, use `"string"` for each entry.
 *
 * For just images, use `Image` for each entry.
 *
 * `null to null` is not supported, and will throw an exception.
 */
@JvmName("Radiobutton")
fun Radiobutton(vararg entries: Pair<PolyImage?, String?>, at: Vec2 = Vec2.ZERO, initial: Int = 0, fontSize: Float = 12f, optionLateralPadding: Float = 6f, optionVerticalPadding: Float = 6f): Block {
    val optAlign = Align(Align.Main.Center, pad = Vec2(optionLateralPadding, optionVerticalPadding))
    val buttons = entries.mapToArray { (img, text) ->
        require(img != null || text != null) { "image and text cannot both be null on Radiobutton" }
        Group(
            if (img != null) Image(img) else null,
            if (text != null) Text(text, fontSize = fontSize).withStates() else null,
            alignment = optAlign,
        ).onClick {
            val children = parent.children!!
            val parent = parent
            if (parent is Inputtable && parent.hasListenersFor(Event.Change.Number::class.java)) {
                val ev = Event.Change.Number(children.indexOf(this) - 1)
                parent.accept(ev)
                if (ev.cancelled) return@onClick false
            }
            val f = children.first()
            Move(f, this.x, this.y, add = false, animation = Animations.Default.create(0.15.seconds)).add()
            Resize(f, this.width, this.height, add = false, animation = Animations.Default.create(0.15.seconds)).add()
            true
        }
    }
    return Block(
        at = at,
        children = buttons,
    ).afterInit { _ ->
        val target = this[initial]
        val it = Block().ignoreLayout().setPalette(polyUI.colors.brand.fg)
        addChild(it, recalculate = false)
        // asm: scaling is applied twice so we can just
        it.at = target.at
        it.size = target.size
        it.relegate()
    }.namedId("Radiobutton")
}

/**
 * For images, use `Image to "string"` for each entry.
 */
@JvmName("Dropdown")
fun Dropdown(vararg entries: String, at: Vec2 = Vec2.ZERO, fontSize: Float = 12f, initial: Int = 0, padding: Float = 12f, textLength: Float = 0f): Block {
    return Dropdown(entries = entries.mapToArray { null to it }, at, fontSize, initial, padding, textLength)
}

@JvmName("Dropdown")
fun Dropdown(vararg entries: Pair<PolyImage?, String>, at: Vec2 = Vec2.ZERO, fontSize: Float = 12f, initial: Int = 0, optPadding: Float = 12f, textLength: Float = 0f): Block {
    var heightTracker = 0f
    val it = Block(
        Text("", fontSize = fontSize, visibleSize = if (textLength == 0f) Vec2.ZERO else Vec2(textLength, fontSize)),
        Image("polyui/chevron-down.svg"),
        at = at,
        focusable = true,
        alignment = Align(main = Align.Main.SpaceBetween, pad = Vec2(8f, 8f), maxRowSize = 0),
    ).withStates().withBoarder()
    val dropdown = Block(
        alignment = Align(mode = Align.Mode.Vertical, pad = Vec2(optPadding, 6f)),
        children = entries.mapToArray { (img, text) ->
            Group(
                if (img != null) Image(img) else null,
                Text(text, fontSize = fontSize).withStates()
            ).onClick { _ ->
                val title = (it[0] as Text)
                val self = ((if (children!!.size == 2) this[1] else this[0]) as Text).text
                if (title.text == self) return@onClick false
                if (it.hasListenersFor(Event.Change.Number::class.java)) {
                    val ev = Event.Change.Number(parent.children!!.indexOf(this))
                    it.accept(ev)
                    if (ev.cancelled) return@onClick false
                }
                title.text = self
                true
            }
        },
    ).namedId("DropdownMenu")
    return it.events {
        Event.Focused.Gained then {
            polyUI.master.addChild(dropdown, recalculate = false)
            dropdown.x = this.x
            dropdown.y = this.y + this.height
            if (dropdown.height != 0f) heightTracker = dropdown.height
            dropdown.height = 0f
            Resize(dropdown, height = heightTracker, add = false, animation = Animations.Default.create(0.15.seconds)).add()
            Rotate(this[1], PI, add = false, animation = Animations.Default.create(0.15.seconds)).add()
        }
        Event.Focused.Lost then {
            Resize(dropdown, height = 0f, add = false, animation = Animations.Default.create(0.15.seconds)) {
                dropdown.parent.removeChild(dropdown, recalculate = false)
            }.add()
            Rotate(this[1], 0.0, add = false, animation = Animations.Default.create(0.15.seconds)).add()
        }
        Event.Lifetime.PostInit then {
            dropdown.setup(polyUI)
            this.width = dropdown.width
            // sets the #created-with-initial-size flag to true, meaning that if it is recalculated, it 'remembers' that it should be
            // this big. not necessary normally important, only if they decided to recalculate the dropdown for any reason.
            this.layoutFlags = layoutFlags or 0b00000010
            this[1].x = this.x + (this.width - this[1].width - alignment.pad.x)

            val first = dropdown[initial]
            (this[0] as Text).text = ((if (first.children!!.size == 2) first[1] else first[0]) as Text).text
        }
        Event.Mouse.Companion.Clicked then {
            if (focused) {
                polyUI.unfocus()
                true
            } else false
        }
    }.namedId("Dropdown")
}

/**
 * Note that slider change events cannot be cancelled.
 */
@JvmName("Slider")
fun Slider(at: Vec2 = Vec2.ZERO, min: Float = 0f, max: Float = 100f, initialValue: Float = min, ptrSize: Float = 24f, length: Float = (max - min) * 2f, integral: Boolean = false, instant: Boolean = false): Drawable {
    require(initialValue in min..max) { "initial value $initialValue is out of range for slider of $min..$max" }
    val barHeight = ptrSize / 2.8f
    val size = Vec2(length + ptrSize, ptrSize)

    val slide: Inputtable.() -> Float = {
        val bar = this.parent[0]
        val half = this.width / 2f
        this.x = this.x.coerceIn(bar.x - half, bar.x + bar.width - half)
        bar[0].width = x - bar.x + half
        val progress = (this.x + half - bar.x) / bar.width
        val value = min + (max - min) * progress
        if (integral) value.toInt().toFloat()
        else value
    }

    return Group(
        Block(
            Block(
                size = Vec2(1f, barHeight),
            ).radius(barHeight / 2f).setPalette { brand.fg },
            size = Vec2(length, barHeight),
            alignment = Align(Align.Main.Start, pad = Vec2.ZERO),
        ).radius(barHeight / 2f),
        Block(
            size = ptrSize.vec,
        ).radius(ptrSize / 2f).setPalette { text.primary }.withStates().draggable(withY = false).onDrag {
            val value = slide()
            val p = parent as Inputtable
            if (instant && p.hasListenersFor(Event.Change.Number::class.java)) {
                p.accept(Event.Change.Number(value))
            }
        }.onDragEnd {
            val value = slide()
            val p = parent as Inputtable
            if (p.hasListenersFor(Event.Change.Number::class.java)) {
                p.accept(Event.Change.Number(value))
            }
        }.events {
            val op = object : ComponentOp.Animatable<Block>(self, Animations.Default.create(0.15.seconds, 1f, 0f)) {
                override fun apply(value: Float) {}

                override fun unapply(value: Float) {
                    self.apply {
                        val maxSize = this.width - 6f
                        val maxRadius = (this.radii?.get(0)?.minus(2f))?.coerceAtLeast(0f) ?: 0f
                        val current = maxSize * value
                        val offset = (this.width - current) / 2f
                        renderer.rect(x + offset, y + offset, current, current, polyUI.colors.brand.fg.normal, maxRadius * value)
                    }
                }

                override fun unapply(): Boolean {
                    unapply(animation!!.value)
                    return false
                }
            }
            op.add()
            Event.Mouse.Exited then {
                op.reverse()
            }
            Event.Mouse.Entered then {
                op.reverse()
            }
            Event.Mouse.Released then {
                val value = slide()
                val p = parent as Inputtable
                if (p.hasListenersFor(Event.Change.Number::class.java)) p.accept(Event.Change.Number(value))
            }
        },
        at = at,
        size = size,
        alignment = Align(Align.Main.Start, pad = Vec2.ZERO),
    ).onPress {
        val ptr = this[1]
        ptr.x = it.x - ptr.width / 2f
        this.polyUI.inputManager.recalculate()
        ptr.inputState = INPUT_PRESSED
        ptr.accept(it)
        ptr.slide()
    }.afterInit {
        val bar = this[0]
        val ptr = this[1]
        ptr.x = bar.x + length * ((initialValue - min) / (max - min))
        bar.x += ptrSize / 2f
        bar[0].width = ptr.x - bar.x + (ptrSize / 2f)
    }.namedId("Slider")
}

@JvmName("Checkbox")
fun Checkbox(at: Vec2 = Vec2.ZERO, size: Float, state: Boolean = false) = Block(
    Image(
        image = PolyImage("polyui/check.svg"),
        size = (size / 1.25f).vec,
    ).fade(state),
    at = at,
    size = Vec2(size, size),
    alignment = Align(pad = ((size - size / 1.25f) / 2f).vec),
).namedId("Checkbox").toggleable(state).onToggle { this[0].fade(it) }

@JvmName("BoxedTextInput")
fun BoxedTextInput(
    image: PolyImage? = null,
    pre: String? = null,
    placeholder: String = "polyui.textinput.placeholder",
    initialValue: String = "",
    fontSize: Float = 12f,
    center: Boolean = false,
    post: String? = null,
    size: Vec2 = Vec2.ZERO,
) = Block(
    if (image != null) Image(image).padded(6f, 0f, 0f, 0f) else null,
    if (pre != null) Text(pre).padded(6f, 0f, 0f, 0f) else null,
    Group(
        TextInput(placeholder = placeholder, text = initialValue, fontSize = fontSize, visibleSize = if (size.isPositive) Vec2(size.x, fontSize) else Vec2.ZERO).run {
            on(Event.Change.Text) {
                parent.position()
                (parent.parent as? Inputtable)?.accept(it) == true
            }
        },
        alignment = Align(main = if (center) Align.Main.Center else Align.Main.Start, pad = Vec2(6f, 10f)),
        size = size,
    ).afterInit {
        val input = this[0]
        input.visibleSize = input.visibleSize.coerceAtMost(this.visibleSize - Vec2(alignment.pad.x * 2f, 0f))
    },
    if (post != null) Block(Text(post).secondary(), alignment = Align(pad = 6f by 10f), radii = floatArrayOf(0f, 8f, 0f, 8f)).afterInit { color = polyUI.colors.page.bg.normal } else null,
    alignment = Align(pad = Vec2.ZERO, main = Align.Main.SpaceBetween)
).withBoarder()

@JvmName("BoxedNumericInput")
fun BoxedNumericInput(
    image: PolyImage? = null,
    pre: String? = null,
    initialValue: Float = 0f,
    min: Float = 0f,
    max: Float = 100f,
    step: Float = 1f,
    integral: Boolean = false,
    fontSize: Float = 12f,
    center: Boolean = false,
    post: String? = null,
    radius: Float = 8f,
    size: Vec2 = Vec2.ZERO,
) = Group(
    BoxedTextInput(
        image, pre,
        placeholder = "${if (integral) max.toInt() else max}",
        initialValue = "${if (integral) initialValue.toInt() else initialValue}",
        fontSize, center, post, size
    ).also {
        require(initialValue in min..max) { "initial value $initialValue is out of range for numeric text input of $min..$max" }
        if (it.children?.size == 3) (it[1][0] as TextInput).numeric(min, max, integral)
        else (it[0][0] as TextInput).numeric(min, max, integral)
    }.radii(radius, 0f, radius, 0f),
    Block(
        Image("polyui/chevron-down.svg").onClick {
            val boxed = parent.parent[0]
            val text = if (boxed.children?.size == 3) boxed[1][0] as Text else boxed[0][0] as Text
            val value = text.text.toFloat()
            val newValue = (value + step).coerceIn(min, max)
            text.text = if (integral) newValue.toInt().toString() else newValue.toString()
            true
        }.withStates().also { it.rotation = PI },
        Image("polyui/chevron-down.svg").onClick {
            val boxed = parent.parent[0]
            val text = if (boxed.children?.size == 3) boxed[1][0] as Text else boxed[0][0] as Text
            val value = text.text.toFloat()
            val newValue = (value - step).coerceIn(min, max)
            text.text = if (integral) newValue.toInt().toString() else newValue.toString()
            true
        }.withStates(),
        radii = floatArrayOf(0f, radius, 0f, radius),
        alignment = Align(main = Align.Main.Center, mode = Align.Mode.Vertical, pad = Vec2(2f, 2f)),
        size = Vec2(0f, 32f)
    ).onScroll { (_, y) ->
        if (y > 0f) this[0].accept(Event.Mouse.Clicked)
        else this[1].accept(Event.Mouse.Clicked)
    }.withBoarder(),
    alignment = Align(pad = Vec2.ZERO)
)

/**
 * Spawn a menu at the mouse position.
 * @param polyUI an instance of PolyUI. If `null`, [openNow] must be `false`, or else an exception will be thrown.
 * @param openNow if `true`, the menu is opened immediately. else, call [PolyUI.focus] on the return value to open it.
 */
@Contract("_, _, _, null, true, _ -> fail")
@JvmName("PopupMenu")
fun PopupMenu(vararg children: Component?, size: Vec2 = Vec2.ZERO, align: Align = AlignDefault, polyUI: PolyUI?, openNow: Boolean = true, position: Point = Point.At): Block {
    val it = Block(
        focusable = true,
        size = size,
        alignment = align,
        children = children,
    ).withBoarder().events {
        Event.Focused.Gained then {
            this.polyUI.master.addChild(this, recalculate = false)
            alpha = 0f
            val mx = this.polyUI.mouseX
            val my = this.polyUI.mouseY
            val sz = this.polyUI.size
            when (position) {
                Point.At -> {
                    x = mx.coerceWithin(0f, sz.x - this.width)
                    y = my.coerceWithin(0f, sz.y - this.height)
                }

                Point.Above -> {
                    x = (mx - (this.width / 2f)).coerceWithin(0f, sz.x - this.width)
                    y = (my - this.height - 6f).coerceWithin(0f, sz.y - this.height)
                }

                Point.Below -> {
                    x = (mx - (this.width / 2f)).coerceWithin(0f, sz.x - this.width)
                    y = (my + 12f).coerceWithin(0f, sz.y - this.height)
                }
            }
            fadeIn(0.2.seconds)
            true
        }
        Event.Focused.Lost then {
            Fade(this, 0f, false, Animations.Default.create(0.2.seconds)) {
                this.polyUI.master.removeChild(this, recalculate = false)
            }.add()
        }
    }
    if (openNow) {
        require(polyUI != null) { "polyUI cannot be null if openNow is true" }
        polyUI.focus(it)
    }
    return it
}
