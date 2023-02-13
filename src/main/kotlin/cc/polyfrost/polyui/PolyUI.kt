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
import cc.polyfrost.polyui.input.KeyBinder
import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.layout.impl.PixelLayout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.px
import cc.polyfrost.polyui.utils.Clock
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
 * ###
 *
 * ## Rendering Pipeline
 * PolyUI has the policy of ***'render what you need ONLY WHEN you need it'***.
 * Most of the time, PolyUI will be drawing frame buffers to the screen instead of drawing directly to the screen, as long as they are [suitably complex][cc.polyfrost.polyui.property.Settings.minItemsForFramebuffer] for it to be worth it.
 * This allows us to have a very fast rendering pipeline, and allows us to have a lot of components on screen at once, without a performance hit.
 *
 * Rendering can be [requested][cc.polyfrost.polyui.component.Component.wantRedraw] by components, and if so, it will be rendered during the next frame. This should only be requested if it is necessary, for example to do an animation or something.
 *
 * During a render cycle, PolyUI will systematically go through every layout, and render it to its framebuffer or to the screen. Each layout will then render its components and child layouts, and so on. Rendering happens in three steps:
 *  - [preRender][cc.polyfrost.polyui.layout.Layout.preRender]: This will do pre-rendering logic, such as setting up transformations, updating animations, and more.
 *  - [render][cc.polyfrost.polyui.layout.Layout.render]: This is where the actual rendering happens.
 *  - [postRender][cc.polyfrost.polyui.layout.Layout.postRender]: This will do post-rendering logic, such as cleaning up transformations.
 *
 * Check out [some components][cc.polyfrost.polyui.component.Component] to see how this works.
 * ###
 * ###
 * ## How it Works
 * [Components][cc.polyfrost.polyui.component.Drawable] are the interactive parts of the UI, such as buttons, text fields, etc.
 *
 * [Layouts][cc.polyfrost.polyui.layout.Layout] are the containers for components, such as a grid layout, or a flex layout, etc. They are responsible for positioning and sizing the components.
 *
 * [Properties][cc.polyfrost.polyui.property.Properties] are the shared states or tokens for the components. They describe default values, and can be overridden by the components.
 *
 * **Interactions** are driven by [events][EventManager], which thanks to Kotlin's inlining are a zero-overhead way of distrbuting events, such as [mouse clicks][cc.polyfrost.polyui.event.Events.MouseClicked], or [key presses][cc.polyfrost.polyui.event.FocusedEvents.KeyPressed].
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
    val keyBinder = KeyBinder()
    private val executors: ArrayList<Clock.FixedTimeExecutor> = arrayListOf()
    private val settings = renderer.settings
    private var renderHooks = arrayListOf<Renderer.() -> kotlin.Unit>()
    internal var focused: (Focusable)? = null

    init {
        master.setup(renderer, this)
        master.calculateBounds()
        master.children.fastEach {
            if (settings.minItemsForFramebuffer > it.children.size + it.components.size) {
                it.fbo = renderer.createFramebuffer(it.width.toInt(), it.height.toInt(), settings.bufferType)
            }
            if (it.width > width || it.height > height) {
                LOGGER.warn("Layout {} is larger than the window. This may cause issues.", it)
            }
        }
        if (settings.masterIsFramebuffer) master.fbo = renderer.createFramebuffer(width, height, settings.bufferType)
        if (this.settings.debugLog) this.master.debugPrint()
        Unit.VUnits.vHeight = height.toFloat()
        Unit.VUnits.vWidth = width.toFloat()
        keyBinder.add('I', KeyModifiers.LCONTROL, KeyModifiers.LSHIFT) {
            settings.debug = !settings.debug
            LOGGER.info("Debug mode {}", if (settings.debug) "enabled" else "disabled")
            true
        }
        keyBinder.add('R', KeyModifiers.LCONTROL) {
            LOGGER.info("Reloading PolyUI")
            onResize(width, height, renderer.pixelRatio, true)
            true
        }
        LOGGER.info("PolyUI initialized")
    }

    fun onResize(newWidth: Int, newHeight: Int, pixelRatio: Float, force: Boolean = false) {
        if (!force && newWidth == width && newHeight == height && pixelRatio == this.renderer.pixelRatio) {
            LOGGER.warn("PolyUI was resized to the same size. Ignoring.")
            return
        }
        if (settings.debug) LOGGER.info("resize: {} x {}", newWidth, newHeight)

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
        executors.fastEach { it.tick() }
    }

    /** add something to be rendered after each frame. */
    fun addRenderHook(func: Renderer.() -> kotlin.Unit) {
        renderHooks.add(func)
    }

    fun removeComponent(drawable: Drawable): PolyUI {
        master.removeComponent(drawable)
        return this
    }

    fun addComponent(drawable: Drawable): PolyUI {
        master.addComponent(drawable)
        return this
    }

    fun addComponents(vararg drawables: Drawable): PolyUI {
        master.addComponents(*drawables)
        return this
    }

    fun addComponents(drawables: Collection<Drawable>): PolyUI {
        master.addComponents(drawables)
        return this
    }

    fun focus(drawable: Focusable) {
        focused?.unfocus()
        focused = drawable
        focused?.focus()
    }

    /** add a function that is called every [millis] milliseconds. */
    fun every(millis: Long, func: () -> kotlin.Unit): PolyUI {
        executors.add(Clock.FixedTimeExecutor(millis, func))
        return this
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
