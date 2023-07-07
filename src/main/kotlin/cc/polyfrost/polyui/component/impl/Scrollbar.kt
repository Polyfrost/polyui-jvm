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

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.impl.ScrollbarProperties
import cc.polyfrost.polyui.unit.origin

class Scrollbar(private val horizontal: Boolean) : Block(null, origin, origin, false, true) {
    override val properties
        get() = super.properties as ScrollbarProperties
    private var thickness
        get() = if (horizontal) height else width
        set(value) = if (horizontal) height = value else width = value
    private var length
        get() = if (horizontal) width else height
        set(value) = if (horizontal) width = value else height = value

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
    private val hideOffset get() = thickness + properties.padding
    private val scrolls get() = scrollingSize < contentSize
    private var m = 0f
    private var offset: Animation? = null
    private var hideTime = 0L
    private var dragging = false

    override fun render() {
        if (!scrolls || (offset?.value ?: 0f) == hideOffset) return
        if (offset?.isFinished == false) wantRedraw()
        if (horizontal) {
            x = (layout.width - length) * scrollPercent
        } else {
            y = (layout.height - length) * scrollPercent
        }
        if (dragging) {
            if (!polyui.mouseDown) {
                dragging = false
            } else {
                cancelHide()
                wantRedraw()
                val mouse = if (horizontal) polyui.mouseX else polyui.mouseY
                scrollPercent = (mouse - m) / (scrollingSize - length)
            }
        }
        super.render()
    }

    fun cancelHide() {
        hideTime = 0L
        if (offset != null) {
            offset = properties.showAnim!!.create(properties.showAnimDuration, offset!!.value, 0f)
            wantRedraw()
        }
    }

    fun tryHide(delta: Long) {
        offset?.update(delta)
        hideTime += delta
        if (offset == null) {
            if (hideTime > properties.timeToHide && properties.showAnim != null) {
                offset = properties.showAnim!!.create(properties.showAnimDuration, 0f, hideOffset)
                wantRedraw()
            }
        } else if (offset!!.value == 0f) {
            offset = null
        }
        if (hideTime < properties.timeToHide || offset != null) {
            if (horizontal) {
                y = layout.visibleSize!!.height - thickness - (layout.trueY - layout.oy) - properties.padding + (offset?.value ?: 0f)
            } else {
                x = layout.visibleSize!!.width - thickness - (layout.trueX - layout.ox) - properties.padding + (offset?.value ?: 0f)
            }
        }
    }

    fun show() = cancelHide()

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
