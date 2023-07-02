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
    val ccolor: Color? = null,
    open val cornerRadii: FloatArray = 0f.radii(),
    open val outlineThickness: Float = 0f
) : Properties() {
    val outlineColor get() = colors.page.border10
    override val color get() = ccolor ?: colors.component.bg
    open val hoverColor get() = colors.component.bgHovered
    open val pressedColor get() = colors.component.bgPressed
    open val disabledColor get() = colors.component.bgDisabled
    open val pressedAnimation: Animations? = Animations.EaseOutExpo
    open val hoverAnimation: Animations? = Animations.EaseOutExpo
    open val pressedAnimationDuration: Long = 0.25.seconds
    open val hoverAnimationDuration: Long = 0.5.seconds
}

open class BackgroundBlockProperties @JvmOverloads constructor(
    cornerRadii: FloatArray = 0f.radii()
) : BlockProperties(null, cornerRadii) {
    override val color: Color get() = colors.page.bg
}

/**
 * Basic block properties with hover and pressed animations.
 */
open class DefaultBlockProperties @JvmOverloads constructor(
    cornerRadii: FloatArray = 0f.radii()
) : BlockProperties(null, cornerRadii) {
    init {
        addEventHandlers(
            Events.MousePressed(0) to {
                recolor(pressedColor, pressedAnimation, pressedAnimationDuration)
                true
            },
            Events.MouseReleased(0) to {
                recolor(hoverColor, pressedAnimation, pressedAnimationDuration)
                true
            },
            Events.MouseEntered to {
                recolor(hoverColor, hoverAnimation, hoverAnimationDuration)
                polyui.cursor = Cursor.Clicker
                true
            },
            Events.MouseExited to {
                recolor(properties.color, hoverAnimation, hoverAnimationDuration)
                polyui.cursor = Cursor.Pointer
                true
            }
        )
    }
}

open class PrimaryBlockProperties @JvmOverloads constructor(
    cornerRadii: FloatArray = 0f.radii()
) : DefaultBlockProperties(cornerRadii) {
    override val color: Color get() = colors.brand.fg
    override val hoverColor: Color get() = colors.brand.fgHovered
    override val pressedColor: Color get() = colors.brand.fgPressed
    override val disabledColor: Color get() = colors.brand.fgDisabled
}

open class StateBlockProperties @JvmOverloads constructor(
    private val state: State = State.Success,
    cornerRadii: FloatArray = 0f.radii()
) : DefaultBlockProperties(cornerRadii) {
    override val color: Color get() = when (state) {
        State.Success -> colors.state.success
        State.Warning -> colors.state.warning
        State.Danger -> colors.state.danger
    }
    override val hoverColor: Color get() = when (state) {
        State.Success -> colors.state.successHovered
        State.Warning -> colors.state.warningHovered
        State.Danger -> colors.state.dangerHovered
    }
    override val pressedColor: Color get() = when (state) {
        State.Success -> colors.state.successPressed
        State.Warning -> colors.state.warningPressed
        State.Danger -> colors.state.dangerPressed
    }
    override val disabledColor: Color get() = when (state) {
        State.Success -> colors.state.successDisabled
        State.Warning -> colors.state.warningDisabled
        State.Danger -> colors.state.dangerDisabled
    }
}
