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

package org.polyfrost.polyui.renderer

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.renderer.data.Cursor

/** # Window
 * This class represents the physical window that PolyUI will render to.
 * As a rendering implementation, you must:
 *  - implement all the methods
 *  - create callbacks for the event methods in PolyUI e.g. [PolyUI.resize]
 *  - create callbacks for all the event-related methods in [org.polyfrost.polyui.event.EventManager]
 *  - call [open] to start the rendering loop; this can be blocking or non-blocking. Please note that after open is called, the rendering implementation will be created. This means that in a thread-based system such as LWJGL's OpenGL, you **must** ensure that it is fully setup before exiting `init {}`.
 */
abstract class Window(open var width: Int, open var height: Int) {
    /** open the window with the specified [PolyUI] instance, and then start the rendering loop. It may be blocking or non-blocking. */
    abstract fun open(polyUI: PolyUI): Window

    /** destroy the window. */
    abstract fun close()

    /** Create the callbacks for window events.
     * @see Window */
    abstract fun createCallbacks()

    /**
     * Return true if your window supports "render pausing", a optimization technique which will not render any frames if not necessary.
     *
     * See [Settings.renderPausing][org.polyfrost.polyui.property.Settings.renderPausingEnabled] for a better explanation.
     * @since 0.25.1
     */
    abstract fun supportsRenderPausing(): Boolean

    /**
     * Get the clipboard string. This is used for the [org.polyfrost.polyui.component.impl.TextInput] class.
     *
     * If the data is empty or not a string, then null is returned.
     */
    abstract fun getClipboard(): String?

    /**
     * Set the clipboard string. This is used for the [org.polyfrost.polyui.component.impl.TextInput] class.
     */
    abstract fun setClipboard(text: String?)

    /**
     * Set the cursor design.
     * @since 0.18.4
     */
    abstract fun setCursor(cursor: Cursor)

    /**
     * Return the name of a key that is not mapped by PolyUI.
     * @see org.polyfrost.polyui.input.Keys
     */
    abstract fun getKeyName(key: Int): String
}
