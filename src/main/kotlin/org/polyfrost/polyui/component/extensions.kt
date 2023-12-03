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

import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.operations.*
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds

/**
 * Make this component draggable.
 * @param consumesEvent weather beginning/ending dragging should cancel the corresponding mouse event
 */
fun <S : Drawable> S.draggable(withX: Boolean = true, withY: Boolean = true, consumesEvent: Boolean = false, onDrag: (S.() -> Unit)? = null): S {
    if (!withX && !withY) return this
    var pressed = false
    var px = 0f
    var py = 0f
    addEventHandler(MousePressed(0)) {
        pressed = true
        px = it.x - x
        py = it.y - y
        prioritize()
        consumesEvent
    }
    addOperation(object : DrawableOp(this) {
        private var prevX = 0f
        private var prevY = 0f
        override fun apply() {
            if (pressed) {
                if (self.inputState != INPUT_PRESSED) {
                    pressed = false
                    return
                }
                val mx = self.polyUI.eventManager.mouseX
                val my = self.polyUI.eventManager.mouseY
                var i = false
                if (withX && prevX != mx) {
                    self.x = mx - px
                    i = true
                }
                if (withY && prevY != my) {
                    self.y = my - py
                    i = true
                }
                if (i) onDrag?.invoke((this@draggable))
                prevX = mx
                prevY = my
            }
        }

        override fun unapply() = false
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

/**
 * Set the color palette of this drawable during initialization, using the PolyUI colors instance.
 */
fun <S : Drawable> S.setPalette(palette: Colors.() -> Colors.Palette): S {
    addEventHandler(Initialization) {
        this.palette = palette(polyUI.colors)
    }
    return this
}

fun <S : Drawable> S.withStates(showClicker: Boolean = true, animation: (() -> Animation)? = { Animations.EaseInOutQuad.create(0.08.seconds) }): S {
    addEventHandler(MouseEntered) {
        Recolor(this, this.palette!!.hovered, animation?.invoke()).add()
        if (showClicker) polyUI.cursor = Cursor.Clicker
        false
    }
    addEventHandler(MouseExited) {
        Recolor(this, this.palette!!.normal, animation?.invoke()).add()
        polyUI.cursor = Cursor.Pointer
        false
    }
    addEventHandler(MousePressed(0)) {
        Recolor(this, this.palette!!.pressed, animation?.invoke()).add()
        false
    }
    addEventHandler(MouseReleased(0)) {
        Recolor(this, this.palette!!.hovered, animation?.invoke()).add()
        false
    }
    return this
}

@EventDSLMarker
fun <S : Drawable> S.events(dsl: EventDSL<S>.() -> Unit): S {
    EventDSL(this).apply(dsl)
    return this
}

/**
 * Bulk add method for all the builtin drawable operations in PolyUI.
 *
 * The default values of this method are all zeroes, meaning that nothing is done.
 *
 * This is the "by" version of this method, meaning that each value is added to the current one of this drawable.
 * See the [animateTo] method for the "to" equivalent, which sets each value to the provided one.
 */
fun <S : Drawable> S.animateBy(
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
fun <S : Drawable> S.animateTo(
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
