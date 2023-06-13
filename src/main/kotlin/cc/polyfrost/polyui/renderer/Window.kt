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

package cc.polyfrost.polyui.renderer

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.renderer.data.Cursor

/** # Window
 * This class represents the physical window that PolyUI will render to.
 * As a rendering implementation, you must:
 *  - implement all the methods
 *  - create callbacks for the event methods in PolyUI e.g. [PolyUI.onResize]
 *  - create callbacks for all the event-related methods in [cc.polyfrost.polyui.event.EventManager]
 *  - call [open] to start the rendering loop; this can be blocking or non-blocking. Please note that after open is called, the rendering implementation will be created. This means that in a thread-based system such as LWJGL's OpenGL, you **must** ensure that it is fully setup before exiting `init {}`.
 */
abstract class Window(open var width: Int, open var height: Int) {
    /** open the window with the specified [PolyUI] instance. This will call [videoSettingsChanged], [createCallbacks] and then start the rendering loop. It may be blocking or non-blocking. */
    abstract fun open(polyUI: PolyUI): Window

    /** destroy the window. */
    abstract fun close()

    /** Create the callbacks for window event.
     * @see Window */
    abstract fun createCallbacks()

    /** this function is called whenever a video setting is changed. Use this to set all settings. It also should be called in the [open] function to initialize the settings. */
    abstract fun videoSettingsChanged()

    /**
     * Get the clipboard string. This is used for the [cc.polyfrost.polyui.component.impl.TextInput] class.
     *
     * If the data is empty or not a string, then null is returned.
     */
    abstract fun getClipboard(): String?

    /**
     * Set the clipboard string. This is used for the [cc.polyfrost.polyui.component.impl.TextInput] class.
     */
    abstract fun setClipboard(text: String?)

    /**
     * Set the cursor design.
     * @since 0.18.4
     */
    abstract fun setCursor(cursor: Cursor)
}
