package cc.polyfrost.polyui.layouts.impl

import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.units.*
import cc.polyfrost.polyui.units.Unit
import kotlin.Boolean
import kotlin.Exception
import kotlin.math.max

/**
 * The most basic layout.
 *
 * Just a container basically, that can infer it's size.
 * */
open class PixelLayout(
    at: Point<Unit>, sized: Size<Unit>? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    acceptInput: Boolean = true,
    vararg items: Drawable,
) : Layout(at, sized, onAdded, onRemoved, acceptInput, *items) {
    init {
        items.forEach {
            if (it.atUnitType() == Unit.Type.Flex) {
                // todo make special exceptions that can tell you more verbosely which component is at fault
                throw Exception("Unit type mismatch: Drawable $it does not have a valid unit type for layout: PixelLayout (using ${it.atUnitType()})")
            }
        }
    }

    override fun getSize(): Vec2<Unit> {
        var width = children.maxOfOrNull { it.x + it.width } ?: 0f
        width = max(width, components.maxOfOrNull { it.x + it.width } ?: 0f)
        var height = children.maxOfOrNull { it.y + it.height } ?: 0f
        height = max(height, components.maxOfOrNull { it.y + it.height } ?: 0f)
        if (width == 0f) throw Exception("unable to infer width of $layout: no sized children or components, please specify a size")
        if (height == 0f) throw Exception("unable to infer height of $layout: no sized children or components, please specify a size")
        return width.px * height.px
    }
}