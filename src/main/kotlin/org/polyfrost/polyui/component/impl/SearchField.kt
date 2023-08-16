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
 * @param searchList the list of objects to search through. So you maintain control of it, you should pass as an [ArrayList] reference.
 * @param searchOut the output of the search, where you can pass a list reference.
 */
class SearchField(
    properties: SearchFieldProperties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>,
    image: PolyImage? = null,
    fontSize: Unit = 16.px,
    searchList: List<Any>,
    searchOut: MutableList<Any>? = null
) : TextInput(properties = properties, at = at, size = size, image = image, fontSize = fontSize) {
    override val properties
        get() = super.properties as SearchFieldProperties
    var searchList = searchList.toArrayList()
    val searchOut = searchOut ?: arrayListOf()
    var query
        get() = super.txt
        set(value) {
            super.txt = value
        }

    /**
     * Return [searchOut], which will be automatically updated when the [query] changes.
     */
    fun getSearch(): List<Any> = searchOut

    override fun accept(event: Event): Boolean {
        if (event is ChangedEvent) {
            searchOut.clear()
            searchList.fastEach {
                if (properties.searchAlgorithm(it, query)) {
                    searchOut.add(it)
                }
            }
        }
        return super.accept(event)
    }
}
