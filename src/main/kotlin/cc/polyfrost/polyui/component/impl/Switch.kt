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

import cc.polyfrost.polyui.component.StateBlock
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.impl.StatedProperties
import cc.polyfrost.polyui.unit.Point
import cc.polyfrost.polyui.unit.Size
import cc.polyfrost.polyui.unit.Unit

/**
 * # Switch
 *
 * A simple switch component, which can be on or off.
 *
 * @param size the size of the switch. Note that this value has to have a width that is at least twice as large as its height. Due to this, only one of the parameters has to be set, and the other one will be inferred.
 */
@Suppress("UNCHECKED_CAST")
class Switch(
    properties: StatedProperties? = null,
    at: Point<Unit>,
    size: Size<Unit>,
    enabled: Boolean = false,
    onEnable: (Switch.(Boolean) -> kotlin.Unit)? = null,
    vararg events: Events.Handler
) : StateBlock(properties, at, size, defaultState = enabled, onStateChange = onEnable as StateBlock.(Boolean) -> kotlin.Unit, events = events) {
    override fun render() {
        TODO("Not yet implemented")
    }
}
