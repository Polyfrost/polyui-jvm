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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.property.impl.CheckboxProperties
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.Size
import org.polyfrost.polyui.unit.Unit

/**
 * A simple checkbox component, which can be checked or unchecked.
 * @since 0.19.3
 */
@Suppress("UNCHECKED_CAST")
open class Checkbox(
    properties: CheckboxProperties? = null,
    at: Point<Unit>,
    size: Size<Unit>,
    enabled: Boolean = false,
    onCheck: (Checkbox.(Boolean) -> kotlin.Unit)? = null,
) : StateBlock(properties, at, size, defaultState = enabled, onStateChange = onCheck as (StateBlock.(Boolean) -> kotlin.Unit)?) {
    override val properties
        get() = super.properties as CheckboxProperties

    inline var checked: Boolean
        get() = active
        set(value) {
            active = value
        }

    override fun render() {
        super.render()
        properties.renderCheck(this)
    }
}
