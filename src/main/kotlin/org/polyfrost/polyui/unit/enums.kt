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

package org.polyfrost.polyui.unit


/**
 * @param main the main axis, for example the X axis in a [Mode.Horizontal] autolayout.
 * @param cross the cross axis, for example the Y axis in a [Mode.Horizontal] autolayout.
 * @param mode the mode for the autolayout. Horizontal fills from left to right, and Vertical fills from top to bottom.
 */
class Align(val main: Main = Main.Center, val cross: Cross = Cross.Middle, val mode: Mode = Mode.Horizontal, val padding: Vec2 = Vec2(6f, 2f)) {
    enum class Main {
        Left, Center, Right, Spread
    }
    enum class Cross {
        Top, Middle, Bottom
    }
    enum class Mode {
        Horizontal, Vertical
    }
}

// todo implement state again
/**
 * Enum to represent the three states a stated component can have in PolyUI.
 * @see org.polyfrost.polyui.color.Colors.State
 * @since 0.17.2
 */
enum class State {
    Danger, Warning, Success
}
