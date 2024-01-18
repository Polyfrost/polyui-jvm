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

package org.polyfrost.polyui.renderer.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.renderer.Window
import org.polyfrost.polyui.renderer.data.Cursor

class NoOpWindow(title: String, width: Int, height: Int) : Window(width, height) {
    override fun open(polyUI: PolyUI): Window {
        while (true) {
            polyUI.render()
        }
    }

    override fun close() {
    }

    override fun getClipboard(): String? = null

    override fun setClipboard(text: String?) {
    }

    override fun setCursor(cursor: Cursor) {
    }

    override fun getKeyName(key: Int) = "unknown"

    override fun supportsRenderPausing() = false
}
