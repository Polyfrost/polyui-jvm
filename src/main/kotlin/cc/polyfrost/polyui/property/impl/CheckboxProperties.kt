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

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.component.impl.Checkbox
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.renderer.data.Cursor
import cc.polyfrost.polyui.utils.radii
import cc.polyfrost.polyui.utils.rgba

open class CheckboxProperties : BlockProperties(null) {
    override val cornerRadii: FloatArray = 6f.radii()
    override val outlineThickness: Float = 2f
    open val checkedColor get() = colors.brand.fg
    open val checkedHoverColor get() = colors.brand.fgHovered
    open val checkedPressedColor get() = colors.brand.fgPressed
    open val checkedDisabledColor get() = colors.brand.fgDisabled

    open val checkmarkColor = rgba(255, 255, 255, 1f)

    protected open var anim: Animation? = null

    /**
     * This function is called when the checkbox is checked.
     */
    open val check: Checkbox.() -> Unit = {
        recolor(checkedColor, pressedAnimation, pressedAnimationDuration)
        anim = pressedAnimation?.create(pressedAnimationDuration, 0f, 1f)
    }

    /**
     * This function is called when the checkbox is unchecked.
     */
    open val uncheck: Checkbox.() -> Unit = {
        recolor(properties.color, pressedAnimation, pressedAnimationDuration)
        anim = pressedAnimation?.create(pressedAnimationDuration, 1f, 0f)
    }

    /**
     * This function is called every time the checkbox is rendered, so you will need to check if [Checkbox.checked] is true.
     */
    open val renderCheck: Checkbox.() -> Unit = {
        if (checked || anim != null) {
            if (anim?.isFinished == true) {
                anim = null
            } else {
                anim?.update(polyui.delta)
                val value = anim?.value ?: 1f
                renderer.translate(x, y)
                renderer.scale(value, value)
                renderer.line(
                    width * 0.2f,
                    height * 0.5f,
                    width * 0.4f,
                    height * 0.7f,
                    checkmarkColor,
                    2f
                )
                renderer.line(
                    width * 0.4f,
                    height * 0.7f,
                    width * 0.8f,
                    height * 0.3f,
                    checkmarkColor,
                    2f
                )
                renderer.scale(1f / value, 1f / value)
                renderer.translate(-x, -y)
            }
        }
    }

    init {
        addEventHandlers(
            Events.MousePressed(0) to {
                if (!(this as Checkbox).checked) {
                    recolor(pressedColor, pressedAnimation, pressedAnimationDuration)
                } else {
                    recolor(checkedPressedColor, pressedAnimation, pressedAnimationDuration)
                }
                true
            },
            Events.MouseReleased(0) to {
                if (!(this as Checkbox).checked) {
                    recolor(hoverColor, pressedAnimation, pressedAnimationDuration)
                } else {
                    recolor(checkedHoverColor, pressedAnimation, pressedAnimationDuration)
                }
                true
            },
            Events.MouseEntered to {
                if (!(this as Checkbox).checked) {
                    recolor(hoverColor, hoverAnimation, hoverAnimationDuration)
                } else {
                    recolor(checkedHoverColor, hoverAnimation, hoverAnimationDuration)
                }
                polyui.cursor = Cursor.Clicker
                true
            },
            Events.MouseExited to {
                if (!(this as Checkbox).checked) {
                    recolor(properties.color, hoverAnimation, hoverAnimationDuration)
                } else {
                    recolor(checkedColor, hoverAnimation, hoverAnimationDuration)
                }
                polyui.cursor = Cursor.Pointer
                true
            }
        )
    }
}
