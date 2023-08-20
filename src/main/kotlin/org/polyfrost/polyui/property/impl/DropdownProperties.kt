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
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.radii

open class DropdownProperties(open val minWidth: Float = 120f) : BlockProperties() {

    open class Entry : Properties() {
        val textProperties = TextProperties()
        val iconProperties = ImageProperties()
        override val palette
            get() = colors.component.bg

        open val contentColor
            get() = colors.text.secondary

        open val hoverAnimation = Animations.EaseOutExpo
        open val hoverAnimationDuration = 0.5.seconds

        open val verticalPadding = 4f
        open val lateralPadding = 12f
    }
    override val palette
        get() = colors.component.bg

    open val verticalPadding = 2f

    open val activeColor: Color
        get() = colors.brand.accent.normal

    open val hoveredColor: Color
        get() = colors.text.primary.hovered

    open val borderColor: Color
        get() = colors.page.border5
    open val activeBorderColor: Color
        get() = colors.brand.fg.normal

    open val openAnimation = Animations.EaseOutExpo
    open val openDuration = 0.5.seconds

    open val borderThickness = 1f

    override val cornerRadii = 8f.radii()
}
