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

package org.polyfrost.polyui.component.extensions

import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.dsl.EventDSL
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.unit.seconds
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@EventDSL.Marker
@OptIn(ExperimentalContracts::class)
inline fun <S : Inputtable> S.events(dsl: EventDSL<S>.() -> Unit): S {
    contract {
        callsInPlace(dsl, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    EventDSL(this).apply(dsl)
    return this
}

fun <S : Inputtable> S.onInit(function: S.(Event.Lifetime.Init) -> Unit): S {
    if (initialized) function(Event.Lifetime.Init)
    on(Event.Lifetime.Init, function)
    return this
}

fun <S : Inputtable> S.afterInit(function: S.(Event.Lifetime.PostInit) -> Unit): S {
    if (initialized) function(Event.Lifetime.PostInit)
    on(Event.Lifetime.PostInit, function)
    return this
}

/**
 * Add an event handler to this drawable's parent, and (optionally) it's parent's parent ([depth]).
 *
 * This is used to run functions that may move or resize this drawable,
 * as if they were ran under this [Event.Lifetime.PostInit] they would be overwritten by the positioning logic.
 */
fun <S : Inputtable> S.afterParentInit(depth: Int = 1, handler: S.() -> Unit): S {
    this.on(Event.Lifetime.PostInit) { _ ->
        var it: Component = parent // will die if parent is null
        for (i in 0 until depth) {
            it._parent?.let { parent -> it = parent } ?: break
        }
        (it as? Inputtable)?.on(Event.Lifetime.PostInit) {
            handler(this@afterParentInit)
        } ?: throw IllegalArgumentException("Parent at depth $depth is not an Inputtable")
        false
    }
    return this
}

fun <S : Inputtable> S.onScroll(func: S.(Event.Mouse.Scrolled) -> Unit): S {
    on(Event.Mouse.Scrolled, func)
    return this
}

@OverloadResolutionByLambdaReturnType
@JvmName("onClickZ")
fun <S : Inputtable> S.onClick(func: S.(Event.Mouse.Clicked) -> Boolean): S {
    on(Event.Mouse.Clicked, func)
    return this
}

@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onClick(func: S.(Event.Mouse.Clicked) -> Unit): S {
    on(Event.Mouse.Clicked, func)
    return this
}

fun <S : Inputtable> S.onRightClick(func: S.(Event.Mouse.Clicked) -> Unit): S {
    on(Event.Mouse.Clicked(1), func)
    return this
}

fun <S : Inputtable> S.onPress(func: S.(Event.Mouse.Pressed) -> Unit): S {
    on(Event.Mouse.Pressed, func)
    return this
}

fun <S : Inputtable> S.onRelease(func: S.(Event.Mouse.Released) -> Unit): S {
    on(Event.Mouse.Released, func)
    return this
}

/**
 * Add a listener for changes to the given String-type property.
 *
 * In PolyUI, this is for [Text] and [TextInput][org.polyfrost.polyui.component.impl.TextInput] only.
 * @since 1.0.6
 */
@JvmName("onChangeString")
fun <S : Inputtable> S.onChange(func: S.(String) -> Boolean): S {
    on(Event.Change.Text) {
        val res = func.invoke(this, it.text)
        it.cancelled = res
        res
    }
    return this
}


/**
 * Use this to add a simple listener for the [Event.Change.State] event.
 * @see [toggleable]
 * @since 1.5.0
 */
fun <S : Inputtable> S.onToggle(func: S.(Boolean) -> Unit): S {
    on(Event.Change.State) {
        func.invoke(this, it.state)
        false
    }
    return this
}


/**
 * Add a listener for changes to the given Boolean-type property.
 *
 * In PolyUI, this is for [Checkbox][org.polyfrost.polyui.component.impl.Checkbox] and [Switch][org.polyfrost.polyui.component.impl.Switch] only.
 * @since 1.0.6
 */
@JvmName("onChangeState")
fun <S : Inputtable> S.onChange(func: S.(Boolean) -> Boolean): S {
    on(Event.Change.State) {
        val res = func.invoke(this, it.state)
        it.cancelled = res
        res
    }
    return this
}

/**
 * Add a listener for changes to the given Int-type property.
 *
 * In PolyUI, this is for [Radiobutton][org.polyfrost.polyui.component.impl.Radiobutton], and [Dropdown][org.polyfrost.polyui.component.impl.Dropdown] only.
 * @since 1.0.6
 */
@JvmName("onChangeIndex")
fun <S : Inputtable> S.onChange(func: S.(Int) -> Boolean): S {
    on(Event.Change.Number) {
        val res = func.invoke(this, it.amount.toInt())
        it.cancelled = res
        res
    }
    return this
}

/**
 * Add a listener for changes to the given Float-type property.
 *
 * In PolyUI, this is for [Slider][org.polyfrost.polyui.component.impl.Slider] only.
 * @since 1.0.6
 */
@JvmName("onChangeNumber")
fun <S : Inputtable> S.onChange(func: S.(Float) -> Boolean): S {
    on(Event.Change.Number) {
        val res = func.invoke(this, it.amount.toFloat())
        it.cancelled = res
        res
    }
    return this
}


/**
 * Set the value on this slider. Unfortunately due to current limitations you must provide the [min] and [max] values that were used for this slider.
 *
 * **Ensure this object is a slider before running this method, as otherwise strange errors may occur**.
 *
 * @since 1.7.2
 */
fun <S : Inputtable> S.setSliderValue(value: Float, min: Float = 0f, max: Float = 100f, dispatch: Boolean = true): S {
    val bar = this[0]
    val ptr = this[1]
    ptr.x = bar.x + (bar.width - ptr.width) * ((value - min) / (max - min))
    bar[0].width = ptr.x - bar.x + (ptr.width / 2f)
    if (dispatch && hasListenersFor(Event.Change.Number::class.java)) accept(Event.Change.Number(value))
    return this
}

/**
 * Toggle any element to the given [state] provided it was made with the [toggleable] method.
 *
 * @since 1.7.2
 */
fun <S : Drawable> S.toggle(state: Boolean): S {
    if (palette == polyUI.colors.brand.fg && state) return this
    if (palette == polyUI.colors.component.bg && !state) return this
    accept(Event.Mouse.Clicked)
    return this
}

/**
 * Set the currently selected entry of this radiobutton to [index] (input is range-checked).
 *
 * **Ensure this object is a radiobutton before running this method, as otherwise strange errors may occur**.
 *
 * @since 1.7.2
 */
fun <S : Inputtable> S.setRadiobuttonEntry(index: Int): S {
    val max = (children?.size ?: 0) - 1
    require(index in 0..<max) { "Index $index is out of bounds for a radiobutton with only $max entries" }
    val selector = this[0]
    val selected = this[index + 1]
    if (selector.x == selected.x) return this
    Move(selector, selected.at, add = false, animation = Animations.Default.create(0.15.seconds)).add()
    Resize(selector, selected.size, add = false, animation = Animations.Default.create(0.15.seconds)).add()
    if (hasListenersFor(Event.Change.Number)) accept(Event.Change.Number(index))
    return this
}
