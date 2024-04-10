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

import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Positioner
import org.polyfrost.polyui.component.countChildren
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Group
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
import org.polyfrost.polyui.unit.immutable
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureNanoTime

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
class PolyUI @JvmOverloads constructor(
    val renderer: Renderer,
    settings: Settings? = null,
    inputManager: InputManager? = null,
    translator: Translator? = null,
    backgroundColor: PolyColor? = null,
    masterAlignment: Align = Align(cross = Align.Cross.Start, padding = Vec2.ZERO),
    colors: Colors = DarkTheme(),
    size: Vec2? = null,
    vararg drawables: Drawable,
) {
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

    /**
     * returns `true` if render pausing is possible.
     * @see Settings.renderPausingEnabled
     * @since 1.1.3
     */
    inline val canPauseRendering get() = settings.renderPausingEnabled && window?.supportsRenderPausing() == true

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
    val master = if (backgroundColor == null) Group(at = Vec2(), size = size, children = drawables, alignment = masterAlignment)
    else Block(at = Vec2(), size = size, children = drawables, alignment = masterAlignment, color = backgroundColor, radii = 0f.radii())


    val inputManager = inputManager?.with(master) ?: InputManager(master, KeyBinder(this.settings), this.settings)

    /**
     * Get the [KeyBinder] for this PolyUI instance.
     *
     * This value may be null as the key binder is optional.
     */
    inline val keyBinder get() = inputManager.keyBinder
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

    inline val size: Vec2 get() = master.size

    private var _iSize: Vec2.Immutable? = null

    /**
     * this property stores the initial size of this PolyUI instance.
     * It is used to make sure that new objects experience the same resizing as others.
     * @since 1.0.5
     */
    val iSize: Vec2.Immutable
        get() {
            return if (master.sizeValid) {
                if (_iSize == null) _iSize = master.size.immutable()
                _iSize!!
            } else Vec2.i_ONE
        }
    private val executors: LinkedList<Clock.Executor> = LinkedList()

    /**
     * the time since the last frame, in nanoseconds. It is used internally a lot for animations, etc.
     * @see Clock
     * @see every
     * @see org.polyfrost.polyui.component.Drawable.preRender
     * @see org.polyfrost.polyui.animate.Animation
     */
    var delta: Long = 0L
        private set

    // telemetry
    var fps: Int = 1
        private set
    private var frames = 0
    private var nframes = 0
    var longestFrame = 0.0
        private set
    var shortestFrame = 100.0
        private set
    private var timeInFrames = 0.0
    var avgFrame = 0.0
        private set
    private var perf: String = ""

    init {
        LOGGER.info("PolyUI initializing...")
        if (master.children.isNullOrEmpty()) LOGGER.warn("PolyUI initialized with no children!")
        master.simpleName += " [Master]"
        master.setup(this)
        master.tryMakeScrolling()

        this.keyBinder?.add(
            KeyBinder.Bind('R', mods = mods(KeyModifiers.LCONTROL)) {
                LOGGER.info("Reloading PolyUI")
                resize(this.size.x, this.size.y, true)
                true
            },
        )
        if (this.settings.debug) {
            addDebug(this.settings.enableDebugKeybind)
            val f = DecimalFormat("#.###")
//            var td = System.nanoTime()
            every(1.seconds) {
//            val diff = (System.nanoTime() - td) / 1_000_000_000.0
//            td = System.nanoTime()
//            LOGGER.info("took $diff sec (accuracy = ${100.0 - (diff - 1.0) * 100.0}%)")
                if (this.settings.debug) {
                    val sb = StringBuilder(64)
                    sb.append("fps: ").append(fps)
                        .append(", avg/max/min: ")
                        .append(f.format(avgFrame)).append("ms; ")
                        .append(f.format(longestFrame)).append("ms; ")
                        .append(f.format(shortestFrame)).append("ms")
                    if (canPauseRendering) {
                        val skipPercent = (1.0 - (frames.toDouble() / nframes)) * 100.0
                        sb.append(", skip=").append(f.format(skipPercent)).append('%')
                    }
                    perf = sb.toString()
                    longestFrame = 0.0
                    shortestFrame = 100.0
                    avgFrame = timeInFrames / fps
                    timeInFrames = 0.0
                    fps = frames
                    frames = 0
                    nframes = 0
                    master.needsRedraw = true
                    if (drew) LOGGER.info(perf)
                }
            }
            println(debugString())
        }
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
        if (!settings.maximumSize.negative) {
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

        val sx = newWidth / size.x
        val sy = newHeight / size.y
        master.rescale(sx, sy)
        master.needsRedraw = true
    }

    fun render() {
        delta = clock.delta
        nframes++
        if (master.needsRedraw) {
            val sz = master.size
            renderer.beginFrame(sz.x, sz.y, pixelRatio)
            window?.preRender(renderer)
            master.draw()
            window?.postRender(renderer)

            // telemetry
            if (settings.debug) {
                val frameTime = (clock.peek()) / 1_000_000.0
                timeInFrames += frameTime
                if (frameTime > longestFrame) longestFrame = frameTime
                if (frameTime < shortestFrame) shortestFrame = frameTime
                frames++
                drawDebugOverlay(0f, size.y - 11f)
                if (inputManager.focused == null) {
                    val mods = inputManager.keyModifiers
                    if (mods.hasControl) {
                        val obj = inputManager.mouseOver
                        if (obj != null) {
                            val os = obj.toString()
                            val w = renderer.textBounds(monospaceFont, os, 10f).x
                            val pos = min(max(0f, mouseX - w / 2f), this.size.x - w - 10f)
                            renderer.rect(pos, mouseY - 14f, w + 10f, 14f, colors.component.bg.hovered)
                            renderer.text(monospaceFont, pos + 5f, mouseY - 10f, text = os, colors.text.primary.normal, 10f)
                            master.needsRedraw = true
                        }
                    }
                    if (mods.hasShift) {
                        val s = "${inputManager.mouseX}x${inputManager.mouseY}"
                        val ww = renderer.textBounds(monospaceFont, s, 10f).x
                        val ppos = min(max(0f, mouseX + 10f), this.size.x - ww - 10f)
                        val pposy = min(max(0f, mouseY), this.size.y - 14f)
                        renderer.rect(ppos, pposy, ww + 10f, 14f, colors.component.bg.hovered)
                        renderer.text(monospaceFont, ppos + 5f, pposy + 4f, text = s, colors.text.primary.normal, 10f)
                        master.needsRedraw = true
                    }
                }
            }

            renderer.endFrame()
            drew = true
            if (!canPauseRendering || !settings.renderPausingEnabled) master.needsRedraw = true
        } else {
            drew = false
        }
        keyBinder?.update(delta, inputManager.mods)
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
    fun focus(focusable: Drawable?) = inputManager.focus(focusable)

    fun unfocus() = inputManager.unfocus()

    /**
     * Return the key name for the given key code, or "Unknown" if the key is not mapped / no window is present.
     */
    fun getKeyName(key: Int) = window?.getKeyName(key) ?: "Unknown"

    /**
     * return a string of this PolyUI instance's components and children in a list, like this:
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
     */
    fun debugString(): String {
        val sb = StringBuilder().append(toString())
        val children = master.children ?: run {
            sb.append(" with 0 children, totalling 0 components.")
            return sb.toString()
        }
        sb.append(" with ${children.size} children, totalling ${master.countChildren()} components:")
        children.fastEach {
            sb.append("\n\t").append(it.toString())
            debugString(it.children ?: return@fastEach, 1, sb)
        }
        return sb.toString()
    }

    private fun debugString(list: LinkedList<Drawable>, depth: Int, sb: StringBuilder) {
        var i = 0
        var ii = 0
        val ndepth = depth + 1
        list.fastEach {
            if (it.initialized) {
                sb.append('\n').append('\t', ndepth).append(it.toString())
                debugString(it.children ?: return@fastEach, ndepth, sb)
                i++
            } else ii += it.countChildren() + 1
            if (i >= 10) {
                sb.append('\n').append('\t', ndepth).append("... ").append(list.size - i).append(" more")
                return
            }
        }
        if (ii != 0) sb.append('\n').append('\t', ndepth).append("... (").append(ii).append(" uninitialized)")
    }

    /**
     * Time the [block] and return how long it took, as well as logging with the [msg] if [debug][org.polyfrost.polyui.property.Settings.debug] is active.
     * @since 0.18.5
     */
    inline fun timed(msg: String? = null, crossinline block: () -> Unit): Long {
        if (msg != null && settings.debug) LOGGER.info(msg)
        val time = measureNanoTime(block)
        if (settings.debug) LOGGER.info("${if (msg != null) "\t\t> " else ""}took ${time / 1_000_000.0}ms")
        return time
    }

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
        val LOGGER: Logger = LoggerFactory.getLogger("PolyUI")

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
        var defaultFonts = FontFamily("Poppins", "Poppins.zip")

        @JvmField
        var monospaceFont = Font("JetBrainsMono-Regular.ttf")

        /**
         * The fallback default image bundled with PolyUI
         * @since 0.11.0
         */
        @JvmField
        var defaultImage = PolyImage("err.png")

        const val INPUT_DISABLED: Byte = -1
        const val INPUT_NONE: Byte = 0
        const val INPUT_HOVERED: Byte = 1
        const val INPUT_PRESSED: Byte = 2

        const val DANGER: Byte = 0
        const val WARNING: Byte = 1
        const val SUCCESS: Byte = 2
    }
}
