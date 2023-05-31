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
import cc.polyfrost.polyui.utils.radii
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

    override fun render() {
        renderer.pushScissor(ox - at.a.px, oy - at.b.px, scrollingSize.a.px, scrollingSize.b.px)

        ptr.removeQueue.fastEach { if (it.canBeRemoved()) removeComponentNow(it) }
        ptr.needsRedraw = false
        val delta = ptr.clock.delta

        val (anim, anim1) = anims
        if (anim?.isFinished == true) {
            anims.first = null
        } else {
            anim?.update(delta)?.also {
                at.a.px = ofsX + anim.value

                ptr.needsRedraw = true
            }
        }
        if (anim1?.isFinished == true) {
            anims.second = null
        } else {
            anim1?.update(delta)?.also {
                at.b.px = ofsY + anim1.value
                ptr.needsRedraw = true
            }
        }
        bars.first.update()
        bars.second.update()

        ptr.components.fastEach {
            it.preRender(delta)
            it.render()
            it.postRender()
        }
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
        return x >= ox && x <= ox + scrollingSize.a.px && y >= oy && y <= oy + scrollingSize.b.px
    }

    override fun accept(event: Events): Boolean {
        if ((scrollsX || scrollsY) && event is Events.MouseScrolled) {
            if (scrollsX && event.amountX != 0) {
                val anim = anims.first
                val rem = anim?.to?.minus(anim.value)?.also {
                    ofsX += anim.value
                } ?: 0f.also {
                    ofsX = ptr.at.a.px
                }
                anims.first = Animation.Type.EaseOutExpo.create(
                    1L.seconds,
                    0f,
                    scroll(rem - event.amountX.toFloat(), true)
                )
            }
            if (scrollsY && event.amountY != 0) {
                val anim = anims.second
                val rem = anim?.to?.minus(anim.value)?.also {
                    ofsY += anim.value
                } ?: 0f.also {
                    ofsY = ptr.at.b.px
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
        override val properties: ScrollbarProperties
            get() = super.properties as ScrollbarProperties
        private inline val contentSize get() = if (horizontal) owner.ptr.size!!.a.px else owner.ptr.size!!.b.px
        private inline val scrollingSize get() = if (horizontal) owner.scrollingSize.a.px else owner.scrollingSize.b.px
        private inline var length
            get() = if (horizontal) size!!.a.px else size!!.b.px
            set(value) = if (horizontal) size!!.a.px = value else size!!.b.px = value

        var shown = true
            private set(value) {
                if (value == field) return
                field = value
                if (!value) {
                    if (horizontal) { // todo hiding of scrollbar
                        this.moveTo(
                            (at.a.px + (properties.width + properties.padding) * 2f).px * at.b.clone(),
                            properties.showAnimation,
                            properties.showAnimationDuration
                        )
                    } else {
                        this.moveTo(
                            at.a.clone() * (at.b.px + (properties.width + properties.padding) * 2f).px,
                            properties.showAnimation,
                            properties.showAnimationDuration
                        )
                    }
                } else {
                    if (horizontal) {
                        this.moveTo(
                            (at.a.px - (properties.width + properties.padding) * 2f).px * at.b.clone(),
                            properties.showAnimation,
                            properties.showAnimationDuration
                        )
                    } else {
                        this.moveTo(
                            at.a.clone() * (at.b.px - (properties.width + properties.padding) * 2f).px,
                            properties.showAnimation,
                            properties.showAnimationDuration
                        )
                    }
                }
            }

        var enabled = true
            private set(value) {
                if (value == field) return
                field = value
            }

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
                if (event.mods != 0.toShort()) return super.accept(event)
                mouseClickX = polyui.eventManager.mouseX - at.a.px
                mouseClickY = polyui.eventManager.mouseY - at.b.px
                mouseDown = true
                owner.ptr.needsRedraw = true
                recolor(properties.clickColor, properties.showAnimation, 0.2.seconds)
                return true
            }
            return super.accept(event)
        }

        fun update() {
            if (horizontal) {
                at.b.px =
                    (owner.scrollingSize.b.px - properties.padding - properties.width) + (owner.oy - owner.ptr.at.b.px)
                at.a.px = (owner.ptr.size!!.a.px - length) * owner.scrollXPercent
            } else {
                at.a.px =
                    owner.scrollingSize.a.px - properties.padding - properties.width + (owner.ox - owner.ptr.at.a.px)
                at.b.px = (owner.ptr.size!!.b.px - length) * owner.scrollYPercent
            }
        }

        override fun render() {
            if (!enabled) return
            if (mouseDown) {
                if (!polyui.eventManager.mouseDown) {
                    recolor(properties.color, properties.showAnimation, .2.seconds)
                    mouseDown = false
                } else {
                    owner.ptr.needsRedraw = true
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
    }

    open class ScrollbarProperties : BlockProperties() {
        override val color: Color = Color(0.5f, 0.5f, 0.5f, 0.5f)
        override val hoverColor = Color(0.5f, 0.5f, 0.5f, 0.75f)
        override val cornerRadii: FloatArray = 2f.radii()
        override val clickColor = Color(0.5f, 0.5f, 0.5f, 0.8f)
        override val padding: Float = 2f
        open val width = 4f
        open val showAnimation: Animations? = Animations.EaseOutExpo
        open val showAnimationDuration: Long = .5.seconds
    }
}
