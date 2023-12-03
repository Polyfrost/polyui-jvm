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

package org.polyfrost.polyui

import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.renderer.impl.GLWindow
import org.polyfrost.polyui.renderer.impl.NVGRenderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2

fun main() {
    val i = PolyImage("icon.png")
    val window = GLWindow("PolyUI Test v2", 800, 600)
    val polyUI = PolyUI(
        renderer = NVGRenderer(Vec2(800f, 600f)),
        drawables = arrayOf(
            Group(
                visibleSize = Vec2(100f, 100f),
                alignment = Align(mode = Align.Mode.Vertical),
                children = arrayOf(
                    Button("Test1", leftImage = PolyImage("chevron-down.svg"), rightImage = PolyImage("icon.png")),
                    Button("Test2", leftImage = PolyImage("chevron-down.svg"), rightImage = PolyImage("icon.png")),
                    Button("Test3", leftImage = PolyImage("chevron-down.svg"), rightImage = PolyImage("icon.png")),
                    Button("Test4", leftImage = PolyImage("chevron-down.svg"), rightImage = PolyImage("icon.png")),
                ),
            ),
            Checkbox(size = 32f),
            Switch(size = 32f),
            Radiobutton(
                entries = arrayOf(
                    i to "hi",
                    i to "amaze!",
                    i to "bye",
                    null to "cri g",
                    i to "never",
                ),
            ),
            Slider(),
            TextInput(),
            Dropdown(
                entries = arrayOf(
                    null to "hello",
                    i to "test3",
                    i to "very amazing",
                    null to "crazy i was",
                    i to "crazy once",
                ),
            ),
        ),
    )

    // window.setIcon("icon.png")
    window.open(polyUI)
}
