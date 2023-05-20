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
import cc.polyfrost.polyui.unit.milliseconds

/** Settings for PolyUI.
 *
 * This contains many values that concern the rendering and event handling of PolyUI internally.
 * */
class Settings(private val renderer: Renderer) {
    /** this enables the debug renderer and various other debug features, including more verbose checks and logging.
     *
     * It can be enabled using `-Dpolyui.debug=true` in the JVM arguments, or by pressing Ctrl+Shift+I in the application [if enabled][enableDebugKeybind].
     */
    var debug = System.getProperty("polyui.debug", "true").toBoolean()

    /** enable the debug keybind in the window (Ctrl+Shift+I)
     * @see debug*/
    var enableDebugKeybind = true

    /** set this to something other than 0 to set a framerate cap for the UI. */
    var maxFPS = 0

    var useAntialiasing = true
    var enableVSync = false

    /** @see cc.polyfrost.polyui.input.PolyTranslator */
    var defaultLocale = System.getProperty("polyui.locale") ?: null

    /** How to handle resource (image and font) loading errors. */
    var resourcePolicy = ResourcePolicy.WARN

    /** If true, the renderer will render all layout and component to a 'master' framebuffer, then every frame, render that.
     * @see minItemsForFramebuffer
     */
    var masterIsFramebuffer = false

    /**
     * minimum number of items in a layout (and its children) before it will use a framebuffer.
     *
     * This value should be set to something relatively high, as the performance gain from using a framebuffer only works if there is a large amount of draw calls.
     *
     * @see [cc.polyfrost.polyui.layout.Layout.countDrawables]
     */
    var minItemsForFramebuffer: Int = 30

    /** the time between clicks for them to be considered as a combo.
     * @see maxComboSize
     * @see clearComboWhenMaxed
     */
    var comboMaxInterval: Long = 200L.milliseconds

    /** maximum amount of clicks that can be comboed in any interval
     * @see comboMaxInterval
     * @see clearComboWhenMaxed
     */
    var maxComboSize: Int = 4

    /** if true, the combo will be cleared when it reaches [maxComboSize].
     * Otherwise, it will just continue to dispatch the max event for future clicks if they are within the [combo frame][comboMaxInterval].
     * @see maxComboSize
     * @see comboMaxInterval
     */
    var clearComboWhenMaxed = false

    /**
     * Scroll multiplier factor, which will multiply the returned value of the callback by this value.
     * First value is the X scroll multiplier, with the second being the Y scroll multiplier.
     */
    var scrollMultiplier: Pair<Int, Int> = 60 to 60

    /** Weather to invert the scroll direction */
    var naturalScrolling = false

    /** set the buffer type to use for rendering.
     * @see BufferType
     */
    var bufferType: BufferType = BufferType.FRAMEBUFFER
        set(value) = if (value == BufferType.RENDERBUFFER && !renderer.supportsRenderbuffer()) {
            PolyUI.LOGGER.warn("Renderbuffer is not supported, using framebuffer instead.")
            field = BufferType.FRAMEBUFFER
        } else {
            field = value
        }

    /**
     * @see bufferType
     */
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

    /** How to handle resource (image and font) loading errors.
     * @see resourcePolicy
     */
    enum class ResourcePolicy {
        /** Throw an exception and crash the program. */
        CRASH,

        /** Warn in the log and use the default resource (bundled with your rendering implementation). */
        WARN
    }
}
