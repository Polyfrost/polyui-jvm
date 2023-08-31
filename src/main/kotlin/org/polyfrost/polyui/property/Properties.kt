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

import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.property.impl.*
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.unit.Size
import org.polyfrost.polyui.unit.Unit

/** # Properties
 *
 * Properties are PolyUI's take on styling through tokens, or shared states. They are used as default values for [components][Drawable], if component-specific values are not set. Every component will have an accompanying property.
 *
 * They contain many values that style a component, such as [palette], [size], and even [eventHandlers].
 *
 * They are used to eliminate repeated code on components, and to make styling easier.
 *
 * @see [BlockProperties]
 * @see [ImageProperties]
 * @see [TextProperties]
 */
abstract class Properties : Cloneable {
    @Transient
    lateinit var colors: Colors

    @Transient
    lateinit var fonts: FontFamily

    abstract val palette: Colors.Palette

    /** use this to set a size for the target component.
     *
     * This will **not** override the size of the component if it is already set.
     * Else, if this is not null and the component's size is null, the component's size will be set to this.
     */
    open val size: Size<Unit>? = null

    @Transient
    val eventHandlers = HashMap<Event, (Component.(Event) -> Boolean)>()

    /**
     * Add a universal event handler to this component's property.
     *
     * This means that every component using this property will have this event handler.
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : Event> addEventHandler(event: E, handler: Component.(E) -> Boolean): Properties {
        eventHandlers[event] = handler as Component.(Event) -> Boolean
        return this
    }

    fun init(colors: Colors, fonts: FontFamily) {
        this.colors = colors
        this.fonts = fonts
    }

    val initialized get() = ::colors.isInitialized
}
