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

import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.Scrollable
import org.polyfrost.polyui.component.impl.PopupMenu
import org.polyfrost.polyui.component.impl.TextInput
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.State
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.Clock
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.toString
import kotlin.math.abs


/**
 * Add some text that is shown when the mouse is left still over this drawable
 * for 1 second or more.
 * @since 1.0.3
 */
@JvmName("addHoverInfo")
fun <S : Inputtable> S.addHoverInfo(vararg drawables: Drawable?, size: Vec2 = Vec2.ZERO, align: Align = AlignDefault, position: SpawnPos = SpawnPos.AboveMouse): S {
    var mx = 0f
    var my = 0f
    val popup = PopupMenu(*drawables, size = size, align = align, polyUI = null, openNow = false, spawnPos = position)
    val exe = Clock.Bomb(0.5.seconds) {
        polyUI.focus(popup)
    }
    on(Event.Mouse.Entered) {
        mx = polyUI.mouseX
        my = polyUI.mouseY
        polyUI.addExecutor(exe)
    }
    on(Event.Mouse.Exited) {
        if (popup.focused) polyUI.unfocus()
        polyUI.removeExecutor(exe)
    }
    val stop: (S.(Event) -> Boolean) = {
        if (popup.focused) {
            if (abs(mx - polyUI.mouseX) > 3f || abs(my - polyUI.mouseY) > 3f) {
                polyUI.unfocus()
                polyUI.addExecutor(exe)
            }
        }
        exe.refuse()
        false
    }
    on(Event.Mouse.Moved, stop)
    on(Event.Mouse.Drag, stop)
    on(Event.Mouse.Scrolled, stop)
    return this
}

/**
 * Make this [TextInput] only accept numbers between [min] and [max]. It will then dispatch a [Event.Change.Number] event.
 *
 * If [integral] is true, it will only accept integers.
 *
 * @since 1.5.0
 */
fun <S : TextInput> S.numeric(min: Float = 0f, max: Float = 100f, initial: Float = min, integral: Boolean = false, ignoreSuffix: String? = null): State<Float> {
    val state = State(initial)
    var lock = false
    theText.listen {
        if (lock) return@listen false
        val value = if (ignoreSuffix != null) it.removeSuffix(ignoreSuffix) else it
        if (value.isEmpty()) return@listen false
        // don't fail when the user types a minus sign (and minus values are allowed)
        if (value == "-") {
            if (min < 0f) return@listen false
            else return@listen true
        }

        if (integral && value.contains('.')) return@listen true
        // silently cancel if they try and type multiple zeroes
        if (value == "-00") return@listen true
        if (value == "00") return@listen true
        val v = value.toFloatOrNull()?.coerceIn(min, max)
        if(v == null) {
            // fail when not a number
            shake(); return@listen true
        }
        lock = true
        val ret = state.set(v)
        lock = false
        ret
    }
    state.listen {
        if (lock) return@listen false
        lock = true
        text = "${if (integral) it.toInt() else it.toString(dps = 2)}${ignoreSuffix ?: ""}"
        lock = false
        false
    }

    on(Event.Focused.Lost) {
        val txt = if(ignoreSuffix != null) text.removeSuffix(ignoreSuffix) else text
        val value = txt.toFloatOrNull()
        if (value == null || value < min) {
            text = "${if (integral) min.toInt() else min.toString(dps = 2)}"
            shake()
        } else if (value > max) {
            text = "${if (integral) max.toInt() else max.toString(dps = 2)}"
            shake()
        }
        false
    }
    return state
}


fun <S : Component> S.namedId(name: String): S {
    this.name = "$name@${this.name.substringAfterLast('@')}"
    return this
}

fun <S : Component> S.named(name: String): S {
    this.name = name
    return this
}

/**
 * Instruct this component to refuse scrolling even if it is deemed necessary.
 */
fun <S : Scrollable> S.dontScroll(): S {
    this.shouldScroll = false
    return this
}

fun <S : Inputtable> S.disable(state: Boolean = true): S {
    this.isEnabled = !state
    return this
}

fun <S : Drawable> S.disable(state: Boolean = true): S {
    this.isEnabled = !state
    this.alpha = if (state) 0.8f else 1f
    return this
}

fun <S : Component> S.hide(state: Boolean = true): S {
    this.renders = !state
    return this
}

/**
 * Specify that this component should be ignored during the layout stage.
 *
 * This means that it will **not be placed automatically** by the positioner.
 *
 * Additionally, this will ignore if there is no size set on this component and no way to infer it.**
 * This means that, if you use this incorrectly **it will not be visible and may break things if your renderer doesn't like zero sizes.**
 * @since 1.4.4
 */
fun <S : Component> S.ignoreLayout(state: Boolean = true): S {
    this.layoutIgnored = state
    return this
}

fun <S : Drawable> S.setAlpha(alpha: Float): S {
    this.alpha = alpha
    return this
}

fun <S : Component> S.padded(xPad: Float, yPad: Float): S {
    this.padding = Vec4.of(xPad, yPad, xPad, yPad)
    return this
}

fun <S : Component> S.padded(left: Float, top: Float, right: Float, bottom: Float): S {
    this.padding = Vec4.of(left, top, right, bottom)
    return this
}

fun <S : Component> S.padded(padding: Vec4): S {
    this.padding = padding
    return this
}

fun <S : Component> S.fix(): S {
    x = x.toInt().toFloat()
    y = y.toInt().toFloat()
    width = width.toInt().toFloat()
    height = height.toInt().toFloat()
    children?.fastEach { it.fix() }
    return this
}

@JvmName("ensureLargerThan")
fun <S : Component> S.ensureLargerThan(vec: Vec2) {
    if (width < vec.x) width = vec.x
    if (height < vec.y) height = vec.y
}


/**
 * Specify a minimum size for this component. **Note that this will also disable scrolling due to optimizations**.
 * @since 1.4.4
 */
@JvmName("minimumSize")
fun <S : Scrollable> S.minimumSize(size: Vec2): S {
    this.shouldScroll = false
    this.visibleSize = size
    return this
}
