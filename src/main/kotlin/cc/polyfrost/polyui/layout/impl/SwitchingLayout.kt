/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - simple, easy to use and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.DrawableOp
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.Clock

/** a switching layout is a layout that can switch between layouts, with cool animations. */
class SwitchingLayout(
    at: Point<Unit>, sized: Size<Unit>? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    private val defaultIndex: Int = 0,
    private val switchOp: DrawableOp? = null,
    vararg layouts: Layout,
) : PixelLayout(at, sized, onAdded, onRemoved, true, *layouts) {
    private val clock = Clock()
    private var currentIndex = defaultIndex
    private var currentSwitchOp: DrawableOp? = null
    var current: Layout = children[defaultIndex]
        private set
    private var next: Layout? = null
    override fun addComponent(drawable: Drawable) {
        if (drawable !is Layout) throw Exception("SwitchingLayout can only contain Layouts (got $drawable)")
        super.addComponent(drawable)
    }

    override fun reRenderIfNecessary() {
        if (currentSwitchOp != null) {
            currentSwitchOp!!.update(clock.getDelta())
            if (currentSwitchOp!!.finished) {
                currentSwitchOp = null
            }
        }
        current.reRenderIfNecessary()
    }


    fun switch(targetIndex: Int) {
        if (targetIndex !in 0 until children.size) throw IndexOutOfBoundsException("targetIndex out of bounds: $targetIndex")
        if (targetIndex == currentIndex) return
        next = children[targetIndex]
        if (switchOp != null) {
            // todo finish
        }
        // update clock so it's not 289381903812
        clock.getDelta()
        currentIndex = targetIndex
    }

    fun switch(layout: Layout) = switch(children.indexOf(layout))

    fun switchNext() {
        if (currentIndex != children.lastIndex) switch(currentIndex + 1)
        else PolyUI.LOGGER.warn("Tried to switch to next layout, but already at last layout!")
    }
}