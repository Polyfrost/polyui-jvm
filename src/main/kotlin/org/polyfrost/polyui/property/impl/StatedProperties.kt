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

package org.polyfrost.polyui.property.impl

import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.impl.StateBlock
import org.polyfrost.polyui.event.MouseEntered
import org.polyfrost.polyui.event.MouseExited
import org.polyfrost.polyui.event.MousePressed
import org.polyfrost.polyui.event.MouseReleased
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.unit.seconds

open class StatedProperties : BlockProperties() {
    /**
     * This is the color palette used when the component is active.
     */
    open val activePalette get() = colors.brand.fg

    /**
     * This is the animation used when the component is activated.
     */
    open val activateAnimation: Animations? = Animations.EaseOutExpo
    open val activateAnimationDuration: Long = 0.5.seconds

    /**
     * This function is called when the component is activated.
     */
    @Transient
    open val onActivate: (StateBlock.() -> Unit)? = null

    /**
     * This function is called when the component is deactivated.
     */
    @Transient
    open val onDeactivate: (StateBlock.() -> Unit)? = null

    init {
        addEventHandler(MousePressed(0)) {
            if (!(this as StateBlock).active) {
                recolor(palette.pressed, pressedAnimation, pressedAnimationDuration)
            } else {
                recolor(activePalette.pressed, pressedAnimation, pressedAnimationDuration)
            }
            true
        }
        addEventHandler(MouseReleased(0)) {
            if (!(this as StateBlock).active) {
                recolor(palette.hovered, pressedAnimation, pressedAnimationDuration)
            } else {
                recolor(activePalette.hovered, pressedAnimation, pressedAnimationDuration)
            }
            true
        }
        addEventHandler(MouseEntered) {
            if (!(this as StateBlock).active) {
                recolor(palette.hovered, hoverAnimation, hoverAnimationDuration)
            } else {
                recolor(activePalette.hovered, hoverAnimation, hoverAnimationDuration)
            }
            polyUI.cursor = Cursor.Clicker
            true
        }
        addEventHandler(MouseExited) {
            if (!(this as StateBlock).active) {
                recolor(palette.normal, hoverAnimation, hoverAnimationDuration)
            } else {
                recolor(activePalette.normal, hoverAnimation, hoverAnimationDuration)
            }
            polyUI.cursor = Cursor.Pointer
            true
        }
    }
}
