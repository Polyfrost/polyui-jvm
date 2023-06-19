/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

package cc.polyfrost.polyui.layout.impl.extension

import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.utils.noneAre

/**
 * A layout that you can drag around.
 *
 * This is a so-called "extension layout", meaning that you apply it to an existing layout, like this:
 * `DraggableLayout(myLayout)` or using [myLayout.draggable()][Layout.draggable]
 */
class DraggableLayout(layout: Layout) : PointerLayout(layout) {
    init {
        ptr.acceptsInput = true
        ptr.simpleName += " [Draggable]"
    }

    private var mouseClickX = 0f
    private var mouseClickY = 0f
    private var mouseDown = false

    override fun accept(event: Events): Boolean {
        if (event is Events.MousePressed) {
            if (event.mods.toInt() != 0) return super.accept(event)
            mouseClickX = polyui.eventManager.mouseX - ptr.at.a.px
            mouseClickY = polyui.eventManager.mouseY - ptr.at.b.px
            mouseDown = components.noneAre { it.mouseOver } && children.noneAre { it.mouseOver }
            ptr.needsRedraw = true
            return true
        }
        if (event is Events.MouseClicked) {
            mouseDown = false
            return true
        }
        if (event is Events.MouseMoved) {
            if (mouseDown) {
                ptr.needsRedraw = true
                ptr.at.a.px = polyui.eventManager.mouseX - mouseClickX
                ptr.at.b.px = polyui.eventManager.mouseY - mouseClickY
            }
            return true
        }
        return super.accept(event)
    }
}
