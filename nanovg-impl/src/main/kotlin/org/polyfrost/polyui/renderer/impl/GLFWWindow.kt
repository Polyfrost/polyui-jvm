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

package org.polyfrost.polyui.renderer.impl

import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWDropCallback
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL.setCapabilities
import org.lwjgl.opengl.GL20C.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.*
import org.lwjgl.system.macosx.ObjCRuntime
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.renderer.Window
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.utils.getResourceStream
import org.polyfrost.polyui.utils.simplifyRatio
import org.polyfrost.polyui.utils.toByteBuffer
import java.io.File
import kotlin.math.max

/**
 * reference implementation of a PolyUI window using GLFW, which registers an OpenGL context, supporting all PolyUI API features.
 *
 * For it to work across on macOS, *forwards compatibility is enabled.* This means that only Core profiles are supported, so **only use GL classes with the `C` suffix.**
 *
 * On macOS, this class is equipped with a workaround to allow it to run without `-XstartOnMainThread`. It can be disabled with `-Dpolyui.glfwnomacfix`, or setting [enableMacOSFix].
 *
 * @param gl2 if true, the window will be registered with OpenGL [2.0C+][org.lwjgl.opengl.GL20C], else, it will use OpenGL [3.2C+][org.lwjgl.opengl.GL32C].
 */
class GLFWWindow @JvmOverloads constructor(
    title: String,
    width: Int,
    height: Int,
    gl2: Boolean = false,
    resizeable: Boolean = true,
    decorated: Boolean = true,
) : Window(width, height) {
    override var height: Int
        get() = super.height
        set(value) {
            val h = IntArray(1)
            glfwGetFramebufferSize(handle, null, h)
            offset = h[0] - value
            super.height = value
        }

    /**
     * This value is a fix for OpenGL as it draws from the bottom left, not the bottom right.
     *
     * It is calculated by doing window size - framebuffer size, and is used by glViewport.
     */
    private var offset = 0
    val handle: Long
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

    var enableMacOSFix = System.getProperty("polyui.glfwnomacfix", "true").toBoolean()

    init {
        if (Platform.get() == Platform.MACOSX && enableMacOSFix) {
            val now = System.nanoTime()
            PolyUI.LOGGER.warn("macOS detected: checking isMainThread()... (disable with -Dpolyui.glfwnomacfix)")
            try {
                // kotlin copy of org.lwjgl.glfw.EventLoop.isMainThread()
                val msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend")
                val currentThread = JNI.invokePPP(ObjCRuntime.objc_getClass("NSThread"), ObjCRuntime.sel_getUid("currentThread"), msgSend)
                val isMainThread = JNI.invokePPZ(currentThread, ObjCRuntime.sel_getUid("isMainThread"), msgSend)
                if (!isMainThread) {
                    PolyUI.LOGGER.warn("VM option -XstartOnMainThread is required on macOS. glfw_async has been set to avoid crashing.")
                    Configuration.GLFW_LIBRARY_NAME.set("glfw_async")
                }
            } catch (e: Exception) {
                PolyUI.LOGGER.error("Failed to check if isMainThread, may crash!", e)
            }
            PolyUI.LOGGER.warn("\t > took: ${(System.nanoTime() - now) / 1_000_000f}ms")
        }

        GLFWErrorCallback.createPrint().set()
        if (!glfwInit()) throw RuntimeException("Failed to init GLFW")

        if (gl2) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0)
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

        glfwMakeContextCurrent(handle)
        createCapabilities()
        println("System Information:")
        println("\tGPU: ${glGetString(GL_RENDERER)}; ${glGetString(GL_VENDOR)}")
        println("\tDriver version: ${glGetString(GL_VERSION)}")
        println("\tOS: ${System.getProperty("os.name")} v${System.getProperty("os.version")}; ${System.getProperty("os.arch")}")
        println("\tJava version: ${System.getProperty("java.version")}; ${System.getProperty("java.vm.name")} from ${System.getProperty("java.vendor")} (${System.getProperty("java.vendor.url")})")

        glfwSetTime(0.0)
    }

    fun createCallbacks(polyUI: PolyUI) {
        // Add some callbacks for window resizing and content scale
        glfwSetFramebufferSizeCallback(handle) { _, width, height ->
            this.width = width
            this.height = height
            val r = polyUI.renderer.pixelRatio
            polyUI.resize(width.toFloat() / r, height.toFloat() / r)
        }

        glfwSetWindowContentScaleCallback(handle) { _, xScale, yScale ->
            val pixelRatio = max(xScale, yScale)
            if (polyUI.settings.debug) PolyUI.LOGGER.info("Pixel ratio: $pixelRatio")
            polyUI.resize(width.toFloat() / pixelRatio, height.toFloat() / pixelRatio, pixelRatio)
        }

        glfwSetMouseButtonCallback(handle) { _, button, action, _ ->
            if (action == GLFW_PRESS) {
                polyUI.inputManager.mousePressed(button)
            } else if (action == GLFW_RELEASE) {
                polyUI.inputManager.mouseReleased(button)
            }
        }

        glfwSetCursorPosCallback(handle) { _, x, y ->
            polyUI.inputManager.mouseMoved(x.toFloat(), y.toFloat())
        }

        glfwSetKeyCallback(handle) { _, keyCode, _, action, mods ->
            if (action == GLFW_REPEAT) return@glfwSetKeyCallback
            if (keyCode < 255 && mods > 1) {
                // accept modded chars, as glfwSetCharModsCallback is deprecated and doesn't work with control
                polyUI.inputManager.keyTyped((keyCode + 32).toChar())
            }
            // p.s. I have performance tested this; and it is very fast (doesn't even show up on profiler). kotlin is good at int ranges lol
            if (keyCode in 255..348) {
                if (keyCode < 340) {
                    val key: Keys = when (keyCode) {
                        GLFW_KEY_F1 -> Keys.F1
                        GLFW_KEY_F2 -> Keys.F2
                        GLFW_KEY_F3 -> Keys.F3
                        GLFW_KEY_F4 -> Keys.F4
                        GLFW_KEY_F5 -> Keys.F5
                        GLFW_KEY_F6 -> Keys.F6
                        GLFW_KEY_F7 -> Keys.F7
                        GLFW_KEY_F8 -> Keys.F8
                        GLFW_KEY_F9 -> Keys.F9
                        GLFW_KEY_F10 -> Keys.F10
                        GLFW_KEY_F11 -> Keys.F11
                        GLFW_KEY_F12 -> Keys.F12

                        GLFW_KEY_ESCAPE -> Keys.ESCAPE

                        GLFW_KEY_ENTER -> Keys.ENTER
                        GLFW_KEY_TAB -> Keys.TAB
                        GLFW_KEY_BACKSPACE -> Keys.BACKSPACE
                        GLFW_KEY_INSERT -> Keys.INSERT
                        GLFW_KEY_DELETE -> Keys.DELETE
                        GLFW_KEY_PAGE_UP -> Keys.PAGE_UP
                        GLFW_KEY_PAGE_DOWN -> Keys.PAGE_DOWN
                        GLFW_KEY_HOME -> Keys.HOME
                        GLFW_KEY_END -> Keys.END

                        GLFW_KEY_RIGHT -> Keys.RIGHT
                        GLFW_KEY_LEFT -> Keys.LEFT
                        GLFW_KEY_DOWN -> Keys.DOWN
                        GLFW_KEY_UP -> Keys.UP

                        else -> Keys.UNKNOWN
                    }

                    if (action == GLFW_PRESS) {
                        polyUI.inputManager.keyDown(key)
                    } else {
                        polyUI.inputManager.keyUp(key)
                    }
                } else {
                    val key: KeyModifiers = (
                            when (keyCode) {
                                GLFW_KEY_LEFT_SHIFT -> KeyModifiers.LSHIFT
                                GLFW_KEY_LEFT_CONTROL -> KeyModifiers.LCONTROL
                                GLFW_KEY_LEFT_ALT -> KeyModifiers.LALT
                                GLFW_KEY_LEFT_SUPER -> KeyModifiers.LMETA
                                GLFW_KEY_RIGHT_SHIFT -> KeyModifiers.RSHIFT
                                GLFW_KEY_RIGHT_CONTROL -> KeyModifiers.RCONTROL
                                GLFW_KEY_RIGHT_ALT -> KeyModifiers.RALT
                                GLFW_KEY_RIGHT_SUPER -> KeyModifiers.RMETA
                                else -> KeyModifiers.UNKNOWN
                            }
                            )
                    if (action == GLFW_PRESS) {
                        polyUI.inputManager.addModifier(key.value)
                    } else {
                        polyUI.inputManager.removeModifier(key.value)
                    }
                }
                return@glfwSetKeyCallback
            }
            if (action == GLFW_PRESS) {
                polyUI.inputManager.keyDown(keyCode)
            } else {
                polyUI.inputManager.keyUp(keyCode)
            }
        }

        glfwSetMouseButtonCallback(handle) { _, button, action, _ ->
            if (action == GLFW_PRESS) {
                polyUI.inputManager.mousePressed(button)
            } else if (action == GLFW_RELEASE) polyUI.inputManager.mouseReleased(button)
        }

        glfwSetCharCallback(handle) { _, codepoint ->
            polyUI.inputManager.keyTyped(codepoint.toChar())
        }

        var ran = false
        glfwSetScrollCallback(handle) { _, x, y ->
            // asm: small scroll amounts are usually trackpads
            if (!ran && (y < 1.0 && x < 1.0) && PolyUI.isOnMac && !polyUI.settings.naturalScrolling) {
                PolyUI.LOGGER.info("Enabled natural scrolling as it has been guessed to be a trackpad on macOS.")
                polyUI.settings.naturalScrolling = true
            }
            ran = true
            polyUI.inputManager.mouseScrolled(x.toFloat(), y.toFloat())
        }

        glfwSetDropCallback(handle) { _, count, names ->
            val files = Array(count) {
                File(GLFWDropCallback.getName(names, it))
            }
            polyUI.inputManager.filesDropped(files)
        }

        glfwSetWindowFocusCallback(handle) { _, focused ->
            if (polyUI.settings.unfocusedFPS != 0) {
                fpsCap = if (focused) polyUI.settings.maxFPS.toDouble() else polyUI.settings.unfocusedFPS.toDouble()
            }
            if (focused) {
                polyUI.master.needsRedraw = true
            }
        }
    }

    override fun open(polyUI: PolyUI): Window {
        this.polyUI = polyUI
        polyUI.window = this
        glfwSwapInterval(if (polyUI.renderer.settings.enableVSync) 1 else 0)

        createCallbacks(polyUI)

        MemoryStack.stackPush().use {
            val wbuf = it.mallocInt(1)
            val hbuf = it.mallocInt(1)
            val sxbuf = it.mallocFloat(1)
            val sybuf = it.mallocFloat(1)
            glfwGetWindowContentScale(handle, sxbuf, sybuf)
            glfwGetFramebufferSize(handle, wbuf, hbuf)
            val sx = sxbuf[0]
            val sy = sybuf[0]
            val w = wbuf[0]
            val h = hbuf[0]

            polyUI.resize(
                w.toFloat() / sx,
                h.toFloat() / sy,
                max(sx, sy),
            )

            this.width = w
            this.height = h
        }

        val (minW, minH) = polyUI.settings.minimumWindowSize
        val (maxW, maxH) = polyUI.settings.maximumWindowSize
        glfwSetWindowSizeLimits(handle, minW, minH, maxW, maxH)

        if (polyUI.settings.windowAspectRatio.first == 0 || polyUI.settings.windowAspectRatio.second == 0) {
            val ratio = (width to height).simplifyRatio()
            PolyUI.LOGGER.info("Inferred aspect ratio: ${ratio.first}:${ratio.second}")
            polyUI.settings.windowAspectRatio = ratio
        }
        glfwSetWindowAspectRatio(handle, polyUI.settings.windowAspectRatio.first, polyUI.settings.windowAspectRatio.second)

        var t = glfwGetTime()
        fpsCap = polyUI.settings.maxFPS.toDouble()
        while (!glfwWindowShouldClose(handle)) {
            if (offset != 0) glViewport(0, offset, width, height)
            glClearColor(0f, 0f, 0f, 0f)

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
        setCapabilities(null)
        Callbacks.glfwFreeCallbacks(handle)
        glfwTerminate()
        glfwSetErrorCallback(null)?.free()
        return this
    }

    override fun close() = glfwSetWindowShouldClose(handle, true)

    /** set the icon of this window according to the given [icon] path. This should be a resource path that can be used by [getResourceStream].
     *
     * **Does not work on macOS.** This is a limitation of GLFW, and is not a bug in PolyUI.
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

    override fun supportsRenderPausing() = true

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
                },
            ),
        )
    }

    override fun getKeyName(key: Int) = glfwGetKeyName(key, glfwGetKeyScancode(key)) ?: "Unknown"

    fun fullscreen() {
        glfwGetVideoMode(glfwGetPrimaryMonitor())?.let {
            glfwSetWindowSize(handle, it.width(), it.height())
        }
    }
}
