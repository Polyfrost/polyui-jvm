/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
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

package org.polyfrost.polyui.component

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Easing
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.Locking
import org.polyfrost.polyui.utils.fastEach

/**
 * Extensions to [Component] which allow it to listen to scroll events and scroll itself.
 *
 * @since 1.6.0
 */
abstract class Scrollable(
    at: Vec2,
    size: Vec2,
    visibleSize: Vec2,
    alignment: Align,
    focusable: Boolean
) : Inputtable(at, size, alignment, focusable) {
    @ApiStatus.Internal
    protected var visWidth = visibleSize.x

    @ApiStatus.Internal
    protected var visHeight = visibleSize.y

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getVisibleSize")
    @set:JvmName("setVisibleSize")
    override var visibleSize: Vec2
        get() = if (hasVisibleSize) Vec2(visWidth, visHeight) else size
        set(value) {
            visWidth = value.x
            visHeight = value.y
        }

    var hasVisibleSize: Boolean
        get() = visWidth > 0f || visHeight > 0f
        set(value) {
            if (hasVisibleSize && !value) {
                visWidth = 0f
                visHeight = 0f
            }
        }

    @Locking
    @set:Synchronized
    private var xScroll: Animation? = null

    @Locking
    @set:Synchronized
    private var yScroll: Animation? = null

    @get:JvmName("isScrolling")
    val scrolling get() = xScroll != null || yScroll != null

    @get:JvmName("isScrollingX")
    val scrollingX get() = xScroll != null

    @get:JvmName("isScrollingY")
    val scrollingY get() = yScroll != null

    open var shouldScroll = true
        set(value) {
            if (!value) {
                if (xScroll != null) {
                    this.x = xScroll?.from ?: this.x
                    xScroll = null
                }
                if (yScroll != null) {
                    this.y = yScroll?.from ?: this.y
                    yScroll = null
                }
            }
            field = value
        }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("screenAt")
    override val screenAt: Vec2
        get() = Vec2(xScroll?.from ?: x, yScroll?.from ?: y)

    override fun setup(polyUI: PolyUI): Boolean {
        if (!super.setup(polyUI)) return false
        tryMakeScrolling()
        return true
    }

    override fun recalculate() {
        super.recalculate()
        tryMakeScrolling()
    }

    fun pushScroll(delta: Long, renderer: Renderer): Boolean {
        val px = x
        val py = y
        var push = false
        xScroll?.let {
            x = it.update(delta)
            push = true
        }
        yScroll?.let {
            y = it.update(delta)
            push = true
        }
        if (push) {
            val vs = visibleSize
            val sa = screenAt
            renderer.pushScissor(sa.x, sa.y, vs.x, vs.y)
            if (x != px || y != py) {
                polyUI.inputManager.recalculate()
                return true
            }
        }
        return false
    }

    fun popScroll(renderer: Renderer) {
        if (xScroll != null || yScroll != null) renderer.popScissor()
    }

    override fun accept(event: Event): Boolean {
        if (!isEnabled) return false
        val res = super.accept(event)
        if (hasVisibleSize && event is Event.Mouse.Scrolled) {
            var ran = false
            xScroll?.let {
                it.durationNanos = 0.6.seconds
                if (yScroll == null) it.to += event.amountY
                it.to += event.amountX
                val s = it.from - (width - visWidth)
                if (s >= it.from) {
                    this.x = it.from
                    xScroll = null
                    return@let
                }
                it.to = it.to.coerceIn(s, it.from)
                ran = true
            }

            yScroll?.let {
                it.durationNanos = 0.6.seconds
                it.to += event.amountY
                val s = it.from - (height - visHeight)
                if (s >= it.from) {
                    this.y = it.from
                    yScroll = null
                    return@let
                }
                it.to = it.to.coerceIn(s, it.from)
                ran = true
            }

            if (ran) {
                if (this is Drawable) needsRedraw = true
                clipChildren()
                return true
            }
        }
        return res
    }

    @Locking(`when` = "this.shouldScroll && this.hasVisibleSize && this.visibleSize > this.size")
    fun tryMakeScrolling() {
        if (!positioned) return
        if (!shouldScroll) return
        if (!hasVisibleSize) return
        var scrolling = false
        if (width > visWidth) {
            if (xScroll == null) {
                scrolling = true
                xScroll = Easing.Expo(Easing.Type.Out, 0L, x, x)
            }
        } else if (xScroll != null) {
            this.x = xScroll?.from ?: this.x
            xScroll = null
        }
        if (height > visHeight) {
            if (yScroll == null) {
                scrolling = true
                yScroll = Easing.Expo(Easing.Type.Out, 0L, y, y)
            }
        } else if (yScroll != null) {
            this.y = yScroll?.from ?: this.y
            yScroll = null
        }
        if (scrolling) acceptsInput = true
    }

    override fun fixVisibleSize() {
        if (shouldScroll) {
            visWidth = visWidth.coerceAtMost(width)
            visHeight = visHeight.coerceAtMost(height)
        } else {
            super.fixVisibleSize()
            hasVisibleSize = false
        }
    }

    override fun rescale0(scaleX: Float, scaleY: Float, position: Boolean) {
        super.rescale0(scaleX, scaleY, position)
        xScroll?.let { it.from *= scaleX; it.to *= scaleX }
        yScroll?.let { it.from *= scaleY; it.to *= scaleY }
        visWidth *= scaleX
        visHeight *= scaleY
    }

    /**
     * reset the initial scroll position to the current position.
     *
     * This method should be used if you externally modify the position of this component with scrolling enabled.
     * @since 1.0.5
     */
    fun resetScroll() {
        xScroll?.let { it.from = x; it.to = x }
        yScroll?.let { it.from = y; it.to = y }
        _resetScroll(this)
    }

    private fun _resetScroll(cur: Component) {
        cur.children?.fastEach {
            if (it is Scrollable) it.resetScroll()
            else _resetScroll(it)
        }
    }
}
