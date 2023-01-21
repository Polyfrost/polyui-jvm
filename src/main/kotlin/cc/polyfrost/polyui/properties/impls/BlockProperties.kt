package cc.polyfrost.polyui.properties.impls

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties

class BlockProperties : Properties() {
    override val color: Color = Color.WHITE
    override val margins: Float = 0F

    override fun accept(event: ComponentEvent): ComponentEvent {
        TODO("Not yet implemented")
    }
}