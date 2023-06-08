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

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastEach
import kotlin.math.max

/**
 * The most basic layout.
 *
 * Just a container basically, that can infer it's size.
 *
 */
open class PixelLayout(
    at: Point<Unit>,
    size: Size<Unit>? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    acceptInput: Boolean = true,
    resizesChildren: Boolean = true,
    vararg items: Drawable
) : Layout(at, size, onAdded, onRemoved, acceptInput, resizesChildren, *items) {

    init {
        items.forEach {
            require(it.atType != Unit.Type.Flex && it.atType != Unit.Type.Grid) { "Unit type mismatch: Drawable $it does not have a valid unit type for layout: ${this.simpleName} (using ${it.atType})" }
        }
    }

    override fun calculateBounds() {
        if (layout != null) doDynamicSize()
        components.fastEach {
            it.layout = this
            it.calculateBounds()
        }
        children.fastEach {
            it.layout = this
            it.calculateBounds()
        }
        if (this.size == null) {
            size = calculateSize()
        }
    }

    override fun calculateSize(): Vec2<Unit> {
        var width = children.maxOfOrNull { it.x + it.width } ?: 0f
        width = max(width, components.maxOfOrNull { it.x + it.width } ?: 0f)
        var height = children.maxOfOrNull { it.y + it.height } ?: 0f
        height = max(height, components.maxOfOrNull { it.y + it.height } ?: 0f)
        if (width == 0f) throw Exception("unable to infer width of ${this.simpleName}: no sized children or components, please specify a size")
        if (height == 0f) throw Exception("unable to infer height of ${this.simpleName}: no sized children or components, please specify a size")
        return width.px * height.px
    }
}
