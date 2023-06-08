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

package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.utils.rgba

/**
 * The default color set used in PolyUI.
 *
 * @see Colors
 * @since 0.17.0
 */
open class DarkTheme : Colors {
    override val page = DarkPage()
    override val brand = DarkBrand()
    override val onBrand = DarkOnBrand()
    override val state = DarkState()
    override val component = DarkComponent()
    override val text = DarkText()

    open class DarkPage : Colors.Page() {
        override val bg = rgba(17, 23, 28)
        override val bgElevated = rgba(26, 34, 41)
        override val bgDepressed = rgba(14, 19, 23)
        override val bgOverlay = rgba(255, 255, 255, 0.1f)

        override val fg = Color.TRANSPARENT
        override val fgElevated = rgba(26, 34, 41)
        override val fgDepressed = rgba(14, 19, 23)
        override val fgOverlay = rgba(255, 255, 255, 0.1f)

        override val border20 = rgba(255, 255, 255, 0.2f)
        override val border10 = rgba(255, 255, 255, 0.1f)
        override val border5 = rgba(255, 255, 255, 0.05f)
    }
    open class DarkBrand : Colors.Brand() {
        override val fg = rgba(43, 75, 255)
        override val fgHovered = rgba(40, 67, 221)
        override val fgPressed = rgba(57, 87, 255)
        override val fgDisabled = rgba(57, 87, 255, 0.5f)
        override val accent = rgba(15, 28, 51)
        override val accentHovered = rgba(12, 23, 41)
        override val accentPressed = rgba(26, 44, 78)
        override val accentDisabled = rgba(15, 28, 51, 0.5f)
    }
    open class DarkOnBrand : Colors.OnBrand() {
        override val fg = rgba(213, 219, 255)
        override val fgHovered = rgba(213, 219, 255, 0.85f)
        override val fgPressed = rgba(225, 229, 255)
        override val fgDisabled = rgba(225, 229, 255, 0.5f)
        override val accent = rgba(63, 124, 228)
        override val accentHovered = rgba(63, 124, 228, 0.85f)
        override val accentPressed = rgba(37, 80, 154)
        override val accentDisabled = rgba(63, 124, 228, 0.5f)
    }
    open class DarkState : Colors.State() {
        override val danger = rgba(255, 68, 68)
        override val dangerHovered = rgba(214, 52, 52)
        override val dangerPressed = rgba(255, 86, 86)
        override val dangerDisabled = rgba(255, 68, 68, 0.5f)
        override val warning = rgba(255, 171, 29)
        override val warningHovered = rgba(233, 156, 27)
        override val warningPressed = rgba(255, 178, 49)
        override val warningDisabled = rgba(255, 171, 29, 0.5f)
        override val success = rgba(35, 154, 96)
        override val successHovered = rgba(26, 135, 82)
        override val successPressed = rgba(44, 172, 110)
        override val successDisabled = rgba(35, 154, 96, 0.5f)
    }
    open class DarkComponent : Colors.Component() {
        override val bg = rgba(26, 34, 41)
        override val bgHovered = rgba(23, 31, 37)
        override val bgPressed = rgba(34, 44, 53)
        override val bgDeselected = Color.TRANSPARENT
        override val bgDisabled = rgba(26, 34, 41, 0.5f)
    }
    open class DarkText : Colors.Text() {
        override val primary = rgba(213, 219, 255)
        override val primaryHovered = rgba(213, 219, 255, 0.85f)
        override val primaryPressed = rgba(225, 229, 255)
        override val primaryDisabled = rgba(225, 229, 255, 0.5f)
        override val secondary = rgba(120, 129, 141)
        override val secondaryHovered = rgba(95, 104, 116)
        override val secondaryPressed = rgba(130, 141, 155)
        override val secondaryDisabled = rgba(120, 129, 141, 0.5f)
    }
}
