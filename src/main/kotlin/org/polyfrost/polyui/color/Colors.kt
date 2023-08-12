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

package org.polyfrost.polyui.color

/**
 * # Colors
 *
 * Colors is the color storage for [PolyUI][org.polyfrost.polyui.PolyUI].
 * All PolyUI [components][org.polyfrost.polyui.component.Component] should use these colors
 * in their [properties][org.polyfrost.polyui.property.Properties], and this can be extended,
 * or even changed on the fly using the [field][org.polyfrost.polyui.PolyUI.colors] in the PolyUI instance, bringing theming to PolyUI.
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

    class Page(val bg: Palette, val bgOverlay: Color, val fg: Palette, val fgOverlay: Color, val border20: Color, val border10: Color, val border5: Color)
    class Brand(val fg: Palette, val accent: Palette)
    class OnBrand(val fg: Palette, val accent: Palette)
    class State(val danger: Palette, val warning: Palette, val success: Palette)
    class Component(val bg: Palette, val bgDeselected: Color)
    class Text(val primary: Palette, val secondary: Palette)

    /**
     * # Colors.Palette
     *
     * A color palette represents a set of four colors in PolyUI representing the four key states that a component can have. They are used widely by [Properties][org.polyfrost.polyui.property.Properties] for states of components.
     *
     * @since 0.20.0
     */
    open class Palette(open val normal: Color, open val hovered: Color, open val pressed: Color, open val disabled: Color)
}
