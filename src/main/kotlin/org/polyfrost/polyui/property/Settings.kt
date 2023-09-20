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

package org.polyfrost.polyui.property

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.unit.milliseconds

/** Settings for PolyUI.
 *
 * This contains many values that concern the rendering and event handling of PolyUI internally.
 * */
class Settings {
    internal lateinit var polyUI: PolyUI

    /** this enables the debug renderer and various other debug features, including more verbose checks and logging.
     *
     * It can be enabled using `-Dpolyui.debug=true` in the JVM arguments, or by pressing Ctrl+Shift+I in the application [if enabled][enableDebugKeybind].
     */
    var debug = System.getProperty("polyui.debug", "true").toBoolean()

    /** enable the debug keybind in the window (Ctrl+Shift+I)
     * @see debug*/
    var enableDebugKeybind = true

    /** set this to something other than 0 to set a framerate cap for the UI.
     * @see unfocusedFPS */
    var maxFPS = 0

    /** set this to something other than 0 to change the framerate from the current [maxFPS] to this value when it is unfocused. */
    var unfocusedFPS = 10

    var useAntialiasing = true
    var enableVSync = true

    /** @see org.polyfrost.polyui.input.Translator */
    var defaultLocale = "en_default"

    /** How to handle resource (image and font) loading errors. */
    var resourcePolicy = ResourcePolicy.WARN

    /** If true, the renderer will render all layout and component to a 'master' framebuffer, then every frame, render that.
     * @see minDrawablesForFramebuffer
     * @since 0.18.0
     */
    var masterIsFramebuffer = false

    /**
     * minimum number of drawables in a layout (and its children) before it will use a framebuffer.
     *
     * This value should be set to something relatively high, as the performance gain from using a framebuffer only works if there is a large amount of draw calls.
     *
     * @see [org.polyfrost.polyui.layout.Layout.countDrawables]
     */
    var minDrawablesForFramebuffer: Int = 30

    /**
     * Weather or not the system should promote dragging drawables to the top of the render queue. This means they are rendered last so look correct.
     *
     * They will still retain their order relative to each other.
     *
     * @since 0.18.2
     * @see org.polyfrost.polyui.layout.Layout.draggable
     * @see org.polyfrost.polyui.component.Component.draggable
     */
    var draggablesOnTop = true

    /**
     * Enable or disable framebuffers. Please note that PolyUI is designed to work with framebuffers, so disabling them may cause performance issues.
     * @see org.polyfrost.polyui.PolyUI
     * @since 0.18.0
     */
    var framebuffersEnabled = true

    /**
     * This optimization enables "render pausing", or allowing the renderer to pause rendering when there are no changes to the UI.
     *
     * This requires a setup such as `if (`[polyUI.drew][PolyUI.drew]`) glfwSwapBuffers(handle)` in your main loop.
     * @since 0.12.0
     */
    var renderPausingEnabled = true

    /** the time between clicks for them to be considered as a combo.
     * @see maxComboSize
     * @see clearComboWhenMaxed
     */
    var comboMaxInterval: Long = 200L.milliseconds

    /** maximum amount of clicks that can be combo-ed in any interval
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
    var scrollMultiplier: Pair<Float, Float> = 10f to 10f

    /** Weather to invert the scroll direction */
    var naturalScrolling = false

    /**
     * Set the minimum window size (width by height) that the window can be resized to.
     *
     * Set to `-1` to disable an axis.
     * @see maximumWindowSize
     * @since 0.18.4
     */
    var minimumWindowSize: Pair<Int, Int> = 100 to 100

    /**
     * Set the maximum window size (width by height) that the window can be resized to.
     *
     * Set to `-1` to disable an axis.
     * @see minimumWindowSize
     * @since 0.18.4
     */
    var maximumWindowSize: Pair<Int, Int> = -1 to -1

    /**
     * Set the window aspect ratio, with the ratio being width:height, e.g. `16 to 9` = `16:9`.
     *
     * While PolyUI will resize the content to this aspect ratio, if the window is not at this aspect ratio it may look odd (content smaller than the window).
     * To prevent this, you must ensure that the window implementation is only allowing resizes to this aspect ratio.
     *
     * Set to `-1` to disable, or `0` to infer it automatically. Any other value is the aspect ratio.
     * @since 0.18.4
     */
    var windowAspectRatio: Pair<Int, Int> = -1 to -1

    /**
     * This property will explicitly clean up after the initialization of PolyUI,
     * calling the garbage collector to free up temporary objects created during initialization.
     *
     * This option will usually halve or more the memory usage, but takes time (~10ms). It is recommended for memory limited scenarios and in production.
     *
     * @since 0.20.0
     */
    var cleanupAfterInit = true

    /**
     * This property will run the cleanup() method in a shutdown hook which will free memory used by PolyUI.
     *
     * @since 0.20.1
     */
    var cleanupOnShutdown = false

    /**
     * Set this to true to enable loading of translation keys upon startup, instead of the default behaviour of loading them when they are first used.
     * @since 0.21.1
     */
    var loadTranslationsOnInit = true

    /**
     * Enable parallel loading of translation files.
     * @since 0.21.1
     */
    var parallelLoading = false

    /** How to handle resource (image and font) loading errors.
     * @see resourcePolicy
     */
    enum class ResourcePolicy {
        /** Throw an exception and crash the program. */
        CRASH,

        /** Warn in the log and use the default resource (bundled with your rendering implementation). */
        WARN,
    }
}
