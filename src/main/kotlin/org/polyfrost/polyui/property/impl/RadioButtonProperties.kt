/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

package org.polyfrost.polyui.property.impl

import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.radii

open class RadioButtonProperties(open val textProperties: TextProperties = TextProperties()) : BlockProperties() {

    val moveAnimationDuration: Long = 0.6.seconds
    open val moveAnimation: Animation.Type? = Animations.EaseOutExpo
    open val verticalPadding: Unit = 10.px
    open val lateralPadding: Unit = 67.px
    open val edgePadding: Unit = 1.px
    override val outlineThickness = 1f
    override val cornerRadii = 8f.radii()
}
