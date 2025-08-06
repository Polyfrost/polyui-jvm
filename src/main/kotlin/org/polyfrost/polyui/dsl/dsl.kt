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
import org.polyfrost.polyui.Settings
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.input.InputManager
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.SpawnPos
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.image
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class DrawableDSL<T : Drawable>(@PublishedApi internal val _this: T) {
    inline operator fun <S : Drawable> S.invoke(init: (DrawableDSL<S>.(S) -> Unit) = {}): S {
        init(DrawableDSL(this), this)
        _this.addChild(this)
        return this
    }

    inline fun configure(init: T.() -> Unit): T {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        init(_this)
        return _this
    }

    inline fun <S : Drawable> S.use(init: (DrawableDSL<S>.(S) -> Unit) = {}): S {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        init(DrawableDSL(this), this)
        _this.addChild(this)
        return this
    }

    inline fun <S : Drawable> S.add(init: (DrawableDSL<S>.(S) -> Unit) = {}): S {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        init(DrawableDSL(this), this)
        _this.addChild(this)
        return this
    }

    @JvmName("block")
    inline fun block(size: Vec2 = Vec2.ZERO, alignment: Align = AlignDefault, init: (DrawableDSL<Block>.(Block) -> Unit) = {}): Block {
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

    inline fun image(image: String, alignment: Align = AlignDefault, init: (Image.() -> Unit) = {}): Image {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        return image(image.image(), alignment, init)
    }

    @JvmName("text")
    inline fun text(text: String, visibleSize: Vec2 = Vec2.ZERO, alignment: Align = AlignDefault, limited: Boolean = false, init: Text.() -> Unit = {}): Text {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = Text(text = text, visibleSize = visibleSize, alignment = alignment, limited = limited)
        init(o)
        _this.addChild(o)
        return o
    }

    @JvmName("popup")
    inline fun popup(vararg children: Component?, size: Vec2 = Vec2.ZERO, alignment: Align = AlignDefault, position: SpawnPos = SpawnPos.AtMouse, init: Block.() -> Unit = {}): Block {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = PopupMenu(children = children, size, alignment, _this.polyUI, true, position)
        init(o)
        return o
    }

    @JvmName("textInput")
    inline fun textInput(text: String = "", visibleSize: Vec2 = Vec2.ZERO, placeholder: String = "polyui.textinput.placeholder", alignment: Align = AlignDefault, init: TextInput.() -> Unit = {}): TextInput {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = TextInput(text = text, visibleSize = visibleSize, placeholder = placeholder, alignment = alignment)
        init(o)
        _this.addChild(o)
        return o
    }

    @JvmName("group")
    inline fun group(size: Vec2 = Vec2.ZERO, visibleSize: Vec2 = Vec2.ZERO, alignment: Align = AlignDefault, init: DrawableDSL<Group>.(Group) -> Unit): Group {
        contract {
            callsInPlace(init, InvocationKind.EXACTLY_ONCE)
        }
        val o = Group(size = size, visibleSize = visibleSize, alignment = alignment)
        init(DrawableDSL(o), o)
        _this.addChild(o)
        return o
    }

    class Master : DrawableDSL<Block>(Block()) {
        @get:JvmName("getSize")
        @set:JvmName("setSize")
        var size: Vec2 = Vec2.ZERO
        private var _renderer: Renderer? = null
        var settings = Settings()
        var inputManager: InputManager? = InputManager(settings)
        val keyBinder get() = inputManager?.keyBinder
        var translator: Translator? = null
        var backgroundColor: PolyColor? = null
        var alignment: Align = Align(line = Align.Line.Start, pad = Vec2.ZERO)
        var colors: Colors? = null

        var renderer: Renderer
            get() = _renderer ?: error("Renderer not set")
            set(value) {
                _renderer = value
            }

        fun build() = PolyUI(
            components = _this.children?.toTypedArray() ?: arrayOf(),
            renderer, settings, inputManager, translator, backgroundColor,
            alignment, colors ?: DarkTheme(), size
        ).also { _this.children?.clear() }
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
inline fun <T : Drawable> T.children(block: DrawableDSL<T>.() -> Unit) = DrawableDSL(this).apply(block)
