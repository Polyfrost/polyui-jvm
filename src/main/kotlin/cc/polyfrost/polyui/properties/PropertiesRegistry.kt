package cc.polyfrost.polyui.properties

import cc.polyfrost.polyui.components.Component

class PropertiesRegistry {
    companion object {
        private val properties = mutableMapOf<String, Properties>()
        fun register(component: Component, properties: Properties) {
            this.properties[component.javaClass.simpleName] = properties
        }

        fun forNew(component: Component): Properties {
            return properties[component.javaClass.simpleName]
                ?: throw NullPointerException("No properties registered for ${component.javaClass.simpleName}")
        }

        fun forNew(component: String): Properties {
            return properties[component]
                ?: throw NullPointerException("No default properties registered for $component")
        }
    }
}