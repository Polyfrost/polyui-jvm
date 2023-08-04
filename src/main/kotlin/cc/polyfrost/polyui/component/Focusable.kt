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

package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.event.FocusedEvent

/** implement this in order to receive focus event, such as key presses */
interface Focusable {
    /**
     * Accept a [FocusedEvent] and handle it.
     * @see FocusedEvent.KeyPressed
     * @see FocusedEvent.KeyReleased
     * @see FocusedEvent.KeyTyped
     */
    fun accept(event: FocusedEvent)

    /**
     * Accept a non-mapped key press. This is called when any key is pressed that is not a [mapped key][cc.polyfrost.polyui.input.Keys].
     *
     * This method should not really be used in normal usage, as it is effectively platform-dependent. It is provided for usage where you need to use a key that is not mapped by PolyUI, such as keybindings.
     *
     * See [PolyUI.getKeyName][cc.polyfrost.polyui.PolyUI.getKeyName] to get the name of the key.
     */
    fun accept(key: Int, down: Boolean) {}
}
