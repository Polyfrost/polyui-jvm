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

package org.polyfrost.polyui.unit

enum class SlideDirection {
    FromLeft, FromRight, FromTop, FromBottom
}

/** enum representing the type of animation to be used. */
sealed class Transitions {
    data object Fade : Transitions()
    data class Slide(val direction: SlideDirection) : Transitions()
}

enum class TextAlign {
    Left, Right, Center
}

enum class Direction {
    Horizontal, Vertical
}

enum class Side {
    Left, Right
}

/**
 * Represents a fixed-size gap between rows or columns, used in flex and grid layouts.
 *
 * @see [org.polyfrost.polyui.layout.impl.FlexLayout]
 * @see [org.polyfrost.polyui.layout.impl.GridLayout]
 */
data class Gap(val mainGap: Unit.Pixel, val crossGap: Unit.Pixel) {

    companion object {
        @JvmField
        @Transient
        val Default = Gap(5.px, 5.px)
    }
}
