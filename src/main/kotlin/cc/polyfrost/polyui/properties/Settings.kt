package cc.polyfrost.polyui.properties

import cc.polyfrost.polyui.renderer.Renderer

class Settings(private val renderer: Renderer) {
    var debug = System.getProperty("polyui.debug")?.toBoolean() ?: false
    var showFPS = false
    var useAntialiasing = true
    var bufferType: BufferType = BufferType.FRAMEBUFFER
        set(value) = if (value == BufferType.RENDERBUFFER && !renderer.supportsRenderbuffer()) {
            field = BufferType.FRAMEBUFFER
        } else {
            field = value
        }


    enum class BufferType {
        /** RenderBuffers are marginally faster than framebuffers, but all read operations will not work.
         *
         * Note that not all renderers will support this, and will use a framebuffer instead. */
        RENDERBUFFER,

        /** Use a framebuffer object for rendering.*/
        FRAMEBUFFER,
    }
}