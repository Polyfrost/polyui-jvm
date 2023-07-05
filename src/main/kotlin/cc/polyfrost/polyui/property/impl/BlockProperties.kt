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

package cc.polyfrost.polyui.property.impl

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.property.State
import cc.polyfrost.polyui.renderer.data.Cursor
import cc.polyfrost.polyui.unit.seconds
import cc.polyfrost.polyui.utils.radii

/**
 * @param cornerRadii The corner radii of the block. The order is top-left, top-right, bottom-right, bottom-left.
 * @param outlineThickness The thickness of this component. If you set it to something other than 0, it will become hollow.
 */
open class BlockProperties @JvmOverloads constructor(
    val colorPalette: Colors.Palette? = null,
    open val cornerRadii: FloatArray = 0f.radii(),
    open val outlineThickness: Float = 0f
) : Properties() {
    @JvmOverloads constructor(color: Color, cornerRadii: FloatArray = 0f.radii(), outlineThickness: Float = 0f) : this(Colors.Palette(color, color, color, color), cornerRadii, outlineThickness)
    val outlineColor get() = colors.page.border10
    override val palette get() = colorPalette ?: colors.component.bg
    open val pressedAnimation: Animations? = Animations.EaseOutExpo
    open val hoverAnimation: Animations? = Animations.EaseOutExpo
    open val pressedAnimationDuration: Long = 0.25.seconds
    open val hoverAnimationDuration: Long = 0.5.seconds
}

open class BackgroundProperties @JvmOverloads constructor(
    cornerRadii: FloatArray = 0f.radii()
) : BlockProperties(null, cornerRadii) {
    override val palette: Colors.Palette get() = colors.page.bg
}

/**
 * Basic block properties with hover and pressed animations.
 */
open class DefaultBlockProperties @JvmOverloads constructor(
    colorPalette: Colors.Palette? = null,
    cornerRadii: FloatArray = 0f.radii()
) : BlockProperties(colorPalette, cornerRadii) {
    init {
        addEventHandlers(
            Events.MousePressed(0) to {
                recolor(palette.pressed, pressedAnimation, pressedAnimationDuration)
                true
            },
            Events.MouseReleased(0) to {
                recolor(palette.hovered, pressedAnimation, pressedAnimationDuration)
                true
            },
            Events.MouseEntered to {
                recolor(palette.hovered, hoverAnimation, hoverAnimationDuration)
                polyui.cursor = Cursor.Clicker
                true
            },
            Events.MouseExited to {
                recolor(palette.normal, hoverAnimation, hoverAnimationDuration)
                polyui.cursor = Cursor.Pointer
                true
            }
        )
    }
}

open class BrandBlockProperties @JvmOverloads constructor(
    cornerRadii: FloatArray = 0f.radii()
) : DefaultBlockProperties(null, cornerRadii) {
    override val palette: Colors.Palette get() = colors.brand.fg
}

open class StateBlockProperties @JvmOverloads constructor(
    private val state: State = State.Success,
    cornerRadii: FloatArray = 0f.radii()
) : DefaultBlockProperties(null, cornerRadii) {
    override val palette: Colors.Palette get() = when (state) {
        State.Success -> colors.state.success
        State.Warning -> colors.state.warning
        State.Danger -> colors.state.danger
    }
}
