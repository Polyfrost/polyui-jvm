/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.impl.*
import cc.polyfrost.polyui.property.impl.*

class PropertyManager(val polyUI: PolyUI) {
    val properties: MutableMap<String, Properties> = mutableMapOf(
        TextInput::class.qualifiedName!!to TextInputProperties(TextProperties()),
        Block::class.qualifiedName!!to BlockProperties(),
        Divider::class.qualifiedName!!to DividerProperties(),
        Image::class.qualifiedName!!to ImageProperties(),
        Text::class.qualifiedName!! to TextProperties()
    )

    inline fun <reified C : Component> get(): Properties = get(C::class.qualifiedName!!)

    fun <T : Properties> get(component: Component): T =
        get(component::class.qualifiedName!!)

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
        this.properties[forComponent::class.qualifiedName!!] = properties
    }

    /**
     * Add the given properties to the property registry (bulk version of [addPropertyType]).
     *
     * This will **OVERWRITE** any existing property for the given component.
     *
     * @param map a map of properties, and their accompanying properties to add.
     */
    fun addPropertyTypes(map: MutableMap<String, Properties>) {
        for ((k, v) in map) {
            properties[k] = v
        }
    }

    /** @see addPropertyTypes */
    fun setTheme(map: MutableMap<String, Properties>) = addPropertyTypes(map)

    /**
     * Add the given property to the property registry.
     *
     * This will **OVERWRITE** any existing property for the given component.
     *
     * @param name the simple class name of the component (e.g. cc.polyfrost.polyui.component.impl.Block)
     * @param properties the property to add
     *
     * @see addPropertyTypes
     */
    fun addPropertyType(name: String, properties: Properties) {
        this.properties[name] = properties
    }
}
