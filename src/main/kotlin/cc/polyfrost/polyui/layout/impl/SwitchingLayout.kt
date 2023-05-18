/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.animate.transitions.Transition
import cc.polyfrost.polyui.animate.transitions.Transitions
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.Clock

/** a switching layout is a layout that can switch between layouts, with cool animations. */
class SwitchingLayout(
    at: Point<Unit>,
    sized: Size<Unit>? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    val transition: Transitions? = null,
    val transitionDuration: Long = 1000L,
    private var current: Layout? = null,
    resizesChildren: Boolean = true
) : PixelLayout(at, sized, onAdded, onRemoved, resizesChildren, false) {
    override var simpleName: String = "SwitchingLayout@${this.toString().substringAfterLast("@")}"
    private val clock = Clock()
    private var goingSwitchOp: Transition? = null
    private var comingSwitchOp: Transition? = null
    private var next: Layout? = null

    @Deprecated(
        "this method should not be used on a SwitchingLayout, as the targeted layout will vary depending on the current layout, and may switch unexpectedly.",
        replaceWith = ReplaceWith("targetLayout.addComponent(drawable)"),
        DeprecationLevel.ERROR
    )
    override fun addComponent(drawable: Drawable) {
    }

    @Deprecated(
        "this method should not be used on a SwitchingLayout, as the targeted layout will vary depending on the current layout, and may switch unexpectedly.",
        ReplaceWith("targetLayout.removeComponent(drawable)"),
        DeprecationLevel.ERROR
    )
    override fun removeComponent(drawable: Drawable) {
    }

    @Deprecated(
        "this method should not be used on a SwitchingLayout, as the targeted layout will vary depending on the current layout, and may switch unexpectedly.",
        ReplaceWith("targetLayout.onAll(onChildLayouts, function)"),
        DeprecationLevel.ERROR
    )
    override fun onAll(onChildLayouts: Boolean, function: Component.() -> kotlin.Unit) {
    }

    @Deprecated(
        "this method should not be used on a SwitchingLayout, as the targeted layout will vary depending on the current layout, and may switch unexpectedly.",
        ReplaceWith("targetLayout.removeComponentNow(drawable)"),
        DeprecationLevel.ERROR
    )
    override fun removeComponentNow(drawable: Drawable) {
    }

    override fun calculateBounds() {
        super.calculateBounds()
        current?.calculateBounds()
    }

    override fun reRenderIfNecessary() {
        if (current == null) return
        if (goingSwitchOp != null) needsRedraw = true
        super.reRenderIfNecessary()
    }

    override fun render() {
        if (goingSwitchOp != null) {
            val delta = clock.getDelta()
            goingSwitchOp!!.update(delta)
            comingSwitchOp!!.update(delta)
        }

        if (goingSwitchOp != null) {
            goingSwitchOp!!.apply(renderer)
            current!!.render()
            goingSwitchOp!!.unapply(renderer)
            comingSwitchOp!!.apply(renderer)
            next!!.render()
            comingSwitchOp!!.unapply(renderer)
        } else {
            current!!.render()
        }

        if (goingSwitchOp?.isFinished == true) {
            goingSwitchOp = null
            comingSwitchOp = null
            needsRedraw = false
        }
    }

    fun switch(layout: Layout) {
        next = layout
        next!!.calculateBounds()
        needsRedraw = true
        if (transition == null || current == null) {
            current = next!!
            return
        }
        goingSwitchOp = transition.create(current!!, transitionDuration)
        comingSwitchOp = transition.create(next!!, transitionDuration)
        // update clock so it's not 289381903812
        clock.getDelta()
    }
}
