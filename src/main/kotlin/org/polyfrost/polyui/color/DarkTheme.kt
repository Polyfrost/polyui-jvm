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

package org.polyfrost.polyui.color

/**
 * The default color set used in PolyUI.
 *
 * @see Colors
 * @since 0.17.0
 */
open class DarkTheme : Colors {
    override val name: String
        get() = "dark"
    override val page = Colors.Page(
        bg = Colors.Palette(
            rgba(17, 23, 28), // Normal - #11171C
            rgba(21, 28, 34), // Elevated/hovered - #151C22
            rgba(14, 19, 23), // Pressed - #0E1317
            rgba(17, 23, 28, 0.5f), // Disabled - #11171C80
        ),
        bgOverlay = rgba(255, 255, 255, 0.1f), // Overlay - #FFFFFF1A
        fg = Colors.Palette(
            rgba(17, 23, 28), // Normal - #11171C
            rgba(26, 34, 41), // Elevated/hovered - #1A2229
            rgba(14, 19, 23), // Pressed - #0E1317
            rgba(26, 34, 41, 0.5f), // Disabled - #1A222980
        ),
        fgOverlay = rgba(255, 255, 255, 0.1f), // Overlay - #FFFFFF1A
        border20 = rgba(255, 255, 255, 0.2f),
        border10 = rgba(255, 255, 255, 0.1f),
        border5 = rgba(255, 255, 255, 0.05f),
    )

    override val brand = Colors.Brand(
        fg = Colors.Palette(
            rgba(43, 75, 255), // Normal - #2B4BFF
            rgba(40, 67, 221), // Elevated/hovered - #2843DD
            rgba(57, 87, 255), // Pressed - #3957FF
            rgba(57, 87, 255, 0.5f), // Disabled - #3957FF80
        ),
        accent = Colors.Palette(
            rgba(15, 28, 51), // Normal - #0F1C33
            rgba(12, 23, 41), // Elevated/hovered - #0C1729
            rgba(26, 44, 78), // Pressed - #1A2C4E
            rgba(15, 28, 51, 0.5f), // Disabled - #0F1C3380
        ),
    )

    override val onBrand = Colors.OnBrand(
        fg = Colors.Palette(
            rgba(213, 219, 255), // Normal - #D5DBFF
            rgba(213, 219, 255, 0.85f), // Elevated/hovered - #D5DBFFDA
            rgba(225, 229, 255), // Pressed - #E1E5FF
            rgba(225, 229, 255, 0.5f), // Disabled - #E1E5FF80
        ),
        accent = Colors.Palette(
            rgba(63, 124, 228), // Normal - #3F7CE4
            rgba(63, 124, 228, 0.85f), // Elevated/hovered - #3F7CE4DA
            rgba(37, 80, 154), // Pressed - #25509A
            rgba(63, 124, 228, 0.5f), // Disabled - #3F7CE480
        ),
    )

    override val state = Colors.State(
        danger = Colors.Palette(
            rgba(255, 68, 68), // Normal - #FF4444
            rgba(214, 52, 52), // Elevated/hovered - #D63434
            rgba(255, 86, 86), // Pressed - #FF5656
            rgba(255, 68, 68, 0.5f), // Disabled - #FF444480
        ),
        warning = Colors.Palette(
            rgba(255, 171, 29), // Normal - #FFAB1D
            rgba(233, 156, 27), // Elevated/hovered - #E99C1B
            rgba(255, 178, 49), // Pressed - #FFB231
            rgba(255, 171, 29, 0.5f), // Disabled - #FFAB1D80
        ),
        success = Colors.Palette(
            rgba(35, 154, 96), // Normal - #239A60
            rgba(26, 139, 82), // Elevated/hovered - #1A8B52
            rgba(44, 172, 110), // Pressed - #2CAC6E
            rgba(35, 154, 96, 0.5f), // Disabled - #239A6080
        ),
    )

    override val component = Colors.Component(
        bg = Colors.Palette(
            rgba(26, 34, 41), // Normal - #1A2229
            rgba(23, 31, 37, 0.85f), // Elevated/hovered - #171F25DA
            rgba(34, 44, 53), // Pressed - #222C35
            rgba(34, 44, 53, 0.5f), // Disabled - #222C3580
        ),
        bgDeselected = Color.TRANSPARENT,
    )

    override val text = Colors.Text(
        primary = Colors.Palette(
            rgba(223, 229, 255), // Normal - #D5DBFF
            rgba(223, 229, 255, 0.85f), // Elevated/hovered - #D5DBFFDA
            rgba(235, 239, 255), // Pressed - #E1E5FF
            rgba(235, 239, 255, 0.5f), // Disabled - #E1E5FF80
        ),
        secondary = Colors.Palette(
            rgba(120, 119, 141), // Normal - #78778D
            rgba(95, 104, 116), // Elevated/hovered - #5F6874
            rgba(130, 141, 155), // Pressed - #828D9B
            rgba(120, 129, 141, 0.5f), // Disabled - #78818D80
        ),
    )
}
