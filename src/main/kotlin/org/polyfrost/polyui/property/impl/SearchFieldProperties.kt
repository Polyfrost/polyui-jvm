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

import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.input.search.SearchAlgorithm
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.px

open class SearchFieldProperties(open val searchAlgorithm: SearchAlgorithm = SearchAlgorithm.levenshtein) : Properties() {
    override val palette: Colors.Palette
        get() = colors.component.bg
    open val fontSize: Unit = 12.px
    open val textImagePadding: Unit = 8.px
    open val lateralPadding: Unit = 8.px
    open val inputProperties: TextInputProperties = TextInputProperties(TextProperties())
}