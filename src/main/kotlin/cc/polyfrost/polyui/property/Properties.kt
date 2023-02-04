package cc.polyfrost.polyui.property

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.event.ComponentEvent
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.property.impl.ImageBlockProperties
import cc.polyfrost.polyui.property.impl.TextProperties

abstract class Properties : Cloneable {
    abstract val color: Color
    abstract val padding: Float
    val eventHandlers = mutableMapOf<ComponentEvent, Component.() -> Unit>()

    /**
     * Add a universal event handler to this component's property.
     *
     * This means that every component using this property will have this event handler.
     */
    fun addEventHandler(type: ComponentEvent, function: Component.() -> Unit) {
        eventHandlers[type] = function
    }

    /** add a universal event handler to this component's property.
     *
     * This means that every component using this property will have this event handler.
     */
    fun addEventHandlers(vararg handlers: ComponentEvent.Handler) {
        handlers.forEach { addEventHandler(it.event, it.handler) }
    }

    companion object {
        private val properties: MutableMap<String, Properties> = mutableMapOf(
            "cc.polyfrost.polyui.component.impl.Block" to BlockProperties(),
            "cc.polyfrost.polyui.component.impl.ImageBlock" to ImageBlockProperties(),
            "cc.polyfrost.polyui.component.impl.Text" to TextProperties()
        )

        inline fun <T : Properties, reified C : Component> get(): T =
            get(C::class.java.name)

        fun <T : Properties> get(component: Component): T =
            get(component::class.java.name)

        @Suppress("UNCHECKED_CAST")
        fun <T : Properties> get(name: String): T = properties[name] as? T
            ?: throw Exception("Properties for component $name not found")

        /**
         * Add the given property to the property' registry.
         *
         * This will **OVERWRITE** any existing property for the given component.
         * @param properties the property to add
         */
        fun addPropertyType(forComponent: Component, properties: Properties) {
            this.properties[forComponent::class.java.simpleName] = properties
        }

        /**
         * Add the given property to the property' registry.
         *
         * This will **OVERWRITE** any existing property for the given component.
         *
         * @param name the simple class name of the component (e.g. cc.polyfrost.polyui.component.impl.Block)
         * @param properties the property to add
         */
        fun addPropertyType(name: String, properties: Properties) {
            this.properties[name] = properties
        }
    }
}