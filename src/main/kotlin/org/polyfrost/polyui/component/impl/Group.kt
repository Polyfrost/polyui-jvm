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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2

/**
 * A drawable that contains other drawables, and does not render anything itself.
 */
class Group(
    vararg children: Drawable?,
    at: Vec2 = Vec2.ZERO,
    alignment: Align = AlignDefault,
    size: Vec2 = Vec2.ZERO,
    visibleSize: Vec2 = Vec2.ZERO,
) : Drawable(children = children, at, alignment, size, visibleSize) {
    override var renders: Boolean
        get() = super.renders && !children.isNullOrEmpty()
        set(value) {
            super.renders = value
        }

    override fun render() {}
}
