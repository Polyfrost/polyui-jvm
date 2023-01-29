package cc.polyfrost.polyui.layouts.impls

import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2
import cc.polyfrost.polyui.utils.px
import kotlin.math.max

class PixelLayout(
    at: Point<Unit>, sized: Size<Unit>? = null, vararg items: Drawable
) : Layout(at, sized, *items) {
    init {
        items.forEach {
            if (it.atUnitType() != Unit.Type.Pixel) {
                // todo make special exceptions that can tell you more verbosely which component is at fault
                throw Exception("Unit type mismatch: Drawable $it does not have a valid unit type for layout: PixelLayout (using ${it.atUnitType()})")
            }
        }
    }

    override fun getSize(): Vec2<Unit> {
        var width = children.maxOfOrNull { it.x() + it.width() } ?: 0f
        width = max(width, components.maxOfOrNull { it.x() + it.width() } ?: 0f)
        var height = children.maxOfOrNull { it.y() + it.height() } ?: 0f
        height = max(height, components.maxOfOrNull { it.y() + it.height() } ?: 0f)
        if (width == 0f) throw Exception("unable to infer width of $layout: no sized children or components, please specify a size")
        if (height == 0f) throw Exception("unable to infer height of $layout: no sized children or components, please specify a size")
        return Vec2(width.px(), height.px())
    }
}