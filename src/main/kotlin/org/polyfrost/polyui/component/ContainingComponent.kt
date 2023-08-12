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

package org.polyfrost.polyui.component

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import org.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.Size
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.addOrReplace
import org.polyfrost.polyui.utils.fastEach

/**
 * This component is used to contain other components.
 *
 * You need to implement the [render] functions as usual (**ensure to call super!**),
 * and you will need to place the components yourself either in [calculateSize] or [calculateBounds],
 * depending on your needs (if it always infers its size, pass `null` to [size] and use [calculateSize]).
 *
 * This is used for situations where there is a component with multiple parts, for example [Button][org.polyfrost.polyui.component.impl.Button] which contains two images and a text as well as itself. It allows for them to share rotation, skew, etc.
 *
 * @since 0.17.4
 */
abstract class ContainingComponent(
    properties: Properties? = null,
    /** position relative to this layout. */
    at: Point<Unit>,
    size: Size<Unit>? = null,
    rawResize: Boolean = false,
    acceptInput: Boolean = true,
    children: Array<out Component?>,
    vararg events: Event.Handler
) : Component(properties, at, size, rawResize, acceptInput, *events) {
    val children: ArrayList<Component> = children.filterNotNull() as ArrayList<Component>

    init {
        this.children.fastEach { it.acceptsInput = false }
    }

    /**
     * Add components to this containing component.
     * @since 0.19.0
     */
    fun addComponents(vararg component: Component?) {
        component.forEach {
            if (it != null) {
                children.addOrReplace(it)
                if (initStage > INIT_NOT_STARTED) {
                    it.layout = this.layout
                    it.setup(renderer, polyUI)
                }
                if (initStage == INIT_COMPLETE) it.calculateBounds()
            }
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
                it.x -= acx
                it.y -= acy
                it.preRender(polyUI.delta)
                it.render()
                it.postRender()
                it.x += acx
                it.y += acy
            }
        } else {
            children.fastEach {
                it.preRender(polyUI.delta)
                it.render()
                it.postRender()
            }
        }
    }

    override fun reset() {
        children.fastEach { it.reset() }
    }

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        children.fastEach {
            it.layout = this.layout
            it.setup(renderer, polyUI)
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        children.fastEach { it.rescale(scaleX, scaleY) }
    }

    fun recolorAll(toColor: Color, animation: Animation.Type? = null, durationNanos: Long = 1L.seconds, onFinish: (Component.() -> kotlin.Unit)? = null) {
        super.recolor(toColor, animation, durationNanos, onFinish)
        children.fastEach { it.recolor(toColor, animation, durationNanos, onFinish) }
    }

    override fun calculateBounds() {
        children.fastEach {
            it.calculateBounds()
        }
        super.calculateBounds()
    }

    /**
     * ## Make sure to call `super.`[onParentInitComplete]`()`!
     */
    override fun onInitComplete() {
        super.onInitComplete()
        placeChildren()
    }

    /**
     * Use this function to place the children components of this component.
     *
     * In this function, only use `+=` and `-=` to move the children components, as their X and Y values are set by this method to the top left corner of this component ([x], [y]).
     *
     * This function is called once, and only once.
     *
     * ### **Make sure to call `super.`[placeChildren]`()`!**
     * @since 0.19.0
     */
    open fun placeChildren() {
        children.fastEach {
            it.x += x
            it.y += y
        }
    }

    override fun accept(event: Event): Boolean {
        // children components cannot cancel an event.
        children.fastEach { it.accept(event) }
        return super.accept(event)
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        children.fastEach { it.onColorsChanged(colors) }
    }
}
