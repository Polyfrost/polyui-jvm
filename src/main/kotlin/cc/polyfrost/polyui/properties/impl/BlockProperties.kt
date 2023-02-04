package cc.polyfrost.polyui.properties.impl

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.units.seconds

open class BlockProperties(override val color: Color = Color.BLACK) : Properties() {
    open val hoverColor = Color(12, 48, 255)
    override val padding: Float = 0F
    open val cornerRadius: Float = 0F

    init {
        addEventHandlers(
            ComponentEvent.MouseEntered to {
                recolor(hoverColor, Animations.EaseInOutQuad, 0.2.seconds)
            },
            ComponentEvent.MouseExited to {
                recolor(properties.color, Animations.EaseInOutQuad, 0.4.seconds)
            },
        )
    }
}