package cc.polyfrost.polyui.layouts.impls

import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.units.Box
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.utils.px

class PixelLayout(
    at: Point<Unit.Pixel>, sized: Size<Unit.Pixel>? = null, vararg items: Drawable
) : Layout(*items) {
    override val layout = this
    override lateinit var box: Box<Unit>

    init {
        if (sized != null) {
            this.box = Box(at, sized) as Box<Unit>
        } else {
            this.box = Box(at, calculateDimensions()) as Box<Unit>
        }
    }

    override val boundingBox: Box<Unit> = box

    fun calculateDimensions(): Size<Unit.Pixel> {
        return Size(20.px(), 20.px())
    }
}