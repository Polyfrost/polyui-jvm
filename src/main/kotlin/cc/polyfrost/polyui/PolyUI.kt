/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui

import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.Focusable
import cc.polyfrost.polyui.event.EventManager
import cc.polyfrost.polyui.layout.impl.PixelLayout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.px
import cc.polyfrost.polyui.utils.fastEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * # PolyUI
 *
 * PolyUI is a declarative UI framework developed for, and by [Polyfrost](https://polyfrost.cc).
 *
 * It is designed to be lightweight, fast, extensible and easy to use, while still being very powerful. Make sure to check out the examples for more information.
 *
 * PolyUI is split into two parts, being PolyUI and its logic, and it's accompanying [Renderer] implementation. This allows us to have PolyUI in many places, including inside Minecraft!
 * It is **declarative**, meaning that you don't have to worry about the logic of the UI, just the layout and the components.
 * ###
 *
 * ## How it Works
 * [Components][cc.polyfrost.polyui.component.Drawable] are the interactive parts of the UI, such as buttons, text fields, etc.
 *
 * [Layouts][cc.polyfrost.polyui.layout.Layout] are the containers for components, such as a grid layout, or a flex layout, etc. They are responsible for positioning and sizing the components.
 *
 * [Properties][cc.polyfrost.polyui.property.Properties] are the shared states or tokens for the components. They describe default values, and can be overridden by the components.
 *
 * **Interactions** are driven by [events][EventManager], which thanks to Kotlin's inlining are a zero-overhead way of distrbuting events, such as [mouse clicks][cc.polyfrost.polyui.event.ComponentEvent.MouseClicked], or [key presses][cc.polyfrost.polyui.event.FocusedEvent.KeyPressed].
 *
 * PolyUI also supports a variety of [animations][cc.polyfrost.polyui.animate.Animation] and [transitions][cc.polyfrost.polyui.animate.transitions.Transition], which can be used to make your UI more dynamic, along with dynamically [adding][addComponent] and [removing][removeComponent] components.
 */
class PolyUI(
    width: Int,
    height: Int,
    val renderer: Renderer,
    vararg items: Drawable
) {
    var width = width
        private set
    var height = height
        private set
    val master = PixelLayout(Point(0.px, 0.px), Size(width.px, height.px), items = items)
    val eventManager = EventManager(this)
    private val settings = renderer.settings
    private var renderHooks = arrayListOf<Renderer.() -> kotlin.Unit>()
    internal var focused: (Focusable)? = null

    init {
        master.giveRenderer(renderer)
        master.calculateBounds()
        master.children.fastEach {
            if (settings.minItemsForFramebuffer > it.children.size + it.components.size) {
                it.fbo = renderer.createFramebuffer(it.width.toInt(), it.height.toInt(), settings.bufferType)
            }
            if (it.width > width || it.height > height) {
                LOGGER.warn("Layout $it is larger than the window. This may cause issues.")
            }
        }
        if (settings.masterIsFramebuffer) master.fbo = renderer.createFramebuffer(width, height, settings.bufferType)
        if (this.settings.debugLog) this.master.debugPrint()
        Unit.VUnits.vHeight = height.toFloat()
        Unit.VUnits.vWidth = width.toFloat()
        LOGGER.info("PolyUI initialized")
    }

    fun onResize(newWidth: Int, newHeight: Int, pixelRatio: Float) {
        if (newWidth == width && newHeight == height && pixelRatio == this.renderer.pixelRatio) {
            LOGGER.warn("PolyUI was resized to the same size. Ignoring.")
            return
        }
        if (settings.debug) LOGGER.info("resize: $newWidth x $newHeight")

        master.sized!!.a.px = newWidth.toFloat()
        master.sized!!.b.px = newHeight.toFloat()
        Unit.VUnits.vHeight = newHeight.toFloat()
        Unit.VUnits.vWidth = newWidth.toFloat()
        master.calculateBounds()
        if (settings.masterIsFramebuffer) {
            renderer.deleteFramebuffer(master.fbo!!)
            master.fbo = renderer.createFramebuffer(newWidth, newHeight, settings.bufferType)
        }

        master.children.fastEach {
            it.rescale(
                newWidth.toFloat() / this.width.toFloat(),
                newHeight.toFloat() / this.height.toFloat()
            )
            if (it.fbo != null) {
                renderer.deleteFramebuffer(it.fbo!!)
                it.fbo = renderer.createFramebuffer(it.width.toInt(), it.height.toInt(), settings.bufferType)
            }
        }
        this.width = newWidth
        this.height = newHeight
        renderer.pixelRatio = pixelRatio
    }

    fun render() {
        renderer.beginFrame(width, height)
        master.reRenderIfNecessary()
        renderHooks.fastEach { it(renderer) }
        renderer.endFrame()
    }

    /** add something to be rendered after each frame. */
    fun addRenderHook(func: Renderer.() -> kotlin.Unit) {
        renderHooks.add(func)
    }

    fun removeComponent(drawable: Drawable) {
        master.removeComponent(drawable)
    }

    fun addComponent(drawable: Drawable) {
        master.addComponent(drawable)
    }

    fun addComponents(vararg drawables: Drawable) {
        master.addComponents(*drawables)
    }

    fun addComponents(drawables: Collection<Drawable>) {
        master.addComponents(drawables)
    }

    fun focus(drawable: Focusable) {
        focused?.unfocus()
        focused = drawable
        focused?.focus()
    }

    /** cleanup the polyUI instance. This will delete all resources, and render this instance unusable. */
    fun cleanup() {
        renderer.cleanup()
        focused = null
        renderHooks.clear()
        master.children.clear()
        master.components.clear()
        Unit.VUnits.vHeight = 0f
        Unit.VUnits.vWidth = 0f
    }

    companion object {
        @JvmField
        val LOGGER: Logger = LoggerFactory.getLogger("PolyUI").also { it.info("PolyUI initializing") }
    }
}
