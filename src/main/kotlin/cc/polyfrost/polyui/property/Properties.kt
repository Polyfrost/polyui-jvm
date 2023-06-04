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
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.impl.*
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
 * @see [ImageProperties]
 * @see [TextProperties]
 */
abstract class Properties : Cloneable {
    lateinit var colors: Colors

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

    val initialized get() = ::colors.isInitialized

    companion object {
        @JvmField
        val primaryProperties = PrimaryBlockProperties()

        @JvmField
        val successProperties = StateBlockProperties(State.Success)

        @JvmField
        val warningProperties = StateBlockProperties(State.Warning)

        @JvmField
        val dangerProperties = StateBlockProperties(State.Danger)
    }
}
