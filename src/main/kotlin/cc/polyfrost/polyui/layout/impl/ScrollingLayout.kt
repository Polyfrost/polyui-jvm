/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import kotlin.math.max

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
    val scrollbar = Scrollbar(this)

    init {
        layout.acceptsInput = true
        layout.components.add(scrollbar)
    }

    override fun accept(event: Events): Boolean {
        if (event is Events.MouseScrolled) {
            println(event.amountY)
            scrollbar.accept(event)
            return true
        }
        return false
    }

    override fun calculateBounds() {
        super.calculateBounds()
    }

    companion object {
        @JvmField
        var scrollbarProperties = ScrollbarProperties()
    }

    class Scrollbar(private val owner: ScrollingLayout) : Block(
        scrollbarProperties,
        (owner.at + owner.size),
        scrollbarProperties.width.px * 0.px
    ) {
        private val contentSize = owner.ptr.sized
        private val windowSize = owner.size
        var shown = false
            private set(value) {
                if (value == field) return
                field = value
                properties as ScrollbarProperties
                if (value) {
                    this.move(
                        -(this.width + properties.padding),
                        0F,
                        properties.showAnimation,
                        properties.showAnimationDuration
                    )
                } else {
                    this.move(
                        this.width + properties.padding,
                        0F,
                        properties.showAnimation,
                        properties.showAnimationDuration
                    )
                }
            }

        var enabled = false
            private set(value) {
                if (value == field) return
                field = value
                if (!field && shown) shown = false
            }

        init {
            properties as ScrollbarProperties
            at.a.px -= properties.width - properties.padding
            owner.components.add(this)
        }

        override fun accept(event: Events): Boolean {
            if (event is Events.MouseScrolled) {
                println("d")
                return true
            }
            return false
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
