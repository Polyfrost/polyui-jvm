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
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.areValuesEqual
import org.polyfrost.polyui.utils.cl1
import org.polyfrost.polyui.utils.radii

open class Block
@JvmOverloads
constructor(
    vararg children: Drawable?,
    at: Vec2? = null,
    size: Vec2? = null,
    alignment: Align = AlignDefault,
    visibleSize: Vec2? = null,
    focusable: Boolean = false,
    color: PolyColor? = null,
    var radii: FloatArray = 8f.radii(),
) : Drawable(children = children, at, alignment, size, visibleSize, focusable = focusable) {
    var boarderColor: PolyColor? = null
    var boarderWidth: Float = 2f

    init {
        require(radii.size == 4) { "Corner radius array must be 4 values" }
        if (color != null) this.color = color
    }

    override fun render() {
        renderer.rect(x, y, width, height, color, radii)
        val outlineColor = boarderColor ?: return
        renderer.hollowRect(x, y, width, height, outlineColor, boarderWidth, radii)
    }

    override fun rescale(scaleX: Float, scaleY: Float, position: Boolean) {
        super.rescale(scaleX, scaleY, position)
        if (radii.areValuesEqual()) {
            val scale = cl1(scaleX, scaleY)
            for (i in 0..3) {
                radii[i] *= scale
            }
        }
    }
}
