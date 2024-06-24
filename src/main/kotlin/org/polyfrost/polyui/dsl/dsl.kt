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

@file:OptIn(ExperimentalContracts::class)

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
import org.polyfrost.polyui.utils.image
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class DrawableDSL(@PublishedApi internal val _this: Drawable) {
    inline operator fun <S : Drawable> S.invoke(init: (DrawableDSL.(S) -> Unit) = {}): S {
        init(DrawableDSL(this), this)
        _this.addChild(this)
        return this
    }

    inline fun <S : Drawable> S.use(init: (DrawableDSL.(S) -> Unit) = {}): S {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        init(DrawableDSL(this), this)
        _this.addChild(this)
        return this
    }

    inline fun <S : Drawable> S.add(init: (DrawableDSL.(S) -> Unit) = {}): S {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        init(DrawableDSL(this), this)
        _this.addChild(this)
        return this
    }

    inline fun block(size: Vec2? = null, alignment: Align = AlignDefault, init: (DrawableDSL.(Block) -> Unit) = {}): Block {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = Block(size = size, alignment = alignment)
        init(DrawableDSL(o), o)
        _this.addChild(o)
        return o
    }

    inline fun image(image: PolyImage, alignment: Align = AlignDefault, init: (Image.() -> Unit) = {}): Image {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = Image(image = image, alignment = alignment)
        init(o)
        _this.addChild(o)
        return o
    }

    inline fun image(image: String, alignment: Align = AlignDefault, init: (Image.() -> Unit) = {}) {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        image(image.image(), alignment, init)
    }

    inline fun text(text: String, visibleSize: Vec2? = null, alignment: Align = AlignDefault, limited: Boolean = false, init: Text.() -> Unit = {}): Text {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = Text(text = text, visibleSize = visibleSize, alignment = alignment, limited = limited)
        init(o)
        _this.addChild(o)
        return o
    }

    inline fun textInput(text: String = "", visibleSize: Vec2? = null, placeholder: String = "polyui.textinput.placeholder", alignment: Align = AlignDefault, init: TextInput.() -> Unit = {}): TextInput {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = TextInput(text = text, visibleSize = visibleSize, placeholder = placeholder, alignment = alignment)
        init(o)
        _this.addChild(o)
        return o
    }

    inline fun group(alignment: Align = AlignDefault, init: DrawableDSL.(Group) -> Unit): Group {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = Group(alignment = alignment)
        init(DrawableDSL(o), o)
        _this.addChild(o)
        return o
    }

    class Master : DrawableDSL(Block()) {
        var size: Vec2? = null
        private var _renderer: Renderer? = null
        var settings = Settings()
        var inputManager: InputManager? = null
        var translator: Translator? = null
        var backgroundColor: PolyColor? = null
        var alignment: Align = Align(cross = Align.Cross.Start, pad = Vec2.ZERO)
        val colors: Colors = DarkTheme()

        var renderer: Renderer
            get() = _renderer ?: error("Renderer not set")
            set(value) {
                _renderer = value
            }

        fun build() = PolyUI(
            drawables = _this.children?.toTypedArray() ?: arrayOf(),
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

@PolyUIDSL
@JvmSynthetic
@ApiStatus.Experimental
inline fun Drawable.children(block: DrawableDSL.() -> Unit) = DrawableDSL(this).apply(block)
