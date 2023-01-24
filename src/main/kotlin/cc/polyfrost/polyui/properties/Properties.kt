package cc.polyfrost.polyui.properties

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.impls.BlockProperties
import cc.polyfrost.polyui.properties.impls.ImageBlockProperties
import cc.polyfrost.polyui.properties.impls.TextProperties
import java.util.*

abstract class Properties : Cloneable {
    abstract val color: Color
    abstract val padding: Float
    val eventHandlers: EnumMap<ComponentEvent.Type, Component.() -> Unit> = EnumMap(ComponentEvent.Type::class.java)
    abstract fun accept(event: ComponentEvent)

    companion object {
        private val properties: MutableMap<String, Properties> = mutableMapOf(
            "cc.polyfrost.polyui.components.impls.Block" to BlockProperties(),
            "cc.polyfrost.polyui.components.impls.ImageBlock" to ImageBlockProperties(),
            "cc.polyfrost.polyui.components.impls.Text" to TextProperties()
        )

        fun <T : Properties> get(component: Component): T {
            return get(component::class.java.simpleName)
        }

        fun <T : Properties> get(name: String): T {
            return (properties[name] ?: throw Exception("Properties for component $name not found")) as T
        }

        /** add the given properties to the properties' registry.
         *
         * This will **OVERWRITE** any existing properties for the given component.
         * @param properties the properties to add
         */
        fun addPropertyType(forComponent: Component, properties: Properties) {
            this.properties[forComponent::class.java.simpleName] = properties
        }

        /** add the given properties to the properties' registry.
         *
         * This will **OVERWRITE** any existing properties for the given component.
         *
         * @param name the simple class name of the component (e.g. cc.polyfrost.polyui.components.impls.Block)
         * @param properties the properties to add
         */
        fun addPropertyType(name: String, properties: Properties) {
            this.properties[name] = properties
        }
    }
}