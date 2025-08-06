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

import org.jetbrains.annotations.Contract
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.dsl.EventDSL
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.State
import org.polyfrost.polyui.operations.Fade
import org.polyfrost.polyui.operations.Move
import org.polyfrost.polyui.operations.Resize
import org.polyfrost.polyui.unit.SpawnPos
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.coerceWithin
import java.lang.ref.WeakReference
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.experimental.or

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

fun <S : Inputtable> S.onFocusLost(func: S.(Event.Focused.Lost) -> Unit): S {
    on(Event.Focused.Lost, func)
    return this
}

fun <S : Inputtable> S.onFocusGained(func: S.(Event.Focused.Gained) -> Unit): S {
    on(Event.Focused.Gained, func)
    return this
}

fun <S : Inputtable> S.onScroll(func: S.(Event.Mouse.Scrolled) -> Unit): S {
    on(Event.Mouse.Scrolled, func)
    return this
}

fun <S : Inputtable> S.onHover(func: S.(Event.Mouse.Entered) -> Unit): S {
    on(Event.Mouse.Entered, func)
    return this
}

fun <S : Inputtable> S.onHoverExit(func: S.(Event.Mouse.Exited) -> Unit): S {
    on(Event.Mouse.Exited, func)
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
 * Add a listener to this drawable for the given [state] property.
 * This is a convenience method for [State.listen].
 *
 * @since 1.12.0
 * @see State
 */
@OverloadResolutionByLambdaReturnType
@JvmName("onChangeStateZ")
fun <T, S : Inputtable> S.onChange(state: State<T>, func: S.(T) -> Boolean): S {
    // ASM: possible memory leak if the drawable is removed as the state would still hold a reference to it
    // so we use a WeakReference to avoid that
    val weakSelfRef = WeakReference(this)
    var listener: ((T) -> Boolean)? = null
    listener = {
        val self = weakSelfRef.get()
        if (self == null) {
            // If the drawable was garbage collected, we don't need to do anything
            state.removeListener(listener!!)
            false
        } else func(self, it)
    }
    state.listen(listener)
    return this
}

/**
 * Add a listener to this drawable for the given [state] property.
 * This is a convenience method for [State.listen].
 *
 * @since 1.12.0
 * @see State
 */
@OverloadResolutionByLambdaReturnType
@JvmName("onChangeState")
fun <T, S : Inputtable> S.onChange(state: State<T>, func: S.(T) -> Unit): S {
    onChange(state) { func(this, it); false }
    return this
}

/**
 * Add a listener for changes to the given String-type property.
 *
 * In PolyUI, this is for [Text] and [TextInput][org.polyfrost.polyui.component.impl.TextInput] only.
 * @since 1.0.6
 */
@JvmName("onChangeStringZ")
@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onChange(func: S.(String) -> Boolean): S {
    on(Event.Change.Text) {
        val res = func.invoke(this, it.text)
        it.cancelled = res
        res
    }
    return this
}

/**
 * Add a listener for changes to the given String-type property.
 *
 * In PolyUI, this is for [Text] and [TextInput][org.polyfrost.polyui.component.impl.TextInput] only.
 * @since 1.0.6
 */
@JvmName("onChangeString")
@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onChange(func: S.(String) -> Unit): S {
    on(Event.Change.Text) {
        func.invoke(this, it.text)
        false
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
@JvmName("onChangeStateZ")
@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onChange(func: S.(Boolean) -> Boolean): S {
    on(Event.Change.State) {
        val res = func.invoke(this, it.state)
        it.cancelled = res
        res
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
@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onChange(func: S.(Boolean) -> Unit): S {
    on(Event.Change.State) {
        func.invoke(this, it.state)
        false
    }
    return this
}

/**
 * Add a listener for changes to the given Int-type property.
 *
 * In PolyUI, this is for [Radiobutton][org.polyfrost.polyui.component.impl.Radiobutton], and [Dropdown][org.polyfrost.polyui.component.impl.Dropdown] only.
 * @since 1.0.6
 */
@JvmName("onChangeIndexZ")
@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onChange(func: S.(Int) -> Boolean): S {
    on(Event.Change.Number) {
        val res = func.invoke(this, it.amount.toInt())
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
@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onChange(func: S.(Int) -> Unit): S {
    on(Event.Change.Number) {
        func.invoke(this, it.amount.toInt())
        false
    }
    return this
}

/**
 * Add a listener for changes to the given Float-type property.
 *
 * In PolyUI, this is for [Slider][org.polyfrost.polyui.component.impl.Slider] only.
 * @since 1.0.6
 */
@JvmName("onChangeNumberZ")
@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onChange(func: S.(Float) -> Boolean): S {
    on(Event.Change.Number) {
        val res = func.invoke(this, it.amount.toFloat())
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
@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onChange(func: S.(Float) -> Unit): S {
    on(Event.Change.Number) {
        func.invoke(this, it.amount.toFloat())
        false
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
    val v = value.coerceIn(min, max)
    val bar = this[0]
    val ptr = this[1]
    ptr.x = bar.x + (bar.width - ptr.width) * ((v - min) / (max - min))
    bar[0].width = ptr.x - bar.x + (ptr.width / 2f)
    if (dispatch && hasListenersFor(Event.Change.Number::class.java)) accept(Event.Change.Number(v))
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
 * Set the currently selected entry of this radiobutton to [index] (input is coerced into the range of the radiobutton).
 *
 * **Ensure this object is a radiobutton before running this method, as otherwise strange errors may occur**.
 * if it is obvious to the system that this is not a radiobutton, an [IllegalArgumentException] will be thrown.
 *
 * @since 1.7.2
 */
fun <S : Inputtable> S.setRadiobuttonEntry(index: Int): S {
    val max = (children?.size ?: throw IllegalArgumentException("Receiver $this is not a radiobutton")) - 1
    val i = index.coerceIn(0, max)
    val selector = this[0]
    val selected = this[i + 1]
    if (selector.x == selected.x) return this
    Move(selector, selected.at, add = false, animation = Animations.Default.create(0.15.seconds)).add()
    Resize(selector, selected.size, add = false, animation = Animations.Default.create(0.15.seconds)).add()
    if (hasListenersFor(Event.Change.Number)) accept(Event.Change.Number(i))
    return this
}


/**
 * Spawn the given component at the mouse position, or above/below it.
 * **The component must be focusable**.
 *
 * @param openNow if true, the component will be focused immediately after spawning. [polyUI] **must not be null** in this case.
 * @param position the position to spawn the component at, relative to the mouse position. **if the component was created with a set position, this will be ignored, and it will spawn at that position instead**.
 * @since 1.12.0
 * @see org.polyfrost.polyui.component.impl.PopupMenu
 */
@Contract("null, true, _ -> fail")
fun <S : Inputtable> S.spawnAtMouse(polyUI: PolyUI?, openNow: Boolean = true, position: SpawnPos = SpawnPos.AtMouse): S {
    require(focusable) { "Component $this must be focusable to spawn at mouse" }
    this.events {
        Event.Focused.Gained then {
            this.polyUI.master.addChild(this, recalculate = false)
            if(!this.createdWithSetPosition) {
                val mx = this.polyUI.mouseX
                val my = this.polyUI.mouseY
                val sz = this.polyUI.size
                when (position) {
                    SpawnPos.AtMouse -> {
                        x = mx.coerceWithin(0f, sz.x - this.width)
                        y = my.coerceWithin(0f, sz.y - this.height)
                    }

                    SpawnPos.AboveMouse -> {
                        x = (mx - (this.width / 2f)).coerceWithin(0f, sz.x - this.width)
                        y = (my - this.height - 6f).coerceWithin(0f, sz.y - this.height)
                    }

                    SpawnPos.BelowMouse -> {
                        x = (mx - (this.width / 2f)).coerceWithin(0f, sz.x - this.width)
                        y = (my + 12f).coerceWithin(0f, sz.y - this.height)
                    }
                }
            }
            // we are going to set #created-with-set-position to true, so that the position is not recalculated again
            this.layoutFlags = this.layoutFlags or 0b00000100
            if (this is Drawable) {
                alpha = 0f
                fadeIn(0.2.seconds)
            }
            true
        }

        Event.Focused.Lost then {
            if (this is Drawable) {
                Fade(this, 0f, false, Animations.Default.create(0.2.seconds)) {
                    this.polyUI.master.removeChild(this, recalculate = false)
                }.add()
            } else {
                this.polyUI.master.removeChild(this, recalculate = false)
            }
        }
    }

    if (openNow) {
        require(polyUI != null) { "polyUI cannot be null if openNow is true" }
        polyUI.focus(this)
    }
    return this
}
