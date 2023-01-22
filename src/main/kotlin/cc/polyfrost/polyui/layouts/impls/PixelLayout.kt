package cc.polyfrost.polyui.layouts.impls

import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit

class PixelLayout(
    at: Point<Unit>, sized: Size<Unit>? = null, vararg items: Drawable
) : Layout(at, sized, *items) {
    override val unitType: Unit.Type = Unit.Type.Pixel

}