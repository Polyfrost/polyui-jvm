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
@file:Suppress("FunctionName", "RedundantCompanionReference")

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
import org.polyfrost.polyui.event.State
import org.polyfrost.polyui.operations.ComponentOp
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.operations.Rotate
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.*
import kotlin.experimental.or
import kotlin.math.PI
import kotlin.math.sign

private val SEARCH = PolyImage.of("polyui/search.svg")
private val CHEVRON_DOWN = PolyImage.of("polyui/chevron-down.svg")
private val CHECK = PolyImage.of("polyui/check.svg")

/**
 * Simple button component, with text and up to 2 images.
 *
 * To use this button, you can use the [onClick] method to detect presses.
 */
@JvmName("Button")
fun Button(leftImage: PolyImage? = null, text: String? = null, rightImage: PolyImage? = null, fontSize: Float = 12f, font: Font? = null, radii: FloatArray = floatArrayOf(8f), padding: Vec2 = Vec2(12f, 6f), at: Vec2 = Vec2.ZERO, size: Vec2 = Vec2.ZERO): Block {
    return Block(
        if (leftImage != null) Image(leftImage) else null,
        if (text != null) Text(text, fontSize = fontSize, font = font) else null,
        if (rightImage != null) Image(rightImage) else null,
        alignment = Align(main = Align.Content.SpaceEvenly, wrap = Align.Wrap.NEVER, pad = padding),
        at = at,
        size = size,
        radii = radii,
    ).withHoverStates().namedId("Button")
}

/**
 * Simple switch component.
 */
@JvmName("Switch")
fun Switch(at: Vec2 = Vec2.ZERO, size: Float, padding: Float = 3f, state: State<Boolean> = State(false), lateralStretch: Float = 1.8f): Block {
    val circleSize = size - (padding + padding)
    return Block(
        Block(size = Vec2(circleSize, circleSize)).radius(circleSize / 2f).setPalette { text.primary }.denyPaletteChanges().disable(),
        at = at,
        size = Vec2(size * lateralStretch, size),
        alignment = Align(main = Align.Content.Start, pad = Vec2(padding, 0f)),
    ).radius(size / 2f).toggleable(state).namedId("Switch").apply {
        if (state.value) afterParentInit {
            val circle = this[0]
            // #created-with-set-position = true
            circle.layoutFlags = circle.layoutFlags or 0b00000100
            circle.x = 2f * this.x + this.width - circle.width - circle.x
        }
    }.apply {
        state.weaklyListen(this) {
            val circle = this[0]
            val target = 2f * this.x + this.width - circle.width - circle.getTargetPosition().x
            Move(circle, x = target, add = false, animation = Animations.Default.create(0.2.seconds)).add()
        }
    }
}

/**
 * A radiobutton component, which allows you to select one of multiple options.
 * For images, use `Image` for each entry.
 *
 * For both strings and images, use `Image to "string"`.
 */
@JvmName("Radiobutton")
fun Radiobutton(vararg entries: String, at: Vec2 = Vec2.ZERO, state: State<Int> = State(0), fontSize: Float = 12f, optionLateralPadding: Float = 6f, optionVerticalPadding: Float = 6f) = Radiobutton(entries = entries.mapToArray { null to it }, at, state, fontSize, optionLateralPadding, optionVerticalPadding)

/**
 * For strings, use `"string"` for each entry.
 *
 * For both strings and images, use `Image to "string"`.
 */
@JvmName("Radiobutton")
fun Radiobutton(vararg entries: PolyImage, at: Vec2 = Vec2.ZERO, state: State<Int> = State(0), fontSize: Float = 12f, optionLateralPadding: Float = 6f, optionVerticalPadding: Float = 6f) = Radiobutton(entries = entries.mapToArray { it to null }, at, state, fontSize, optionLateralPadding, optionVerticalPadding)

/**
 * For just strings, use `"string"` for each entry.
 *
 * For just images, use `Image` for each entry.
 *
 * `null to null` is not supported, and will throw an exception.
 */
@JvmName("Radiobutton")
fun Radiobutton(vararg entries: Pair<PolyImage?, String?>, at: Vec2 = Vec2.ZERO, state: State<Int> = State(0), fontSize: Float = 12f, optionLateralPadding: Float = 6f, optionVerticalPadding: Float = 6f): Block {
    val optAlign = Align(Align.Content.Center, pad = Vec2(optionLateralPadding, optionVerticalPadding))
    val buttons = entries.mapToArray { (img, text) ->
        require(img != null || text != null) { "image and text cannot both be null on Radiobutton" }
        Group(
            if (img != null) Image(img) else null,
            if (text != null) Text(text, fontSize = fontSize).withHoverStates() else null,
            alignment = optAlign,
        ).onClick {
            state.set(this.siblings.indexOf(this) - 1)
        }
    }

    return Block(
        at = at,
        children = buttons,
        alignment = Align(wrap = Align.Wrap.NEVER)
    ).afterInit { _ ->
        val target = this[state.value]
        val it = Block().ignoreLayout().setPalette(polyUI.colors.brand.fg)
        addChild(it, recalculate = false)
        // asm: scaling is applied twice so we can just
        it.at = target.at
        it.size = target.size
        it.relegate()
    }.apply {
        state.weaklyListen(this) {
            val children = this.children ?: throw IllegalStateException("Radiobutton has no children (??)")
            if (it < 0 || it >= children.size - 1) throw IndexOutOfBoundsException("Radiobutton state index $it is out of bounds for ${children.size - 1} options")
            val selector = children[0]
            val target = children[it + 1]
            Move(selector, target.x, target.y, add = false, animation = Animations.Default.create(0.15.seconds)).add()
            Resize(selector, target.width, target.height, add = false, animation = Animations.Default.create(0.15.seconds)).add()
        }
    }.namedId("Radiobutton")
}

//fun OrderedList(title: String, entries: MutableList<String>, at: Vec2 = Vec2.ZERO, fontSize: Float = 12f, padding = 12f, textLength: Float = 0f): Block {
//
//}

/**
 * For images, use `Image to "string"` for each entry.
 */
@JvmName("Dropdown")
fun Dropdown(vararg entries: String, at: Vec2 = Vec2.ZERO, size: Vec2 = Vec2.ZERO, fontSize: Float = 12f, state: State<Int> = State(0), padding: Float = 12f): Block {
    return Dropdown(entries = entries.mapToArray { null to it }, at, size, fontSize, state, padding)
}

@JvmName("Dropdown")
fun Dropdown(vararg entries: Pair<PolyImage?, String>, at: Vec2 = Vec2.ZERO, size: Vec2 = Vec2.ZERO, fontSize: Float = 12f, state: State<Int> = State(0), optPadding: Float = 12f): Block {
    var heightTracker = 0f
    var titleText: String? = null
    val title = TextInput("", fontSize = fontSize, visibleSize = Vec2(0f, fontSize))
    val icon = Image(CHEVRON_DOWN)
    val it = Block(
        title, icon,
        at = at,
        size = size,
        focusable = true,
        alignment = Align(main = Align.Content.SpaceBetween, pad = Vec2(8f, 8f), wrap = Align.Wrap.NEVER),
    ).withHoverStates().withBorder()
    val dropdown = Block(
        alignment = Align(mode = Align.Mode.Vertical, padBetween = Vec2(optPadding, 6f), line = if (size.isPositive) Align.Line.Start else Align.Line.Center),
        children = entries.mapToArray { (img, text) ->
            Group(
                if (img != null) Image(img) else null,
                Text(text, fontSize = fontSize).withHoverStates()
            ).onClick { _ ->
                state.set(this.siblings.indexOf(this))
            }
        },
    ).addRethemingListeners().namedId("DropdownMenu")
    state.weaklyListen(dropdown) {
        val entry = this[it]
        val textComp = (if (entry.children!!.size == 2) entry[1] else entry[0]) as Text
        title.text = textComp.text
    }
    return it.events {
        Event.Focused.Gained then {
            polyUI.master.addChild(dropdown, recalculate = false)
            if (dropdown.width != this.width) {
                val scale = this.width / dropdown.width
                dropdown.rescale(scale, scale)
            }
            dropdown.x = this.x
            dropdown.y = this.y + this.height
            if (dropdown.height != 0f) heightTracker = dropdown.height
            dropdown.height = 0.01f
            Resize(dropdown, height = heightTracker, add = false, withVisible = false, animation = Animations.Default.create(0.15.seconds)).add()
            Rotate(icon, PI, add = false, animation = Animations.Default.create(0.15.seconds)).add()
        }
        Event.Focused.Lost then {
            Resize(dropdown, height = 0f, add = false, withVisible = false, animation = Animations.Default.create(0.15.seconds)) {
                dropdown.parent.removeChild(dropdown, recalculate = false)
            }.add()
            if (titleText != null) {
                title.text = titleText!!
                title.focused = false
                titleText = null
                icon.rotation = PI
                icon.image = CHEVRON_DOWN
            }
            Rotate(icon, 0.0, add = false, animation = Animations.Default.create(0.15.seconds)).add()
        }
        Event.Focused.Companion.KeyTyped then {
            if (titleText == null) {
                titleText = title.text
                title.placeholder = titleText!!
                title.text = ""
                icon.rotation = 0.0
                icon.image = SEARCH
            }
            title.focused = true
            title.acceptsInput = false
            title.accept(it)
            needsRedraw = true
        }
        Event.Focused.Companion.KeyPressed then {
            title.accept(it)
            needsRedraw = true
        }
        Event.Lifetime.PostInit then {
            dropdown.setup(polyUI)
            if (createdWithSetSize) {
                dropdown.width = this.width
            } else this.width = dropdown.width
            // sets the #created-with-initial-size flag to true, meaning that if it is recalculated, it 'remembers' that it should be
            // this big. not necessary normally important, only if they decided to recalculate the dropdown for any reason.
            this.layoutFlags = layoutFlags or 0b00000010
            val space = (this.width - icon.width - alignment.padEdges.x)
            icon.x = this.x + space

            title.acceptsInput = false
            val first = dropdown[state.value]
            title.text = ((if (first.children!!.size == 2) first[1] else first[0]) as Text).text
            title.visibleSize = Vec2(space - alignment.padEdges.x - 2f, fontSize)
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
fun Slider(at: Vec2 = Vec2.ZERO, min: Float = 0f, max: Float = 100f, state: State<out Number> = State(min), ptrSize: Float = 24f, length: Float = (max - min) * 2f, steps: Int = 0, instant: Boolean = false): Drawable {
    require(state.value.toFloat() in min..max) { "Initial slider state value ${state.value} is out of bounds ($min to $max)" }
    val barHeight = ptrSize / 2.8f
    val size = Vec2(length + ptrSize, ptrSize)
    val nsteps = steps + 1

    val slide: Inputtable.() -> Float = slider@{
        val bar = this.parent[0]
        val half = this.width / 2f
        bar.apply {
            if (nsteps != 1) {
                val stepSize = this.width / nsteps
                val snapAmount = stepSize / 2f - 4f
                for (i in 0 until nsteps) {
                    val stepX = x + i * stepSize
                    if (this@slider.x + half in (stepX - snapAmount)..(stepX + snapAmount)) {
                        this@slider.x = stepX - half
                        break
                    }
                }
            }
        }
        this.x = this.x.coerceIn(bar.x - half, bar.x + bar.width - half)
        bar[0].width = x - bar.x + half
        val progress = ((this.x + half - bar.x) / bar.width).coerceIn(0f, 1f)
        min + (max - min) * progress
    }
    val animation = Animations.Default.create(0.15.seconds, 1f, 0f)

    return Group(
        Block(
            Block(
                size = Vec2(1f, barHeight),
            ).ignoreLayout().radius(barHeight / 2f).setPalette { brand.fg },
            size = Vec2(length, barHeight),
        ).radius(barHeight / 2f).apply {
            addOperation {
                val value = animation.value
                if (nsteps != 1) {
                    val stepSize = width / nsteps
                    val curHeight = (height + 10f) * value
                    val centerY = y + height / 2f
                    val yPos = centerY - curHeight / 2f
                    for (i in 1 until nsteps) {
                        val stepX = x + i * stepSize
                        renderer.rect(stepX, yPos, 4f, curHeight, color, 2f)
                    }
                }
            }
        },
        Block(
            size = ptrSize.vec,
        ).radius(ptrSize / 2f).setPalette { text.primary }.denyPaletteChanges().withHoverStates().draggable(withY = false).ignoreLayout().onDrag {
            if (instant) state.setNumber(slide())
        }.onDragEnd {
            state.setNumber(slide())
        }.events {
            val op = object : ComponentOp.Animatable<Block>(self, animation) {
                override fun apply(value: Float) {}

                override fun unapply(value: Float) {
                    self.apply {
                        val maxSize = this.width - 6f
                        val maxRadius = if (!polyUI.settings.roundedCorners) 0f else (this.radii?.get(0)?.minus(2f))?.coerceAtLeast(0f) ?: 0f
                        val current = maxSize * value
                        val offset = (this.width - current) / 2f
                        renderer.rect(x + offset, y + offset, current, current, polyUI.colors.brand.fg.normal, maxRadius * value)
                    }
                }

                override fun unapply(): Boolean {
                    unapply(this.animation!!.value)
                    return false
                }
            }
            op.add()
            Event.Mouse.Exited then {
                op.reverse()
                animation.to = 0f
            }
            Event.Mouse.Entered then {
                op.reverse()
                animation.to = 1f
            }
            Event.Mouse.Companion.Pressed then {
                animation.from = animation.value
                animation.to = 0.8f
                animation.reset()
                // asm: don't move the slider if the mouse is inside the blob (they are doing a slide operation, not a click to set)
                if (isInside(it.x, it.y)) return@then
                x = it.x - width / 2f
                this.polyUI.inputManager.recalculate()
                inputState = INPUT_PRESSED
                accept(it)
                state.setNumber(slide())
            }
            Event.Mouse.Companion.Released then {
                animation.from = animation.to
                animation.to = 1f
                animation.reset()
                false
            }
        },
        at = at,
        size = size,
        alignment = Align(Align.Content.Center, pad = Vec2.ZERO),
    ).afterInit {
        @Suppress("DEPRECATION")
        setSliderValue(state.value.toFloat(), min, max)
    }.apply {
        state.weaklyListen(this) {
            @Suppress("DEPRECATION")
            setSliderValue(it.toFloat(), min, max); false
        }
    }.namedId("Slider")
}

@JvmName("Checkbox")
fun Checkbox(at: Vec2 = Vec2.ZERO, size: Float, state: State<Boolean> = State(false)) = Block(
    Image(
        image = CHECK,
    ).fade(state.value),
    at = at,
    size = Vec2(size, size),
    alignment = Align(pad = ((size - size / 1.25f) / 2f).vec),
).namedId("Checkbox").toggleable(state).radius(size / 4f).apply {
    state.weaklyListen(this) {
        this[0].fade(it)
        false
    }
}

@JvmName("BoxedTextInput")
@Suppress("UNCHECKED_CAST")
fun BoxedTextInput(
    image: PolyImage? = null,
    pre: String? = null,
    placeholder: String = "polyui.textinput.placeholder",
    value: Any = "",
    fontSize: Float = 12f,
    center: Boolean = false,
    post: String? = null,
    size: Vec2 = Vec2.ZERO,
) = Block(
    if (image != null) Image(image).padded(6f, 0f, 0f, 0f) else null,
    if (pre != null) Text(pre).padded(6f, 0f, 0f, 0f) else null,
    Group(
        when (value) {
            is String -> TextInput(placeholder = placeholder, text = value, fontSize = fontSize)
            is State<*> -> TextInput(placeholder = placeholder, text = value as State<String>, fontSize = fontSize)
            else -> throw IllegalArgumentException("initialValue must be a String or a State<String>")
        },
        alignment = Align(main = if (center) Align.Content.Center else Align.Content.Start, cross = Align.Content.Center, pad = Vec2.ZERO),
    ).padded(6f, 0f, if (post == null) 6f else 0f, 0f),
    if (post != null) Block(Text(post).secondary(), alignment = Align(pad = 6f by 10f), radii = floatArrayOf(0f, 8f, 0f, 8f)).afterInit { color = polyUI.colors.page.bg.normal }.padded(6f, 0f, 0f, 0f) else null,
    alignment = Align(pad = Vec2(0f, if (post == null) 6f else 0f), main = Align.Content.SpaceBetween, cross = Align.Content.Center, line = Align.Line.Center, wrap = Align.Wrap.NEVER),
    size = size,
).afterInit {
    if (center) this.getTextFromBoxedTextInput().theText.listen {
        this.getTextFromBoxedTextInput().parent.position()
    }
}.withBorder().namedId("BoxedTextInput")

@JvmName("BoxedNumericInput")
fun BoxedNumericInput(
    image: PolyImage? = null,
    pre: String? = null,
    min: Float = 0f,
    max: Float = 100f,
    state: State<out Number> = State(min),
    step: Float = 1f,
    fontSize: Float = 12f,
    center: Boolean = false,
    post: String? = null,
    arrows: Boolean = true,
    size: Vec2 = Vec2.ZERO,
) = BoxedTextInput(
    image, pre,
    placeholder = max.toString(dps = 2),
    value = state.value.toFloat().toString(dps = 2),
    fontSize, center, post, size
).also {
    require(state.value.toFloat() in min..max) { "initial value ${state.value} is out of range for numeric text input of $min..$max" }
    val text = it.getTextFromBoxedTextInput()
    text.numeric(min, max, state)
    if (arrows) {
        it.children?.let { children ->
            val arrowsUnit = Group(
                Image("polyui/chevron-down.svg".image(), size = Vec2(14f, 14f)).onClick { _ ->
                    state.setNumber((state.value.toFloat() + step).coerceIn(min, max))
                }.withHoverStates().also { it.rotation = PI },
                Image("polyui/chevron-down.svg".image(), size = Vec2(14f, 14f)).onClick { _ ->
                    state.setNumber((state.value.toFloat() - step).coerceIn(min, max))
                }.withHoverStates(),
                alignment = Align(main = Align.Content.Center, mode = Align.Mode.Vertical, pad = Vec2(1f, 0f)),
                size = Vec2(0f, 32f)
            ).onScroll { (_, y) ->
                state.setNumber((state.value.toFloat() + sign(y) * step).coerceIn(min, max))
            }
            val last = children.last()
            if (last is Block) {
                // asm: cheat but is a lot cleaner
                arrowsUnit.padded(-3f, 0f)
                last.children?.first()?.padded(2f, 0f)
                // asm: has a post box, so we will add the arrows into here instead.
                last.alignment = Align(pad = Vec2(3f, 0f), wrap = Align.Wrap.NEVER)
                last.addChild(arrowsUnit)
            } else {
                // asm: no post box, so we will add the arrows into the main block.
                it.addChild(arrowsUnit)
            }
        }
    }
    it.afterInit {
        val text = getTextFromBoxedTextInput()
        text.visibleSize = text.parent.visibleSize
    }
}.namedId("BoxedNumericInput")

/**
 * Numeric text input, with an icon or a pre text that can be dragged to change the value.
 * Note that either `icon` or `pre` must be provided, and not both, or neither, as an exception will be thrown.
 *
 * Compatible with [org.polyfrost.polyui.component.extensions.getTextFromBoxedTextInput].
 * @since 1.10.5
 */
@Contract("null, null, _, _, _, _, _, _ -> fail")
@JvmName("DraggingNumericTextInput")
fun DraggingNumericTextInput(
    icon: PolyImage? = null,
    pre: String? = null,
    min: Float = 0f,
    max: Float = 100f,
    state: State<out Number> = State(min),
    step: Float = 1f,
    fontSize: Float = 12f,
    suffix: String? = null,
    size: Vec2 = Vec2.ZERO
): Block {
    require(state.value.toFloat() in min..max) { "initial value ${state.value} is out of range for numeric text input of $min..$max" }

    val pre = if (icon != null) Image(icon) else if (pre != null) Text(pre, fontSize = fontSize) else throw IllegalArgumentException("Either icon or pre text must be provided for DraggingNumericTextInput")
    var prevX = 0f
    pre.onDrag {
        if (polyUI.mouseX == prevX) return@onDrag
        val input = parent[1] as TextInput
        val isNegative = polyUI.mouseX < prevX
        if (input.text.isEmpty()) {
            input.text = if (isNegative) "0" else step.toString(dps = 2)
        } else {
            val txt = if (suffix != null) input.text.removeSuffix(suffix) else input.text
            input.text = txt.toFloatOrNull()?.let { if (isNegative) (it - step).coerceAtLeast(min) else (it + step).coerceAtMost(max) }.toString(dps = 2)
            needsRedraw = true
        }
        prevX = polyUI.mouseX
    }
    if (suffix != null) pre.onDragEnd {
        (parent[1] as TextInput).text += suffix
    }
    val maxDigits = if (state.isIntegral()) max.toInt().digits else Int.MAX_VALUE
    val out = Block(
        alignment = Align(cross = Align.Content.Center, main = Align.Content.SpaceBetween, wrap = Align.Wrap.NEVER),
        size = size
    ).withBorder().namedId("DraggingNumericTextInput")


    out.addChild(
        pre,
        TextInput(
            text = if (state.value == 0f) "" else if (suffix != null) "${state.value.toFloat().toString(dps = 2)}$suffix" else state.value.toFloat().toString(dps = 2),
            placeholder = if (suffix != null) "${max.toString(dps = 2)}$suffix" else max.toString(dps = 2),
            fontSize = fontSize
        ).apply {
            this.numeric(min = min, max = max, state = state, ignoreSuffix = suffix)
            if (suffix != null) {
                onFocusGained {
                    this.text = this.text.removeSuffix(suffix)
                }
                onFocusLost {
                    this.text += suffix
                }
            }
            theText.listen {
                if (it.length > maxDigits) {
                    shake(); return@listen true
                }
                false
            }
        },
    )
    return out
}

/**
 * Spawn a menu at the mouse position.
 * @param polyUI an instance of PolyUI. If `null`, [openNow] must be `false`, or else an exception will be thrown.
 * @param openNow if `true`, the menu is opened immediately. else, call [PolyUI.focus] on the return value to open it.
 * @see [spawnAtMouse] *(revised 1.12.0)* this function is now a wrapper for [Block.spawnAtMouse].
 */
@Contract("_, _, _, null, true, _ -> fail")
@JvmName("PopupMenu")
fun PopupMenu(vararg children: Component?, size: Vec2 = Vec2.ZERO, align: Align = AlignDefault, polyUI: PolyUI?, openNow: Boolean = true, spawnPos: SpawnPos = SpawnPos.AtMouse) = Block(
    focusable = true,
    size = size,
    alignment = align,
    children = children,
).withBorder().spawnAtMouse(polyUI, openNow, spawnPos).setPalette { page.bg }
