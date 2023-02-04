package cc.polyfrost.polyui.properties

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.renderer.Renderer

class Settings(private val renderer: Renderer) {
    var debug = System.getProperty("polyui.debug")?.toBoolean() ?: true
    var debugLog = System.getProperty("polyui.debug.logAll")?.toBoolean() ?: false

    var showFPS = false
    var useAntialiasing = true

    /** If true, the renderer will render all layouts and components to a 'master' framebuffer, then every frame, render that. */
    var masterIsFramebuffer = false

    /** minimum number of items in a layout before it will use a framebuffer. */
    var minItemsForFramebuffer: Int = 5

    /** the time between clicks for them to be considered as a combo. */
    var multiClickInterval: Long = 500L

    /** maximum amount of clicks that can be 'combo-ed' in any interval */
    var maxClicksThatCanCombo: Int = 2

    /** set the buffer type to use for rendering. */
    var bufferType: BufferType = BufferType.FRAMEBUFFER
        set(value) = if (value == BufferType.RENDERBUFFER && !renderer.supportsRenderbuffer()) {
            PolyUI.LOGGER.warn("Renderbuffer is not supported, using framebuffer instead.")
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