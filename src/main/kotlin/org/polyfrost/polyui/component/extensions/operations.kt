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

package org.polyfrost.polyui.component.extensions

import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.operations.*
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.fastEach


/**
 * Add the given [ComponentOp] to this component.
 * @since 1.6.2
 */
context(S)
fun <S : Component> ComponentOp.add() {
    this@S.addOperation(this)
}

/**
 * Bulk add method for all the builtin drawable operations in PolyUI.
 *
 * The default values of this method are all zeroes, meaning that nothing is done.
 *
 * This is the "by" version of this method, meaning that each value is added to the current one of this drawable.
 * See the [animateTo] method for the "to" equivalent, which sets each value to the provided one.
 */
@JvmName("animateBy")
fun Drawable.animateBy(
    at: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2.ZERO,
    rotation: Double = 0.0,
    scaleX: Float = 0f,
    scaleY: Float = 0f,
    skewX: Double = 0.0,
    skewY: Double = 0.0,
    color: PolyColor? = null,
    animation: Animation? = null,
) {
    if (!at.isZero) Move(this, at, true, animation).add()
    if (!at.isZero) Resize(this, size, true, animation = animation).add()
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
@JvmName("animateTo")
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
    if (size != this.size) Resize(this, size, false, animation = animation).add()
    if (rotation != this.rotation) Rotate(this, rotation, false, animation).add()
    if (skewX != this.skewX || skewY != this.skewY) Skew(this, skewX, skewY, false, animation).add()
    if (scaleX != this.scaleX || scaleY != this.scaleY) Scale(this, scaleX, scaleY, false, animation).add()
    if (color != this.color) Recolor(this, color, animation).add()
}

/**
 * Return the position that this component is currently moving to, or its current position if it is not moving.
 *
 * @see Move
 * @see getTargetSize
 * @since 1.6.02
 */
fun <S : Component> S.getTargetPosition(): Vec2 {
    val operations = this.operations ?: return this.at
    operations.fastEach {
        if (it is Move<*>) return it.target
    }
    return this.at
}

/**
 * Return the size that this component is currently resizing to, or its current size if it is not resizing.
 *
 * @see Resize
 * @see getTargetPosition
 * @since 1.6.02
 */
fun <S : Component> S.getTargetSize(): Vec2 {
    val operations = this.operations ?: return this.size
    operations.fastEach {
        if (it is Resize<*>) return it.target
    }
    return this.size
}
