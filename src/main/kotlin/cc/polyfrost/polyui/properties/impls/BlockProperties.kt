package cc.polyfrost.polyui.properties.impls

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.events.ComponentEvents
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.utils.seconds

open class BlockProperties : Properties() {
    override val color: Color = Color.BLACK
    val hoverColor = Color(12, 48, 255)
    override val padding: Float = 0F
    val cornerRadius: Float = 0F

    init {
        addEventHandlers(
            ComponentEvents.MouseEntered to { recolor(hoverColor, Animations.EaseInOutQuad, 0.2.seconds()) },
            ComponentEvents.MouseExited to { recolor(properties.color, Animations.EaseInOutQuad, 0.4.seconds()) },
        )
    }
}