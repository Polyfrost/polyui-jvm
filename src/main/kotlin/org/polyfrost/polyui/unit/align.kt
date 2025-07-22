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
 * @param line the cross axis, for example the Y axis in a [Mode.Horizontal] autolayout.
 * @param mode the mode for the autolayout. Horizontal fills from left to right, and Vertical fills from top to bottom.
 * @param padBetween the padding between each item in the autolayout.
 * @param padEdges *(since 1.11.0)* the padding on the edges of the autolayout.
 * @param wrap the wrap mode to use for the autolayout. [Wrap.AUTO] will wrap if the items do not fit in the available space, [Wrap.NEVER] will never wrap, and [Wrap.ALWAYS] will always wrap after each item.
 *
 * **Hot-Tip**: A wrap mode of [Wrap.ALWAYS] is usually equivalent to [Mode.Vertical] and [Wrap.NEVER], and is more efficient in terms of layout calculations.
 */
data class Align(val main: Content = Content.Start, val cross: Content = Content.Start, val line: Line = Line.Center, val mode: Mode = Mode.Horizontal, @get:JvmName("padBetween") val padBetween: Vec2 = Vec2(6f, 6f), @get:JvmName("padEdges") val padEdges: Vec2 = Vec2(6f, 6f), val wrap: Wrap = Wrap.AUTO) {
    constructor(main: Content = Content.Start, cross: Content = Content.Start, line: Line = Line.Center, mode: Mode = Mode.Horizontal, px: Float, py: Float) : this(main, cross, line, mode, Vec2(px, py), Vec2(px, py))

    /**
     * *(since 1.11.0)* the padding now supports seperate [padEdges] and [padBetween]. you can use that constructor instead if you wish.
     */
    @JvmOverloads
    constructor(main: Content = Content.Start, cross: Content = Content.Start, line: Line = Line.Center, mode: Mode = Mode.Horizontal, pad: Vec2 = Vec2(6f, 6f), wrap: Wrap = Wrap.AUTO) : this(main, cross, line, mode, pad, pad, wrap)

    enum class Content {
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

    enum class Line {
        /** Each item in the is line is against the top edge. */
        Start,

        /** Each item is centered on the current line. */
        Center,

        /** Each item in the line is against the bottom edge. */
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
