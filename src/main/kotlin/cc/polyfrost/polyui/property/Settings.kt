/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.renderer.Renderer

/** Settings for PolyUI.
 *
 * This contains many values that concern the rendering and event handling of PolyUI internally.
 * */
class Settings(private val renderer: Renderer) {
    var debug = System.getProperty("polyui.debug", "true").toBoolean()
    var debugLog = System.getProperty("polyui.debug.logAll", "false").toBoolean()

    var showFPS = false
    var useAntialiasing = true

    /** How to handle resource (image and font) loading errors. */
    var resourcePolicy = ResourcePolicy.WARN

    /** If true, the renderer will render all layout and component to a 'master' framebuffer, then every frame, render that. */
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
        /**
         * RenderBuffers are marginally faster than framebuffers, but all read
         * operations will not work.
         *
         * Note that not all renderers will support this, and you might use a
         * [FRAMEBUFFER] instead.
         */
        RENDERBUFFER,

        /** Use a framebuffer object for rendering.*/
        FRAMEBUFFER
    }

    /** How to handle resource (image and font) loading errors. */
    enum class ResourcePolicy {
        /** Throw an exception and crash the program. */
        CRASH,

        /** Warn in the log and use the default resource (bundled with your rendering implementation). */
        WARN
    }
}
