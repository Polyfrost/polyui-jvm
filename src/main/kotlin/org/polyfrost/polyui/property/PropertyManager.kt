/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
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

package org.polyfrost.polyui.property

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.layout.impl.SwitchingLayout
import org.polyfrost.polyui.property.impl.*
import org.polyfrost.polyui.renderer.data.FontFamily
import kotlin.reflect.KClass

class PropertyManager(val colors: Colors, val fonts: FontFamily = PolyUI.defaultFonts) : Cloneable {

    val properties: HashMap<String, Properties> = hashMapOf(
        TextInput::class.java.name to TextInputProperties(TextProperties()),
        Block::class.java.name to BlockProperties(withStates = true),
        Divider::class.java.name to DividerProperties(),
        Image::class.java.name to ImageProperties(),
        Text::class.java.name to TextProperties(),
        Button::class.java.name to ButtonProperties(),
        Dropdown::class.java.name to DropdownProperties(),
        Dropdown.Entry::class.java.name to DropdownProperties.Entry(),
        Slider::class.java.name to SliderProperties(),
        Checkbox::class.java.name to CheckboxProperties(),
        Scrollbar::class.java.name to ScrollbarProperties(),
        Switch::class.java.name to SwitchProperties(),
        SwitchingLayout::class.java.name to SwitchingLayoutProperties(),
        SearchField::class.java.name to SearchFieldProperties(),
        RadioButton::class.java.name to RadioButtonProperties(),
        Keybind::class.java.name to KeybindProperties(),
    )

    inline fun <reified C : Component> get(): Properties = get(C::class.java.name)

    fun <T : Properties> get(component: Component): T =
        get(component::class.java.name)

    @Suppress("UNCHECKED_CAST")
    fun <T : Properties> get(name: String): T {
        return (
            properties[name] as? T
                ?: throw Exception("Properties for component $name not found")
            ).also {
            if (!it.initialized) {
                it.colors = colors
                it.fonts = fonts
            }
        }
    }

    /**
     * Add the given property to the property' registry.
     *
     * This will **OVERWRITE** any existing property for the given component.
     * @param properties the property to add
     */
    fun addPropertyType(forComponent: Component, properties: Properties) {
        this.properties[forComponent::class.java.name] = properties
    }

    fun addPropertyType(forComponent: Class<*>, properties: Properties) {
        this.properties[forComponent.canonicalName!!] = properties
    }

    fun addPropertyType(forComponent: KClass<*>, properties: Properties) {
        this.properties[forComponent.java.name] = properties
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
     * @param name the simple class name of the component (e.g. org.polyfrost.polyui.component.impl.Block)
     * @param properties the property to add
     *
     * @see addPropertyTypes
     */
    fun addPropertyType(name: String, properties: Properties) {
        this.properties[name] = properties
    }

    public override fun clone(): PropertyManager {
        return PropertyManager(colors, fonts).also {
            it.properties.putAll(properties)
        }
    }
}
