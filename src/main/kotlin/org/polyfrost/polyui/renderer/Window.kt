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

package org.polyfrost.polyui.renderer

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.event.InputManager
import org.polyfrost.polyui.renderer.data.Cursor

/** # Window
 * This class represents the physical window that PolyUI will render to.
 * As a window implementation, you must:
 *  - implement these methods
 *  - call [PolyUI.resize] when the window is resized
 *  - call [PolyUI.render] every frame, inside a rendering loop started with [open]. It may be blocking or non-blocking.
 *  - callbacks for [InputManager.mousePressed]
 *  - callbacks for [InputManager.mouseReleased]
 *  - callbacks for [InputManager.mouseMoved]
 *  - callbacks for [InputManager.mouseScrolled]
 *  - callbacks for [InputManager.keyDown]
 *  - callbacks for [InputManager.keyUp]
 *  - callbacks for [InputManager.keyTyped]
 *  - callbacks for [InputManager.filesDropped]
 */
abstract class Window(open var width: Int, open var height: Int, open var pixelRatio: Float = 1f) {
    /**
     * open the window with the specified [PolyUI] instance, and then start the rendering loop. It may be blocking or non-blocking.
     * Every frame, call the [PolyUI.render] function to render the UI.
     */
    abstract fun open(polyUI: PolyUI): Window

    /** destroy the window. */
    abstract fun close()

    /**
     * this function hooks into the rendering loop. it is ran before a render cycle.
     * @see postRender
     * @since 1.1.3
     */
    open fun preRender(renderer: Renderer) {}

    /**
     * this function hooks into the rendering loop. it is ran after a render cycle.
     * @see preRender
     * @since 1.1.3
     */
    open fun postRender(renderer: Renderer) {}

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
