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

package org.polyfrost.polyui.property.impl

import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.radii
import org.polyfrost.polyui.utils.rgba

open class ScrollbarProperties : BlockProperties(withStates = true) {
    override val palette = Colors.Palette(
        rgba(0.5f, 0.5f, 0.5f, 0.5f),
        rgba(0.5f, 0.5f, 0.5f, 0.75f),
        rgba(0.5f, 0.5f, 0.5f, 0.8f),
        rgba(0.5f, 0.5f, 0.5f, 0.2f)
    )
    override val cornerRadii: FloatArray = 2f.radii()
    open val padding = 2f
    open val thickness = 4f
    open val showAnim: Animations? = Animations.EaseOutExpo
    open val showAnimDuration: Long = .5.seconds
    open val timeToHide = 2L.seconds
}
