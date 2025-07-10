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

package org.polyfrost.polyui.unit

/**
 * @param main the main axis, for example the X axis in a [Mode.Horizontal] autolayout.
 * @param cross the cross axis, for example the Y axis in a [Mode.Horizontal] autolayout.
 * @param mode the mode for the autolayout. Horizontal fills from left to right, and Vertical fills from top to bottom.
 * @param pad the padding between each item in the autolayout.
 * @param maxRowSize the maximum row size to use if wrapping is required. set to `0` to specify you do not want to be wrapped.
 */
data class Align(val main: Main = Main.Start, val cross: Cross = Cross.Center, val mode: Mode = Mode.Horizontal, @get:JvmName("pad") val pad: Vec2 = Vec2(6f, 6f), val wrap: Wrap = Wrap.AUTO) {
    @JvmOverloads
    constructor(main: Main = Main.Start, cross: Cross = Cross.Center, mode: Mode = Mode.Horizontal, px: Float, py: Float) : this(main, cross, mode, Vec2(px, py))
    enum class Main {
        /** Items are packed in order they are added from the start of the row. */
        Start,

        /** Items are packed in the order they are added, but they are centered on each row. */
        Center,

        /** Items are packed in the order they are added, but they are packed from the end of each row. */
        End,

        /** Items are packed so the last items each touch the ends of the row. */
        SpaceBetween,

        /** Items are packed so the space between each item and the edges is equal. */
        SpaceEvenly,
    }

    enum class Cross {
        /** Each row is placed on after the other, with the first row at the top. */
        Start,

        /** Each row is placed on after the other, with the rows centered overall. */
        Center,

        /** Each row is placed on after the other, with the last row at the very bottom. */
        End,
    }

    enum class Mode {
        /** Items are packed from left to right, so the main axis is the x-axis and cross is the y-axis. */
        Horizontal,

        /** Items are packed from top to bottom, so the main axis is the y-axis and cross is the x-axis. */
        Vertical,
    }

    enum class Wrap {
        NEVER,
        ALWAYS,
        AUTO
    }
}

enum class Point {
    Above, At, Below
}
