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

import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.property.impl.DividerProperties
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit

/**
 * A static divider component.
 */
open class Divider @JvmOverloads constructor(
    properties: Properties? = null,
    at: Vec2<Unit>,
    val length: Unit,
    val direction: Direction = Direction.Horizontal
) : Component(properties, at, null, false) {
    override fun render() {
        renderer.drawLine(at.a.px, at.b.px, at.a.px + size!!.a.px, at.b.px + size!!.b.px, properties.color, (properties as DividerProperties).thickness)
    }

    override fun calculateBounds() {
        size = calculateSize()
    }

    override fun calculateSize(): Vec2<Unit> {
        return when (direction) {
            Direction.Horizontal -> Vec2(length, 0.px)
            Direction.Vertical -> Vec2(0.px, length)
        }
    }
}
