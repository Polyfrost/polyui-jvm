package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.renderer.Window
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.Platform

class GLWindow(title: String, width: Int, height: Int) : Window(title, width, height) {
    val handle: Long
    var fps: Int = 0

    // memory usage is key kids
    private val mouseXBuf = MemoryStack.stackPush().mallocDouble(1)
    private val mouseYBuf = MemoryStack.stackPush().mallocDouble(1)
    lateinit var polyUI: PolyUI

    init {
        GLFWErrorCallback.createPrint().set()
        if (!glfwInit()) throw RuntimeException("Failed to init GLFW")
        if (Platform.get() === Platform.MACOSX) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        }

        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE)

        handle = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL)
        if (handle == MemoryUtil.NULL) {
            glfwTerminate()
            throw RuntimeException("Failed to create the window.")
        }

        glfwMakeContextCurrent(handle)
        createCapabilities()
        glfwSetTime(0.0)
        glfwSwapInterval(0)
    }

    fun getCursorPos() {
        mouseXBuf.clear()
        mouseYBuf.clear()
        glfwGetCursorPos(handle, mouseXBuf, mouseYBuf)
        polyUI.eventManager.setMousePosAndUpdate(mouseXBuf[0].toFloat(), mouseYBuf[0].toFloat())
    }

    override fun createCallbacks() {
        // Add some callbacks for window resizing and content scale
        glfwSetFramebufferSizeCallback(handle) { _, width, height ->
            polyUI.onResize(width, height)
        }

        glfwSetMouseButtonCallback(handle) { _, button, action, _ ->
            if (action == GLFW_PRESS) {
                polyUI.eventManager.onMousePressed(button)
            } else if (action == GLFW_RELEASE) {
                polyUI.eventManager.onMouseReleased(button)
            }
        }

        glfwSetKeyCallback(handle) { _, key, _, action, _ ->
            if (action == GLFW_PRESS) {
                polyUI.eventManager.onKeyPressed(key)
            } else if (action == GLFW_RELEASE) {
                polyUI.eventManager.onKeyReleased(key)
            }
        }

        glfwSetScrollCallback(handle) { _, _, yoffset ->
            polyUI.eventManager.onMouseScrolled(yoffset.toInt())
        }
    }

    override fun open(polyUI: PolyUI): Window {
        this.polyUI = polyUI
        var frames = 0
        //var memoryDiff = 0
        //var lastMemory = -1L
        var lastSecond = System.currentTimeMillis()

        createCallbacks()
        while (!glfwWindowShouldClose(handle)) {

            glViewport(0, 0, width, height)
            glClearColor(0.1f, 0.1f, 0.1f, 0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

            this.polyUI.render()


            glfwSwapBuffers(handle)
            glfwPollEvents()
            getCursorPos()

            if (lastSecond + 1000 < System.currentTimeMillis()) {
                lastSecond = System.currentTimeMillis()
                fps = frames
                frames = 0
                println("FPS: $fps")

                //val memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                //memoryDiff = if (lastMemory == -1L) 0 else (memory - lastMemory).toInt()
                //lastMemory = memory
            } else frames++
        }

        GL.setCapabilities(null)
        Callbacks.glfwFreeCallbacks(handle)
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
        return this
    }

    override fun closeWindow() {
        TODO("Not yet implemented")
    }

    override fun setIcon(icon: String) {
        TODO("Not yet implemented")
    }

    override fun renameWindow(title: String) {
        TODO("Not yet implemented")
    }

    override fun fullscreen() {
        TODO("Not yet implemented")
    }

}