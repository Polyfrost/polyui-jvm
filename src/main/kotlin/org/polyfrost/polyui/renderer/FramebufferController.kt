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

import org.polyfrost.polyui.data.Framebuffer

/**
 * Implement this interface to allow the PolyUI instance to use framebuffers.
 *
 * This enables many optimizations which can improve performance. There are configuration values in [org.polyfrost.polyui.Settings] which can be used to control this.
 * @since 1.7.0
 */
interface FramebufferController {
    /** Create a new framebuffer. It is down to you (as a rendering implementation) to cache this, and dispose of it as necessary.
     * @return a PolyUI framebuffer object using the width and height passed to this method. This is used by PolyUI to identify it.
     */
    fun createFramebuffer(width: Float, height: Float): Framebuffer

    /** Bind the given framebuffer. */
    fun bindFramebuffer(fbo: Framebuffer)

    /** Unbind the currently bound framebuffer. */
    fun unbindFramebuffer()

    /** draw the given framebuffer to the screen. */
    fun drawFramebuffer(
        fbo: Framebuffer,
        x: Float,
        y: Float,
        width: Float = fbo.width,
        height: Float = fbo.height,
    )

    /** Delete the given framebuffer. Ignore if null. */
    fun delete(fbo: Framebuffer?)
}
