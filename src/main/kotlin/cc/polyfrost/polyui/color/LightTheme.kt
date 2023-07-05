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
 * The default light color set used in PolyUI.
 *
 * @see Colors
 * @since 0.17.0
 */
open class LightTheme : Colors {
    override val page = LightPage()
    override val brand = LightBrand()
    override val onBrand = LightOnBrand()
    override val state = LightState()
    override val component = LightComponent()
    override val text = LightText()

    open class LightPage : Colors.Page() {
        override val bg: Colors.Palette = Colors.Palette(
            rgba(232, 237, 255),
            rgba(222, 228, 252),
            rgba(239, 243, 255),
            rgba(232, 237, 255, 0.5f)
        )
        override val bgOverlay: Color = rgba(0, 0, 0, 0.25f)
        override val fg: Colors.Palette = Colors.Palette(
            rgba(17, 23, 28),
            rgba(26, 34, 41),
            rgba(14, 19, 23),
            rgba(17, 23, 28, 0.5f)
        )
        override val fgOverlay: Color = rgba(255, 255, 255, 0.1f)
        override val border20: Color = rgba(0, 0, 0, 0.2f)
        override val border10: Color = rgba(0, 0, 0, 0.1f)
        override val border5: Color = rgba(0, 0, 0, 0.05f)
    }
    open class LightBrand : Colors.Brand() {
        override val fg: Colors.Palette = Colors.Palette(
            rgba(64, 93, 255),
            rgba(40, 67, 221),
            rgba(57, 87, 255),
            rgba(64, 93, 255, 0.5f)
        )
        override val accent: Colors.Palette = Colors.Palette(
            rgba(223, 236, 253),
            rgba(183, 208, 251),
            rgba(177, 206, 255),
            rgba(15, 28, 51, 0.5f)
        )
    }
    open class LightOnBrand : Colors.OnBrand() {
        override val fg: Colors.Palette = Colors.Palette(
            rgba(213, 219, 255),
            rgba(215, 220, 251),
            rgba(225, 229, 255),
            rgba(213, 219, 255, 0.5f)
        )
        override val accent: Colors.Palette = Colors.Palette(
            rgba(63, 124, 228),
            rgba(63, 124, 228, 0.85f),
            rgba(37, 80, 154),
            rgba(63, 124, 228, 0.5f)
        )
    }
    open class LightState : Colors.State() {
        override val danger: Colors.Palette = Colors.Palette(
            rgba(255, 68, 68),
            rgba(214, 52, 52),
            rgba(255, 86, 86),
            rgba(255, 68, 68, 0.5f)
        )
        override val warning: Colors.Palette = Colors.Palette(
            rgba(255, 171, 29),
            rgba(233, 156, 27),
            rgba(255, 178, 49),
            rgba(255, 171, 29, 0.5f)
        )
        override val success: Colors.Palette = Colors.Palette(
            rgba(35, 154, 96),
            rgba(26, 135, 82),
            rgba(44, 172, 110),
            rgba(35, 154, 96, 0.5f)
        )
    }
    open class LightComponent : Colors.Component() {
        override val bg: Colors.Palette = Colors.Palette(
            rgba(222, 228, 252),
            rgba(213, 219, 243),
            rgba(238, 241, 255),
            rgba(222, 228, 252, 0.5f)
        )
        override val bgDeselected = Color.TRANSPARENT
    }

    open class LightText : Colors.Text() {
        override val primary: Colors.Palette = Colors.Palette(
            rgba(2, 3, 7),
            rgba(11, 15, 33),
            rgba(2, 5, 15),
            rgba(2, 3, 7, 0.5f)
        )
        override val secondary: Colors.Palette = Colors.Palette(
            rgba(117, 120, 131),
            rgba(101, 104, 116),
            rgba(136, 139, 150),
            rgba(117, 120, 131, 0.5f)
        )
    }
}
