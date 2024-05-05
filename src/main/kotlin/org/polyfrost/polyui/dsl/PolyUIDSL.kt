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
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.event.InputManager
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.property.Settings
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2

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

    @PolyUIDSL
    fun block(size: Vec2? = null, alignment: Align = AlignDefault, init: (DrawableDSL.(Block) -> Unit)? = null): Block {
        val o = Block(size = size, alignment = alignment).apply { init?.invoke(DrawableDSL(this), this) }
        _this.addChild(o)
        return o
    }

    @PolyUIDSL
    fun image(image: PolyImage, alignment: Align = AlignDefault, init: (Image.() -> Unit)? = null): Image {
        val o = Image(image = image, alignment = alignment).apply { init?.invoke(this) }
        _this.addChild(o)
        return o
    }

    @PolyUIDSL
    fun text(text: String, alignment: Align = AlignDefault, init: (Text.() -> Unit)? = null): Text {
        val o = Text(text = text, alignment = alignment).apply { init?.invoke(this) }
        _this.addChild(o)
        return o
    }

    @PolyUIDSL
    fun textInput(text: String, placeholder: String = "polyui.textinput.placeholder", alignment: Align = AlignDefault, init: (TextInput.() -> Unit)? = null): TextInput {
        val o = TextInput(text = text, placeholder = placeholder, alignment = alignment).apply { init?.invoke(this) }
        _this.addChild(o)
        return o
    }

    @PolyUIDSL
    fun spacer(size: Vec2, init: (DrawableDSL.() -> Unit)? = null) {
        _this.addChild(DrawableDSL(Spacer(size = size)).apply { init?.invoke(this) }._this)
    }

    @PolyUIDSL
    fun group(alignment: Align = AlignDefault, init: DrawableDSL.(Group) -> Unit): Group {
        val o = Group(alignment = alignment).apply { init.invoke(DrawableDSL(this), this) }
        _this.addChild(o)
        return o
    }

    class PolyUI : DrawableDSL(Block()) {
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
    }
}

@DslMarker
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
private annotation class PolyUIDSL


@PolyUIDSL
@JvmSynthetic
@ApiStatus.Experimental
fun polyUI(block: DrawableDSL.PolyUI.() -> Unit): PolyUI {
    val d = DrawableDSL.PolyUI().apply(block)
    return PolyUI(drawables = d._this.children!!.toTypedArray(), d.renderer, d.settings, d.inputManager, d.translator, d.backgroundColor, d.alignment, d.colors, d.size)
}
