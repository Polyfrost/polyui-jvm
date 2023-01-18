package cc.polyfrost.polyui.properties

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import java.util.*

abstract class Properties {
    abstract val color: Color
    abstract val margins: Float
    val eventHandlers: EnumMap<ComponentEvent.Type, (Component) -> Unit> = EnumMap(ComponentEvent.Type::class.java)
    abstract fun accept(event: ComponentEvent): ComponentEvent

    companion object {
        /** convenience method for [PropertiesRegistry.forNew] */
        fun create(component: Component): Properties {
            return PropertiesRegistry.forNew(component)
        }
    }
}