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
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.ComponentEvent
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
    override var sized: Size<Unit>? = null,
    override var onAdded: (Drawable.() -> kotlin.Unit)? = null,
    override var onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    final override var acceptInput: Boolean = true,
    vararg items: Drawable,
) : Drawable {
    protected open val simpleName = this.toString().substringAfterLast(".")
    val components = arrayListOf(*items.filterIsInstance<Component>().toTypedArray())
    val children = arrayListOf(*items.filterIsInstance<Layout>().toTypedArray())

    protected val removeQueue = arrayListOf<Drawable>()
    var fbo: Framebuffer? = null
    final override lateinit var renderer: Renderer

    /** reference to parent */
    override var layout: Layout? = null

    init {
        if (items.isEmpty()) throw IllegalStateException("Layout cannot be empty!")
        components.fastEach { it.layout = this }
        children.fastEach { it.layout = this }
    }

    var needsRedraw = true
    var needsRecalculation = true

    open fun reRenderIfNecessary() {
        children.fastEach { it.reRenderIfNecessary() }
        if (fbo != null) {
            if (needsRedraw) {
                // TODO framebuffer issues
//                renderer.bindFramebuffer(fbo!!)
                preRender()
                render()
                postRender()
                if (renderer.settings.debug) debugRender()
//                renderer.unbindFramebuffer(fbo!!)
//                needsRedraw = false
            } else renderer.drawFramebuffer(fbo!!, x, y, width, height)
        } else {
            preRender()
            render()
            postRender()
            if (renderer.settings.debug) debugRender()
        }
    }

    /** perform the given [function] on all this layout's components, and [optionally][onChildLayouts] on all child layouts. */
    open fun onAll(onChildLayouts: Boolean = false, function: Drawable.() -> kotlin.Unit) {
        components.fastEach { it.function() }
        if (onChildLayouts) children.fastEach { it.onAll(true, function) }
    }

    /**
     * adds the given components/layouts to this layout.
     *
     * this will add the drawables to the [Layout.components] or [children] list, and invoke its [onAdded] function.
     */
    fun addComponents(components: Collection<Drawable>) {
        components.forEach { addComponent(it) }
    }

    /**
     * adds the given components/layouts to this layout.
     *
     * this will add the drawables to the [Layout.components] or [children] list, and invoke its [onAdded] function.
     */
    fun addComponents(vararg components: Drawable) {
        components.forEach { addComponent(it) }
    }

    /**
     * adds the given component/layout to this layout.
     *
     * this will add the drawable to the [Layout.components] or [children] list, and invoke its [onAdded] function.
     */
    open fun addComponent(drawable: Drawable) {
        when (drawable) {
            is Component -> {
                components.add(drawable)
                drawable.renderer = renderer
                drawable.layout = this
                // allows the properties' onAdded to be called
                drawable.accept(ComponentEvent.Added)
            }

            is Layout -> {
                children.add(drawable)
                drawable.renderer = renderer
                drawable.layout = this
                drawable.onAdded?.invoke(drawable)
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        drawable.calculateBounds()
        needsRedraw = true
    }

    /**
     * removes a component from this layout.
     *
     * this will add the component to the removal queue, and invoke its [onRemoved] function.
     *
     * This removal queue is used so that component can finish up any animations they're doing before being removed.
     */
    open fun removeComponent(drawable: Drawable) {
        when (drawable) {
            is Component -> {
                removeQueue.add(components[components.indexOf(drawable)])
                drawable.accept(ComponentEvent.Removed)
            }

            is Layout -> {
                removeQueue.add(children[children.indexOf(drawable)])
                drawable.onRemoved?.invoke(drawable)
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
    }

    /** removes a component immediately, without waiting for it to finish up.
     *
     * This is marked as internal because you should be using [removeComponent] for most cases to remove a component, as it waits for it to finish and play any removal animations. */
    @ApiStatus.Internal
    fun removeComponentNow(drawable: Drawable) {
        removeQueue.remove(drawable)
        when (drawable) {
            is Component -> {
                if (!components.remove(drawable)) PolyUI.LOGGER.warn("Tried to remove component $drawable from $this, but it wasn't found!")
            }

            is Layout -> {
                if (!children.remove(drawable)) PolyUI.LOGGER.warn("Tried to remove layout $drawable from $this, but it wasn't found!")
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        needsRedraw = true
    }

    override fun calculateBounds() {
        components.fastEach {
            it.calculateBounds()
        }
        children.fastEach {
            it.calculateBounds()
        }
        if (this.sized == null)
            this.sized = getSize()
                ?: throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
        needsRecalculation = false
    }

    override fun preRender() {
        removeQueue.fastEach { if (it.canBeRemoved()) removeComponentNow(it) }
        components.fastEach { it.preRender() }
    }

    override fun render() {
        components.fastEach { it.render() }
    }

    override fun postRender() {
        components.fastEach { it.postRender() }
    }

    override fun debugPrint() {
        println("Layout: $simpleName")
        println("Children: ${children.size}")
        println("Components: ${components.size}")
        println("At: $at")
        println("Sized: $sized")
        println("Needs redraw: $needsRedraw")
        println("Needs recalculation: $needsRecalculation")
        println("FBO: $fbo")
        println("Layout: $layout")
        println()
        children.fastEach { it.debugPrint() }
    }

    override fun debugRender() {
        renderer.drawHollowRect(x, y, width, height, Color.GRAYf, 2)
        renderer.drawText(renderer.defaultFont, x + 1, y + 1, 0f, simpleName, Color.WHITE, 10f)
    }

    /** give this, and all its children, a renderer. */
    fun giveRenderer(renderer: Renderer) {
        if (this::renderer.isInitialized) throw Exception("Renderer already initialized!") // sanity check
        this.renderer = renderer
        components.fastEach { it.renderer = renderer }
        children.fastEach { it.giveRenderer(renderer) }
    }

    override fun canBeRemoved(): Boolean {
        return !needsRedraw
    }

    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun items(vararg items: Drawable): Array<out Drawable> {
            return items
        }
    }
}
