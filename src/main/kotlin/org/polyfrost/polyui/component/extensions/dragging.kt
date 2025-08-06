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

import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.fastEachIndexed

/**
 * Make this component draggable by the user with their mouse.
 *
 * @param free if this is true, the component will be able to be dragged outside its parent.
 * This is achieved by briefly removing this from its parent and adding it to the master.
 */
fun <S : Inputtable> S.draggable(withX: Boolean = true, withY: Boolean = true, free: Boolean = false): S {
    var px = 0f
    var py = 0f
    on(Event.Mouse.Drag.Started) {
        px = polyUI.inputManager.mouseX - x
        py = polyUI.inputManager.mouseY - y
        if (free && _parent !== polyUI.master) {
            parent.children!!.remove(this)
            polyUI.master.children!!.add(this)
        }
        false
    }
    on(Event.Mouse.Drag) {
        val mx = polyUI.inputManager.mouseX
        val my = polyUI.inputManager.mouseY
        if (this is Drawable) needsRedraw = true
        if (withX) x = mx - px
        if (withY) y = my - py
        false
    }
    if (free) {
        on(Event.Mouse.Drag.Ended) {
            if (_parent !== polyUI.master) {
                polyUI.master.children!!.remove(this)
                parent.children!!.add(this)
            }
            false
        }
    }
    return this
}

/**
 * Turn this component into a 'rearrangeable grid', meaning that the children in it
 * can be dragged around and rearranged in any order by the user. It will automatically adapt and insert the children in
 * the correct order.
 *
 * @since 1.6.1
 */
fun <S : Inputtable> S.makeRearrangeableGrid(): S {
    children?.fastEach { cmp ->
        val child = cmp as? Inputtable ?: return@fastEach
        child.draggable().onDragStart {
            if (this is Drawable) alpha = 0.6f
        }.onDrag { _ ->
            val px = x
            val py = y
            val pw = width
            val siblings = parent.children ?: return@onDrag
            siblings.fastEachIndexed { i, it ->
                if (it === this) return@fastEachIndexed
                if (it.intersects(px, py, pw, height)) {
                    siblings.remove(this)
                    val middleX = px + pw / 2f
                    val itMiddleX = it.x + it.width / 2f
                    siblings.add(if (middleX > itMiddleX) i else (i - 1).coerceAtLeast(0), this)
                }
            }
            parent.position()
            x = px
            y = py
        }.onDragEnd {
            if (this is Drawable) {
                alpha = 1f
                needsRedraw = true
            }
            parent.position()
            true
        }
    }
    return this
}

@OverloadResolutionByLambdaReturnType
@JvmName("onDragZ")
fun <S : Inputtable> S.onDrag(func: S.(Event.Mouse.Drag) -> Boolean): S {
    on(Event.Mouse.Drag, func)
    return this
}

@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onDrag(func: S.(Event.Mouse.Drag) -> Unit): S {
    on(Event.Mouse.Drag, func)
    return this
}

@OverloadResolutionByLambdaReturnType
@JvmName("onDragStartZ")
fun <S : Inputtable> S.onDragStart(func: S.(Event.Mouse.Drag.Started) -> Boolean): S {
    on(Event.Mouse.Drag.Started, func)
    return this
}

@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onDragStart(func: S.(Event.Mouse.Drag.Started) -> Unit): S {
    on(Event.Mouse.Drag.Started, func)
    return this
}

@OverloadResolutionByLambdaReturnType
@JvmName("onDragEndZ")
fun <S : Inputtable> S.onDragEnd(func: S.(Event.Mouse.Drag.Ended) -> Boolean): S {
    on(Event.Mouse.Drag.Ended, func)
    return this
}

@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onDragEnd(func: S.(Event.Mouse.Drag.Ended) -> Unit): S {
    on(Event.Mouse.Drag.Ended, func)
    return this
}
