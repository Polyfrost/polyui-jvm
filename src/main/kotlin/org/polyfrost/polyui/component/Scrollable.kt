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
import org.polyfrost.polyui.utils.annotations.Locking
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.roundTo

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
    protected var visWidth = visibleSize.x.roundTo(PolyUI.ROUNDING)
        set(value) {
            field = value.roundTo(PolyUI.ROUNDING)
        }

    @ApiStatus.Internal
    protected var visHeight = visibleSize.y.roundTo(PolyUI.ROUNDING)
        set(value) {
            field = value.roundTo(PolyUI.ROUNDING)
        }

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
    var xScroll: Animation? = null
        private set

    @Locking
    @set:Synchronized
    var yScroll: Animation? = null
        private set

    private var pushed = false

    @get:JvmName("isScrolling")
    val scrolling get() = yScroll != null || xScroll != null

    @get:JvmName("isScrollingX")
    val scrollingX get() = xScroll != null

    @get:JvmName("isScrollingY")
    val scrollingY get() = yScroll != null

    open var shouldScroll = true
        set(value) {
            if (!value) {
                if (xScroll != null) {
                    this.x = screenAt.x
                    xScroll = null
                }
                if (yScroll != null) {
                    this.y = screenAt.y
                    yScroll = null
                }
            }
            field = value
        }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("screenAt")
    @set:JvmName("setScreenAt")
    final override var screenAt: Vec2 = Vec2.ZERO
        get() = if (scrolling) field else this.at
        set(value) {
            if (!scrolling) return
            field = value
        }

    override fun setup(polyUI: PolyUI): Boolean {
        if (!super.setup(polyUI)) return false
        tryMakeScrolling()
        return true
    }

    override fun recalculate(move: Boolean) {
        super.recalculate(move)
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
            pushed = true
            val (vsx, vsy) = visibleSize
            val (sax, say) = screenAt
            renderer.pushScissorIntersecting(sax, say, vsx, vsy)
            if (x != px || y != py) {
                clipChildren()
                return true
            }
        }
        return false
    }

    fun popScroll(renderer: Renderer) {
        if (pushed) {
            renderer.popScissor()
            pushed = false
        }
    }

    override fun accept(event: Event): Boolean {
        if (!isEnabled) return false
        val res = super.accept(event)
        if (hasVisibleSize && event is Event.Mouse.Scrolled) {
            var ran = false
            val screenAt = screenAt
            xScroll?.let {
                if (yScroll == null) it.extend(event.amountY)
                it.extend(event.amountX)
                val s = screenAt.x - (width - visWidth)
                if (s >= screenAt.x) {
                    this.x = screenAt.x
                    xScroll = null
                    return@let
                }
                it.to = it.to.coerceIn(s, screenAt.x)
                ran = true
            }

            yScroll?.let {
                it.extend(event.amountY)
                val s = screenAt.y - (height - visHeight)
                if (s >= screenAt.y) {
                    this.y = screenAt.y
                    yScroll = null
                    return@let
                }
                it.to = it.to.coerceIn(s, screenAt.y)
                ran = true
            }

            if (ran) {
                if (this is Drawable) needsRedraw = true
                return true
            }
        }
        return res
    }

    override fun recalculateBounds(move: Boolean) {
        super.recalculateBounds(move)
        tryMakeScrolling()
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
                xScroll = Easing.Expo(Easing.Type.Out, 0.2.seconds, x, x)
                screenAt = at
            }
        } else if (xScroll != null) {
            this.x = screenAt.x
            xScroll = null
        }
        if (height > visHeight) {
            if (yScroll == null) {
                scrolling = true
                yScroll = Easing.Expo(Easing.Type.Out, 0.2.seconds, y, y)
                screenAt = at
            }
        } else if (yScroll != null) {
            this.y = screenAt.y
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

    override fun rescale(scaleX: Float, scaleY: Float, uniform: Float, withChildren: Boolean) {
        super.rescale(scaleX, scaleY, uniform, withChildren)
        if (hasVisibleSize) {
            if (rawRescaleSize) {
                visWidth *= scaleX
                visHeight *= scaleY
            } else {
                visWidth *= uniform
                visHeight *= uniform
            }
        }
        val scaleX = if (rawRescalePosition) scaleX else uniform
        val scaleY = if (rawRescalePosition) scaleY else uniform
        screenAt = screenAt.timesWithRounding(scaleX, scaleY, PolyUI.ROUNDING)
        xScroll?.let { it.from = (it.from * scaleX).roundTo(PolyUI.ROUNDING); it.to = (it.to * scaleX).roundTo(PolyUI.ROUNDING) }
        yScroll?.let { it.from = (it.from * scaleY).roundTo(PolyUI.ROUNDING); it.to = (it.to * scaleY).roundTo(PolyUI.ROUNDING) }
    }

    /**
     * reset the initial scroll position to the current position.
     *
     * This method should be used if you externally modify the position of this component with scrolling enabled.
     * @since 1.0.5
     */
    fun resetScroll() {
        screenAt = at
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
