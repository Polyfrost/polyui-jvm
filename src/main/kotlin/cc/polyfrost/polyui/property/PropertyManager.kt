/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package cc.polyfrost.polyui.property

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.impl.*
import cc.polyfrost.polyui.property.impl.*

class PropertyManager(private val polyUI: PolyUI) {
    val properties: HashMap<String, Properties> = hashMapOf(
        TextInput::class.qualifiedName!! to TextInputProperties(TextProperties()),
        Block::class.qualifiedName!! to DefaultBlockProperties(),
        Divider::class.qualifiedName!! to DividerProperties(),
        Image::class.qualifiedName!! to ImageProperties(),
        Text::class.qualifiedName!! to TextProperties(),
        Button::class.qualifiedName!! to ButtonProperties(),
        Dropdown::class.qualifiedName!! to DropdownProperties(),
        Dropdown.Entry::class.qualifiedName!! to DropdownProperties.Entry(),
        Slider::class.qualifiedName!! to SliderProperties(),
        Checkbox::class.qualifiedName!! to CheckboxProperties()
    )

    inline fun <reified C : Component> get(): Properties = get(C::class.qualifiedName!!)

    fun <T : Properties> get(component: Component): T =
        get(component::class.qualifiedName!!)

    @Suppress("UNCHECKED_CAST")
    fun <T : Properties> get(name: String): T {
        return (
            properties[name] as? T
                ?: throw Exception("Properties for component $name not found")
            ).also {
            if (!it.initialized) it.colors = polyUI.colors
        }
    }

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
    fun addPropertyTypes(map: HashMap<String, Properties>) {
        for ((k, v) in map) {
            properties[k] = v
        }
    }

    /** @see addPropertyTypes */
    fun setTheme(map: HashMap<String, Properties>) = addPropertyTypes(map)

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
