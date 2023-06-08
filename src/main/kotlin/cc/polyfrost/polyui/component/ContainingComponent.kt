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

package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastEach

/**
 * This component is used to contain other components.
 *
 * You need to implement the [render] functions as usual (**ensure to call super!**),
 * and you will need to place the components yourself either in [calculateSize] or [calculateBounds],
 * depending on your needs (if it always infers its size, pass `null` to [size] and use [calculateSize]).
 *
 * This is used for situations where there is a component with multiple parts, for example [Button][cc.polyfrost.polyui.component.impl.Button] which contains two images and a text as well as itself. It allows for them to share rotation, skew, etc.
 *
 * @since 0.17.4
 */
abstract class ContainingComponent(
    properties: Properties? = null,
    /** position relative to this layout. */
    at: Point<Unit>,
    size: Size<Unit>? = null,
    acceptInput: Boolean = true,
    children: Array<out Component?>,
    vararg events: Events.Handler
) : Component(properties, at, size, acceptInput, *events) {
    val children: ArrayList<Component> = children.filterNotNull() as ArrayList<Component>
    init {
        this.children.fastEach { it.acceptsInput = false }
    }

    /** Add components to this containing component. Please note that this should only be called in a constructor or `init` block. */
    fun addComponents(vararg component: Component?) {
        component.forEach {
            if (it != null) children.add(it)
        }
    }

    /**
     * Render the children of this component.
     *
     * **you must** call [super.render()][render]!
     */
    override fun render() {
        if (rotation != 0.0) {
            children.fastEach {
                // this will move it so that it renders around the middle of the main component.
                it.at.a.px -= acx
                it.at.b.px -= acy
                it.preRender(polyui.delta)
                it.render()
                it.postRender()
                it.at.a.px += acx
                it.at.b.px += acy
            }
        } else {
            children.fastEach {
                it.preRender(polyui.delta)
                it.render()
                it.postRender()
            }
        }
    }

    override fun reset() {
        children.fastEach { it.reset() }
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        children.fastEach {
            it.layout = this.layout
            it.setup(renderer, polyui)
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        children.fastEach { it.rescale(scaleX, scaleY) }
    }

    override fun accept(event: Events): Boolean {
        // children components cannot cancel an event.
        children.fastEach { it.accept(event) }
        return super.accept(event)
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        children.fastEach { it.onColorsChanged(colors) }
    }
}
