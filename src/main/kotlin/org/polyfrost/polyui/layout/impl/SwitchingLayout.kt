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

package org.polyfrost.polyui.layout.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import org.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import org.polyfrost.polyui.PolyUI.Companion.INIT_SETUP
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.DrawableOp
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.property.impl.SwitchingLayoutProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit

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

    private var current: Layout? = null

    override val properties
        get() = super.properties as SwitchingLayoutProperties

    /**
     * Switches the given layout into this layout.
     * @param new the layout to switch into this.
     */
    @JvmName("switchLayout")
    fun switch(new: Layout) {
        require(initStage == INIT_COMPLETE) { "Cannot switch layouts before initialization is complete!" }

        if (new.initStage == INIT_NOT_STARTED) {
            new.layout = this.layout
            new.setup(renderer, polyUI)
            new.calculateBounds()
            this.layout.addComponent(new)
        } else if (new.initStage == INIT_SETUP) {
            PolyUI.LOGGER.warn("[SwitchingLayout] received partially initialized layout? $new")
            new.calculateBounds()
            this.layout.addComponent(new)
        }

        if (autoSized && width == 0f && height == 0f) {
            PolyUI.LOGGER.warn("SwitchingLayout has no size; setting to first given layout: (${new.width}x${new.height})")
            width = new.width
            height = new.height
        }
        if (makesScrolling) {
            if (new.width > width || new.height > height) {
                new.scrolling(width.px * height.px)
            }
        } else {
            new.rescale(new.width / width, new.height / height)
        }

        @Suppress("UNCHECKED_CAST")
        current?.let { old ->
            val oldx = old.x
            val oldy = old.y
            val oldl = old.layout

            val oldOp = object : DrawableOp.Persistent(old) {
                override fun apply(renderer: Renderer) {
                    renderer.pushScissor(x - drawable.x, y - drawable.y, width, height)
                }
                override fun unapply(renderer: Renderer) {
                    renderer.popScissor()
                }
            }

            val newOp = object : DrawableOp.Persistent(new) {
                override fun apply(renderer: Renderer) {
                    renderer.pushScissor(x - drawable.x, y - drawable.y, width, height)
                }
                override fun unapply(renderer: Renderer) {
                    renderer.popScissor()
                }
            }

            val reset: Layout.() -> kotlin.Unit = {
                this.x = oldx
                this.y = oldy
                this.layout?.removeComponent(this)
                if (oldl !== this.layout) {
                    this.layout = oldl
                    this.layout?.addComponent(this)
                }
                remove(oldOp)
            }
            if (new.layout != this@SwitchingLayout.layout) {
                PolyUI.LOGGER.warn("[SwitchingLayout] $new is not a child of this, moving!")
                new.layout?.removeComponent(new)
                new.layout = this@SwitchingLayout.layout
                this@SwitchingLayout.layout.addComponent(new)
            }

            reset as Drawable.() -> kotlin.Unit
            when (val transition = properties.transition) {
                is Transitions.Slide -> {
                    when (transition.direction) {
                        SlideDirection.FromLeft -> {
                            old.addOperation(oldOp)
                            old.moveTo((this.x + this.width).px * this.y.px, properties.transitionCurve, properties.transitionDuration, reset)
                            new.x = this.x - this.width
                            new.y = this.y
                            new.addOperation(newOp)
                            new.moveTo(this.x.px * this.y.px, properties.transitionCurve, properties.transitionDuration) {
                                remove(newOp)
                            }
                        }
                        SlideDirection.FromRight -> {
                            old.addOperation(oldOp)
                            old.moveTo((this.x - this.width).px * this.y.px, properties.transitionCurve, properties.transitionDuration, reset)
                            new.x = this.x + this.width
                            new.y = this.y
                            new.addOperation(newOp)
                            new.moveTo(this.x.px * this.y.px, properties.transitionCurve, properties.transitionDuration) {
                                remove(newOp)
                            }
                        }
                        SlideDirection.FromBottom -> {
                            old.addOperation(oldOp)
                            old.moveTo(this.x.px * (this.y - this.height).px, properties.transitionCurve, properties.transitionDuration, reset)
                            new.x = this.x
                            new.y = this.y + this.height
                            new.addOperation(newOp)
                            new.moveTo(this.x.px * this.y.px, properties.transitionCurve, properties.transitionDuration) {
                                remove(newOp)
                            }
                        }
                        SlideDirection.FromTop -> {
                            old.addOperation(oldOp)
                            old.moveTo(this.x.px * (this.y + this.height).px, properties.transitionCurve, properties.transitionDuration, reset)
                            new.x = this.x
                            new.y = this.y - this.height
                            new.addOperation(newOp)
                            new.moveTo(this.x.px * this.y.px, properties.transitionCurve, properties.transitionDuration) {
                                remove(newOp)
                            }
                        }
                    }
                }
                Transitions.Fade -> {
                    new.alpha = 0f
                    new.moveTo(this.at)
                    old.fadeTo(0f, properties.transitionCurve, properties.transitionDuration / 2L) {
                        reset(this)
                        new.fadeTo(1f, properties.transitionCurve, properties.transitionDuration / 2L)
                    }
                }
                null -> {
                    reset(old)
                    new.x = this.x
                    new.y = this.y
                }
            }
        } ?: run {
            new.x = this.x
            new.y = this.y
        }
        current = new
    }

    private fun Drawable.remove(op: DrawableOp) {
        // for performance, fastRemoveIf doesn't have any co-mod checks, so this would cause an exception if executed normally
        polyUI.addHook {
            this@remove.removeOperation(op)
            true
        }
    }

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
