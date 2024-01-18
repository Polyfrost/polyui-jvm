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

package org.polyfrost.polyui.component

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI.Companion.DANGER
import org.polyfrost.polyui.PolyUI.Companion.INPUT_HOVERED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.PolyUI.Companion.SUCCESS
import org.polyfrost.polyui.PolyUI.Companion.WARNING
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.operations.*
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.unit.toChromaSpeed
import kotlin.jvm.internal.Ref.LongRef
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


private var dragging = false
const val MIN_DRAG = 3f

/**
 * Make this component draggable.
 * @param free if this is true, the component will be able to be dragged outside its parent.
 *
 */
fun <S : Drawable> S.draggable(withX: Boolean = true, withY: Boolean = true, free: Boolean = false, onStart: (S.() -> Unit)? = null, onDrag: (S.() -> Unit)? = null, onDrop: (S.() -> Unit)? = null): S {
    if (!withX && !withY) return this
    var pressed = false
    var px = 0f
    var py = 0f
    addEventHandler(Event.Mouse.Pressed(0)) {
        if (dragging) return@addEventHandler false
        dragging = true
        pressed = true
        px = it.x - x
        py = it.y - y
        false
    }
    addOperation(object : DrawableOp(this) {
        private var prevX = 0f
        private var prevY = 0f
        private var started = false
        override fun apply() {
            if (pressed) {
                if (self.inputState != INPUT_PRESSED) {
                    dragging = false
                    if (started) {
                        if (free && self.parent !== self.polyUI.master) {
                            self.polyUI.master.children!!.remove(self)
                            self.parent!!.children!!.add(self)
                        }
                        onDrop?.invoke(this@draggable)
                        started = false
                    }
                    pressed = false
                    return
                }
                if (!started) {
                    // asm: only start dragging if it has moved at least MIN_DRAG
                    if (abs(px + x - polyUI.inputManager.mouseX) > MIN_DRAG || abs(py + y - polyUI.inputManager.mouseY) > MIN_DRAG) {
                        started = true
                        self.polyUI.unfocus()
                        onStart?.invoke(this@draggable)
                        if (free && self.parent !== self.polyUI.master) {
                            self.parent!!.children!!.remove(self)
                            self.polyUI.master.children!!.add(self)
                        }
                    } else return
                }
                val mx = self.polyUI.inputManager.mouseX
                val my = self.polyUI.inputManager.mouseY
                var i = false
                if (withX && prevX != mx) {
                    self.x = mx - px
                    i = true
                }
                if (withY && prevY != my) {
                    self.y = my - py
                    i = true
                }
                needsRedraw = i
                if (i) onDrag?.invoke(this@draggable)
                prevX = mx
                prevY = my
            }
        }

        override fun unapply() = false
    })
    return this
}

/**
 * Add some text that is shown when the mouse is left still over this drawable
 * for 1 second or more.
 * @since 1.0.3
 */
fun <S : Drawable> S.addHoverInfo(text: String?): S {
    if (text == null) return this
    val obj = Block(
        children = arrayOf(Text(text))
    ).hide()
    obj.alpha = 0f
    onInit {
        obj.setup(polyUI)
        (parent ?: polyUI.master).addChild(obj, reposition = false)
        acceptsInput = true
    }
    var mx = 0f
    var open = false
    addOperation(object : DrawableOp.Animatable<S>(this, Animations.Linear.create(1.seconds)) {
        override fun apply() {
            if (self.inputState == INPUT_HOVERED) {
                super.apply()
            }
            if (open && mx != self.polyUI.mouseX) {
                animation?.reset()
                open = false
                Fade(obj, 0f, false, Animations.EaseInOutQuad.create(0.1.seconds)) {
                    renders = false
                }.add()
            }
        }

        override fun apply(value: Float) {
            if (!open && value == 1f) {
                mx = self.polyUI.mouseX
                obj.x = min(max(0f, mx - obj.width / 2f), self.polyUI.size.x - obj.width)
                obj.y = min(max(0f, self.polyUI.mouseY - obj.height - 4f), self.polyUI.size.y)
                obj.renders = true
                open = true
                Fade(obj, 1f, false, Animations.EaseInOutQuad.create(0.1.seconds)).add()
            }
        }

        override fun unapply(): Boolean {
            super.unapply()
            isFinished = false
            return false
        }
    })
    return this
}

fun <S : Drawable> S.namedId(name: String): S {
    this.simpleName = "$name@${this.simpleName.substringAfterLast('@')}"
    return this
}

fun <S : Drawable> S.named(name: String): S {
    this.simpleName = name
    return this
}

fun <S : Drawable> S.disable(state: Boolean = true): S {
    this.enabled = !state
    return this
}

fun <S : Drawable> S.hide(state: Boolean = true): S {
    this.renders = !state
    return this
}

fun <S : Drawable> S.setAlpha(alpha: Float): S {
    this.alpha = alpha
    return this
}

/**
 * Set the color palette of this drawable during initialization, using the PolyUI colors instance.
 */
fun <S : Drawable> S.setPalette(palette: Colors.() -> Colors.Palette): S {
    onInit {
        this.setPalette(polyUI.colors.palette())
    }
    return this
}

fun <S : Text> S.secondary(): S {
    setPalette { text.secondary }
    return this
}

/**
 * Add a validation function to this text component.
 *
 * The given [func] will have the text that is requested as the argument.
 * Return `false` to cancel the change, meaning the text will not be set.
 */
fun <S : Text> S.addValidationFunction(func: S.(String) -> Boolean): S {
    addEventHandler(Event.Change.Text()) {
        if (!func(this, it.text)) {
            it.cancel()
        }
    }
    return this
}

fun <S : Drawable> S.setDestructivePalette() = setPalette { Colors.Palette(text.primary.normal, state.danger.hovered, state.danger.pressed, text.primary.disabled) }

fun <S : Drawable> S.withStates(consume: Boolean = false, showClicker: Boolean = true, animation: (() -> Animation)? = { Animations.EaseInOutQuad.create(0.08.seconds) }): S {
    addEventHandler(Event.Mouse.Entered) {
        Recolor(this, this.palette.hovered, animation?.invoke()).add()
        if (showClicker) polyUI.cursor = Cursor.Clicker
        consume
    }
    addEventHandler(Event.Mouse.Exited) {
        Recolor(this, this.palette.normal, animation?.invoke()).add()
        polyUI.cursor = Cursor.Pointer
        consume
    }
    addEventHandler(Event.Mouse.Pressed(0)) {
        Recolor(this, this.palette.pressed, animation?.invoke()).add()
        consume
    }
    addEventHandler(Event.Mouse.Released(0)) {
        Recolor(this, this.palette.hovered, animation?.invoke()).add()
        consume
    }
    return this
}

fun <S : Block> S.withBoarder(color: PolyColor, width: Float = 1f): S {
    this.boarderColor = color
    this.boarderWidth = width
    return this
}

fun <S : Block> S.withBoarder(width: Float = 1f, color: (Colors.() -> PolyColor) = { page.border5 }): S {
    onInit {
        this.boarderColor = polyUI.colors.color()
        this.boarderWidth = width
    }
    return this
}

/**
 * Set the palette of this drawable according to one of the three possible component states in PolyUI.
 * @param state the state to set. One of [DANGER]/0 (red), [WARNING]/1 (yellow), [SUCCESS]/2 (green).
 */
fun <S : Drawable> S.setState(state: Byte): S {
    setPalette(
        when (state) {
            DANGER -> polyUI.colors.state.danger
            WARNING -> polyUI.colors.state.warning
            SUCCESS -> polyUI.colors.state.success
            else -> throw IllegalArgumentException("Invalid state: $state")
        }
    )
    return this
}

/**
 * Prioritize this drawable, meaning it will, in relation to its siblings:
 * - be drawn last
 * - receive events first
 * @since 1.0.0
 */
fun <S : Drawable> S.prioritize(): S {
    val children = parent?.children ?: return this
    if (children.last() === this) return this
    children.remove(this)
    children.add(this)
    return this
}

/**
 * Returns a count of this drawable's children, including children of children.
 * @since 1.0.1
 */
fun Drawable.countChildren(): Int {
    var i = children?.size ?: 0
    children?.fastEach {
        i += it.countChildren()
    }
    return i
}

/**
 * Returns `true` if this drawable has a parent that is the provided drawable.
 */
fun Drawable.hasParentOf(drawable: Drawable): Boolean {
    // asm: fast path: master is always a parent
    if (drawable === this.polyUI.master) return true
    var parent = this.parent
    while (parent != null) {
        if (parent === drawable) return true
        parent = parent.parent
    }
    return false
}

/**
 * @return `true` if this drawable has a child that intersects the provided rectangle.
 * @since 1.0.4
 */
fun Drawable.hasChildIn(x: Float, y: Float, width: Float, height: Float): Boolean {
    val children = this.children ?: return false
    children.fastEach {
        if (it.intersects(x, y, width, height)) return true
    }
    return false
}

fun <S : Drawable> S.onInit(function: S.(Event.Lifetime.Init) -> Unit): S {
    addEventHandler(Event.Lifetime.Init, function)
    return this
}

fun <S : Drawable> S.afterInit(function: S.(Event.Lifetime.PostInit) -> Unit): S {
    addEventHandler(Event.Lifetime.PostInit, function)
    return this
}

/**
 * Add an event handler to this drawable's parent, and (optionally) it's parent's parent ([depth]).
 *
 * This is used to run functions that may move or resize this drawable,
 * as if they were ran under this [Event.Lifetime.PostInit] they would be overwritten by the positioning logic.
 */
fun <S : Drawable> S.afterParentInit(depth: Int = 1, handler: S.() -> Unit): S {
    this.addEventHandler(Event.Lifetime.PostInit) { _ ->
        var it: Drawable = this
        for (i in 0 until depth) {
            it.parent?.let { parent -> it = parent } ?: break
        }
        it.addEventHandler(Event.Lifetime.PostInit) {
            handler(this@afterParentInit)
            false
        }
        false
    }
    return this
}

@EventDSL.Marker
fun <S : Drawable> S.events(dsl: EventDSL<S>.() -> Unit): S {
    EventDSL(this).apply(dsl)
    return this
}

fun <S : Drawable> S.onClick(func: S.(Event.Mouse.Clicked) -> Unit): S {
    addEventHandler(Event.Mouse.Clicked(0), func)
    return this
}

/**
 * Make the given color chroma, meaning it will change its hue over time.
 *
 * If you wish to change the speed of the color, you can change the value of the [LongRef] returned by [toChromaSpeed]:
 * ```
 * // apply the chroma to the color of this Drawable
 * val speed = 5.seconds.toChromaSpeed()
 * this.color.applyChroma(speed)
 *
 * // now you can mutate the speed at any point you like
 * speed.element = 10.seconds
 * ```
 *
 * @param speedNanos the speed of the chroma, in nanoseconds. The default is 5 seconds. use the [toChromaSpeed] extension function to convert from other units.
 * @return the [DrawableOp] that was applied to this drawable, so you can [remove][DrawableOp.remove] it later to stop the chroma.
 * @since 1.0.5
 */
context(S)
@ApiStatus.Experimental
fun <S : Drawable> PolyColor.makeChroma(speedNanos: LongRef = 5.seconds.toChromaSpeed()): DrawableOp {
    val p = object : DrawableOp(this@S) {
        private var time = ((hue % 360f) * speedNanos.element.toFloat()).toLong()
        override fun apply() {
            time += polyUI.delta
            val speed = speedNanos.element
            hue = (time % speed) / speed.toFloat()
        }

        override fun unapply() = false
    }
    p.add()
    return p
}

/**
 * Bulk add method for all the builtin drawable operations in PolyUI.
 *
 * The default values of this method are all zeroes, meaning that nothing is done.
 *
 * This is the "by" version of this method, meaning that each value is added to the current one of this drawable.
 * See the [animateTo] method for the "to" equivalent, which sets each value to the provided one.
 */
fun Drawable.animateBy(
    at: Vec2? = null,
    size: Vec2? = null,
    rotation: Double = 0.0,
    scaleX: Float = 0f,
    scaleY: Float = 0f,
    skewX: Double = 0.0,
    skewY: Double = 0.0,
    color: PolyColor? = null,
    animation: Animation? = null,
) {
    if (at != null) Move(this, at, true, animation).add()
    if (size != null) Resize(this, size, true, animation).add()
    if (rotation != 0.0) Rotate(this, rotation, true, animation).add()
    if (skewX != 0.0 || skewY != 0.0) Skew(this, skewX, skewY, true, animation).add()
    if (scaleX != 0f || scaleY != 0f) Scale(this, scaleX, scaleY, true, animation).add()
    if (color != null) Recolor(this, color, animation).add()
}

/**
 * Bulk add method for all the builtin drawable operations in PolyUI.
 *
 * The default values of this method are all the current ones of this drawable, meaning that nothing is done.
 *
 * This is the "to" version of this method, meaning that each value is set to the provided one.
 * See the [animateBy] method for the "by" equivalent, which adds each value to the current one of this drawable.
 */
fun Drawable.animateTo(
    at: Vec2 = this.at,
    size: Vec2 = this.size,
    rotation: Double = this.rotation,
    skewX: Double = this.skewX,
    skewY: Double = this.skewY,
    scaleX: Float = this.scaleX,
    scaleY: Float = this.scaleY,
    color: PolyColor = this.color,
    animation: Animation? = null,
) {
    if (at != this.at) Move(this, at, false, animation).add()
    if (size != this.size) Resize(this, size, false, animation).add()
    if (rotation != this.rotation) Rotate(this, rotation, false, animation).add()
    if (skewX != this.skewX || skewY != this.skewY) Skew(this, skewX, skewY, false, animation).add()
    if (scaleX != this.scaleX || scaleY != this.scaleY) Scale(this, scaleX, scaleY, false, animation).add()
    if (color != this.color) Recolor(this, color, animation).add()
}
