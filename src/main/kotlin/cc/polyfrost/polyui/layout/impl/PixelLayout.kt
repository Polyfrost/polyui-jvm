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
import cc.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.PropertyManager
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.anyAre
import kotlin.math.max

/**
 * The most basic layout.
 *
 * Just a container basically, that can infer it's size.
 *
 */
open class PixelLayout(
    at: Point<Unit> = origin,
    size: Size<Unit>? = null,
    onAdded: (Drawable.(Events.Added) -> kotlin.Unit)? = null,
    onRemoved: (Drawable.(Events.Removed) -> kotlin.Unit)? = null,
    propertyManager: PropertyManager? = null,
    acceptInput: Boolean = true,
    rawResize: Boolean = false,
    resizesChildren: Boolean = true,
    vararg drawables: Drawable
) : Layout(at, size, onAdded, onRemoved, propertyManager, rawResize, resizesChildren, acceptInput, *drawables) {
    /**
     * This variable represents weather this layout was created with 'dynamic inference', meaning it has no size specified, but has dynamically sized children.
     *
     * This of course is a recursive issue, as these children depend on the size of this layout, but this layout depends on the size of its children.
     *
     * To avoid this, we calculate the size of this layout based on the size of its concrete children (not dynamic), and then calculate the size of its children based on the size produced by this.
     * This means that percentages are not 100% accurate, but it is better than nothing as the situation is not really ideal and should not be used in the first place.
     */
    private var dynamicInference = false

    init {
        drawables.forEach {
            require(it.atType != Unit.Type.Flex && it.atType != Unit.Type.Grid) { "Unit type mismatch: Drawable $it does not have a valid unit type for layout: ${this.simpleName} (using ${it.atType})" }
        }
    }

    override fun calculateBounds() {
        if (this.size == null) size = calculateSize()
        super.calculateBounds()
    }

    override fun onInitComplete() {
        super.onInitComplete()
        if (dynamicInference) {
            size = calculateSize()
            calculateBounds()
        }
    }

    override fun calculateSize(): Vec2<Unit> {
        var width = children.maxOfOrNull { cx(it) } ?: 0f
        width = max(width, components.maxOfOrNull { if (!dynamicInference && it.isDynamic) 0f else it.x + it.width } ?: 0f)
        var height = children.maxOfOrNull { cy(it) } ?: 0f
        height = max(height, components.maxOfOrNull { if (!dynamicInference && it.isDynamic) 0f else it.y + it.height } ?: 0f)
        dynamicInference = !dynamicInference && children.anyAre { it.isDynamic } || components.anyAre { it.isDynamic }
        if (initStage != INIT_COMPLETE && dynamicInference) {
            PolyUI.LOGGER.warn("${this.simpleName} has dynamically sized children, but it does not have a size itself. This is not ideal and may lead to visual issues. Please set a size. See PixelLayout#dynamicInference")
        }
        if (width == 0f) throw Exception("unable to infer width of ${this.simpleName}: no concrete sized children or components, please specify a size")
        if (height == 0f) throw Exception("unable to infer height of ${this.simpleName}: no concrete sized children or components, please specify a size")
        return width.px * height.px
    }

    protected fun cx(it: Layout) = if (!dynamicInference && it.isDynamic) 0f else it.x + (it.visibleSize?.width ?: it.width)
    protected fun cy(it: Layout) = if (!dynamicInference && it.isDynamic) 0f else it.y + (it.visibleSize?.height ?: it.height)
}
