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

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.anyAre
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
    rawResize: Boolean = false,
    resizesChildren: Boolean = true,
    vararg drawables: Drawable
) : Layout(at, size, onAdded, onRemoved, rawResize, resizesChildren, acceptInput, *drawables) {
    private var dyn = false
    private var warned = false

    init {
        drawables.forEach {
            require(it.atType != Unit.Type.Flex && it.atType != Unit.Type.Grid) { "Unit type mismatch: Drawable $it does not have a valid unit type for layout: ${this.simpleName} (using ${it.atType})" }
        }
    }

    override fun calculateBounds() {
        if (this.size == null) {
            size = calculateSize()
        }
        if (layout != null) doDynamicSize()
        components.fastEach {
            it.layout = this
            it.calculateBounds()
        }
        children.fastEach {
            it.layout = this
            it.calculateBounds()
        }
        if (dyn) {
            size = calculateSize()
        }
    }

    override fun calculateSize(): Vec2<Unit> {
        var width = children.maxOfOrNull { if (!dyn && it.isDynamic) 0f else it.x + it.width } ?: 0f
        width = max(width, components.maxOfOrNull { if (!dyn && it.isDynamic) 0f else it.x + it.width } ?: 0f)
        var height = children.maxOfOrNull { if (!dyn && it.isDynamic) 0f else it.y + it.height } ?: 0f
        height = max(height, components.maxOfOrNull { if (!dyn && it.isDynamic) 0f else it.y + it.height } ?: 0f)
        dyn = !dyn && children.anyAre { it.isDynamic } || components.anyAre { it.isDynamic }
        if (!warned && dyn) {
            PolyUI.LOGGER.warn("${this.simpleName} has dynamically sized children, but it does not have a size itself. It will have its units inferred based on its concrete sized children.")
            warned = true
        }
        if (width == 0f) throw Exception("unable to infer width of ${this.simpleName}: no concrete sized children or components, please specify a size")
        if (height == 0f) throw Exception("unable to infer height of ${this.simpleName}: no concrete sized children or components, please specify a size")
        return width.px * height.px
    }
}
