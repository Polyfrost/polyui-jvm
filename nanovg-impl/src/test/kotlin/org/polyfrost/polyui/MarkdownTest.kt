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

import org.polyfrost.polyui.markdown.MarkdownComponent
import org.polyfrost.polyui.renderer.impl.GLWindow
import org.polyfrost.polyui.renderer.impl.NVGRenderer
import org.polyfrost.polyui.unit.Vec2

fun main() {
    val window = GLWindow("PolyUI Markdown Test", 800, 500)
    val polyUI =
        PolyUI(
            renderer = NVGRenderer(Vec2(800f, 500f)),
            drawables =
            arrayOf(
                MarkdownComponent(
                    markdown = "Test **string** *with* a<br>newline because why not and also a [link](https://polyfrost.org), this should also automatically wrap if I make this text long enough",
                    at = Vec2(150f, 0f),
                    size = Vec2(500f, 800f)
                )
            ),
        )

    window.open(polyUI)
}
