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

import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.radii

/**
 * @param cornerRadii The corner radii of the block. The order is top-left, top-right, bottom-right, bottom-left.
 * @param outlineThickness The thickness of this component. If you set it to something other than 0, it will become hollow.
 */
open class BlockProperties @JvmOverloads constructor(
    open val cornerRadii: FloatArray = 0f.radii(),
    open val outlineThickness: Float = 0f,
    withStates: Boolean = false,
    @Transient open var paletteGet: Properties.() -> Colors.Palette = { colors.component.bg },
) : Properties() {

    val outlineColor get() = colors.page.border10
    override val palette get() = paletteGet(this)
    open val pressedAnimation: Animations? = Animations.EaseOutExpo
    open val hoverAnimation: Animations? = Animations.EaseOutExpo
    open val pressedAnimationDuration: Long = 0.25.seconds
    open val hoverAnimationDuration: Long = 0.5.seconds

    init {
        if (withStates) {
            @Suppress("LeakingThis")
            withStates(pressedAnimation, hoverAnimation, pressedAnimationDuration, hoverAnimationDuration)
        }
    }

    companion object {
        @JvmStatic
        fun background(cornerRadii: FloatArray = 0f.radii(), outlineThickness: Float = 0f) = BlockProperties(cornerRadii, outlineThickness, false) { colors.page.bg }

        @JvmStatic
        fun brand(cornerRadii: FloatArray = 0f.radii(), outlineThickness: Float = 0f) = BlockProperties(cornerRadii, outlineThickness, true) { colors.brand.fg }

        @JvmStatic
        fun warning(cornerRadii: FloatArray = 0f.radii(), outlineThickness: Float = 0f) = BlockProperties(cornerRadii, outlineThickness, true) { colors.state.warning }

        @JvmStatic
        fun danger(cornerRadii: FloatArray = 0f.radii(), outlineThickness: Float = 0f) = BlockProperties(cornerRadii, outlineThickness, true) { colors.state.danger }

        @JvmStatic
        fun success(cornerRadii: FloatArray = 0f.radii(), outlineThickness: Float = 0f) = BlockProperties(cornerRadii, outlineThickness, true) { colors.state.success }

        @JvmField
        @Transient
        val brandBlock = brand()

        @JvmField
        @Transient
        val successBlock = success()

        @JvmField
        @Transient
        val warningBlock = warning()

        @JvmField
        @Transient
        val dangerBlock = danger()

        @JvmField
        @Transient
        val backgroundBlock = background()

        fun <P : Properties> P.withStates(pressedAnimation: Animations? = Animations.EaseOutExpo, hoverAnimation: Animations? = Animations.EaseOutExpo, pressedAnimationDuration: Long = 0.25.seconds, hoverAnimationDuration: Long = 0.5.seconds): P {
            var old = false
            addEventHandler(MousePressed(0)) {
                recolor(palette.pressed, pressedAnimation, pressedAnimationDuration)
                true
            }
            addEventHandler(MouseReleased(0)) {
                recolor(palette.hovered, pressedAnimation, pressedAnimationDuration)
                true
            }
            addEventHandler(MouseEntered) {
                recolor(palette.hovered, hoverAnimation, hoverAnimationDuration)
                polyUI.cursor = Cursor.Clicker
                true
            }
            addEventHandler(MouseExited) {
                recolor(palette.normal, hoverAnimation, hoverAnimationDuration)
                polyUI.cursor = Cursor.Pointer
                true
            }
            addEventHandler(Disabled) {
                old = acceptsInput
                acceptsInput = false
                recolor(palette.disabled, hoverAnimation, hoverAnimationDuration)
                true
            }
            addEventHandler(Enabled) {
                acceptsInput = old
                recolor(palette.normal, hoverAnimation, hoverAnimationDuration)
                true
            }
            return this
        }
    }
}
