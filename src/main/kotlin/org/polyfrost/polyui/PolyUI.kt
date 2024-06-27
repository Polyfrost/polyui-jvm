/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
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

package org.polyfrost.polyui

import org.apache.logging.log4j.LogManager
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Positioner
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Group
import org.polyfrost.polyui.component.radius
import org.polyfrost.polyui.event.InputManager
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.property.Settings
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.Window
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.*
import kotlin.math.min

// todo rewrite this doc

/**
 * # PolyUI
 *
 * PolyUI is a declarative UI framework developed for, and by [Polyfrost](https://polyfrost.org).
 *
 * It is designed to be lightweight, fast, extensible and easy to use, while still being very powerful. Make sure to check out the examples for more information.
 *
 * PolyUI is split into two parts, being PolyUI and its logic, and it's accompanying [Renderer] implementation. This allows us to have PolyUI in many places, including inside Minecraft!
 * It is **declarative**, meaning that you don't have to worry about the logic of the UI, just the layout and the components.
 *
 *
 * ## Rendering Pipeline
 * PolyUI has the policy of ***'render what you need ONLY WHEN you need it'***.
 * Most of the time, PolyUI will be drawing frame buffers to the screen instead of drawing directly to the screen, as long as they are [suitably complex][org.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer] for it to be worth it; or [not drawing at all][drew]!
 * This allows us to have a very fast rendering pipeline, and allows us to have a lot of components on screen at once, without a performance hit.
 *
 * Rendering can be [requested][org.polyfrost.polyui.component.Drawable.needsRedraw] by components, and if so, it will be rendered during the next frame. This should only be requested if it is necessary, for example to do an animation or something.
 *
 * During a render cycle, PolyUI will systematically go through every layout, and [render][org.polyfrost.polyui.component.Drawable.draw] it to its framebuffer or to the screen. Each layout will then render its components and child layouts, and so on. Rendering happens in three steps:
 *  - [preRender][org.polyfrost.polyui.component.Drawable.preRender]: This will do pre-rendering logic, such as setting up transformations, updating animations, and more.
 *  - [render][org.polyfrost.polyui.component.Drawable.render]: This is where the actual rendering happens.
 *  - [postRender][org.polyfrost.polyui.component.Drawable.postRender]: This will do post-rendering logic, such as cleaning up transformations.
 *
 * Check out [some components][org.polyfrost.polyui.component.impl.Block] to see how this works.
 *
 * ## How it Works
 * [Components][org.polyfrost.polyui.component.Drawable] are the interactive parts of the UI, such as buttons, text fields, etc.
 *
 * **Interactions** are driven by [events][InputManager], which thanks to Kotlin's inlining are a zero-overhead way of distributing events, such as [mouse clicks][org.polyfrost.polyui.event.Event.Mouse.Clicked], or [key presses][org.polyfrost.polyui.event.Event.Focused.KeyPressed].
 *
 * PolyUI also supports a variety of [animations][org.polyfrost.polyui.animate.Animation] and transitions, which can be used to make your UI more dynamic, along with dynamically [adding][Drawable.addChild] and [removing][Drawable.removeChild] components.
 */
class PolyUI(
    vararg drawables: Drawable,
    val renderer: Renderer,
    settings: Settings? = null,
    inputManager: InputManager? = null,
    translator: Translator? = null,
    backgroundColor: PolyColor? = null,
    masterAlignment: Align = Align(cross = Align.Cross.Start, pad = Vec2.ZERO),
    colors: Colors = DarkTheme(),
    size: Vec2 = Vec2.ZERO,
) {
    @JvmOverloads
    constructor(
        vararg drawables: Drawable,
        renderer: Renderer,
        settings: Settings? = null,
        inputManager: InputManager? = null,
        translator: Translator? = null,
        backgroundColor: PolyColor? = null,
        masterAlignment: Align = Align(cross = Align.Cross.Start, pad = Vec2.ZERO),
        colors: Colors = DarkTheme(),
        width: Float, height: Float
    ) : this(drawables = drawables, renderer, settings, inputManager, translator, backgroundColor, masterAlignment, colors, Vec2(width, height))

    init {
        renderer.init()
    }

    val settings = settings ?: Settings()

    /** Colors attached to this PolyUI instance. This contains all the colors needed for the UI.
     *
     * **Note that changing this value** can be an expensive operation while the UI is running, as it has to update all the components.
     * @since 0.17.0
     */
    var colors = colors
//        get() = master.colors
//        set(value) {
//            timed("Changing colors from ${master.colors} to $value") {
//                master.onAllLayouts {
//     changeColors(value)
//                }
//            }
//        }

    /** Fonts attached to this PolyUI instance. This contains all the fonts needed for the UI.
     *
     * **Note that changing this value** can be an expensive operation while the UI is running, as it has to update all the components.
     * @since 0.22.0
     */
    var fonts = defaultFonts
//        get() = master.fonts
//        set(value) {
//            timed("Changing fonts from ${master.fonts} to $value") {
//                master.onAllLayouts {
//                    changeFonts(value)
//                }
//            }
//        }

    /**
     * Monospace font attached to this PolyUI instance. This is used for debugging and other purposes.
     */
    var monospaceFont = PolyUI.monospaceFont

    /**
     * The window this polyUI was opened with.
     *
     * Since `v1.1.3`, a window is not required to be set. If it is not set, the clipboard, cursor and render [hooks][Window.preRender] will not work.
     *
     * @since 0.18.3
     */
    var window: Window? = null
        set(value) {
            if (value == null) return
            if (field === value) return
            if (field != null) {
                if (settings.debug) LOGGER.info("window change: $field -> $value")
                field = value
                resize(value.width.toFloat(), value.height.toFloat(), false)
            } else field = value
        }

    /**
     * returns `true` if render pausing is possible.
     * @see Settings.renderPausingEnabled
     * @since 1.1.3
     */
    inline val canPauseRendering get() = settings.renderPausingEnabled && window?.supportsRenderPausing() == true

    inline val canUseFramebuffers get() = settings.framebuffersEnabled && renderer.supportsFramebuffers()

    inline val pixelRatio get() = window?.pixelRatio ?: 1f

    /**
     * Access the system clipboard as a string.
     *
     * @since 0.18.3
     */
    inline var clipboard: String?
        get() = window?.getClipboard()
        set(value) {
            window?.setClipboard(value)
        }

    /**
     * This is the root layout of the UI. It is the parent of all other layouts.
     */
    val master = if (backgroundColor == null) Group(size = size, children = drawables, alignment = masterAlignment)
    else Block(size = size, children = drawables, alignment = masterAlignment, color = backgroundColor).radius(0f)


    val inputManager = inputManager?.with(master) ?: InputManager(master, KeyBinder(this.settings), this.settings)

    /**
     * Get the [KeyBinder] for this PolyUI instance.
     *
     * This value may be null as the key binder is optional.
     */
    inline val keyBinder get() = inputManager.keyBinder
    val translator = translator ?: Translator(this.settings, "")
    val clock = Clock()
    var positioner: Positioner = Positioner.Default()

    /** weather this PolyUI instance drew on this frame.
     *
     * You can use this value to implement a 'render pausing' function, if you are using a dual-buffered rendering implementation (like OpenGL/GLFW) **by enabling [this flag][Settings.renderPausingEnabled]**:
     *
     * `if (polyUI.drew) glfwSwapBuffers(handle)`
     *
     * This is very handy as it will *drastically* reduce required system resources. If you are looking for more efficiency optimizations,
     * see the settings for [max fps][org.polyfrost.polyui.property.Settings.maxFPS] and [framebuffer settings][org.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer].
     */
    var drew = false
        private set

    inline val mouseX get() = inputManager.mouseX

    inline val mouseY get() = inputManager.mouseY

    inline val mouseDown get() = inputManager.mouseDown

    /**
     * The cursor currently being displayed.
     * @since 0.18.4
     */
    var cursor: Cursor = Cursor.Pointer
        set(value) {
            field = value
            window?.setCursor(value)
        }

    @get:JvmName("getSize")
    inline val size get() = master.size

    /**
     * this property stores the initial size of this PolyUI instance.
     * It is used to make sure that new objects experience the same resizing as others.
     *
     * **note:** access to this too early will result in a size of `1x1`. As this value is used for scaling, this is not a problem.
     * just be careful if you are using this value for anything else.
     *
     * @see Settings.forceSetsInitialSize
     * @see resize
     * @since 1.0.5
     */
    @get:JvmName("getInitialSize")
    var iSize = Vec2.ONE
        get() {
            if (field == Vec2.ONE && master.sizeValid) {
                field = master.size
            }
            return field
        }
        private set(value) {
            if (settings.debug && field != value) LOGGER.info("initial size: $value")
            field = value
        }
    private val executors: ArrayList<Clock.Executor> = ArrayList(4)

    /**
     * the time since the last frame, in nanoseconds. It is used internally a lot for animations, etc.
     * @see Clock
     * @see every
     * @see org.polyfrost.polyui.component.Drawable.preRender
     * @see org.polyfrost.polyui.animate.Animation
     */
    var delta: Long = 0L
        private set

    /**
     * the debugger attached to this PolyUI instance.
     * @see Debugger
     * @since 1.1.7
     */
    val debugger = Debugger(this)

    init {
        LOGGER.info("PolyUI initializing...")
        if (master.children.isNullOrEmpty()) LOGGER.warn("PolyUI initialized with no children!")
        master.simpleName += " [Master]"
        master.setup(this)

        this.keyBinder?.add(
            KeyBinder.Bind('R', mods = mods(KeyModifiers.CONTROL)) {
                LOGGER.info("Reloading PolyUI")
                resize(this.size.x, this.size.y, true)
                true
            },
        )
        if (this.settings.debug) LOGGER.info(debugger.debugString())
        if (this.settings.cleanupOnShutdown) {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    LOGGER.info("Quitting!")
                    this.cleanup()
                },
            )
        }
        if (this.settings.cleanupAfterInit) {
            val currentUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            timed("Running Post-init Cleanup...") {
                Runtime.getRuntime().gc()
            }
            val newUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            LOGGER.info("\t\t> Freed ${(currentUsage - newUsage) / 1024L}KB of memory")
        }
        iSize // initialize the initial size
        LOGGER.info("PolyUI initialized!")
    }

    /**
     * Resize this PolyUI instance.
     */
    @Suppress("NAME_SHADOWING")
    fun resize(newWidth: Float, newHeight: Float, force: Boolean = false) {
        // step 1: check if the new size is valid (not same or zero)
        if (newWidth == 0f || newHeight == 0f) {
            LOGGER.error("Cannot resize to zero size: ${newWidth}x${newHeight}")
            return
        }
        if (!force && newWidth == size.x && newHeight == size.y) {
            LOGGER.warn("PolyUI was resized to the same size. Ignoring.")
            return
        }


        // step 2: check if the new size is within the bounds
        var newWidth = newWidth
        var newHeight = newHeight
        val (minW, minH) = settings.minimumSize
        if (newWidth < minW || newHeight < minH) {
            LOGGER.warn("Cannot resize to size smaller than minimum: ${newWidth}x${newHeight}")
            newWidth = minW.coerceAtLeast(newWidth)
            newHeight = minH.coerceAtLeast(newHeight)
            window?.width = newWidth.toInt()
            window?.height = newHeight.toInt()
        }
        if (!settings.maximumSize.isNegative) {
            val (maxW, maxH) = settings.maximumSize
            if (newWidth > maxW || newHeight > maxH) {
                LOGGER.warn("Cannot resize to size larger than maximum: ${newWidth}x${newHeight}")
                newWidth = maxW.coerceAtMost(newWidth)
                newHeight = maxH.coerceAtMost(newHeight)
                window?.width = newWidth.toInt()
                window?.height = newHeight.toInt()
            }
        }

        val (num, denom) = settings.aspectRatio
        if (num != -1 && denom != -1) {
            val aspectRatio = num.toFloat() / denom.toFloat()
            if (newWidth / newHeight != aspectRatio) {
                LOGGER.warn("Cannot resize to size with incorrect aspect ratio: ${newWidth}x${newHeight}, forcing changes, this may cause visual issues!")
                val newAspectRatio = newWidth / newHeight
                if (newAspectRatio > aspectRatio) {
                    newWidth = (newHeight * aspectRatio)
                    window?.width = newWidth.toInt()
                } else {
                    newHeight = (newWidth / aspectRatio)
                    window?.height = newHeight.toInt()
                }
            }
        }

        if (settings.debug) LOGGER.info("resize: ${newWidth}x$newHeight")
        if (force && settings.forceSetsInitialSize) iSize = Vec2(newWidth, newHeight)

        val sx = newWidth / size.x
        val sy = newHeight / size.y
        master.rescale(sx, sy)
        master.needsRedraw = true
    }

    /**
     * Perform a render cycle.
     * @return *(revised 1.1.7)* the maximum amount of time that can be waited, as long as no events are dispatched, that
     * can be waited before [render] needs to be called again.
     */
    fun render(): Long {
        debugger.nframes++
        delta = clock.delta
        if (master.needsRedraw) {
            renderer.beginFrame(master.width, master.height, pixelRatio)
            window?.preRender(renderer)
            master.draw()
            debugger.takeReadings()
            if (settings.debug) debugger.render()
            window?.postRender(renderer)
            renderer.endFrame()
            drew = true
        } else {
            drew = false
        }
        if (!canPauseRendering) master.needsRedraw = true
        keyBinder?.update(delta, inputManager.mods)
        var wait = Long.MAX_VALUE
        executors.fastRemoveIfReversed l@{
            val o = it.tick(delta)
            if (o < 0L) return@l true
            wait = min(wait, o)
            false
        }
        return if (master.needsRedraw) 0L else wait
    }

    /**
     * add a function that is called every [nanos] nanoseconds.
     *
     * Note that the requested time is not guaranteed to be accurate, especially on small values (less than 5ms).
     * It will be executed at the end of the next frame. The executor will overshoot much more often that it will undershoot.
     *
     * From testing, a requested time of 1 second is about 99.3% accurate. `(+7ms, -0.01ms)`
     *
     * @see removeExecutor
     */
    fun every(nanos: Long, repeats: Int = 0, func: () -> Unit): Clock.Executor {
        val exec = Clock.FixedTimeExecutor(nanos, repeats, func)
        executors.add(exec)
        return exec
    }

    fun addExecutor(executor: Clock.Executor) {
        executors.addIfAbsent(executor)
    }

    /**
     * Remove a clock-based executor from this PolyUI instance.
     * @return `true` if the executor was successfully removed.
     * @see every
     * @since 1.0.5
     */
    fun removeExecutor(executor: Clock.Executor?): Boolean {
        if (executor == null) return false
        return executors.remove(executor)
    }

    /**
     * Sets the focus to the specified focusable element.
     *
     * @param focusable the element to set focus on
     * @return true if focus was successfully set, false if the provided focusable is already focused
     */
    fun focus(focusable: Drawable) {
        if (inputManager.focus(focusable)) master.needsRedraw = true
    }

    fun unfocus() = inputManager.unfocus()

    /**
     * Return the key name for the given key code, or "Unknown" if the key is not mapped / no window is present.
     */
    fun getKeyName(key: Int) = window?.getKeyName(key) ?: "Unknown"

    /**
     * Time the [block] and return how long it took, as well as logging with the [msg] if [debug][org.polyfrost.polyui.property.Settings.debug] is active.
     * @since 0.18.5
     */
    inline fun timed(msg: String? = null, block: () -> Unit) = timed(settings.debug, msg, block)

    override fun toString() = "PolyUI($size)"

    /** cleanup the polyUI instance. This will delete all resources, and render this instance unusable. */
    fun cleanup() {
        unfocus()
        renderer.cleanup()
        executors.clear()
        master.children?.clear()
    }

    companion object {
        @JvmField
        @PublishedApi
        internal val LOGGER = LogManager.getLogger("PolyUI")

        /**
         * If the current OS is detected as macOS
         */
        @JvmField
        val isOnMac = System.getProperty("os.name").contains("mac", true)

        /**
         * Fallback font library bundled with PolyUI
         * @since 0.22.0
         */
        @JvmField
        val defaultFonts = FontFamily("Poppins", "polyui/fonts/poppins/")

        @JvmField
        val monospaceFont = Font("polyui/fonts/JetBrainsMono-Regular.ttf")

        /**
         * The fallback default image bundled with PolyUI
         * @since 0.11.0
         */
        @JvmField
        val defaultImage = PolyImage("polyui/err.png")

        /**
         * Time the [block] and return how long it took, as well as logging with the [msg] if [log] is `true`.
         * @since 1.1.61
         */
        @JvmStatic
        inline fun timed(log: Boolean = true, msg: String? = null, block: () -> Unit): Long {
            if (log && msg != null) LOGGER.info(msg)
            val now = Clock.time
            block()
            val time = Clock.time - now
            if (log) LOGGER.info("${if (msg != null) "\t\t> " else ""}took ${time / 1_000_000.0}ms")
            return time
        }

        const val INPUT_DISABLED: Byte = -1
        const val INPUT_NONE: Byte = 0
        const val INPUT_HOVERED: Byte = 1
        const val INPUT_PRESSED: Byte = 2

        const val DANGER: Byte = 0
        const val WARNING: Byte = 1
        const val SUCCESS: Byte = 2
    }
}
