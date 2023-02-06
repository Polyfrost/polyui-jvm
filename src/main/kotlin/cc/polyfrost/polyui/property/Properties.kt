/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - simple, easy to use and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.event.ComponentEvent
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.property.impl.ImageBlockProperties
import cc.polyfrost.polyui.property.impl.TextProperties
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit

abstract class Properties : Cloneable {
    abstract val color: Color

    /** use this to set a size for the target component.
     *
     * This will **not** override the size of the component if it is already set.
     * Else, if this is not null and the component's size is null, the component's size will be set to this.
     */
    open val size: Size<Unit>? = null
    abstract val padding: Float
    val eventHandlers = mutableMapOf<ComponentEvent, Component.() -> kotlin.Unit>()

    /**
     * Add a universal event handler to this component's property.
     *
     * This means that every component using this property will have this event handler.
     */
    fun addEventHandler(type: ComponentEvent, function: Component.() -> kotlin.Unit) {
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