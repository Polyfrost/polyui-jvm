package cc.polyfrost.polyui.properties

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.impl.BlockProperties
import cc.polyfrost.polyui.properties.impl.ImageBlockProperties
import cc.polyfrost.polyui.properties.impl.TextProperties

abstract class Properties : Cloneable {
    abstract val color: Color
    abstract val padding: Float
    val eventHandlers = mutableMapOf<ComponentEvent, Component.() -> Unit>()

    /**
     * Add a universal event handler to this component's properties.
     *
     * This means that every component using this property will have this event handler.
     */
    fun addEventHandler(type: ComponentEvent, function: Component.() -> Unit) {
        eventHandlers[type] = function
    }

    /** add a universal event handler to this component's properties.
     *
     * This means that every component using this property will have this event handler.
     */
    fun addEventHandlers(vararg handlers: ComponentEvent.Handler) {
        handlers.forEach { addEventHandler(it.event, it.handler) }
    }

    companion object {
        private val properties: MutableMap<String, Properties> = mutableMapOf(
            "cc.polyfrost.polyui.components.impl.Block" to BlockProperties(),
            "cc.polyfrost.polyui.components.impl.ImageBlock" to ImageBlockProperties(),
            "cc.polyfrost.polyui.components.impl.Text" to TextProperties()
        )

        inline fun <T : Properties, reified C : Component> get(): T =
            get(C::class.java.name)

        fun <T : Properties> get(component: Component): T =
            get(component::class.java.name)

        @Suppress("UNCHECKED_CAST")
        fun <T : Properties> get(name: String): T = properties[name] as? T
            ?: throw Exception("Properties for component $name not found")

        /**
         * Add the given properties to the properties' registry.
         *
         * This will **OVERWRITE** any existing properties for the given component.
         * @param properties the properties to add
         */
        fun addPropertyType(forComponent: Component, properties: Properties) {
            this.properties[forComponent::class.java.simpleName] = properties
        }

        /**
         * Add the given properties to the properties' registry.
         *
         * This will **OVERWRITE** any existing properties for the given component.
         *
         * @param name the simple class name of the component (e.g. cc.polyfrost.polyui.components.impl.Block)
         * @param properties the properties to add
         */
        fun addPropertyType(name: String, properties: Properties) {
            this.properties[name] = properties
        }
    }
}