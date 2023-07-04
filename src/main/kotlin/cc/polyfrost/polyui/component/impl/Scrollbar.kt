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

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.impl.ScrollbarProperties
import cc.polyfrost.polyui.unit.origin

class Scrollbar(private val horizontal: Boolean) : Block(null, origin, origin, false, true) {
    override val properties
        get() = super.properties as ScrollbarProperties
    private var thickness by if (horizontal) ::height else ::width
    private var length by if (horizontal) ::width else ::height

    // no delegates on lateinit :(
    private val scrollingSize get() = if (horizontal) layout.visibleSize!!.width else layout.visibleSize!!.height
    private val contentSize get() = if (horizontal) layout.width else layout.height
    private var scrollPercent: Float
        get() {
            val p = if (horizontal) {
                (layout.ox - layout.trueX) / (contentSize - scrollingSize)
            } else {
                (layout.oy - layout.trueY) / (contentSize - scrollingSize)
            }
            return if (p.isNaN()) 0f else p
        }
        set(value) {
            if (horizontal) {
                layout.x = -(value.coerceIn(0f, 1f) * (layout.width - layout.visibleSize!!.width) - layout.ox)
            } else {
                layout.y = -(value.coerceIn(0f, 1f) * (layout.height - layout.visibleSize!!.height) - layout.oy)
            }
        }
    private val scrolls get() = scrollingSize < contentSize
    private var m = 0f
    private var dragging = false

    override fun render() {
        if (!scrolls) return
        if (horizontal) {
            x = (layout.width - length) * scrollPercent
            y = layout.visibleSize!!.height - thickness - (layout.trueY - layout.oy) - properties.padding
        } else {
            y = (layout.height - length) * scrollPercent
            x = layout.visibleSize!!.width - thickness - (layout.trueX - layout.ox) - properties.padding
        }
        if (dragging) {
            if (!polyui.mouseDown) {
                dragging = false
            } else {
                wantRedraw()
                val mouse = if (horizontal) polyui.mouseX else polyui.mouseY
                scrollPercent = (mouse - m) / (scrollingSize - length)
            }
        }
        super.render()
    }

    override fun calculateBounds() {
        thickness = properties.thickness
        length = scrollingSize * (scrollingSize / contentSize)
        super.calculateBounds()
    }

    override fun accept(event: Events): Boolean {
        if (!scrolls) return super.accept(event)
        if (event is Events.MousePressed) {
            if (event.button == 0 && event.mods.toInt() == 0) {
                dragging = true
                m = if (horizontal) polyui.mouseX - x - (layout.trueX - layout.ox) else polyui.mouseY - y - (layout.trueY - layout.oy)
            }
        }
        return super.accept(event)
    }
}
