/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.animate.transitions.Transition
import cc.polyfrost.polyui.animate.transitions.Transitions
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.layout.impl.extension.DraggableLayout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastEach
import kotlin.math.max

/** a switching layout is a layout that can switch between layouts, with cool animations. */
class SwitchingLayout(
    at: Point<Unit>,
    size: Size<Unit>? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    val transition: Transitions?,
    val transitionDuration: Long = 1000L,
    val defaultIndex: Int = 0,
    vararg layouts: Layout,
    /** this variable also controls for SwitchingLayout how it behaves with a layout that is smaller/larger than this size.
     * If this is true, and this size is not null, it will be scaled accordingly.
     * Else, it will take the size of the largest one, and the smaller ones will be scaled to fit. */
    resizesChildren: Boolean = true
) : PixelLayout(at, size, onAdded, onRemoved, resizesChildren, false) {
    private var goingSwitchOp: Transition? = null
    private var comingSwitchOp: Transition? = null
    private var idx: Int = defaultIndex
    private var current: Layout? = null
    private var next: Layout? = null
    private var autoSized = false
    private var init = false

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

    override fun accept(event: Events): Boolean {
        return if (current?.accept(event) == true) {
            true // todo finish
        } else {
            super.accept(event)
        }
    }

    override fun calculateBounds() {
        children.fastEach {
            it.calculateBounds()
        }
        super.calculateBounds()
        if (autoSized) {
            var width: Float = width
            var height: Float = height
            children.fastEach {
                width = max(width, it.width)
                height = max(height, it.height)
            }
            children.fastEach {
                val sx = it.width / width
                val sy = it.height / height
                it.rescale(sx, sy)
            }
            this.size!!.a.px = width
            this.size!!.b.px = height
        }
    }

    override fun calculateSize(): Vec2<Unit> {
        autoSized = true
        // so I can reuse the logic
        return origin
    }

    override fun debugRender() {
        renderer.drawHollowRect(at.a.px, at.b.px, size!!.a.px, size!!.b.px, Color.GRAYf, 2f)
        renderer.drawText(Renderer.DefaultFont, at.a.px + 1f, at.b.px + 1f, simpleName, Color.WHITE, 10f)
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        init = true
        children.fastEach {
            init(it)
        }
        current = children.getOrNull(defaultIndex)
            ?: throw IllegalArgumentException("SwitchingLayout's default index $defaultIndex has no layout present at initialization!")
    }

    override fun render() {
        if (goingSwitchOp != null) {
            val delta = clock.delta
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

    fun switch(layout: Layout?) {
        if (layout == null) return
        current?.onAdded?.invoke(current!!)
        next = layout
        next!!.setup(renderer, polyui)
        next!!.calculateBounds()
        next!!.onAdded?.invoke(next!!)
        needsRedraw = true
        if (transition == null || current == null) {
            current = next!!
            return
        }
        goingSwitchOp = transition.create(current!!, transitionDuration)
        comingSwitchOp = transition.create(next!!, transitionDuration)
        // update clock so it's not 289381903812
        clock.delta
    }

    fun next() {
        idx++
        if (idx > children.lastIndex) idx = 0
        switch(children[idx])
    }

    fun back() {
        idx--
        if (idx < 0) idx = children.lastIndex
        switch(children[idx])
    }

    fun default() {
        idx = defaultIndex
        switch(children[idx])
    }

    fun addLayouts(vararg layout: Layout) {
        for (l in layout) {
            children.add(init(l))
        }
    }

    private fun init(layout: Layout): Layout {
        if (layout is DraggableLayout) throw IllegalArgumentException("SwitchingLayout cannot contain draggable layouts (${layout.simpleName})!")
        layout.layout = this
        if (!init) return layout
        layout.setup(renderer, polyui)
        layout.calculateBounds()
        return layout
    }

    fun append(layout: Layout) = children.add(init(layout))

    fun addAt(index: Int, layout: Layout) = children.add(index, init(layout))

    fun addNext(layout: Layout) = children.add(idx + 1, init(layout))

    fun remove(layout: Layout) = children.remove(layout)
    fun removeAt(index: Int) = children.removeAt(index)
}
