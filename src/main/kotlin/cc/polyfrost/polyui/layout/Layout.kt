/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout

import cc.polyfrost.polyui.PolyUI
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
import cc.polyfrost.polyui.utils.fastEach
import org.jetbrains.annotations.ApiStatus

/** # Layout
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
    override val at: Point<Unit>,
    override var size: Size<Unit>? = null,
    internal val onAdded: (Drawable.() -> kotlin.Unit)? = null,
    internal val onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    /** If this layout can receive events (separate to its children!). */
    acceptInput: Boolean = false,
    val resizesChildren: Boolean = true,
    vararg items: Drawable
) : Drawable(acceptInput) {
    /** list of components in this layout. */
    open val components = items.filterIsInstance<Component>() as ArrayList

    /** list of child layouts in this layout */
    open val children = items.filterIsInstance<Layout>() as ArrayList

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
    open val removeQueue = arrayListOf<Drawable>()

    /** set this to true if you want this layout to never use a framebuffer. Recommended in situations with chroma colors */
    open var refuseFramebuffer: Boolean = false

    /** framebuffer attached to this layout.
     * @see refuseFramebuffer
     * @see cc.polyfrost.polyui.property.Settings.minItemsForFramebuffer
     */
    open var fbo: Framebuffer? = null // these all have to be open for ptr layout
        set(value) {
            if (!refuseFramebuffer) field = value
        }

    /** reference to parent */
    override var layout: Layout? = null

    init {
        if (onAdded != null) addEventHook(Events.Added, onAdded)
        if (onRemoved != null) addEventHook(Events.Removed, onRemoved)
    }

    /** this is the function that is called every frame. It decides whether the layout needs to be entirely re-rendered.
     * If so, the [render] function is called which will redraw all its children and components.
     *
     * If this layout is [using a framebuffer][cc.polyfrost.polyui.property.Settings.minItemsForFramebuffer], it will be drawn to the framebuffer, and then drawn to the screen.
     *
     * **Note:** Do not call this function yourself.
     */
    open fun reRenderIfNecessary() {
        renderChildren()
        if (fbo != null && fboTracker < 2) {
            if (needsRedraw) {
                if (fboTracker < 2) fboTracker++
                renderer.bindFramebuffer(fbo!!)
                render()
                renderer.unbindFramebuffer(fbo!!)
            }
            renderer.drawFramebuffer(fbo!!, at.a.px, at.b.px, size!!.a.px, size!!.b.px)
        } else {
            val x = at.a.px // fix scrolling issue where objects could move very slightly
            val y = at.b.px
            renderer.pushScissor(x, y, size!!.a.px, size!!.b.px)
            renderer.translate(x, y)
            render()
            renderer.translate(-x, -y)
            renderer.popScissor()
            if (!needsRedraw && fboTracker > 1) {
                needsRedraw = true
                fboTracker = 0
            }
        }
    }

    protected open fun renderChildren() {
        children.fastEach {
            it.reRenderIfNecessary()
        }
    }

    /** perform the given [function] on all this layout's components, and [optionally][onChildLayouts] on all child layouts. */
    open fun onAll(onChildLayouts: Boolean = false, function: Component.() -> kotlin.Unit) {
        components.fastEach { it.function() }
        if (onChildLayouts) children.fastEach { it.onAll(true, function) }
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
                components.add(drawable)
                drawable.layout = this
            }

            is Layout -> {
                children.add(drawable)
                drawable.layout = this
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        drawable.setup(renderer, polyui)
        drawable.calculateBounds()
        drawable.accept(Events.Added)
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
    open fun removeComponentNow(drawable: Drawable) {
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
        if (layout != null) doDynamicSize()
        components.fastEach {
            it.layout = this
            it.calculateBounds()
        }
        children.fastEach {
            it.layout = this
            it.calculateBounds()
        }
        if (this.size == null) {
            this.size = calculateSize()
                ?: throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
        }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        if (resizesChildren) {
            children.fastEach { it.rescale(scaleX, scaleY) }
            components.fastEach { it.rescale(scaleX, scaleY) }
        }
    }

    /** render this layout's components, and remove them if they are ready to be removed. */
    override fun render() {
        removeQueue.fastEach { if (it.canBeRemoved()) removeComponentNow(it) }
        val delta = polyui.delta
        if (components.isEmpty()) return
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
        renderer.drawHollowRect(at.a.px, at.b.px, size!!.a.px, size!!.b.px, polyui.colors.page.border20, 2f)
        renderer.drawText(Renderer.DefaultFont, at.a.px + 1f, at.b.px + 1f, simpleName, polyui.colors.text.primary, 10f)
        children.fastEach { it.debugRender() }
        components.fastEach { it.debugRender() }
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        components.fastEach { it.setup(renderer, polyui) }
        children.fastEach { it.setup(renderer, polyui) }
    }

    override fun canBeRemoved() = !needsRedraw

    final override fun reset() {
        components.fastEach { it.reset() }
        children.fastEach { it.reset() }
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
        return "$simpleName(${trueX}x$trueY, ${size!!.a.px} x ${size!!.b.px}${if (fbo != null) ", buffered" else ""}${if (fbo != null && needsRedraw) ", needsRedraw" else ""})"
    }

    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun items(vararg items: Drawable): Array<out Drawable> {
            return items
        }
    }
}
