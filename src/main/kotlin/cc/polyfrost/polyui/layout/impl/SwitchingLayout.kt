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

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import cc.polyfrost.polyui.animate.Transition
import cc.polyfrost.polyui.animate.Transitions
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.PropertyManager
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.indexOfOrDie
import kotlin.math.max

/** a switching layout is a layout that can switch between layouts, with cool animations. */
class SwitchingLayout(
    at: Point<Unit>,
    size: Size<Unit>? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    propertyManager: PropertyManager? = null,
    val transition: Transitions?,
    val transitionDuration: Long = 1L.seconds,
    val defaultIndex: Int = 0,
    vararg layouts: Layout,
    resizesChildren: Boolean = true
) : PixelLayout(at, size, onAdded, onRemoved, propertyManager, true, false, resizesChildren) {
    private var goingSwitchOp: Transition? = null
    private var comingSwitchOp: Transition? = null
    private var idx: Int = defaultIndex
    private var current: Layout? = null
    private var next: Layout? = null
    private var autoSized = false

    // children still can have framebuffers, but the layout itself doesn't
    override var refuseFramebuffer = true

    override var needsRedraw: Boolean
        get() = current?.needsRedraw == true || goingSwitchOp != null
        set(value) {
            super.needsRedraw = value
        }

    init {
        addLayouts(*layouts)
    }

    @Deprecated(
        "this method should not be used on a SwitchingLayout, as the targeted layout will vary depending on the current layout, and may switch unexpectedly.",
        replaceWith = ReplaceWith("targetLayout.addComponent(drawable)"),
        DeprecationLevel.WARNING
    )
    override fun addComponent(drawable: Drawable) {
        super.addComponent(drawable)
    }

    @Deprecated(
        "this method should not be used on a SwitchingLayout, as the targeted layout will vary depending on the current layout, and may switch unexpectedly.",
        ReplaceWith("targetLayout.removeComponent(drawable)"),
        DeprecationLevel.WARNING
    )
    override fun removeComponent(drawable: Drawable) {
        super.removeComponent(drawable)
    }

    @Deprecated(
        "this method should not be used on a SwitchingLayout, as the targeted layout will vary depending on the current layout, and may switch unexpectedly.",
        ReplaceWith("targetLayout.removeComponentNow(drawable)"),
        DeprecationLevel.WARNING
    )
    override fun removeComponentNow(drawable: Drawable?) {
        super.removeComponentNow(drawable)
    }

    override fun reRenderIfNecessary() {
        current?.reRenderIfNecessary() ?: throw NullPointerException("${this.simpleName}'s current layout is null!")
    }

    override fun accept(event: Events): Boolean {
        return current?.accept(event) == true
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
            this.width = width
            this.height = height
        }
        children.getOrNull(defaultIndex)
            ?: throw IllegalArgumentException("SwitchingLayout's default index $defaultIndex has no layout present at initialization!")
        switch(defaultIndex)
    }

    override fun calculateSize(): Vec2<Unit> {
        autoSized = true
        // so I can reuse the logic
        return origin
    }

    override fun debugRender() {
        renderer.hollowRect(x, y, width, height, colors.page.border20, 2f)
        renderer.text(Renderer.DefaultFont, x + 1f, y + 1f, simpleName, colors.text.primary, 10f)
    }

//    override fun reRenderIfNecessary() {
//        if (goingSwitchOp != null) {
//            val delta = polyui.delta
//            goingSwitchOp!!.update(delta)
//            comingSwitchOp!!.update(delta)
//        }
//
//        if (goingSwitchOp != null) {
//            goingSwitchOp!!.apply(renderer)
//            current!!.reRenderIfNecessary()
//            goingSwitchOp!!.unapply(renderer)
//            comingSwitchOp!!.apply(renderer)
//            next!!.reRenderIfNecessary()
//            comingSwitchOp!!.unapply(renderer)
//        } else {
//            current!!.reRenderIfNecessary()
//        }
//
//        if (goingSwitchOp?.isFinished == true) {
//            goingSwitchOp = null
//            comingSwitchOp = null
//            needsRedraw = false
//        }
//    }

//    override fun render() {
//    }

    fun switch(index: Int) {
        val layout = children.getOrNull(index) ?: throw IndexOutOfBoundsException("SwitchingLayout has no layout at index $index")
        current?.onAllLayouts {
            components.fastEach {
                it.acceptsInput = false
            }
            acceptsInput = false
        }
        current?.onRemoved?.invoke(current!!)

        layout.acceptsInput = true
        layout.onAllLayouts {
            components.fastEach {
                it.acceptsInput = true
            }
            acceptsInput = true
        }
        layout.onAdded?.invoke(layout)
        needsRedraw = true
        if (transition == null) {
            this.current = layout
            return
        }
        next = layout
        if (current != null) goingSwitchOp = transition.create(current!!, transitionDuration)
        comingSwitchOp = transition.create(layout, transitionDuration)
    }

    fun switch(layout: Layout) = switch(children.indexOfOrDie(layout))

    fun next() {
        idx++
        if (idx > children.lastIndex) idx = 0
        switch(idx)
    }

    fun previous() {
        idx--
        if (idx < 0) idx = children.lastIndex
        switch(idx)
    }

    fun default() {
        idx = defaultIndex
        switch(idx)
    }

    fun addLayouts(vararg layout: Layout) {
        layout.forEach {
            children.add(init(it))
            it.onAllLayouts {
                it.components.fastEach { cmp ->
                    cmp.layout = this
                }
            }
        }
    }

    private fun init(layout: Layout): Layout {
        layout.layout = this
        if (initStage != INIT_NOT_STARTED) {
            layout.setup(renderer, polyui)
        }
        return layout
    }

    operator fun get(index: Int) = children[index]
    operator fun set(index: Int, it: Layout) = addAt(index, it)

    fun append(layout: Layout) = children.add(init(layout))

    fun addAt(index: Int, layout: Layout) = children.add(index, init(layout))

    fun addNext(layout: Layout) = children.add(idx + 1, init(layout))

    @Suppress("DEPRECATION")
    fun remove(layout: Layout) = removeComponent(layout)

    @Suppress("DEPRECATION")
    fun removeAt(index: Int) = removeComponent(children[index])
}
