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

package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.input.Keys
import cc.polyfrost.polyui.renderer.Window
import cc.polyfrost.polyui.renderer.data.Cursor
import cc.polyfrost.polyui.utils.getResourceStream
import cc.polyfrost.polyui.utils.simplifyRatio
import cc.polyfrost.polyui.utils.toByteBuffer
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Platform
import kotlin.math.max

class GLWindow @JvmOverloads constructor(
    title: String,
    width: Int,
    height: Int,
    gl2: Boolean = false,
    resizeable: Boolean = true,
    decorated: Boolean = true
) : Window(width, height) {

    override var height: Int = height
        set(value) {
            val h = IntArray(1)
            glfwGetFramebufferSize(handle, null, h)
            offset = h[0] - value
            field = value
        }

    /**
     * This value is a fix for OpenGL as it draws from the bottom left, not the bottom right.
     *
     * It is calculated by doing window size - framebuffer size, and is used by glViewport.
     */
    private var offset = 0
    val handle: Long
    var contentScaleX = 1f
        private set
    var contentScaleY = 1f
        private set
    lateinit var polyUI: PolyUI
        private set
    var fpsCap: Double = 0.0
        set(value) {
            field = if (value == 0.0) 0.0 else 1.0 / value.toInt()
        }

    var title = title
        set(new) {
            field = new
            glfwSetWindowTitle(handle, new)
        }

    init {
        GLFWErrorCallback.createPrint().set()
        if (!glfwInit()) throw RuntimeException("Failed to init GLFW")

        if (gl2) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1)
        } else {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        }

        if (!resizeable) glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        if (!decorated) glfwWindowHint(GLFW_DECORATED, GLFW_FALSE)

        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE)

        handle = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
        if (handle == MemoryUtil.NULL) {
            glfwTerminate()
            throw RuntimeException("Failed to create the window.")
        }
        setIcon("icon.png")

        glfwMakeContextCurrent(handle)
        createCapabilities()
        println("System Information:")
        println("\tGPU: ${glGetString(GL_RENDERER)}")
        println("\tDriver version: ${glGetString(GL_VERSION)}")
        println("\tOS: ${System.getProperty("os.name")} v${System.getProperty("os.version")}; ${System.getProperty("os.arch")}")
        println("\tJava version: ${System.getProperty("java.version")}; ${System.getProperty("java.vm.name")} from ${System.getProperty("java.vendor")} (${System.getProperty("java.vendor.url")})")

        glfwSetTime(0.0)
    }

    override fun createCallbacks() {
        // Add some callbacks for window resizing and content scale
        glfwSetFramebufferSizeCallback(handle) { _, width, height ->
            this.width = width
            this.height = height
            polyUI.onResize((width / polyUI.renderer.pixelRatio).toInt(), (height / polyUI.renderer.pixelRatio).toInt(), polyUI.renderer.pixelRatio)
        }

        glfwSetWindowContentScaleCallback(handle) { _, xScale, yScale ->
            val pixelRatio = max(xScale, yScale)
            if (polyUI.settings.debug) PolyUI.LOGGER.info("Pixel ratio: $pixelRatio")
            polyUI.onResize((width / pixelRatio).toInt(), (height / pixelRatio).toInt(), pixelRatio)
        }

        glfwSetMouseButtonCallback(handle) { _, button, action, _ ->
            if (action == GLFW_PRESS) {
                polyUI.eventManager.onMousePressed(button)
            } else if (action == GLFW_RELEASE) {
                polyUI.eventManager.onMouseReleased(button)
            }
        }

        glfwSetCursorPosCallback(handle) { _, x, y ->
            @Suppress("NAME_SHADOWING")
            var x = x.toFloat()

            @Suppress("NAME_SHADOWING")
            var y = y.toFloat()
            if (polyUI.renderer.pixelRatio != 1f) {
                if (Platform.get() == Platform.WINDOWS) {
                    x /= polyUI.renderer.pixelRatio
                    y /= polyUI.renderer.pixelRatio
                }
            }
            polyUI.eventManager.setMousePosAndUpdate(x, y)
        }

        glfwSetKeyCallback(handle) { _, keyCode, _, action, mods ->
            // p.s. I have performance tested this; and it is very fast (doesn't even show up on profiler). kotlin is good at int ranges lol
            if (keyCode < 100) {
                if (mods > 1 && action != GLFW_RELEASE) {
                    polyUI.eventManager.onKeyTyped(
                        keyCode.toChar(),
                        action == GLFW_REPEAT
                    )
                }
            } else if (keyCode < 340) {
                val key: Keys = (
                    when (keyCode) {
                        // insert, pg down, etc
                        in 256..261 -> Keys.fromValue(keyCode - 156)
                        in 266..269 -> Keys.fromValue(keyCode - 160)
                        // arrows
                        in 262..265 -> Keys.fromValue(keyCode - 62)
                        // function keys
                        in 290..314 -> Keys.fromValue(keyCode - 289)
                        else -> Keys.UNKNOWN
                    }
                    )
                if (action != GLFW_RELEASE) polyUI.eventManager.onUnprintableKeyTyped(key, action == GLFW_REPEAT)
            } else {
                val key: KeyModifiers = (
                    when (keyCode) {
                        340 -> KeyModifiers.LSHIFT
                        341 -> KeyModifiers.LCONTROL
                        342 -> KeyModifiers.LALT
                        343 -> KeyModifiers.LMETA
                        344 -> KeyModifiers.RSHIFT
                        345 -> KeyModifiers.RCONTROL
                        346 -> KeyModifiers.RALT
                        347 -> KeyModifiers.RMETA
                        else -> KeyModifiers.UNKNOWN
                    }
                    )
                if (action == GLFW_PRESS) {
                    polyUI.eventManager.addModifier(key.value)
                } else if (action == GLFW_RELEASE) {
                    polyUI.eventManager.removeModifier(key.value)
                }
            }
        }

        glfwSetCharCallback(handle) { _, codepoint ->
            polyUI.eventManager.onKeyTyped(codepoint.toChar(), false)
        }

        glfwSetScrollCallback(handle) { _, x, y ->
            polyUI.eventManager.onMouseScrolled(x.toInt(), y.toInt())
        }

//        glfwSetDropCallback(handle) { _, count, names ->
//            val files = Array(count) {
//                File(GLFWDropCallback.getName(names, it))
//            }
//        }

        glfwSetWindowFocusCallback(handle) { _, focused ->
            if (polyUI.settings.unfocusedFPS != 0) {
                fpsCap = if (focused) polyUI.settings.maxFPS.toDouble() else polyUI.settings.unfocusedFPS.toDouble()
            }
            glfwSetWindowSize(handle, width, height)
            if (focused) {
                polyUI.master.needsRedraw = true
            }
        }

        // fix macOS windows being small when focus is lost
        glfwSetWindowIconifyCallback(handle) { _, iconified ->
            if (iconified) {
                glfwSetWindowSize(handle, width, height)
            }
        }
    }

    override fun videoSettingsChanged() {
        glfwSwapInterval(if (polyUI.renderer.settings.enableVSync) 1 else 0)
    }

    override fun open(polyUI: PolyUI): Window {
        this.polyUI = polyUI
        polyUI.window = this
        videoSettingsChanged()

        createCallbacks()

        MemoryStack.stackPush().use {
            val w = it.mallocInt(1)
            val h = it.mallocInt(1)
            val contentScaleX = it.mallocFloat(1)
            val contentScaleY = it.mallocFloat(1)
            glfwGetFramebufferSize(handle, w, h)
            glfwGetWindowContentScale(handle, contentScaleX, contentScaleY)

            this.contentScaleX = contentScaleX[0]
            this.contentScaleY = contentScaleY[0]

            this.width = w[0]
            this.height = h[0]

            polyUI.onResize(
                (this.width / this.contentScaleX).toInt(),
                (this.height / this.contentScaleY).toInt(),
                max(this.contentScaleX, this.contentScaleY)
            )
        }

        val (minW, minH) = polyUI.settings.minimumWindowSize
        val (maxW, maxH) = polyUI.settings.maximumWindowSize
        glfwSetWindowSizeLimits(handle, minW, minH, maxW, maxH)

        if (polyUI.settings.windowAspectRatio.first == 0 || polyUI.settings.windowAspectRatio.second == 0) {
            val ratio = (width to height).simplifyRatio()
            PolyUI.LOGGER.info("Inferred aspect ratio: {}:{}", ratio.first, ratio.second)
            polyUI.settings.windowAspectRatio = ratio
        }
        glfwSetWindowAspectRatio(handle, polyUI.settings.windowAspectRatio.first, polyUI.settings.windowAspectRatio.second)

        var t = glfwGetTime()
        fpsCap = polyUI.settings.maxFPS.toDouble()
        while (!glfwWindowShouldClose(handle)) {
            glViewport(0, offset, width, height)
            val c = polyUI.colors.page.bg.normal
            glClearColor(c.r / 255f, c.g / 255f, c.b / 255f, 0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

            this.polyUI.render()
            if (fpsCap != 0.0) {
                val e = glfwGetTime() - t
                if (e < fpsCap) {
                    Thread.sleep(((fpsCap - e) * 1_000.0).toLong())
                }
                t = glfwGetTime()
            }
            glfwPollEvents()
            if (polyUI.drew) glfwSwapBuffers(handle)
        }

        polyUI.cleanup()
        GL.setCapabilities(null)
        Callbacks.glfwFreeCallbacks(handle)
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
        return this
    }

    override fun close() = glfwSetWindowShouldClose(handle, true)

    /** set the icon of this window according to the given [icon] path. This should be a resource path that can be used by [getResourceStream].
     *
     * The icon should be a PNG, BMP or JPG, and in a 2x size (i.e. 16x16, 32x32, 128x128, etc)
     * @throws Exception if the image does not exist, or a different IO error occurs.
     *
     * [SIGSEGV](https://en.wikipedia.org/wiki/Segmentation_fault) - if something else goes wrong in this method. Your JVM will crash blaming `[libglfw.so+0x211xx]` or something.
     */
    fun setIcon(icon: String) {
        val w = IntArray(1)
        val h = IntArray(1)
        val data = STBImage.stbi_load_from_memory(getResourceStream(icon).toByteBuffer(), w, h, IntArray(1), 4)
            ?: throw Exception("error occurred while loading icon!")
        glfwSetWindowIcon(handle, GLFWImage.malloc(1).put(0, GLFWImage.malloc().set(w[0], h[0], data)))
    }

    override fun getClipboard() = glfwGetClipboardString(handle)

    override fun setClipboard(text: String?) = if (text != null) glfwSetClipboardString(handle, text as CharSequence) else Unit

    override fun setCursor(cursor: Cursor) {
        glfwSetCursor(
            handle,
            glfwCreateStandardCursor(
                when (cursor) {
                    Cursor.Pointer -> GLFW_ARROW_CURSOR
                    Cursor.Clicker -> GLFW_POINTING_HAND_CURSOR
                    Cursor.Text -> GLFW_IBEAM_CURSOR
                }
            )
        )
    }

    fun fullscreen() {
        glfwGetVideoMode(glfwGetPrimaryMonitor())?.let {
            glfwSetWindowSize(handle, it.width(), it.height())
        }
    }
}
