/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl.extension

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.MutablePair
import cc.polyfrost.polyui.utils.clz
import cc.polyfrost.polyui.utils.fastEach
import kotlin.math.max
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
    @get:JvmName("scrollingLayoutSize")
    val size: Size<Unit>
) : PointerLayout(layout) {
    private val anims: MutablePair<Animation?, Animation?> = MutablePair(null, null)
    private val offset = MutablePair(at.a.px, at.b.px)
    private val origin = MutablePair(at.a.px, at.b.px)
    // private val bars = MutablePair(Scrollbar(this, ScrollbarProperties()), Scrollbar(this, ScrollbarProperties()))

    init {
        ptr.simpleName += " [Scrollable]"
        ptr.acceptsInput = true
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        // ptr.addComponent(scrollbar)
    }

    override fun render() {
        val (ox, oy) = origin
        renderer.pushScissor(ox - at.a.px, oy - at.b.px, size.a.px, size.b.px)

        ptr.removeQueue.fastEach { if (it.canBeRemoved()) removeComponentNow(it) }
        ptr.needsRedraw = false
        val delta = ptr.clock.getDelta()

        val (anim, anim1) = anims
        if (anim?.isFinished == true) {
            anims.first = null
        } else {
            anim?.update(delta)?.also {
                at.a.px = offset.first + anim.value
                ptr.needsRedraw = true
            }
        }
        if (anim1?.isFinished == true) {
            anims.second = null
        } else {
            anim1?.update(delta)?.also {
                at.b.px = offset.second + anim1.value
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
        renderer.pushScissor(ox - 1f, oy - 1f, size.a.px + 3f, size.b.px + 3f) // the outline overlaps the edge
        super.debugRender()
        renderer.popScissor()
    }

    override fun isInside(x: Float, y: Float): Boolean {
        val (ox, oy) = origin
        return x >= ox && x <= ox + size.a.px && y >= oy && y <= oy + size.b.px
    }

    override fun accept(event: Events): Boolean {
        if (event is Events.MouseScrolled) {
            // todo polish and scrollbars
            // help I cant read your code! I'm enjoying my functional programming okay
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

            // scrollbar.accept(event)
            ptr.clock.getDelta()
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
                clz(-((ptr.sized!!.a.px - size.a.px) - (origin.first - (at.a.px))), toAdd)
            }
        } else {
            if (toAdd > 0f) { // scroll up
                min(origin.second - at.b.px, toAdd)
            } else { // scroll down
                clz(-((ptr.sized!!.b.px - size.b.px) - (origin.second - (at.b.px))), toAdd)
            }
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        size.scale(scaleX, scaleY)
    }

    override fun calculateBounds() {
        super.calculateBounds()
        origin.first = at.a.px
        origin.second = at.b.px
    }

    class Scrollbar(private val owner: ScrollingLayout, properties: ScrollbarProperties) : Block(
        properties,
        (owner.at + owner.size),
        properties.width.px * 0.px
    ) {
        private val contentSize = owner.ptr.sized
        private val windowSize = owner.size
        private val normalPos =
            ((owner.at.a.px + owner.size.b.px) - this.properties.padding - (this.properties as ScrollbarProperties).width).px * owner.at.b.px.px
        private val hiddenPos =
            normalPos.clone()
                .also { it.a.px += (this.properties as ScrollbarProperties).width + this.properties.padding }
        var shown = false
            private set(value) {
                if (value == field) return
                field = value
                properties as ScrollbarProperties
                if (value) {
                    this.move(normalPos, properties.showAnimation, properties.showAnimationDuration)
                } else {
                    this.move(hiddenPos, properties.showAnimation, properties.showAnimationDuration)
                }
            }

        var enabled = false
            private set(value) {
                if (value == field) return
                field = value
                if (!field && shown) shown = false
            }

        init {
            this.properties as ScrollbarProperties
            at.a.px -= this.properties.width - this.properties.padding
            owner.components.add(this)
        }

        override fun accept(event: Events): Boolean {
            return event is Events.MouseScrolled
        }

        override fun render() {
            if (enabled && operations.size != 0) super.render()
        }

        override fun calculateBounds() {
            super.calculateBounds()
            enabled = contentSize!!.b.px >= windowSize.b.px
            if (!enabled) {
                shown = false
                return
            }
            properties as ScrollbarProperties
            sized!!.b.px = max((contentSize.b.px / owner.size.b.px) * contentSize.b.px, properties.minimumHeight)
        }
    }

    open class ScrollbarProperties : BlockProperties() {
        override val color: Color = Color(0.5f, 0.5f, 0.5f, 0.5f)
        override val hoverColor = Color(0.5f, 0.5f, 0.5f, 0.75f)
        open val clickColor = Color(0.5f, 0.5f, 0.5f, 0.8f)
        override val padding: Float = 5f
        open val width = 12f
        open val minimumHeight = 20f
        open val showAnimation: Animations? = Animations.EaseOutExpo
        open val showAnimationDuration: Long = 0.5.seconds
    }
}
