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

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.component.Focusable
import org.polyfrost.polyui.event.FocusedEvent
import org.polyfrost.polyui.property.impl.SearchFieldProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.origin
import org.polyfrost.polyui.unit.px
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
    fontSize: Unit? = 16.px,
    searchList: List<Any>,
    searchOut: MutableList<Any>? = null
) : ContainingComponent(properties, at, size, children = arrayOf()), Focusable {
    override val properties
        get() = super.properties as SearchFieldProperties
    private val image = if (image != null) Image(image = image, at = origin, acceptInput = false) else null
    private lateinit var input: TextInput
    private val fs = fontSize
    var searchList = searchList.toArrayList()
    val searchOut = searchOut ?: arrayListOf()
    var query get() = input.txt
        set(value) {
            input.txt = value
        }

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        (properties.fontSize as? Unit.Dynamic)?.set(this.size!!.b)
        (properties.textImagePadding as? Unit.Dynamic)?.set(this.size!!.a)
        (properties.lateralPadding as? Unit.Dynamic)?.set(this.size!!.a)
        val fontSize = fs ?: properties.fontSize

        image?.size = image?.calculateSize()
        input = TextInput(properties.inputProperties, at = origin, size = size!!, fontSize = fontSize)
        addComponents(input, image)
    }

    override fun placeChildren() {
        super.placeChildren()
        input.text.x = this.x
        input.text.y = this.y + this.height / 2f - input.text.fontSize / 2f
        input.text.x += properties.lateralPadding.px + if (image != null) image.width + properties.textImagePadding.px else 0f
        if (image != null) {
            image.x += properties.lateralPadding.px
            image.y = this.y + this.height / 2f - image.height / 2f
        }
    }

    /**
     * Return [searchOut], which will be automatically updated when the [query] changes.
     */
    fun getSearch(): List<Any> = searchOut

    override fun accept(event: FocusedEvent) {
        input.accept(event)
        if(event !== FocusedEvent.Gained && event !== FocusedEvent.Lost) {
            searchOut.clear()
            properties.searchAlgorithm.search(input.txt, searchList, searchOut)
        }
    }
}
