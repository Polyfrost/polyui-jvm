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

@file:Suppress("UNUSED")

package org.polyfrost.polyui.color

import org.polyfrost.polyui.PolyUI.Companion.INPUT_HOVERED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED

/**
 * # Colors
 *
 * Colors is the color storage for [PolyUI][org.polyfrost.polyui.PolyUI].
 * All PolyUI [drawables][org.polyfrost.polyui.component.Drawable] use these colors.
 * or even changed on the fly using the [field][org.polyfrost.polyui.PolyUI.colors] in the PolyUI instance, bringing theming to PolyUI.
 *
 * @since 0.17.0
 */
interface Colors {
    val name: String
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
     * A color palette represents a set of four colors in PolyUI representing the four key states that a component can have.
     *
     * @since 0.20.0
     */
    open class Palette(open val normal: Color, open val hovered: Color, open val pressed: Color, open val disabled: Color) {
        fun get(state: Byte): Color {
            return when (state) {
                INPUT_NONE -> normal
                INPUT_HOVERED -> hovered
                INPUT_PRESSED -> pressed
                else -> normal
            }
        }
    }

    /**
     * Calculate and return the new [Palette] for the given [currentPalette] based on the current and new [Colors].
     * This is useful for theming, where you want to change the palette of a component based on the current theme.
     * If the [currentPalette] is not found in the current [Colors], it will return null.
     *
     * @since 1.8.3
     * @see getNewColor
     */
    fun getNewPalette(currentPalette: Palette?, current: Colors): Palette? {
        if (currentPalette == null) return null
        return if (currentPalette === current.page.bg) page.bg
        else if (currentPalette === current.page.fg) page.fg

        else if (currentPalette === current.brand.fg) brand.fg
        else if (currentPalette === current.brand.accent) brand.accent
        else if (currentPalette === current.onBrand.fg) onBrand.fg
        else if (currentPalette === current.onBrand.accent) onBrand.accent

        else if (currentPalette === current.state.danger) state.danger
        else if (currentPalette === current.state.warning) state.warning
        else if (currentPalette === current.state.success) state.success

        else if (currentPalette === current.component.bg) component.bg

        else if (currentPalette === current.text.primary) text.primary
        else if (currentPalette === current.text.secondary) text.secondary
        else null
    }

    /**
     * Calculate and return the new [Color] for the given [currentColor] based on the current and new [Colors].
     * This is useful for theming, where you want to change the color of a component based on the current theme.
     * If the [currentColor] is not found in the current [Colors], it will return null.
     *
     * @since 1.8.3
     * @see getNewPalette
     */
    fun getNewColor(currentColor: Color, current: Colors): Color? {
        return if (currentColor === current.page.bgOverlay) page.bgOverlay
        else if (currentColor === current.page.fgOverlay) page.fgOverlay

        else if (currentColor === current.page.border20) page.border20
        else if (currentColor === current.page.border10) page.border10
        else if (currentColor === current.page.border5) page.border5

        else if (currentColor === current.component.bgDeselected) component.bgDeselected
        else null
    }
}
