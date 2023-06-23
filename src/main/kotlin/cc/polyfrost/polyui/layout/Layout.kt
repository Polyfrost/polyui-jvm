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

package cc.polyfrost.polyui.layout

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import cc.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import cc.polyfrost.polyui.PolyUI.Companion.INIT_SETUP
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.impl.extension.DraggableLayout
import cc.polyfrost.polyui.layout.impl.extension.ScrollingLayout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.*
import org.jetbrains.annotations.ApiStatus

/**
 * # Layout
 * Layout is PolyUI's take on containers. They can contain [components] and other [layouts][children], as children.
 *
 * They can dynamically [add][addComponent] and [remove][removeComponent] their children and components.
 *
 * They are responsible for all their children's sizes, positions and rendering.
 *
 * @see cc.polyfrost.polyui.layout.impl.FlexLayout
 * @see cc.polyfrost.polyui.layout.impl.PixelLayout
 * @see cc.polyfrost.polyui.layout.impl.SwitchingLayout
 */
abstract class Layout(
    at: Point<Unit>,
    override var size: Size<Unit>? = null,
    internal val onAdded: (Drawable.() -> kotlin.Unit)? = null,
    internal val onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    rawResize: Boolean = false,
    /**
     * If this layout resizes its children (**components and layouts!**) when it is resized.
     * @since 0.19.0
     */
    val resizesChildren: Boolean = true,
    /** If this layout can receive events (separate to its children!). */
    acceptInput: Boolean = false,
    vararg drawables: Drawable
) : Drawable(at, rawResize, acceptInput) {
    /** list of components in this layout. */
    open val components = drawables.filterIsInstance<Component>() as ArrayList
        get() = if (enabled) field else EMPTY_CMPLIST

    /** list of child layouts in this layout */
    open val children = drawables.filterIsInstance<Layout>() as ArrayList
        get() = if (enabled) field else EMPTY_CHLDLIST

    /**
     * Weather this layout needs redrawing.
     */
    internal open var needsRedraw = true
        set(value) {
            if (value && !field) {
                layout?.needsRedraw = true
            }
            field = value
        }

    /** tracker variable for framebuffer disabling/enabling. don't touch this. */
    internal open var fboTracker = 0

    /** removal queue of drawables.
     * @see removeComponent
     */
    internal open val removeQueue = arrayListOf<Drawable>()

    /** set this to true if you want this layout to never use a framebuffer. Recommended in situations with chroma colors */
    open var refuseFramebuffer: Boolean = false

    /** framebuffer attached to this layout.
     * @see refuseFramebuffer
     * @see cc.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer
     */
    internal open var fbo: Framebuffer? = null // these all have to be open for ptr layout
        set(value) {
            if (!refuseFramebuffer) field = value
        }

    /** reference to parent */
    override var layout: Layout? = null

    /**
     * This function is executed just before it renders its components and children.
     * @since 0.19.0
     */
    var preRender: (Layout.() -> kotlin.Unit)? = null

    /**
     * This function is executed just after it renders its components and children.
     * @since 0.19.0
     */
    var postRender: (Layout.() -> kotlin.Unit)? = null

    /**
     * This flag simply controls whether the layout exists. If it is false, it will not be rendered, and will report having 0 components.
     *
     * @since 0.19.0
     */
    var enabled = true

    init {
        if (onAdded != null) addEventHandler(Events.Added, onAdded)
        if (onRemoved != null) addEventHandler(Events.Removed, onRemoved)
    }

    /** this is the function that is called every frame. It decides whether the layout needs to be entirely re-rendered.
     * If so, the [render] function is called which will redraw all its' and components, and this function will redraw all its child layouts as well.
     *
     * If this layout is [using a framebuffer][cc.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer], it will be [drawn][rasterize] to the framebuffer, and then drawn to the screen.
     *
     * **Note:** Do not call this function yourself.
     */
    open fun reRenderIfNecessary() {
        if (initStage != INIT_COMPLETE) throw IllegalStateException("${this.simpleName} was attempted to be rendered before it was fully initialized (stage $initStage)")
        if (!enabled) return
        rasterChildren()
        rasterize()
        if (fbo != null && fboTracker < 2) {
            renderer.drawFramebuffer(fbo!!, x, y, width, height)
        } else {
            val x = x
            val y = y
            renderer.pushScissor(x, y, width, height)
            renderer.translate(x, y)
            preRender?.invoke(this)
            render()
            renderChildren()
            postRender?.invoke(this)
            renderer.translate(-x, -y)
            renderer.popScissor()
            if (!needsRedraw && fboTracker > 1) {
                needsRedraw = true
                fboTracker = 0
            }
        }
    }

    /**
     * This function will rasterize the layout to its framebuffer, if it has one.
     * @since 0.18.0
     */
    protected open fun rasterize() {
        if (fbo != null && needsRedraw && fboTracker < 2) {
            fboTracker++
            renderer.bindFramebuffer(fbo)
            render()
            renderChildren()
            renderer.unbindFramebuffer(fbo)
        }
    }

    /**
     * perform the [reRenderIfNecessary] function on this layout's children.
     * @since 0.18.0
     */
    protected open fun renderChildren() {
        children.fastEach {
            it.reRenderIfNecessary()
        }
    }

    /**
     * perform the [rasterize] function on this layout's children.
     * @since 0.18.0
     */
    protected open fun rasterChildren() {
        children.fastEach {
            it.rasterize()
        }
    }

    /** perform the given [function] on all this layout's components, and [optionally][onChildLayouts] on all child layouts. */
    open fun onAll(onChildLayouts: Boolean = false, function: Component.() -> kotlin.Unit) {
        components.fastEach(function)
        if (onChildLayouts) children.fastEach { it.onAll(true, function) }
    }

    /**
     * perform the given [function] on all this layout's children and itself.
     * @since 0.18.0
     */
    open fun onAllLayouts(reversed: Boolean = false, function: Layout.() -> kotlin.Unit) {
        if (reversed) {
            children.fastEachReversed { it.onAllLayouts(true, function) }
        } else {
            children.fastEach { it.onAllLayouts(false, function) }
        }
        function(this)
    }

    /**
     * return a component in this layout, by its simple name. This can be accessed using [debugPrint][cc.polyfrost.polyui.PolyUI.debugPrint] or using the [field][cc.polyfrost.polyui.component.Component.simpleName].
     * @throws IllegalArgumentException if the component does not exist
     * @see getOrNull
     */
    inline operator fun get(simpleName: String) = getOrNull(simpleName) ?: throw IllegalArgumentException("No component found in ${this.simpleName} with ID $simpleName!")

    /**
     * return a component in this layout, by its simple name. This can be accessed using [debugPrint][cc.polyfrost.polyui.PolyUI.debugPrint] or using the [field][cc.polyfrost.polyui.component.Component.simpleName].
     * Returns null if not found.
     * @see get
     */
    fun getOrNull(simpleName: String): Component? {
        components.fastEach { if (it.simpleName == simpleName) return it }
        return null
    }

    /**
     * adds the given components/layouts to this layout.
     *
     * this will add the drawables to the [Layout.components] or [children] list, and invoke its added event function.
     */
    fun addComponents(components: Collection<Drawable>) {
        components.forEach { addComponent(it) }
    }

    /**
     * adds the given components/layouts to this layout.
     *
     * this will add the drawables to the [Layout.components] or [children] list, and invoke its added event function.
     */
    fun addComponents(vararg components: Drawable) {
        components.forEach { addComponent(it) }
    }

    /**
     * adds the given component/layout to this layout.
     *
     * this will add the drawable to the [Layout.components] or [children] list, and invoke its [Events.Added] if it is present.
     */
    open fun addComponent(drawable: Drawable) {
        when (drawable) {
            is Component -> {
                if (components.addOrReplace(drawable) != null) PolyUI.LOGGER.warn("${drawable.simpleName} was attempted to be added to layout ${this.simpleName} multiple times!")
                if (initStage > INIT_NOT_STARTED) drawable.layout = this
            }

            is Layout -> {
                if (children.addOrReplace(drawable) != null) PolyUI.LOGGER.warn("${drawable.simpleName} was attempted to be added to layout ${this.simpleName} multiple times!")
                if (initStage > INIT_NOT_STARTED) drawable.layout = this
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        if (initStage > INIT_NOT_STARTED) drawable.setup(renderer, polyui)
        if (initStage == INIT_COMPLETE) {
            drawable.calculateBounds()
            drawable.accept(Events.Added)
        }
        needsRedraw = true
    }

    /**
     * removes a component from this layout.
     *
     * this will add the component to the removal queue, and invoke its removal event function.
     *
     * This removal queue is used so that component can finish up any animations they're doing before being removed.
     */
    open fun removeComponent(drawable: Drawable) {
        when (drawable) {
            is Component -> {
                removeQueue.add(components[components.indexOf(drawable)])
            }

            is Layout -> {
                removeQueue.add(children[children.indexOf(drawable)])
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        drawable.accept(Events.Removed)
    }

    /** removes a component immediately, without waiting for it to finish up.
     *
     * This is marked as internal because you should be using [removeComponent] for most cases to remove a component, as it waits for it to finish and play any removal animations.
     */
    @ApiStatus.Internal
    open fun removeComponentNow(drawable: Drawable?) {
        if (drawable == null) return
        removeQueue.remove(drawable)
        when (drawable) {
            is Component -> {
                if (!components.remove(drawable)) {
                    PolyUI.LOGGER.warn(
                        "Tried to remove component {} from {}, but it wasn't found!",
                        drawable,
                        this
                    )
                }
            }

            is Layout -> {
                if (!children.remove(drawable)) {
                    PolyUI.LOGGER.warn(
                        "Tried to remove layout {} from {}, but it wasn't found!",
                        drawable,
                        this
                    )
                }
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        needsRedraw = true
    }

    override fun calculateBounds() {
        if (initStage == INIT_NOT_STARTED) throw IllegalStateException("${this.simpleName} has not been setup, but calculateBounds() was called!")
        if (layout != null) doDynamicSize()
        components.fastEach {
            it.calculateBounds()
        }
        children.fastEach {
            it.calculateBounds()
        }
        if (this.size == null) {
            this.size = calculateSize()
                ?: throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
        }
        if (initStage != INIT_COMPLETE) {
            initStage = INIT_COMPLETE
            onInitComplete()
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        if (resizesChildren) {
            children.fastEach { it.rescale(scaleX, scaleY) }
            components.fastEach { it.rescale(scaleX, scaleY) }
        } else {
            children.fastEach { it.rescale(1f, 1f) }
            components.fastEach { it.rescale(1f, 1f) }
        }
    }

    /** render this layout's components, and remove them if they are ready to be removed. */
    override fun render() {
        removeQueue.fastEach { if (it.canBeRemoved()) removeComponentNow(it) }
        val delta = polyui.delta
        needsRedraw = false
        components.fastEach {
            it.preRender(delta)
            it.render()
            it.postRender()
        }
    }

    /** count the amount of drawables this contains, including the drawables its children have */
    fun countDrawables(): Int {
        var i = 0
        children.fastEach { i += it.countDrawables() }
        i += components.size
        return i
    }

    override fun debugPrint() {
        println("Layout: $simpleName")
        println("Children: ${children.size}")
        println("Components: ${components.size}")
        println("At: $at")
        println("Size: $size")
        println("Needs redraw: $needsRedraw")
        println("FBO: $fbo")
        println("Layout: $layout")
        println()
        children.fastEach { it.debugPrint() }
    }

    override fun debugRender() {
        if (!enabled) return
        renderer.drawHollowRect(trueX, trueY, width, height, polyui.colors.page.border20, 2f)
        renderer.drawText(Renderer.DefaultFont, trueX + 1f, trueY + 1f, simpleName, polyui.colors.text.primary, 10f)
        children.fastEach { it.debugRender() }
        components.fastEach { it.debugRender() }
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        components.fastEach {
            it.layout = this
            it.setup(renderer, polyui)
        }
        children.fastEach {
            it.layout = this
            it.setup(renderer, polyui)
        }
        initStage = INIT_SETUP
    }

    override fun canBeRemoved() = !needsRedraw

    final override fun reset() {
        components.fastEach { it.reset() }
        children.fastEach { it.reset() }
    }

    /**
     * add a function that is called every [nanos] nanoseconds.
     * @since 0.17.1
     */
    fun every(nanos: Long, repeats: Int = 0, func: Layout.() -> kotlin.Unit): Layout {
        polyui.every(nanos, repeats) {
            func(this)
        }
        return this
    }

    /** wraps this layout in a [DraggableLayout] (so you can drag it) */
    fun draggable(): DraggableLayout {
        if (this is DraggableLayout) return this
        return DraggableLayout(this)
    }

    /** wraps this layout in a [ScrollingLayout] (so you can scroll it)
     * @param size the scrollable area to set. This is the physical size of the layout on the screen. Set either to `0` for it to be set to the same as the layout size. If either is larger than the content size, it will be trimmed.
     */
    fun scrolling(size: Size<Unit>): ScrollingLayout {
        if (this is ScrollingLayout) return this
        return ScrollingLayout(this, size)
    }

    override fun toString(): String {
        return "$simpleName(${trueX}x$trueY, ${width}x${height}${if (fbo != null) ", buffered" else ""}${if (fbo != null && needsRedraw) ", needsRedraw" else ""})"
    }

    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun drawables(vararg drawables: Drawable): Array<out Drawable> {
            return drawables
        }

        @JvmField
        val EMPTY_CMPLIST = ArrayList<Component>(0)

        @JvmField
        val EMPTY_CHLDLIST = ArrayList<Layout>(0)
    }
}
