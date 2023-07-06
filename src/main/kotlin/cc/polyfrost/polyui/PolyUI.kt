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

package cc.polyfrost.polyui

import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.color.DarkTheme
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.Focusable
import cc.polyfrost.polyui.event.EventManager
import cc.polyfrost.polyui.event.FocusedEvents
import cc.polyfrost.polyui.input.KeyBinder
import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.input.Modifiers.Companion.mods
import cc.polyfrost.polyui.input.PolyTranslator
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.layout.impl.PixelLayout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.Window
import cc.polyfrost.polyui.renderer.data.Cursor
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * # PolyUI
 *
 * PolyUI is a declarative UI framework developed for, and by [Polyfrost](https://polyfrost.cc).
 *
 * It is designed to be lightweight, fast, extensible and easy to use, while still being very powerful. Make sure to check out the examples for more information.
 *
 * PolyUI is split into two parts, being PolyUI and its logic, and it's accompanying [Renderer] implementation. This allows us to have PolyUI in many places, including inside Minecraft!
 * It is **declarative**, meaning that you don't have to worry about the logic of the UI, just the layout and the components.
 *
 *
 * ## Rendering Pipeline
 * PolyUI has the policy of ***'render what you need ONLY WHEN you need it'***.
 * Most of the time, PolyUI will be drawing frame buffers to the screen instead of drawing directly to the screen, as long as they are [suitably complex][cc.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer] for it to be worth it; or [not drawing at all][drew]!
 * This allows us to have a very fast rendering pipeline, and allows us to have a lot of components on screen at once, without a performance hit.
 *
 * Rendering can be [requested][cc.polyfrost.polyui.component.Component.wantRedraw] by components, and if so, it will be rendered during the next frame. This should only be requested if it is necessary, for example to do an animation or something.
 *
 * During a render cycle, PolyUI will systematically go through every layout, and [render][cc.polyfrost.polyui.layout.Layout.reRenderIfNecessary] it to its framebuffer or to the screen. Each layout will then render its components and child layouts, and so on. Rendering happens in three steps:
 *  - [preRender][cc.polyfrost.polyui.component.Component.preRender]: This will do pre-rendering logic, such as setting up transformations, updating animations, and more.
 *  - [render][cc.polyfrost.polyui.component.Component.render]: This is where the actual rendering happens.
 *  - [postRender][cc.polyfrost.polyui.component.Component.postRender]: This will do post-rendering logic, such as cleaning up transformations.
 *
 * Check out [some components][cc.polyfrost.polyui.component.Component] to see how this works.
 *
 * ## How it Works
 * [Components][cc.polyfrost.polyui.component.Drawable] are the interactive parts of the UI, such as buttons, text fields, etc.
 *
 * [Layouts][cc.polyfrost.polyui.layout.Layout] are the containers for components, such as a grid layout, or a flex layout, etc. They are responsible for positioning and sizing the components.
 *
 * [Properties][cc.polyfrost.polyui.property.Properties] are the shared states or tokens for the components. They describe default values, and can be overridden by the components.
 *
 * **Interactions** are driven by [events][EventManager], which thanks to Kotlin's inlining are a zero-overhead way of distributing events, such as [mouse clicks][cc.polyfrost.polyui.event.Events.MouseClicked], or [key presses][cc.polyfrost.polyui.event.FocusedEvents.KeyPressed].
 *
 * PolyUI also supports a variety of [animations][cc.polyfrost.polyui.animate.Animation] and [transitions][cc.polyfrost.polyui.animate.Transition], which can be used to make your UI more dynamic, along with dynamically [adding][addComponent] and [removing][removeComponent] components.
 */
class PolyUI @JvmOverloads constructor(
    translationDirectory: String? = null,
    val renderer: Renderer,
    colors: Colors = DarkTheme(),
    vararg drawables: Drawable
) {
    /** Colors attached to this PolyUI instance. This contains all the colors needed for the UI.
     *
     * **Note that changing this value** can be an expensive operation while the UI is running, as it has to update all the components.
     * @since 0.17.0
     */
    var colors = colors
        set(value) {
            field = value
            timed("Changing colors from $field to $value") {
                master.onAllLayouts {
                    onColorsChanged(value)
                }
                colorChangeListener?.invoke(this)
            }
        }

    /**
     * A listener for color changes PolyUI instance.
     *
     * This lambda function is invoked when the [colors] field is changed on this PolyUI instance, which updates **every** color on the UI.
     *
     * @see PolyUI
     * @since 0.19.2
     */
    var colorChangeListener: (PolyUI.() -> kotlin.Unit)? = null

    /**
     * The window this polyUI was opened with.
     *
     * @since 0.18.3
     */
    lateinit var window: Window

    /**
     * Access the system clipboard as a string.
     *
     * @since 0.18.3
     */
    var clipboard: String?
        get() = window.getClipboard()
        set(value) = window.setClipboard(value)

    /**
     * This is the root layout of the UI. It is the parent of all other layouts.
     */
    val master = PixelLayout(Point(0.px, 0.px), Size(renderer.width.px, renderer.height.px), drawables = drawables, rawResize = true, acceptInput = false)
    val eventManager = EventManager(this)
    val keyBinder = KeyBinder()
    val translator = PolyTranslator(this, translationDirectory ?: "")

    /** weather this PolyUI instance drew on this frame.
     *
     * You can use this value to implement a 'frame skipping' function, if you are using a dual-buffered rendering implementation (like OpenGL/GLFW):
     *
     * `if (polyUI.drew) glfwSwapBuffers(handle)`
     *
     * This is very handy as it will *drastically* reduce required system resources. If you are looking for more efficiency optimizations,
     * see the settings for [max fps][cc.polyfrost.polyui.property.Settings.maxFPS] and [framebuffer settings][cc.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer].
     */
    var drew = false
        private set
    private val clock = Clock()
    private val executors: ArrayList<Clock.Executor> = arrayListOf()
    inline val settings get() = renderer.settings
    inline val mouseX get() = eventManager.mouseX
    inline val mouseY get() = eventManager.mouseY
    inline val mouseDown get() = eventManager.mouseDown

    /**
     * The cursor currently being displayed.
     * @since 0.18.4
     */
    var cursor: Cursor = Cursor.Pointer
        set(value) {
            field = value
            window.setCursor(value)
        }
    var width
        set(value) {
            Unit.VUnits.vWidth = value
            renderer.width = value
        }
        inline get() = renderer.width
    var height
        set(value) {
            Unit.VUnits.vHeight = value
            renderer.height = value
        }
        inline get() = renderer.height
    private var hooks = ConcurrentLinkedQueue<Renderer.() -> kotlin.Unit>()

    /**
     * the time since the last frame, in nanoseconds. It is used internally a lot for animations, etc.
     * @see Clock
     * @see every
     * @see cc.polyfrost.polyui.component.Component.preRender
     * @see cc.polyfrost.polyui.animate.Animation
     */
    var delta: Long = 0L
        private set
    internal var focused: (Focusable)? = null

    // telemetry
    var fps: Int = 1
        private set
    private var frames = 0
    var longestFrame = 0f
        private set
    var shortestFrame = 100f
        private set
    private var timeInFrames = 0f
    var avgFrame = 0f
        private set
    private var perf: String = ""

    init {
        LOGGER.info("PolyUI initializing...")
        master.setup(renderer, this)
        master.simpleName += " [Master]"
        master.calculateBounds()
        if (settings.framebuffersEnabled && settings.masterIsFramebuffer) {
            if (settings.debug) LOGGER.info("Master is using framebuffer")
            master.fbo = renderer.createFramebuffer(width, height)
        }
        master.children.fastEach {
            it.onAllLayouts {
                if (width > this@PolyUI.width || height > this@PolyUI.height) {
                    LOGGER.warn("Layout {} is larger than the window. This may cause issues.", simpleName)
                }
                if (!settings.framebuffersEnabled) return@onAllLayouts
                if (!refuseFramebuffer && settings.minDrawablesForFramebuffer < components.size) {
                    fbo = renderer.createFramebuffer(width, height)
                    if (settings.debug) {
                        LOGGER.info("Layout {} ({} items) created with {}", varargs(simpleName, components.size, fbo))
                    }
                }
            }
        }

        if (settings.draggablesOnTop) {
            // apparently hashsets are faster than lists for this, thanks intellij
            val draggables = master.children.filterTo(HashSet()) { it.draggable }
            master.children.removeAll(draggables)
            master.children.addAll(draggables)
        }

        Unit.VUnits.vHeight = height
        Unit.VUnits.vWidth = width
        if (settings.enableDebugKeybind) {
            keyBinder.add('I', mods = mods(KeyModifiers.LCONTROL, KeyModifiers.LSHIFT)) {
                settings.debug = !settings.debug
                master.needsRedraw = true
                LOGGER.info(
                    "Debug mode {}",
                    if (settings.debug) {
                        frames = 0; "enabled"
                    } else {
                        "disabled"
                    }
                )
            }
        }
        keyBinder.add('R', mods = mods(KeyModifiers.LCONTROL)) {
            LOGGER.info("Reloading PolyUI")
            onResize(width.toInt(), height.toInt(), renderer.pixelRatio, true)
            true
        }
        every(1.seconds) {
            if (settings.debug && drew) {
                perf = "fps: $fps, avg/max/min: ${"%.4f".format(avgFrame)}ms; ${"%.4f".format(longestFrame)}ms; ${"%.4f".format(shortestFrame)}ms"
                longestFrame = 0f
                shortestFrame = 100f
                avgFrame = timeInFrames / fps
                timeInFrames = 0f
                fps = frames
                frames = 0
                LOGGER.info(perf)
            }
        }
        LOGGER.info("PolyUI initialized!")
        if (settings.debug) debugPrint()
    }

    /**
     * Resize this PolyUI instance.
     */
    @Suppress("NAME_SHADOWING")
    fun onResize(newWidth: Int, newHeight: Int, pixelRatio: Float = renderer.pixelRatio, force: Boolean = false) {
        if (newWidth == 0 || newHeight == 0) {
            LOGGER.error("Cannot resize to zero size: {}x{}", newWidth, newHeight)
            return
        }
        if (!force && newWidth == width.toInt() && newHeight == height.toInt() && pixelRatio == this.renderer.pixelRatio) {
            LOGGER.warn("PolyUI was resized to the same size. Ignoring.")
            return
        }

        var newWidth = newWidth
        var newHeight = newHeight
        val (minW, minH) = settings.minimumWindowSize
        val (maxW, maxH) = settings.maximumWindowSize
        if ((minW != -1 && newWidth < minW) || (minH != -1 && newHeight < minH)) {
            LOGGER.warn("Cannot resize to size smaller than minimum: {}x{}", newWidth, newHeight)
            newWidth = minW.coerceAtLeast(newWidth)
            newHeight = minH.coerceAtLeast(newHeight)
            window.width = newWidth
            window.height = newHeight
        }
        if ((maxW != -1 && newWidth > maxW) || (maxH != -1 && newHeight > maxH)) {
            LOGGER.warn("Cannot resize to size larger than maximum: {}x{}", newWidth, newHeight)
            newWidth = maxW.coerceAtMost(newWidth)
            newHeight = maxH.coerceAtMost(newHeight)
            window.width = newWidth
            window.height = newHeight
        }

        val(num, denom) = settings.windowAspectRatio
        if (num != -1 && denom != -1) {
            val aspectRatio = num.toFloat() / denom.toFloat()
            if (newWidth.toFloat() / newHeight.toFloat() != aspectRatio) {
                LOGGER.warn("Cannot resize to size with incorrect aspect ratio: {}x{}, forcing changes, this may cause visual issues!", newWidth, newHeight)
                val newAspectRatio = newWidth.toFloat() / newHeight.toFloat()
                if (newAspectRatio > aspectRatio) {
                    newWidth = (newHeight * aspectRatio).toInt()
                } else {
                    newHeight = (newWidth / aspectRatio).toInt()
                }
                window.width = newWidth
                window.height = newHeight
            }
        }

        if (settings.debug) LOGGER.info("resize: {}x{}", newWidth, newHeight)

        master.calculateBounds()
        master.rescale(
            newWidth.toFloat() / this.width,
            newHeight.toFloat() / this.height
        )

        master.onAllLayouts {
            if (fbo != null) {
                renderer.deleteFramebuffer(fbo)
                fbo = renderer.createFramebuffer(width, height)
            }
            needsRedraw = true // lol that was funny to debug
        }
        this.width = newWidth.toFloat()
        this.height = newHeight.toFloat()
        renderer.pixelRatio = pixelRatio
    }

    fun render() {
        delta = clock.delta
        if (master.needsRedraw) {
            val now = clock.now
            renderer.beginFrame()
            master.reRenderIfNecessary()

            var s: (Renderer.() -> kotlin.Unit)?
            while (hooks.poll().also { s = it } != null) {
                s!!(renderer)
            }

            // telemetry
            if (settings.debug) {
                val frameTime = (System.nanoTime() - now) / 1_000_000f
                timeInFrames += frameTime
                if (frameTime > longestFrame) longestFrame = frameTime
                if (frameTime < shortestFrame) shortestFrame = frameTime
                frames++
                master.debugRender()
                drawDebugOverlay(width - 1f, height - 11f)
            }

            renderer.endFrame()
            drew = true
        } else {
            drew = false
        }
        executors.fastRemoveIf { it.tick(delta) }
    }

    /** draw the debug overlay text. It is right-aligned. */
    fun drawDebugOverlay(x: Float, y: Float) {
        renderer.text(
            Renderer.DefaultFont,
            x,
            y,
            text = perf,
            color = colors.text.primary.normal,
            fontSize = 10f,
            textAlign = TextAlign.Right
        )
    }

    /**
     * add a function to be executed on the main thread, after rendering the UI. a renderer is provided if you want to do any rendering.
     * @since 0.18.5
     */
    fun addHook(func: Renderer.() -> kotlin.Unit) {
        hooks.add(func)
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

    /** add a function that is called every [nanos] nanoseconds. */
    fun every(nanos: Long, repeats: Int = 0, func: () -> kotlin.Unit): PolyUI {
        executors.add(Clock.FixedTimeExecutor(nanos, repeats, func))
        return this
    }

    /**
     * Add a function that is called every [nanos] nanoseconds until [forNanos] nanoseconds have passed.
     * @since 0.18.1
     */
    fun until(nanos: Long, forNanos: Long, func: () -> kotlin.Unit): PolyUI {
        executors.add(Clock.UntilExecutor(nanos, forNanos, func))
        return this
    }

    /**
     * Add a function that is called every [everyNanos] nanoseconds until [condition] returns true.
     * @since 0.18.1
     */
    fun doWhile(everyNanos: Long, condition: () -> Boolean, func: () -> kotlin.Unit): PolyUI {
        executors.add(Clock.ConditionalExecutor(everyNanos, condition, func))
        return this
    }

    /**
     * Add a function that is called after [timeNanos] nanoseconds.
     * @since 0.18.3
     */
    fun after(timeNanos: Long, func: () -> kotlin.Unit): PolyUI {
        executors.add(Clock.AfterExecutor(timeNanos, func))
        return this
    }

    /**
     * Return all components in the given rectangle.
     * @since 0.17.4
     */
    fun getComponentsIn(x: Float, y: Float, width: Float, height: Float): ArrayList<Component> {
        val list = ArrayList<Component>()
        master.onAll(true) {
            if (x < trueX + this.width && x + width > trueX && y < trueY + this.height && y + height > trueY) {
                list.add(this)
            }
        }
        return list
    }

    /**
     * Sets the focus to the specified focusable element.
     *
     * @param focusable the element to set focus on
     * @return true if focus was successfully set, false if the provided focusable is already focused
     */
    fun focus(focusable: Focusable?): Boolean {
        if (focusable === focused) return false
        focused?.accept(FocusedEvents.FocusLost)
        focused = focusable
        focused?.accept(FocusedEvents.FocusGained)
        return true
    }

    fun unfocus() = focus(null)

    /**
     * print this PolyUI instance's components and children in a list, like this:
     * ```
     * PolyUI(800.0 x 800.0) with 0 components and 2 layouts:
     *   PixelLayout@5bcab519 [Draggable](20.0x570.0, 540.0 x 150.0)
     * 	 Text@6eebc39e(40.0x570.0, 520.0x32.0)
     * 	 Block@2f686d1f(40.0x600.0, 120.0x120.0)
     * 	 Block@7085bdee(220.0x600.0, 120.0x120.0)
     * 	 Image@6fd02e5(400.0x600.0, 120.0x120.0 (auto))
     * 	 ... 2 more
     *   FlexLayout@73846619 [Scrollable](20.0x30.0, 693.73267 x 409.47168, buffered, needsRedraw)
     * 	 Block@32e6e9c3(20.0x30.0, 61.111263x42.21167)
     * 	 Block@5056dfcb(86.11127x30.0, 40.909004x76.32132)
     * 	 Block@6574b225(132.02026x30.0, 52.75415x52.59597)
     * 	 Block@2669b199(189.77441x30.0, 76.59671x45.275665)
     * ```
     * The string is also returned.
     */
    fun debugPrint(): String {
        val sb = StringBuilder()
        sb.append(toString()).append(" with ${master.components.size} components and ${master.children.size} layouts:")
        master.components.fastEach {
            sb.append("\n\t").append(it.toString())
        }
        master.children.fastEach {
            debugPrint(it, 0, sb)
        }
        return sb.toString().stdout()
    }

    private fun debugPrint(it: Layout, depth: Int, sb: StringBuilder) {
        sb.append("\n").append("\t", depth).append(it.toString())
        var i = 0
        it.children.fastEach {
            debugPrint(it, depth + 1, sb)
        }
        it.components.fastEach { c ->
            sb.append("\n").append("\t", depth + 1).append(c.toString())
            i++
            if (i >= 10) {
                sb.append("\n").append("\t", depth + 2).append("... ").append(it.components.size - i).append(" more")
                return
            }
        }
    }

    /**
     * Time the [block] and return how long it took, as well as logging with the [msg] if [debug][cc.polyfrost.polyui.property.Settings.debug] is active.
     * @since 0.18.5
     */
    fun timed(msg: String? = null, block: () -> kotlin.Unit): Long {
        if (msg != null && settings.debug) LOGGER.info(msg)
        val now = System.nanoTime()
        block()
        val time = System.nanoTime() - now
        if (settings.debug) LOGGER.info("${if (msg != null) "\t\t> " else ""}took ${time / 1_000_000.0}ms")
        return time
    }

    override fun toString() = "PolyUI($width x $height)"

    /** cleanup the polyUI instance. This will delete all resources, and render this instance unusable. */
    fun cleanup() {
        renderer.cleanup()
        focused = null
        hooks.clear()
        master.children.clear()
        master.components.clear()
    }

    companion object {
        @JvmField
        val LOGGER: Logger = LoggerFactory.getLogger("PolyUI")

        /**
         * State 0 of initialization. Nothing has been done yet.
         * @since 0.19.0
         * @see INIT_SETUP
         * @see INIT_COMPLETE
         */
        const val INIT_NOT_STARTED = 0

        /**
         * Stage 1 of initialization, where the component has access to a [PolyUI] and [Renderer], and its [Properties][cc.polyfrost.polyui.property.Properties] if it is a component, but has not been calculated.
         * @since 0.19.0
         * @see INIT_NOT_STARTED
         * @see INIT_COMPLETE
         */
        const val INIT_SETUP = 1

        /**
         * Stage 2 of initialization, where the component has been calculated, and is ready to be drawn.
         * @since 0.19.0
         * @see INIT_NOT_STARTED
         * @see INIT_SETUP
         */
        const val INIT_COMPLETE = 2
    }
}
