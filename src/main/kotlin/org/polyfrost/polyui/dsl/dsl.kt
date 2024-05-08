/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2024 Polyfrost and its contributors.
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

package org.polyfrost.polyui.dsl

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.Group
import org.polyfrost.polyui.event.InputManager
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.property.Settings
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.component.impl.Block as Block0
import org.polyfrost.polyui.component.impl.Image as Image0
import org.polyfrost.polyui.component.impl.Spacer as Spacer0
import org.polyfrost.polyui.component.impl.Text as Text0
import org.polyfrost.polyui.component.impl.TextInput as TextInput0

open class DrawableDSL private constructor(val _this: Drawable) {
    operator fun <S : Drawable> S.invoke(init: (DrawableDSL.(S) -> Unit)? = null): S {
        val s = this.apply { init?.invoke(DrawableDSL(this), this) }
        _this.addChild(s)
        return s
    }

    fun <S : Drawable> S.use(init: (DrawableDSL.(S) -> Unit)? = null): S {
        val s = this.apply { init?.invoke(DrawableDSL(this), this) }
        _this.addChild(s)
        return s
    }

    fun <S : Drawable> S.add(init: (DrawableDSL.(S) -> Unit)? = null): S {
        val s = this.apply { init?.invoke(DrawableDSL(this), this) }
        _this.addChild(s)
        return s
    }

    fun Block(size: Vec2? = null, alignment: Align = AlignDefault, init: (DrawableDSL.(Block0) -> Unit)? = null): Block0 {
        val o = Block0(size = size, alignment = alignment).apply { init?.invoke(DrawableDSL(this), this) }
        _this.addChild(o)
        return o
    }

    fun Image(image: PolyImage, alignment: Align = AlignDefault, init: (Image0.() -> Unit)? = null): Image0 {
        val o = Image0(image = image, alignment = alignment).apply { init?.invoke(this) }
        _this.addChild(o)
        return o
    }

    fun Text(text: String, alignment: Align = AlignDefault, init: (Text0.() -> Unit)? = null): Text0 {
        val o = Text0(text = text, alignment = alignment).apply { init?.invoke(this) }
        _this.addChild(o)
        return o
    }

    fun TextInput(text: String, placeholder: String = "polyui.textinput.placeholder", alignment: Align = AlignDefault, init: (TextInput0.() -> Unit)? = null): TextInput0 {
        val o = TextInput0(text = text, placeholder = placeholder, alignment = alignment).apply { init?.invoke(this) }
        _this.addChild(o)
        return o
    }

    fun Spacer(size: Vec2, init: (DrawableDSL.() -> Unit)? = null) {
        _this.addChild(DrawableDSL(Spacer0(size = size)).apply { init?.invoke(this) }._this)
    }

    fun Group(alignment: Align = AlignDefault, init: DrawableDSL.(Group) -> Unit): Group {
        val o = Group(alignment = alignment).apply { init.invoke(DrawableDSL(this), this) }
        _this.addChild(o)
        return o
    }

    class Master : DrawableDSL(Block0()) {
        var size: Vec2? = null
        private var _renderer: Renderer? = null
        var settings = Settings()
        var inputManager: InputManager? = null
        var translator: Translator? = null
        var backgroundColor: PolyColor? = null
        var alignment: Align = Align(cross = Align.Cross.Start, padding = Vec2.ZERO)
        val colors: Colors = DarkTheme()

        var renderer: Renderer
            get() = _renderer ?: error("Renderer not set")
            set(value) {
                _renderer = value
            }

        fun build() = PolyUI(
            drawables = _this.children!!.toTypedArray(),
            renderer, settings, inputManager, translator, backgroundColor,
            alignment, colors, size
        )
    }
}

@DslMarker
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
private annotation class PolyUIDSL


@PolyUIDSL
@JvmSynthetic
@ApiStatus.Experimental
inline fun polyUI(block: DrawableDSL.Master.() -> Unit) = DrawableDSL.Master().apply(block).build()
