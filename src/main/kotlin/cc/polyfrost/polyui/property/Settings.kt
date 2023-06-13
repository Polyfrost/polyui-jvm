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

package cc.polyfrost.polyui.property

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

    /** set this to something other than 0 to set a framerate cap for the UI.
     * @see unfocusedFPS */
    var maxFPS = 0

    /** set this to something other than 0 to change the framerate from the current [maxFPS] to this value when it is unfocused. */
    var unfocusedFPS = 10

    var useAntialiasing = true
    var enableVSync = false

    /** @see cc.polyfrost.polyui.input.PolyTranslator */
    var defaultLocale = "en_xx"

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
     * @see [cc.polyfrost.polyui.layout.Layout.countDrawables]
     */
    var minDrawablesForFramebuffer: Int = 30

    /**
     * Weather or not the system should promote [DraggableLayouts][cc.polyfrost.polyui.layout.impl.extension.DraggableLayout]
     * to the top of the render queue, so they are rendered on top of other layouts (they are rendered last)
     *
     * They will still retain their order relative to each other.
     *
     * @since 0.18.2
     */
    var draggablesOnTop = true

    /**
     * Enable or disable framebuffers. Please note that PolyUI is designed to work with framebuffers, so disabling them may cause performance issues.
     * @see cc.polyfrost.polyui.PolyUI
     * @since 0.18.0
     */
    var framebuffersEnabled = true

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
    var scrollMultiplier: Pair<Int, Int> = 60 to 60

    /** Weather to invert the scroll direction */
    var naturalScrolling = false

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
