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

package cc.polyfrost.polyui.layout.impl.extension

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.*
import kotlin.math.min

/**
 * A layout that you can scroll.
 *
 * This is a so-called "extension layout", meaning that you apply it to an existing layout, like this:
 * `ScrollingLayout(myLayout)` or using [myLayout.scrolling()][Layout.scrolling]
 */
class ScrollingLayout(
    layout: Layout,
    /** the size of the scrollable area, aka the display size for this layout. Seperate from its actual size. */
    val scrollingSize: Size<Unit>
) : PointerLayout(layout) {
    private val anims: MutablePair<Animation?, Animation?> = MutablePair(null, null)
    private var ofsX = at.a.px
    private var ofsY = at.b.px

    /** origin x (in wider coordinate space) */
    var ox = at.a.px
        private set

    /** origin y (in wider coordinate space) */
    var oy = at.b.px
        private set
    private var added = false
    var scrollsX = true
        private set
    var scrollsY = true
        private set
    private val bars =
        MutablePair(Scrollbar(this, ScrollbarProperties(), true), Scrollbar(this, ScrollbarProperties(), false))

    /** float in the range 0-1 to represent the percentage that this layout is scrolled (i.e. 0=no scrolling, 1=fully scrolled) */
    inline var scrollYPercent
        get() = (oy - ptr.at.b.px) / (ptr.size!!.b.px - scrollingSize.b.px)
        private set(value) {
            // given the equation: v = (o - x) / (c - s)
            // solve for x (gcse maths coming in handy here) [3 marks]
            // v * (c - s) = o - x
            // v * (c - s) - o = -x
            // -(v * (c - s) - o) = x
            at.b.px = -(value.coerceIn(0f, 1f) * (ptr.size!!.b.px - scrollingSize.b.px) - oy)
        }

    /** float in the range 0-1 to represent the percentage that this layout is scrolled (i.e. 0=no scrolling, 1=fully scrolled) */
    inline var scrollXPercent
        get() = (ox - ptr.at.a.px) / (ptr.size!!.a.px - scrollingSize.a.px)
        private set(value) {
            at.a.px = -(value.coerceIn(0f, 1f) * (ptr.size!!.a.px - scrollingSize.a.px) - ox)
        }

    init {
        ptr.simpleName += " [Scrollable]"
        ptr.acceptsInput = true
    }

    override fun reRenderIfNecessary() {
        val (barX, barY) = bars
        barX.hideTime += polyui.delta
        barY.hideTime += polyui.delta
        if (barX.hideTime > barX.properties.timeToHide) barX.shown = false
        if (barY.hideTime > barY.properties.timeToHide) barY.shown = false
        super.reRenderIfNecessary()
    }

    override fun render() {
        renderer.pushScissor(ox - at.a.px, oy - at.b.px, scrollingSize.a.px, scrollingSize.b.px)

        ptr.removeQueue.fastEach { if (it.canBeRemoved()) removeComponentNow(it) }
        ptr.needsRedraw = false
        val delta = polyui.delta

        val (anim, anim1) = anims
        if (anim?.isFinished == true) {
            anims.first = null
        } else {
            bars.first.hideTime = 0L
            anim?.update(delta)?.also {
                at.a.px = ofsX + anim.value
                ptr.needsRedraw = true
            }
        }
        if (anim1?.isFinished == true) {
            anims.second = null
        } else {
            bars.second.hideTime = 0L
            anim1?.update(delta)?.also {
                at.b.px = ofsY + anim1.value
                ptr.needsRedraw = true
            }
        }

        ptr.components.fastEach {
            it.preRender(delta)
            it.render()
            it.postRender()
        }
        bars.first.update()
        bars.second.update()
        renderer.popScissor()
    }

    override fun debugRender() {
        renderer.pushScissor(
            ox - 1f,
            oy - 1f,
            scrollingSize.a.px + 3f,
            scrollingSize.b.px + 3f
        ) // the outline overlaps the edge
        super.debugRender()
        renderer.popScissor()
    }

    override fun isInside(x: Float, y: Float): Boolean {
        val w = ox + scrollingSize.a.px
        val h = oy + scrollingSize.b.px
        val inside = x in ox..w && y >= oy && y <= h
        if (inside && x > w - (scrollingSize.a.px / 10f)) bars.second.shown = true
        if (inside && y > h - (scrollingSize.b.px / 10f)) bars.first.shown = true
        return inside
    }

    override fun accept(event: Events): Boolean {
        if (event is Events.MouseEntered) {
            bars.first.shown = true
            bars.second.shown = true
        }
        if ((scrollsX || scrollsY) && event is Events.MouseScrolled) {
            if (scrollsX && event.amountX != 0) {
                bars.first.cancel()
                val anim = anims.first
                val rem = anim?.to?.minus(anim.value)?.also {
                    ofsX += anim.value
                } ?: 0f.also {
                    ofsX = ptr.at.a.px
                }
                anims.first = Animation.Type.EaseOutExpo.create(
                    .5.seconds,
                    0f,
                    calc(rem - event.amountX.toFloat(), true)
                )
            }
            if (scrollsY && event.amountY != 0) {
                bars.second.cancel()
                val anim = anims.second
                val rem = anim?.to?.minus(anim.value)?.also {
                    ofsY += anim.value
                } ?: 0f.also {
                    ofsY = ptr.at.b.px
                }
                anims.second = Animation.Type.EaseOutExpo.create(
                    .5.seconds,
                    0f,
                    calc(rem - event.amountY.toFloat(), false)
                )
            }

            needsRedraw = true
            return true
        }
        return super.accept(event)
    }

    private fun calc(toAdd: Float, horizontal: Boolean): Float {
        return if (horizontal) {
            if (toAdd > 0f) { // scroll left
                min(ox - at.a.px, toAdd)
            } else { // scroll right
                clz(-(ptr.size!!.a.px - scrollingSize.a.px - (ox - at.a.px)), toAdd)
            }
        } else {
            if (toAdd > 0f) { // scroll up
                min(oy - ptr.at.b.px, toAdd)
            } else { // scroll down
                clz(-(ptr.size!!.b.px - scrollingSize.b.px - (oy - ptr.at.b.px)), toAdd)
            }
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        val p = scrollXPercent
        val p1 = scrollYPercent
        super.rescale(scaleX, scaleY)
        scrollingSize.scale(scaleX, scaleY)
        scrollXPercent = p
        scrollYPercent = p1
        bars.first.update()
        bars.second.update()
    }

    override fun calculateBounds() {
        super.calculateBounds()
        if (!added) {
            ptr.addComponent(bars.first)
            ptr.addComponent(bars.second)
            added = true
        }
        if (scrollingSize.a.px == 0f || scrollingSize.a.px > ptr.size!!.a.px) scrollingSize.a.px = ptr.size!!.a.px
        if (scrollingSize.b.px == 0f || scrollingSize.b.px > ptr.size!!.b.px) scrollingSize.b.px = ptr.size!!.b.px
        if (ptr.size!!.a.px < scrollingSize.a.px) scrollsX = false
        if (ptr.size!!.b.px < scrollingSize.b.px) scrollsY = false
        ox = at.a.px
        oy = at.b.px
        bars.first.calculateBounds()
        bars.second.calculateBounds()
    }

    class Scrollbar(
        private val owner: ScrollingLayout,
        properties: ScrollbarProperties,
        private val horizontal: Boolean = false
    ) : Block(
        properties,
        origin,
        if (horizontal) properties.width.px * 0f.px else 0f.px * properties.width.px
    ) {
        private var mouseClickX = 0f
        private var mouseClickY = 0f
        private var mouseDown = false
        internal var hideTime = 0L
        private var hideAmount = properties.width + (properties.padding * 2f)
        override val properties: ScrollbarProperties
            get() = super.properties as ScrollbarProperties
        private inline val contentSize get() = if (horizontal) owner.ptr.size!!.a.px else owner.ptr.size!!.b.px
        private inline val scrollingSize get() = if (horizontal) owner.scrollingSize.a.px else owner.scrollingSize.b.px
        private inline var length
            get() = if (horizontal) size!!.a.px else size!!.b.px
            set(value) = if (horizontal) size!!.a.px = value else size!!.b.px = value

        var shown = true
            set(value) {
                if (value == field) return
                field = value
                hideTime = 0L
                if (!value) {
                    if (horizontal) {
                        this.moveBy(0f.px * hideAmount.px, properties.showAnim, properties.showAnimDuration) {
                            enabled = false
                        }
                    } else {
                        this.moveBy(hideAmount.px * 0f.px, properties.showAnim, properties.showAnimDuration) {
                            enabled = false
                        }
                    }
                } else {
                    if (horizontal) {
                        at.b.px =
                            (owner.scrollingSize.b.px - properties.padding - properties.width) + (owner.oy - owner.ptr.at.b.px) + hideAmount
                        this.moveBy(0f.px * (-hideAmount).px, properties.showAnim, properties.showAnimDuration)
                    } else {
                        at.a.px =
                            owner.scrollingSize.a.px - properties.padding - properties.width + (owner.ox - owner.ptr.at.a.px) + hideAmount
                        this.moveBy((-hideAmount).px * 0f.px, properties.showAnim, properties.showAnimDuration)
                    }
                    enabled = contentSize > scrollingSize
                }
            }

        var enabled = true
            private set

        override fun calculateBounds() {
            super.calculateBounds()
            enabled = contentSize > scrollingSize
            if (!enabled) {
                return
            }
            if (horizontal) {
                at.b.px = owner.scrollingSize.b.px - properties.padding - properties.width
                at.a.px = 0f
                length = owner.scrollingSize.a.px * (owner.scrollingSize.a.px / owner.ptr.size!!.a.px)
            } else {
                at.a.px = owner.scrollingSize.a.px - properties.padding - properties.width
                at.b.px = 0f
                length = owner.scrollingSize.b.px * (owner.scrollingSize.b.px / owner.ptr.size!!.b.px)
            }
        }

        override fun accept(event: Events): Boolean {
            if (!enabled) return super.accept(event)
            if (event is Events.MousePressed) {
                if (event.mods.toInt() != 0) return super.accept(event)
                mouseClickX = polyui.eventManager.mouseX - at.a.px
                mouseClickY = polyui.eventManager.mouseY - at.b.px
                mouseDown = true
                owner.ptr.needsRedraw = true
                recolor(properties.pressedColor, properties.showAnim, 0.2.seconds)
                return true
            }
            return super.accept(event)
        }

        fun update() {
            if (!enabled) return
            if (horizontal) {
                at.b.px =
                    (owner.scrollingSize.b.px - properties.padding - properties.width) + (owner.oy - owner.ptr.at.b.px) + if (!shown) hideAmount else 0f
            } else {
                at.a.px =
                    owner.scrollingSize.a.px - properties.padding - properties.width + (owner.ox - owner.ptr.at.a.px) + if (!shown) hideAmount else 0f
            }
            if (!shown) return
            if (horizontal) {
                at.a.px = (owner.ptr.size!!.a.px - length) * owner.scrollXPercent
            } else {
                at.b.px = (owner.ptr.size!!.b.px - length) * owner.scrollYPercent
            }
        }

        override fun render() {
            if (!enabled) return
            if (mouseDown) {
                if (!polyui.eventManager.mouseDown) {
                    recolor(properties.color, properties.showAnim, .2.seconds)
                    mouseDown = false
                } else {
                    owner.ptr.needsRedraw = true
                    hideTime = 0L
                    if (horizontal) {
                        owner.scrollXPercent =
                            (polyui.eventManager.mouseX - mouseClickX) / (owner.scrollingSize.a.px - length)
                    } else {
                        owner.scrollYPercent =
                            (polyui.eventManager.mouseY - mouseClickY) / (owner.scrollingSize.b.px - length)
                    }
                }
            }
            super.render()
        }

        fun cancel() {
            if (!shown) {
                hideTime = 0L
                shown = true
                operations.clear()
            }
        }
    }

    open class ScrollbarProperties : BlockProperties() {
        override val color: Color = rgba(0.5f, 0.5f, 0.5f, 0.5f)
        override val hoverColor = rgba(0.5f, 0.5f, 0.5f, 0.75f)
        override val cornerRadii: FloatArray = 2f.radii()
        override val pressedColor = rgba(0.5f, 0.5f, 0.5f, 0.8f)
        override val padding: Float = 2f
        open val width = 4f
        open val showAnim: Animations? = Animations.EaseOutExpo
        open val showAnimDuration: Long = .5.seconds
        open val timeToHide = 2L.seconds
    }
}
