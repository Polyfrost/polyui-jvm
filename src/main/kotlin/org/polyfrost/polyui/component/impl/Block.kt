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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.areElementsEqual

open class Block(
    vararg children: Component?,
    at: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2.ZERO,
    alignment: Align = AlignDefault,
    visibleSize: Vec2 = Vec2.ZERO,
    focusable: Boolean = false,
    color: PolyColor? = null,
    radii: FloatArray? = floatArrayOf(8f),
) : Drawable(children = children, at, alignment, size, visibleSize, focusable = focusable) {
    var borderColor: PolyColor? = null
    var borderWidth: Float = 2f
    var radii = radii
        set(value) {
            field = when {
                value == null -> null
                value.isEmpty() -> null
                value.areElementsEqual() && value[0] == 0f -> null
                else -> value
            }
        }

    init {
        if (color != null) this.color = color
    }

    override fun render() {
        val radii = this.radii
        when {
            radii == null -> {
                renderer.rect(x, y, width, height, color)
                borderColor?.let { renderer.hollowRect(x, y, width, height, it, borderWidth) }
            }

            radii.size < 4 -> {
                renderer.rect(x, y, width, height, color, radii[0])
                borderColor?.let { renderer.hollowRect(x, y, width, height, it, borderWidth, radii[0]) }
            }

            else -> {
                renderer.rect(x, y, width, height, color, radii)
                borderColor?.let { renderer.hollowRect(x, y, width, height, it, borderWidth, radii) }
            }
        }
    }

    override fun rescale0(scaleX: Float, scaleY: Float, withChildren: Boolean) {
        super.rescale0(scaleX, scaleY, withChildren)
        val radii = this.radii
        when {
            radii == null -> {}
            radii.areElementsEqual() -> {
                for (i in radii.indices) {
                    radii[i] *= scaleX
                }
            }
        }
    }
}
