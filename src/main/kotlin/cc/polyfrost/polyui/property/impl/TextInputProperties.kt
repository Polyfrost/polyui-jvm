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

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.input.PolyText
import cc.polyfrost.polyui.input.PolyTranslator.Companion.localised
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.utils.radii

class TextInputProperties(val text: TextProperties) : Properties() {
    override val palette: Colors.Palette get() = colors.component.bg
    val paddingFromTextLateral: Float = 12f
    val paddingFromTextVertical: Float = 8f
    val defaultText: PolyText = "polyui.text.default".localised()
    val cornerRadii: FloatArray = 0f.radii()
    val backgroundColor: Color get() = colors.component.bgDeselected
    val outlineColor: Color get() = colors.page.border20
    val outlineThickness: Float = 1f
}
