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

import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.utils.rgba

open class SwitchProperties : StatedProperties() {
    @Deprecated("Switches do not support corner radii in this way. As they are round, the corner radii is inferred based on the size. See bitPadding for more info.", ReplaceWith("bitPadding"))
    override val cornerRadii: FloatArray
        get() = super.cornerRadii

    /**
     * This is the padding between the switch background and the switch 'bit' (the bit that moves)
     */
    open val bitPadding: Unit = 4.px

    open val bitColor = rgba(233, 236, 239, 1f)

    open val labelColor get() = colors.text.primary.normal

    open val labelFontSize: Unit = 12.px

    open val labelFont get() = fonts.regular

    open val lateralPadding: Unit = 13.px

    /** only used if fontSize is 0 */
    open val verticalPadding: Unit = 6.px
}
