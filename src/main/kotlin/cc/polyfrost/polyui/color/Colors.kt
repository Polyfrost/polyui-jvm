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

@file:Suppress("UNUSED")

package cc.polyfrost.polyui.color

/**
 * # Colors
 *
 * Colors is the color storage for [PolyUI][cc.polyfrost.polyui.PolyUI].
 * All PolyUI [components][cc.polyfrost.polyui.component.Component] should use these colors
 * in their [properties][cc.polyfrost.polyui.property.Properties], and this can be extended,
 * or even changed on the fly using the [field][cc.polyfrost.polyui.PolyUI.colors] in the PolyUI instance, bringing theming to PolyUI.
 *
 * @since 0.17.0
 */
interface Colors {
    val page: Page
    val brand: Brand
    val onBrand: OnBrand
    val state: State
    val component: Component
    val text: Text

    abstract class Page {
        abstract val bg: Color
        abstract val bgElevated: Color
        abstract val bgDepressed: Color
        abstract val bgOverlay: Color

        abstract val fg: Color
        abstract val fgElevated: Color
        abstract val fgDepressed: Color
        abstract val fgOverlay: Color

        abstract val border20: Color
        abstract val border10: Color
        abstract val border5: Color
    }
    abstract class Brand {
        abstract val fg: Color
        abstract val fgHovered: Color
        abstract val fgPressed: Color
        abstract val fgDisabled: Color

        abstract val accent: Color
        abstract val accentHovered: Color
        abstract val accentPressed: Color
        abstract val accentDisabled: Color
    }
    abstract class OnBrand {
        abstract val fg: Color
        abstract val fgHovered: Color
        abstract val fgPressed: Color
        abstract val fgDisabled: Color

        abstract val accent: Color
        abstract val accentHovered: Color
        abstract val accentPressed: Color
        abstract val accentDisabled: Color
    }
    abstract class State {
        abstract val danger: Color
        abstract val dangerHovered: Color
        abstract val dangerPressed: Color
        abstract val dangerDisabled: Color

        abstract val warning: Color
        abstract val warningHovered: Color
        abstract val warningPressed: Color
        abstract val warningDisabled: Color

        abstract val success: Color
        abstract val successHovered: Color
        abstract val successPressed: Color
        abstract val successDisabled: Color
    }
    abstract class Component {
        abstract val bg: Color
        abstract val bgHovered: Color
        abstract val bgPressed: Color
        abstract val bgDeselected: Color
        abstract val bgDisabled: Color
    }
    abstract class Text {
        abstract val primary: Color
        abstract val primaryHovered: Color
        abstract val primaryPressed: Color
        abstract val primaryDisabled: Color

        abstract val secondary: Color
        abstract val secondaryHovered: Color
        abstract val secondaryPressed: Color
        abstract val secondaryDisabled: Color
    }
}
