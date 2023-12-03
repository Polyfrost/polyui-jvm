/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Focusable
import org.polyfrost.polyui.component.Positioner
import org.polyfrost.polyui.component.impl.Group
import org.polyfrost.polyui.event.EventManager
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Modifiers
import org.polyfrost.polyui.input.Modifiers.Companion.mods
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.property.Settings
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.Window
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max
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
 * Rendering can be [requested][org.polyfrost.polyui.component.Component.wantRedraw] by components, and if so, it will be rendered during the next frame. This should only be requested if it is necessary, for example to do an animation or something.
 *
 * During a render cycle, PolyUI will systematically go through every layout, and [render][org.polyfrost.polyui.layout.Layout.reRenderIfNecessary] it to its framebuffer or to the screen. Each layout will then render its components and child layouts, and so on. Rendering happens in three steps:
 *  - [preRender][org.polyfrost.polyui.component.Component.preRender]: This will do pre-rendering logic, such as setting up transformations, updating animations, and more.
 *  - [render][org.polyfrost.polyui.component.Component.render]: This is where the actual rendering happens.
 *  - [postRender][org.polyfrost.polyui.component.Component.postRender]: This will do post-rendering logic, such as cleaning up transformations.
 *
 * Check out [some components][org.polyfrost.polyui.component.Component] to see how this works.
 *
 * ## How it Works
 * [Components][org.polyfrost.polyui.component.Drawable] are the interactive parts of the UI, such as buttons, text fields, etc.
 *
 * [Layouts][org.polyfrost.polyui.layout.Layout] are the containers for components, such as a grid layout, or a flex layout, etc. They are responsible for positioning and sizing the components.
 *
 * [Properties][org.polyfrost.polyui.property.Properties] are the shared states or tokens for the components. They describe default values, and can be overridden by the components.
 *
 * **Interactions** are driven by [events][EventManager], which thanks to Kotlin's inlining are a zero-overhead way of distributing events, such as [mouse clicks][org.polyfrost.polyui.event.MouseClicked], or [key presses][org.polyfrost.polyui.event.FocusedEvent.KeyPressed].
 *
 * PolyUI also supports a variety of [animations][org.polyfrost.polyui.animate.Animation] and transitions, which can be used to make your UI more dynamic, along with dynamically [adding][Layout.add] and [removing][Layout.remove] components.
 */
class PolyUI @JvmOverloads constructor(
    val renderer: Renderer,
    settings: Settings? = null,
    eventManager: EventManager? = null,
    translator: Translator? = null,
    colors: Colors = DarkTheme(),
    vararg drawables: Drawable,
) {
    val settings = settings ?: Settings()

    init {
        require(renderer.size > 0f) { "width/height must be greater than 0 (${renderer.size})" }
        renderer.settings = this.settings
        renderer.init()
    }

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

    /** Fonts attached to this PolyUI instance. This contains all the colors needed for the UI.
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
    val master = Group(at = Vec2(), size = renderer.size, children = drawables, alignment = Align(mode = Align.Mode.Vertical))
    val eventManager = eventManager?.with(master.children) ?: EventManager(master.children, KeyBinder(this.settings), this.settings)
    inline val keyBinder get() = eventManager.keyBinder
    val translator = translator ?: Translator(this.settings, "")
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
    private val clock = Clock()
    private val executors: LinkedList<Clock.Executor> = LinkedList()
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
    inline val size: Vec2 get() = renderer.size
    private val hooks = LinkedList<Renderer.() -> Boolean>()
    private val preHooks = LinkedList<Renderer.() -> Boolean>()

    /**
     * the time since the last frame, in nanoseconds. It is used internally a lot for animations, etc.
     * @see Clock
     * @see every
     * @see org.polyfrost.polyui.component.Component.preRender
     * @see org.polyfrost.polyui.animate.Animation
     */
    var delta: Long = 0L
        private set

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
        master.simpleName += " [Master]"
        master.setup(renderer, this)
        @Suppress("NAME_SHADOWING")
        val settings = this.settings
//        if (settings.framebuffersEnabled && renderer.supportsFramebuffers()) {
//            if (settings.isMasterFrameBuffer) {
//                if (settings.debug) LOGGER.info("Master is using framebuffer")
//                master.fbo = renderer.createFramebuffer(width, height)
//            }
//            master.children?.fastEach {
//                if (it.refuseFramebuffer || (it.children?.size ?: 0) < settings.minDrawablesForFramebuffer) return@fastEach
//                it.fbo = renderer.createFramebuffer(it.visibleWidth, it.visibleHeight)
//                if (settings.debug) LOGGER.info("Layout ${it.simpleName} (${it.children?.size} items) created with ${it.fbo}")
//            }
//        }


        if (settings.enableDebugKeybind) {
            this.keyBinder?.add(
                KeyBinder.Bind('I', mods = mods(KeyModifiers.LCONTROL, KeyModifiers.LSHIFT)) {
                    settings.debug = !settings.debug
                    // master.needsRedraw = true
                    LOGGER.info(
                        "Debug mode {}",
                        if (settings.debug) {
                            frames = 0
                            "enabled"
                        } else {
                            "disabled"
                        },
                    )
                    true
                },
            )
        }
        this.keyBinder?.add(
            KeyBinder.Bind('R', mods = mods(KeyModifiers.LCONTROL)) {
                LOGGER.info("Reloading PolyUI")
                resize(size.width, size.height, renderer.pixelRatio, true)
                true
            },
        )
//        var td = System.nanoTime()
        every(1.seconds) {
//            val diff = (System.nanoTime() - td) / 1_000_000_000f
//            td = System.nanoTime()
//            LOGGER.info("took $diff sec (accuracy = ${100f - (diff - 1f) * 100f}%)")
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
        if (settings.debug) debugPrint()
        if (settings.cleanupOnShutdown) {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    LOGGER.info("Quitting!")
                    this.cleanup()
                },
            )
        }
        if (settings.cleanupAfterInit) {
            val currentUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            timed("Running Post-init Cleanup...") {
                Runtime.getRuntime().gc()
            }
            val newUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            LOGGER.info("\t\t> Freed {}KB of memory", (currentUsage - newUsage) / 1024L)
        }
        LOGGER.info("PolyUI initialized!")
    }

    /**
     * Resize this PolyUI instance.
     */
    @Suppress("NAME_SHADOWING")
    fun resize(newWidth: Float, newHeight: Float, pixelRatio: Float = renderer.pixelRatio, force: Boolean = false) {
        if (newWidth == 0f || newHeight == 0f) {
            LOGGER.error("Cannot resize to zero size: {}x{}", newWidth, newHeight)
            return
        }
        if (!force && newWidth == size.x && newHeight == size.height && pixelRatio == this.renderer.pixelRatio) {
            LOGGER.warn("PolyUI was resized to the same size. Ignoring.")
            return
        }

        renderer.pixelRatio = pixelRatio
        var newWidth = newWidth
        var newHeight = newHeight
        val (minW, minH) = settings.minimumWindowSize
        val (maxW, maxH) = settings.maximumWindowSize
        if ((minW != -1 && newWidth < minW) || (minH != -1 && newHeight < minH)) {
            LOGGER.warn("Cannot resize to size smaller than minimum: {}x{}", newWidth, newHeight)
            newWidth = minW.toFloat().coerceAtLeast(newWidth)
            newHeight = minH.toFloat().coerceAtLeast(newHeight)
            window.width = newWidth.toInt()
            window.height = newHeight.toInt()
        }
        if ((maxW != -1 && newWidth > maxW) || (maxH != -1 && newHeight > maxH)) {
            LOGGER.warn("Cannot resize to size larger than maximum: {}x{}", newWidth, newHeight)
            newWidth = maxW.toFloat().coerceAtMost(newWidth)
            newHeight = maxH.toFloat().coerceAtMost(newHeight)
            window.width = newWidth.toInt()
            window.height = newHeight.toInt()
        }

        val (num, denom) = settings.windowAspectRatio
        if (num != -1 && denom != -1) {
            val aspectRatio = num.toFloat() / denom.toFloat()
            if (newWidth / newHeight != aspectRatio) {
                LOGGER.warn("Cannot resize to size with incorrect aspect ratio: {}x{}, forcing changes, this may cause visual issues!", newWidth, newHeight)
                val newAspectRatio = newWidth / newHeight
                if (newAspectRatio > aspectRatio) {
                    newWidth = (newHeight * aspectRatio)
                } else {
                    newHeight = (newWidth / aspectRatio)
                }
                window.width = newWidth.toInt()
                window.height = newHeight.toInt()
            }
        }

        if (settings.debug) LOGGER.info("resize: {}x{}", newWidth, newHeight)

//        master.rescale(
//            newWidth / this.size.x,
//            newHeight / this.size.y,
//        )

//        master.onAllLayouts {
//            if (fbo != null) {
//                renderer.delete(fbo)
//                fbo = renderer.createFramebuffer(width, height)
//            }
//            needsRedraw = true // lol that was funny to debug
//        }
        this.size.x = newWidth
        this.size.y = newHeight
    }

    fun render() {
        delta = clock.delta
//        if (master.needsRedraw) {
        val now = clock.now
        renderer.beginFrame()
        preHooks.fastRemoveIfReversed { it(renderer) }
        master.draw()

        hooks.fastRemoveIfReversed { it(renderer) }

        // telemetry
        if (settings.debug) {
            val frameTime = (System.nanoTime() - now) / 1_000_000f
            timeInFrames += frameTime
            if (frameTime > longestFrame) longestFrame = frameTime
            if (frameTime < shortestFrame) shortestFrame = frameTime
            frames++
            drawDebugOverlay(0f, size.y - 11f)
            if (!eventManager.hasFocused) {
                if (eventManager.keyModifiers.hasModifier(Modifiers.LCONTROL)) {
                    val obj = eventManager.mouseOver ?: eventManager.primaryCandidate
                    if (obj != null) {
                        val os = obj.toString()
                        val w = renderer.textBounds(monospaceFont, os, 10f).width
                        val pos = min(max(0f, mouseX - w / 2f), this.size.x - w - 10f)
                        renderer.rect(pos, mouseY - 14f, w + 10f, 14f, colors.component.bg.hovered)
                        renderer.text(monospaceFont, pos + 5f, mouseY - 10f, text = os, colors.text.primary.normal, 10f)
//                            master.needsRedraw = true
                    }
                }
                if (eventManager.keyModifiers.hasModifier(Modifiers.LSHIFT)) {
                    val s = "${eventManager.mouseX}x${eventManager.mouseY}"
                    val ww = renderer.textBounds(monospaceFont, s, 10f).width
                    val ppos = min(mouseX + 10f, this.size.y - ww - 10f)
                    renderer.rect(ppos, mouseY, ww + 10f, 14f, colors.component.bg.hovered)
                    renderer.text(monospaceFont, ppos + 5f, mouseY + 4f, text = s, colors.text.primary.normal, 10f)
//                        master.needsRedraw = true
                }
            }
        }

        renderer.endFrame()
        drew = true
//            if (!window.supportsRenderPausing() || !settings.renderPausingEnabled) master.needsRedraw = true
//        } else {
//            drew = false
//        }
        keyBinder?.update(delta, eventManager.keyModifiers)
        executors.fastRemoveIfReversed { it.tick(delta) }
    }

    /** draw the debug overlay text. It is right-aligned. */
    fun drawDebugOverlay(x: Float, y: Float) {
        renderer.text(
            monospaceFont,
            x,
            y,
            text = perf,
            color = colors.text.primary.normal,
            fontSize = 10f,
        )
        master.debugDraw()
    }

    /**
     * add a function to be executed on the main thread, after rendering the UI. a renderer is provided if you want to do any rendering.
     *
     * @return `true` at any time to remove the hook.
     * @see beforeRender
     * @since 0.18.5
     */
    fun addHook(func: Renderer.() -> Boolean) {
        hooks.add(func)
    }

    /**
     * Remove a drawable on the next frame where [Drawable.canBeRemoved] returns true.
     *
     * Once this function is called, it cannot be revoked. Calling this function twice with the same drawable will break.
     */
    fun forRemoval(children: LinkedList<Drawable>, index: Int) {
        hooks.add {
            children.fastRemoveIfIndexed { i, it -> i == index && it.canBeRemoved() }
        }
    }

    /**
     * Add a function to be executed just before a render cycle begins.
     *
     * Any modifications to the renderer at this point such as transformations will affect the rendering of the entire UI.
     * @since 0.24.3
     * @see addHook
     */
    fun beforeRender(func: Renderer.() -> Boolean) {
        preHooks.add(func)
    }

    /** add a function that is called every [nanos] nanoseconds. */
    fun every(nanos: Long, repeats: Int = 0, func: () -> Unit): PolyUI {
        executors.add(Clock.FixedTimeExecutor(nanos, repeats, func))
        return this
    }

    /**
     * Sets the focus to the specified focusable element.
     *
     * @param focusable the element to set focus on
     * @return true if focus was successfully set, false if the provided focusable is already focused
     */
    fun focus(focusable: Focusable?) = eventManager.focus(focusable)

    fun unfocus() = eventManager.unfocus()

    /**
     * Return the key name for the given key code.
     */
    fun getKeyName(key: Int) = window.getKeyName(key)

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
        val sb = StringBuilder().append(toString())
        val children = master.children ?: run {
            sb.append(" with 0 components")
            @Suppress("DEPRECATION")
            return sb.toString().stdout()
        }
        sb.append(" with ${children.size} children, totalling ${master.countChildren()} components:")
        children.fastEach {
            sb.append("\n\t").append(it.toString())
            debugPrint(it.children ?: return@fastEach, 1, sb)
        }
        @Suppress("DEPRECATION")
        return sb.toString().stdout()
    }

    private fun debugPrint(list: LinkedList<Drawable>, depth: Int, sb: StringBuilder) {
        var i = 0
        list.fastEach {
            sb.append("\n").append("\t", depth + 1).append(it.toString())
            i++
            if (i >= 10) {
                sb.append("\n").append("\t", depth + 2).append("... ").append(list.size - i).append(" more")
                return
            }
            debugPrint(it.children ?: return@fastEach, depth + 1, sb)
        }
    }

    /**
     * Time the [block] and return how long it took, as well as logging with the [msg] if [debug][org.polyfrost.polyui.property.Settings.debug] is active.
     * @since 0.18.5
     */
    fun timed(msg: String? = null, block: () -> Unit): Long {
        if (msg != null && settings.debug) LOGGER.info(msg)
        val now = System.nanoTime()
        block()
        val time = System.nanoTime() - now
        if (settings.debug) LOGGER.info("${if (msg != null) "\t\t> " else ""}took ${time / 1_000_000.0}ms")
        return time
    }

    override fun toString() = "PolyUI($size)"

    /** cleanup the polyUI instance. This will delete all resources, and render this instance unusable. */
    fun cleanup() {
        renderer.cleanup()
        unfocus()
        hooks.clear()
        master.children?.clear()
    }

    companion object {
        @JvmField
        val LOGGER: Logger = LoggerFactory.getLogger("PolyUI")

        /**
         * Fallback font library bundled with PolyUI
         * @since 0.22.0
         */
        @JvmField
        var defaultFonts = FontFamily("Poppins", "Poppins.zip")

        @JvmField
        var monospaceFont = Font("JetBrainsMono-Regular.ttf")

        /**
         * The fallback default image bundled with PolyUI
         * @since 0.11.0
         */
        @JvmField
        var defaultImage = PolyImage("err.png")
    }
}
