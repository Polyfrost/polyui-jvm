/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.property.impl.ImageBlockProperties
import cc.polyfrost.polyui.property.impl.TextProperties
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit

/** # Properties
 *
 * Properties are PolyUI's take on styling through tokens, or shared states. They are used as default values for [components][Drawable], if component-specific values are not set. Every component will have an accompanying property.
 *
 * They contain many values that style a component, such as [color], [padding], [size], and even [eventHandlers].
 *
 * They are used to eliminate repeated code on components, and to make styling easier.
 *
 * @see [BlockProperties]
 * @see [ImageBlockProperties]
 * @see [TextProperties]
 */
abstract class Properties : Cloneable {
    abstract val color: Color

    /** use this to set a size for the target component.
     *
     * This will **not** override the size of the component if it is already set.
     * Else, if this is not null and the component's size is null, the component's size will be set to this.
     */
    open val size: Size<Unit>? = null
    abstract val padding: Float
    val eventHandlers = mutableMapOf<Events, Component.() -> Boolean>()

    /**
     * Add a universal event handler to this component's property.
     *
     * This means that every component using this property will have this event handler.
     */
    fun addEventHandler(type: Events, function: Component.() -> Boolean) {
        eventHandlers[type] = function
    }

    @JvmName("addEventhandler")
    fun addEventHandler(event: Events, handler: Component.() -> kotlin.Unit) {
        this.eventHandlers[event] = {
            handler(this)
            true
        }
    }

    /** add a universal event handler to this component's property.
     *
     * This means that every component using this property will have this event handler.
     */
    fun addEventHandlers(vararg handlers: Events.Handler) {
        handlers.forEach { addEventHandler(it.event, it.handler) }
    }

    companion object {
        private val properties: MutableMap<String, Properties> = mutableMapOf(
            "cc.polyfrost.polyui.component.impl.Block" to BlockProperties(),
            "cc.polyfrost.polyui.component.impl.ImageBlock" to ImageBlockProperties(),
            "cc.polyfrost.polyui.component.impl.Text" to TextProperties()
        )

        @JvmStatic
        inline fun <T : Properties, reified C : Component> get(): T =
            get(C::class.java.name)

        @JvmStatic
        fun <T : Properties> get(component: Component): T =
            get(component::class.java.name)

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        fun <T : Properties> get(name: String): T = properties[name] as? T
            ?: throw Exception("Properties for component $name not found")

        /**
         * Add the given property to the property' registry.
         *
         * This will **OVERWRITE** any existing property for the given component.
         * @param properties the property to add
         */
        @JvmStatic
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
        @JvmStatic
        fun addPropertyType(name: String, properties: Properties) {
            this.properties[name] = properties
        }
    }
}
