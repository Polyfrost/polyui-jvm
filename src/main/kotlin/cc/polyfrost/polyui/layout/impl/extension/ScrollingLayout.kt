/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
import cc.polyfrost.polyui.utils.MutablePair
import cc.polyfrost.polyui.utils.clz
import cc.polyfrost.polyui.utils.fastEach
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
    private val offset = MutablePair(at.a.px, at.b.px)
    private val origin = MutablePair(at.a.px, at.b.px)
    private var added = false
    private val bars =
        MutablePair(Scrollbar(this, ScrollbarProperties(), true), Scrollbar(this, ScrollbarProperties(), false))
    private inline val excessY get() = ptr.size!!.b.px - scrollingSize.b.px
    private inline val excessX get() = ptr.size!!.a.px - scrollingSize.a.px

    init {
        ptr.simpleName += " [Scrollable]"
        ptr.acceptsInput = true
    }

    override fun render() {
        val (ox, oy) = origin
        renderer.pushScissor(ox - at.a.px, oy - at.b.px, scrollingSize.a.px, scrollingSize.b.px)

        ptr.removeQueue.fastEach { if (it.canBeRemoved()) removeComponentNow(it) }
        ptr.needsRedraw = false
        val delta = ptr.clock.delta

        val (anim, anim1) = anims
        if (anim?.isFinished == true) {
            anims.first = null
        } else {
            anim?.update(delta)?.also {
                at.a.px = offset.first + anim.value
                bars.first.update()
                ptr.needsRedraw = true
            }
        }
        if (anim1?.isFinished == true) {
            anims.second = null
        } else {
            anim1?.update(delta)?.also {
                at.b.px = offset.second + anim1.value
                bars.second.update()
                ptr.needsRedraw = true
            }
        }

        ptr.components.fastEach {
            it.preRender(delta)
            it.render()
            it.postRender()
        }
        renderer.popScissor()
    }

    override fun debugRender() {
        val (ox, oy) = origin
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
        val (ox, oy) = origin
        return x >= ox && x <= ox + scrollingSize.a.px && y >= oy && y <= oy + scrollingSize.b.px
    }

    override fun accept(event: Events): Boolean {
        if (event is Events.MouseScrolled) {
            if (event.amountX != 0) {
                val anim = anims.first
                val rem = anim?.to?.minus(anim.value)?.also {
                    offset.first += anim.value
                } ?: 0f.also {
                    offset.first = ptr.at.a.px
                }
                anims.first = Animation.Type.EaseOutExpo.create(
                    1L.seconds,
                    0f,
                    scroll(rem - event.amountX.toFloat(), true)
                )
            }
            if (event.amountY != 0) {
                val anim = anims.second
                val rem = anim?.to?.minus(anim.value)?.also {
                    offset.second += anim.value
                } ?: 0f.also {
                    offset.second = ptr.at.b.px
                }
                anims.second = Animation.Type.EaseOutExpo.create(
                    1L.seconds,
                    0f,
                    scroll(rem - event.amountY.toFloat(), false)
                )
            }

            ptr.clock.delta
            needsRedraw = true
            return true
        }
        return super.accept(event)
    }

    fun scroll(toAdd: Float, sideways: Boolean): Float {
        return if (sideways) {
            if (toAdd > 0f) { // scroll left
                min(origin.first - at.a.px, toAdd)
            } else { // scroll right
                clz(-(ptr.size!!.a.px - scrollingSize.a.px - (origin.first - at.a.px)), toAdd)
            }
        } else {
            if (toAdd > 0f) { // scroll up
                min(origin.second - ptr.at.b.px, toAdd)
            } else { // scroll down
                clz(-(ptr.size!!.b.px - scrollingSize.b.px - (origin.second - ptr.at.b.px)), toAdd)
            }
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        scrollingSize.scale(scaleX, scaleY)
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
        origin.first = at.a.px
        origin.second = at.b.px
        bars.first.calculateBounds()
        // bars.second.calculateBounds() see to do
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
        override val properties: ScrollbarProperties
            get() = super.properties as ScrollbarProperties
        private inline val contentSize get() = if (horizontal) owner.ptr.size!!.a.px else owner.ptr.size!!.b.px
        private inline val scrollingSize get() = if (horizontal) owner.scrollingSize.a.px else owner.scrollingSize.b.px
        private inline var length
            get() = if (horizontal) size!!.a.px else size!!.b.px
            set(value) = if (horizontal) size!!.a.px = value else size!!.b.px = value

        var shown = false
            private set(value) {
                if (value == field) return
                field = value
                if (value) {
//                    this.move(normalPos, properties.showAnimation, properties.showAnimationDuration)      // todo animations, and clicking of scrollbars, scrollbars other axis (i.e. horizontal does not move down when moved scrolled down)
                } else {
//                    this.move(hiddenPos, properties.showAnimation, properties.showAnimationDuration) { shown = false }
                }
            }

        var enabled = false
            private set(value) {
                if (value == field) return
                field = value
                if (!value && shown) shown = false
            }

        override fun calculateBounds() {
            super.calculateBounds()
            enabled = contentSize > scrollingSize
            if (!enabled) {
                shown = false
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

        fun update() {
            if (horizontal) {
                val scrollOffset = owner.origin.first - owner.ptr.at.a.px
                at.a.px = (owner.ptr.size!!.a.px - length) * (scrollOffset / owner.excessX)
            } else {
                val scrollOffset = owner.origin.second - owner.ptr.at.b.px
                at.b.px = (owner.ptr.size!!.b.px - length) * (scrollOffset / owner.excessY)
            }
        }
    }

    open class ScrollbarProperties : BlockProperties() {
        override val color: Color = Color(0.5f, 0.5f, 0.5f, 0.5f)
        override val hoverColor = Color(0.5f, 0.5f, 0.5f, 0.75f)
        override val cornerRadii: FloatArray = floatArrayOf(2f, 2f, 2f, 2f)
        open val clickColor = Color(0.5f, 0.5f, 0.5f, 0.8f)
        override val padding: Float = 2f
        open val width = 4f
        open val showAnimation: Animations? = Animations.EaseOutExpo
        open val showAnimationDuration: Long = 0.5.seconds
    }
}
