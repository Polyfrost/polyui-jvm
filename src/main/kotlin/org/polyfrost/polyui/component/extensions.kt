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

@file:Suppress("UNUSED")

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
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.fastEach
import kotlin.math.abs

private var dragging = false
const val MIN_DRAG = 3f

/**
 * Make this component draggable.
 * @param free if this is true, the component will be able to be dragged outside its parent.
 *
 */
fun <S : Drawable> S.draggable(withX: Boolean = true, withY: Boolean = true, free: Boolean = false, onStart: (S.() -> Unit)? = null, onDrag: (S.() -> Unit)? = null, onDrop: (S.() -> Unit)? = null): S {
    var pressed = false
    var px = 0f
    var py = 0f
    on(Event.Mouse.Pressed) {
        if (dragging) return@on false
        needsRedraw = true
        dragging = true
        pressed = true
        px = it.x - x
        py = it.y - y
        false
    }
    on(Event.Mouse.Dragged) {
        if (dragging) {
            needsRedraw = true
        }
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
                        if (free && self._parent !== self.polyUI.master) {
                            self.polyUI.master.children!!.remove(self)
                            self.parent.children!!.add(self)
                        }
                        onDrop?.invoke(this@draggable)
                        started = false
                    }
                    pressed = false
                    return
                }
                if (!started) {
                    // asm: only start dragging if it has moved at least MIN_DRAG
                    if (abs(px + x - self.polyUI.inputManager.mouseX) > MIN_DRAG || abs(py + y - self.polyUI.inputManager.mouseY) > MIN_DRAG) {
                        started = true
                        onStart?.invoke(this@draggable)
                        if (free && self._parent !== self.polyUI.master) {
                            if (self.polyUI.inputManager.focused !== self) self.polyUI.unfocus()
                            self.parent.children!!.remove(self)
                            self.polyUI.master.children!!.add(self)
                        }
                    } else return
                }
                val mx = self.polyUI.inputManager.mouseX
                val my = self.polyUI.inputManager.mouseY
                var i = false
                if (prevX != mx) {
                    if (withX) self.x = mx - px
                    i = true
                }
                if (prevY != my) {
                    if (withY) self.y = my - py
                    i = true
                }
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
    val obj = Block(Text(text)).hide()
    obj.alpha = 0f
    onInit {
        obj.setup(polyUI)
        (_parent ?: polyUI.master).addChild(obj, recalculate = false)
        obj.renders = false
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
                obj.x = (mx - obj.width / 2f).coerceIn(0f, self.polyUI.size.x - obj.width)
                obj.y = (self.polyUI.mouseY - obj.height - 4f).coerceIn(0f, self.polyUI.size.y)
                obj.renders = true
                open = true
                Fade(obj, 1f, false, Animations.EaseInOutQuad.create(0.1.seconds)).add()
            }
        }

        override fun unapply(): Boolean {
            super.unapply()
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

fun <S : Drawable> S.padded(xPad: Float, yPad: Float): S {
    this.padding = Vec2(xPad, yPad)
    return this
}

fun <S : Drawable> S.padded(padding: Vec2): S {
    this.padding = padding
    return this
}

/**
 * Add a listener for changes to the given String-type property.
 *
 * In PolyUI, this is for [Text] and [TextInput][org.polyfrost.polyui.component.impl.TextInput] only.
 * @since 1.0.6
 */
@JvmName("onChangeString")
fun <S : Text> S.onChange(func: S.(String) -> Boolean): S {
    on(Event.Change.Text) {
        val res = func.invoke(this, it.text)
        it.cancelled = res
        res
    }
    return this
}

/**
 * Add a listener for changes to the given Boolean-type property.
 *
 * In PolyUI, this is for [Checkbox][org.polyfrost.polyui.component.impl.Checkbox] and [Switch][org.polyfrost.polyui.component.impl.Switch] only.
 * @since 1.0.6
 */
@JvmName("onChangeState")
fun <S : Drawable> S.onChange(func: S.(Boolean) -> Boolean): S {
    on(Event.Change.State) {
        val res = func.invoke(this, it.state)
        it.cancelled = res
        res
    }
    return this
}

/**
 * Add a listener for changes to the given Int-type property.
 *
 * In PolyUI, this is for [Radiobutton][org.polyfrost.polyui.component.impl.Radiobutton], and [Dropdown][org.polyfrost.polyui.component.impl.Dropdown] only.
 * @since 1.0.6
 */
@JvmName("onChangeIndex")
fun <S : Drawable> S.onChange(func: S.(Int) -> Boolean): S {
    on(Event.Change.Number) {
        val res = func.invoke(this, it.amount.toInt())
        it.cancelled = res
        res
    }
    return this
}

/**
 * Add a listener for changes to the given Float-type property.
 *
 * In PolyUI, this is for [Slider][org.polyfrost.polyui.component.impl.Slider] only.
 * @since 1.0.6
 */
@JvmName("onChangeNumber")
fun <S : Drawable> S.onChange(func: S.(Float) -> Boolean): S {
    on(Event.Change.Number) {
        val res = func.invoke(this, it.amount.toFloat())
        it.cancelled = res
        res
    }
    return this
}

/**
 * Set the color palette of this drawable during initialization, using the PolyUI colors instance.
 */
fun <S : Drawable> S.setPalette(palette: Colors.() -> Colors.Palette): S {
    onInit {
        this.palette = polyUI.colors.palette()
    }
    return this
}

/**
 * Set the font of this text component during initialization, using the PolyUI fonts instance.
 * @since 1.1.3
 */
fun <S : Text> S.setFont(font: FontFamily.() -> Font): S {
    onInit {
        this.font = polyUI.fonts.font()
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
    on(Event.Change.Text) {
        if (!func(this, it.text)) {
            it.cancel()
        }
    }
    return this
}

fun <S : Drawable> S.setDestructivePalette() = setPalette {
    Colors.Palette(text.primary.normal, state.danger.hovered, state.danger.pressed, text.primary.disabled)
}

private val defEnter: Drawable.(Event.Mouse.Entered) -> Boolean = {
    Recolor(this, this.palette.hovered, Animations.EaseInOutQuad.create(0.08.seconds)).add()
    polyUI.cursor = Cursor.Clicker
    false
}

private val defExit: Drawable.(Event.Mouse.Exited) -> Boolean = {
    Recolor(this, this.palette.normal, Animations.EaseInOutQuad.create(0.08.seconds)).add()
    polyUI.cursor = Cursor.Pointer
    false
}

private val defPressed: Drawable.(Event.Mouse.Pressed) -> Boolean = {
    Recolor(this, this.palette.pressed, Animations.EaseInOutQuad.create(0.08.seconds)).add()
    false
}

private val defReleased: Drawable.(Event.Mouse.Released) -> Boolean = {
    Recolor(this, this.palette.hovered, Animations.EaseInOutQuad.create(0.08.seconds)).add()
    false
}

fun <S : Drawable> S.withStates(): S {
    on(Event.Mouse.Entered, defEnter)
    on(Event.Mouse.Exited, defExit)
    on(Event.Mouse.Pressed, defPressed)
    on(Event.Mouse.Released, defReleased)
    return this
}

fun <S : Drawable> S.withStates(
    consume: Boolean = false, showClicker: Boolean = true,
    animation: (() -> Animation)? = {
        Animations.EaseInOutQuad.create(0.08.seconds)
    },
): S {
    on(Event.Mouse.Entered) {
        Recolor(this, this.palette.hovered, animation?.invoke()).add()
        if (showClicker) polyUI.cursor = Cursor.Clicker
        consume
    }
    on(Event.Mouse.Exited) {
        Recolor(this, this.palette.normal, animation?.invoke()).add()
        polyUI.cursor = Cursor.Pointer
        consume
    }
    on(Event.Mouse.Pressed) {
        Recolor(this, this.palette.pressed, animation?.invoke()).add()
        consume
    }
    on(Event.Mouse.Released) {
        Recolor(this, this.palette.hovered, animation?.invoke()).add()
        consume
    }
    return this
}

fun <S : Drawable> S.withCursor(cursor: Cursor = Cursor.Clicker): S {
    on(Event.Mouse.Entered) {
        polyUI.cursor = cursor
    }
    on(Event.Mouse.Exited) {
        polyUI.cursor = Cursor.Pointer
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
    palette = when (state) {
        DANGER -> polyUI.colors.state.danger
        WARNING -> polyUI.colors.state.warning
        SUCCESS -> polyUI.colors.state.success
        else -> throw IllegalArgumentException("Invalid state: $state")
    }
    return this
}

/**
 * Prioritize this drawable, meaning it will, in relation to its siblings:
 * - be drawn last (so visually on top)
 * - receive events first
 *
 * **this method is experimental as it may interfere with statically-typed references to children.**
 * @see relegate
 * @since 1.0.0
 */
@ApiStatus.Experimental
fun <S : Drawable> S.prioritize(): S {
    val children = _parent?.children ?: return this
    if (children.last() === this) return this
    children.remove(this)
    children.add(this)
    return this
}

/**
 * Relegate this drawable, meaning it will, in relation to its siblings:
 * - be drawn first (so visually on the bottom)
 * - receive events last
 *
 * **this method is experimental as it may interfere with statically-typed references to children.**
 * @see prioritize
 * @since 1.1.71
 */
@ApiStatus.Experimental
fun <S : Drawable> S.relegate(): S {
    val children = _parent?.children ?: return this
    if (children.first() === this) return this
    children.remove(this)
    children.add(0, this)
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
 * @return `true` if this drawable has a child that intersects the provided rectangle.
 * @since 1.0.4
 */
fun Drawable.hasChildIn(x: Float, y: Float, width: Float, height: Float): Boolean {
    val children = this.children ?: return false
    children.fastEach {
        if (!it.renders) return@fastEach
        if (it.intersects(x, y, width, height)) return true
    }
    return false
}

fun <S : Drawable> S.onInit(function: S.(Event.Lifetime.Init) -> Unit): S {
    on(Event.Lifetime.Init, function)
    return this
}

fun <S : Drawable> S.afterInit(function: S.(Event.Lifetime.PostInit) -> Unit): S {
    on(Event.Lifetime.PostInit, function)
    return this
}

/**
 * Add an event handler to this drawable's parent, and (optionally) it's parent's parent ([depth]).
 *
 * This is used to run functions that may move or resize this drawable,
 * as if they were ran under this [Event.Lifetime.PostInit] they would be overwritten by the positioning logic.
 */
fun <S : Drawable> S.afterParentInit(depth: Int = 1, handler: S.() -> Unit): S {
    this.on(Event.Lifetime.PostInit) { _ ->
        var it: Drawable = this
        for (i in 0 until depth) {
            it._parent?.let { parent -> it = parent } ?: break
        }
        if (it.initialized) {
            handler(this@afterParentInit)
        } else {
            it.on(Event.Lifetime.PostInit) {
                handler(this@afterParentInit)
            }
        }
        false
    }
    return this
}

@EventDSL.Marker
inline fun <S : Drawable> S.events(dsl: EventDSL<S>.() -> Unit): S {
    EventDSL(this).apply(dsl)
    return this
}

@OverloadResolutionByLambdaReturnType
@JvmName("onClickZ")
fun <S : Drawable> S.onClick(func: S.(Event.Mouse.Clicked) -> Boolean): S {
    on(Event.Mouse.Clicked, func)
    return this
}

@OverloadResolutionByLambdaReturnType
fun <S : Drawable> S.onClick(func: S.(Event.Mouse.Clicked) -> Unit): S {
    on(Event.Mouse.Clicked, func)
    return this
}

/**
 * Locate a drawable by its name.
 *
 * This method is recursive, meaning it will search through all children of this drawable.
 * @param id the name of the drawable to locate.
 * @return the drawable with the given name, or `null` if it was not found.
 * @since 1.1.72
 */
fun <S : Drawable> Drawable.locate(id: String): S? {
    @Suppress("UNCHECKED_CAST")
    if (this.simpleName == id) return this as S
    children?.fastEach {
        val res = it.locate<S>(id)
        if (res != null) return res
    }
    return null
}

/**
 * Returns `true` if this drawable is a child of the specified [drawable].
 * @see isRelatedTo
 * @since 1.4.2
 */
fun Drawable.isChildOf(drawable: Drawable?): Boolean {
    if (drawable == null) return false
    var p: Drawable? = this._parent
    while (p != null) {
        if (p === drawable) return true
        p = p._parent
    }
    return false
}

/**
 * Returns `true` if this drawable is a child of the specified [drawable], or if the [drawable] is a child of this.
 * @see isChildOf
 * @since 1.4.2
 */
fun Drawable.isRelatedTo(drawable: Drawable?) = drawable != null && drawable.isChildOf(this) || this.isChildOf(drawable)

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
    tx: Float = this.x,
    ty: Float = this.y,
    size: Vec2 = this.size,
    rotation: Double = this.rotation,
    skewX: Double = this.skewX,
    skewY: Double = this.skewY,
    scaleX: Float = this.scaleX,
    scaleY: Float = this.scaleY,
    color: PolyColor = this.color,
    animation: Animation? = null,
) {
    if (tx != this.x || ty != this.y) Move(this, tx, ty, false, animation).add()
    if (size != this.size) Resize(this, size, false, animation).add()
    if (rotation != this.rotation) Rotate(this, rotation, false, animation).add()
    if (skewX != this.skewX || skewY != this.skewY) Skew(this, skewX, skewY, false, animation).add()
    if (scaleX != this.scaleX || scaleY != this.scaleY) Scale(this, scaleX, scaleY, false, animation).add()
    if (color != this.color) Recolor(this, color, animation).add()
}
