package cc.polyfrost.polyui.properties

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.impls.BlockProperties
import java.util.*

abstract class Properties : Cloneable {
    abstract val color: Color
    abstract val margins: Float
    val eventHandlers: EnumMap<ComponentEvent.Type, Component.() -> Unit> = EnumMap(ComponentEvent.Type::class.java)
    abstract fun accept(event: ComponentEvent): ComponentEvent

    companion object {
        private val properties: MutableMap<String, Properties> = mutableMapOf(
            "cc.polyfrost.polyui.components.impls.Block" to BlockProperties()
        )

        fun <T : Properties> new(component: Component): T {
            return new(component::class.java.simpleName)
        }

        fun <T : Properties> new(name: String): T {
            return (properties[name] ?: throw Exception("Properties for component $name not found")).clone() as T
        }

        fun addPropertyType(forComponent: Component, properties: Properties) {
            this.properties[forComponent::class.java.simpleName] = properties
        }

        fun addPropertyType(name: String, properties: Properties) {
            this.properties[name] = properties
        }
    }
}