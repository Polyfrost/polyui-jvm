package cc.polyfrost.polyui.layouts.impls

import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.units.*
import cc.polyfrost.polyui.units.Unit

class PixelLayout(at: Point<Unit.Pixel>, vararg items: Drawable, sized: Size<Unit.Pixel>? = null): Layout(*items) {
    override lateinit var box: Box<Unit>
    override val boundingBox: Box<Unit> = box

    init {
        if (sized != null) {
            this.box = Box(at, sized) as Box<Unit>
        } else {
            this.box = Box(at, calculateDimensions()) as Box<Unit>
        }
    }

    fun calculateDimensions(): Size<Unit.Pixel> {
        TODO()
    }
}