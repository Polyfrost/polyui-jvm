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

import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.impl.CheckboxProperties
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit

/**
 * A simple checkbox component, which can be checked or unchecked.
 * @since 0.19.3
 */
open class Checkbox(properties: CheckboxProperties? = null, at: Point<Unit>, size: Size<Unit>, private val onCheck: ((Checkbox, Boolean) -> kotlin.Unit)? = null) : Block(properties, at, size) {
    override val properties
        get() = super.properties as CheckboxProperties

    @set:JvmName("check")
    @get:JvmName("isChecked")
    var checked = false
        set(value) {
            if (value == field) return
            field = value
            onCheck?.invoke(this, value)
            if (value) {
                properties.check(this)
            } else {
                properties.uncheck(this)
            }
        }
    override fun accept(event: Events): Boolean {
        if (event is Events.MouseClicked) {
            if (event.button == 0) {
                checked = !checked
            }
        }
        return super.accept(event)
    }

    override fun render() {
        super.render()
        properties.renderCheck(this)
    }

    override fun onColorsChanged(colors: Colors) {
        super.onColorsChanged(colors)
        if (checked) color = properties.checkedColor.toMutable()
    }
}
