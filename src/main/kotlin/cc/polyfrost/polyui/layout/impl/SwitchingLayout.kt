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

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import cc.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import cc.polyfrost.polyui.PolyUI.Companion.INIT_SETUP
import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.DrawableOp
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.impl.SwitchingLayoutProperties
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit

/**
 * # SwitchingLayout
 *
 * A layout that is completely empty, but acts as a "handler", which moves layouts in and out of its position.
 *
 * @param size the visible size of this layout. If `null`, it will take the size of the first layout switched into this.
 * @param makesScrolling if `true`, any layout that is switched into this will be able to scroll and not resized, otherwise, the layout will be resized to this size.
 *
 */
class SwitchingLayout(
    properties: SwitchingLayoutProperties? = null,
    at: Point<Unit>,
    size: Size<Unit>? = null,
    private val makesScrolling: Boolean = false
) : Component(properties, at, size, false, false) {
    // don't tell anyone it's actually a component ;)
    private var old: Layout? = null
    private var oldOwner: Layout? = null
    private var cache = FloatArray(4)
    private var cacheNew = FloatArray(4)

    override val properties
        get() = super.properties as SwitchingLayoutProperties

    /**
     * Switches the given layout into this layout.
     * @param layout the layout to switch into this.
     */
    @JvmName("switchLayout")
    fun switch(layout: Layout) {
        require(initStage == INIT_COMPLETE) { "Cannot switch layouts before initialization is complete!" }

        if (layout.initStage == INIT_NOT_STARTED) {
            layout.layout = this.layout
            layout.setup(renderer, polyui)
            layout.calculateBounds()
            this.layout.addComponent(layout)
        } else if (layout.initStage == INIT_SETUP) {
            PolyUI.LOGGER.warn("[SwitchingLayout] received partially initialized layout: $layout, this is wierd")
            layout.calculateBounds()
            this.layout.addComponent(layout)
        }
        cacheNew.apply {
            set(0, layout.x)
            set(1, layout.y)
            set(2, layout.width)
            set(3, layout.height)
        }

        if (layout.layout != this.layout) {
            PolyUI.LOGGER.warn("[SwitchingLayout] $layout is not a child of this, moving!")
            oldOwner = layout.layout
            layout.layout?.removeComponent(layout)
            layout.layout = layout
            this.layout.addComponent(layout)
        } else {
            oldOwner = null
        }
        if (autoSized && width == 0f && height == 0f) {
            PolyUI.LOGGER.warn("SwitchingLayout has no size; setting to first given layout: (${layout.width}x${layout.height})")
            width = layout.width
            height = layout.height
        }
        if (makesScrolling) {
            if (layout.width > width || layout.height > height) {
                layout.scrolling(width.px * height.px)
            }
        } else {
            layout.rescale(layout.width / width, layout.height / height)
        }

        val func: Drawable.() -> kotlin.Unit = {
            x = cache[0]
            y = cache[1]
            rescale(cache[2] / width, cache[3] / height)
            this@SwitchingLayout.layout.removeComponent(this)
            if (oldOwner != null) {
                oldOwner!!.addComponent(this)
            }
            old = null
            oldOwner = null
        }

        // todo broken

        when (val transition = properties.transition) {
            is Transitions.Slide -> {
                when (transition.direction) {
                    SlideDirection.FromLeft -> {
                        old?.moveTo((this.x + this.width).px * this.y.px, properties.transitionCurve, properties.transitionDuration, func)
                        layout.x = this.x - this.width
                        layout.y = this.y
                        layout.moveTo(this.x.px * this.y.px, properties.transitionCurve, properties.transitionDuration)
                        layout.addOperation(op(layout))
                    }
                    SlideDirection.FromRight -> {
                        old?.moveTo((this.x - this.width).px * this.y.px, properties.transitionCurve, properties.transitionDuration, func)
                        layout.x = this.x + this.width
                        layout.y = this.y
                        layout.moveTo(this.x.px * this.y.px, properties.transitionCurve, properties.transitionDuration)
                        layout.addOperation(op(layout))
                    }
                    SlideDirection.FromBottom -> {
                        old?.moveTo(this.x.px * (this.y - this.height).px, properties.transitionCurve, properties.transitionDuration, func)
                        layout.x = this.x
                        layout.y = this.y + this.height
                        layout.moveTo(this.x.px * this.y.px, properties.transitionCurve, properties.transitionDuration)
                        layout.addOperation(op(layout))
                    }
                    SlideDirection.FromTop -> {
                        old?.moveTo(this.x.px * (this.y + this.height).px, properties.transitionCurve, properties.transitionDuration, func)
                        layout.x = this.x
                        layout.y = this.y - this.height
                        layout.moveTo(this.x.px * this.y.px, properties.transitionCurve, properties.transitionDuration)
                        layout.addOperation(op(layout))
                    }
                }
            }
            Transitions.Fade -> {
                if (old != null) {
                    old?.fadeTo(0f, properties.transitionCurve, properties.transitionDuration / 2L) {
                        layout.x = this.x
                        layout.y = this.y
                        layout.fadeTo(1f, properties.transitionCurve, properties.transitionDuration / 2L)
                        func(this)
                    }
                } else {
                    layout.x = this.x
                    layout.y = this.y
                    layout.fadeTo(1f, properties.transitionCurve, properties.transitionDuration)
                }
            }
            null -> {
                if (old != null) func(old!!)
                layout.x = this.x
                layout.y = this.y
            }
        }
        cache = cacheNew
        if (old == null) old = layout
    }

    private fun op(layout: Layout): DrawableOp.Scissor = DrawableOp.Scissor(layout, origin, size)

    override fun preRender(deltaTimeNanos: Long) {
    }
    override fun render() {
    }

    override fun postRender() {
    }

    @Deprecated("SwitchingLayouts cannot be drawn, animated, or colored")
    override fun recolor(toColor: Color, animation: Animation.Type?, durationNanos: Long, onFinish: (Component.() -> kotlin.Unit)?) {
    }

    override fun reset() {
    }

    override fun calculateSize() = origin
}
