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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.property.impl.SearchFieldProperties
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.toArrayList

/**
 * Search field in PolyUI.
 *
 * @param input the list of objects to search through. So you maintain control of it, you should pass as an [ArrayList] reference.
 * @param output the output of the search, where you can pass a list reference.
 * @since 0.22.0
 */
@Suppress("UNCHECKED_CAST")
class SearchField(
    properties: SearchFieldProperties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>,
    image: PolyImage? = null,
    fontSize: Unit = 16.px,
    input: List<Any>,
    output: MutableList<Any>? = null,
    events: EventDSL<SearchField>.() -> kotlin.Unit = {},
) : TextInput(properties = properties, at = at, size = size, image = image, fontSize = fontSize, events = events as EventDSL<TextInput>.() -> kotlin.Unit) {
    override val properties
        get() = super.properties as SearchFieldProperties
    var input = input.toArrayList()
    val output = output ?: arrayListOf()
    var query
        get() = super.txt
        set(value) {
            super.txt = value
        }

    override fun accept(event: Event): Boolean {
        if (event is ChangedEvent) {
            output.clear()
            input.fastEach {
                if (properties.searchAlgorithm(it, query)) {
                    output.add(it)
                }
            }
        }
        return super.accept(event)
    }
}
