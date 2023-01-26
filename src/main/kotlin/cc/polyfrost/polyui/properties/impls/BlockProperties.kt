package cc.polyfrost.polyui.properties.impls

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties

open class BlockProperties : Properties() {
    override val color: Color = Color.BLACK
    override val padding: Float = 0F
    val cornerRadius: Float = 0F

    override fun accept(event: ComponentEvent) {
    }
}