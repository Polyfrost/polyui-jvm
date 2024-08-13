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

package org.polyfrost.polyui.utils

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.Settings
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.input.InputManager
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2

open class PolyUIBuilder {
    private var manager: InputManager? = null
    private var renderer: Renderer? = null
    private var translator: Translator? = null
    private var alignment = AlignDefault
    private var colors: Colors = DarkTheme()
    private var backgroundColor: PolyColor? = null
    val settings = Settings()
    var size = Vec2.ZERO
    private var pauses = false
    private var blurs = false

    fun input(manager: InputManager?): PolyUIBuilder {
        this.manager = manager
        return this
    }

    fun translator(translator: Translator?): PolyUIBuilder {
        this.translator = translator
        return this
    }

    fun translatorDelegate(translationDir: String): PolyUIBuilder {
        val translator = this.translator ?: Translator(settings, "", null).also { this.translator = it }
        translator.addDelegate(translationDir)
        return this
    }

    fun align(alignment: Align): PolyUIBuilder {
        this.alignment = alignment
        return this
    }

    fun pauses(): PolyUIBuilder {
        pauses = true
        return this
    }

    fun blurs(): PolyUIBuilder {
        blurs = true
        return this
    }

    fun colors(colors: Colors): PolyUIBuilder {
        this.colors = colors
        return this
    }

    inline fun configure(block: Settings.() -> Unit): PolyUIBuilder {
        settings.block()
        return this
    }

    fun backgroundColor(color: PolyColor?): PolyUIBuilder {
        this.backgroundColor = color
        return this
    }

    fun renderer(renderer: Renderer?): PolyUIBuilder {
        this.renderer = renderer
        return this
    }

    fun size(width: Float, height: Float): PolyUIBuilder {
        this.size = Vec2(width, height)
        return this
    }

    fun make(vararg components: Component): PolyUI {
        val renderer = renderer
        require(renderer != null) { "Renderer must be set" }
        return PolyUI(*components, renderer = renderer, settings = settings, inputManager = manager, translator = translator, backgroundColor = backgroundColor, masterAlignment = alignment, colors = colors, size = size)
    }

    companion object {
        @JvmStatic
        fun builder() = PolyUIBuilder()
    }
}
