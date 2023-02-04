package cc.polyfrost.polyui.properties.impl

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.renderer.data.Font

open class TextProperties : Properties() {
    override val color: Color = Color.WHITE
    override val padding: Float = 0F
    val font = Font("/Inter-Regular.ttf")
}