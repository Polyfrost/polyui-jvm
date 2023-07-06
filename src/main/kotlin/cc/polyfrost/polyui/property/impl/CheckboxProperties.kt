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
import cc.polyfrost.polyui.component.StateBlock
import cc.polyfrost.polyui.component.impl.Checkbox
import cc.polyfrost.polyui.utils.radii
import cc.polyfrost.polyui.utils.rgba

open class CheckboxProperties : StatedProperties() {
    override val cornerRadii: FloatArray = 6f.radii()
    override val outlineThickness: Float = 2f

    open val checkmarkColor = rgba(255, 255, 255)

    protected open var anim: Animation? = null

    /**
     * This function is called when the checkbox is checked.
     */
    override val onActivate: StateBlock.() -> Unit = {
        anim = activateAnimation?.create(activateAnimationDuration, 0f, 1f)
    }

    /**
     * This function is called when the checkbox is unchecked.
     */
    override val onDeactivate: StateBlock.() -> Unit = {
        anim = activateAnimation?.create(activateAnimationDuration, 1f, 0f)
    }

    /**
     * This function is called every time the checkbox is rendered, so you will need to check if [Checkbox.checked] is true.
     */
    open val renderCheck: Checkbox.() -> Unit = {
        if (active || anim != null) {
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
}
