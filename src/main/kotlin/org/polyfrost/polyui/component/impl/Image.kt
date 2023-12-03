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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2

open class Image(image: PolyImage, at: Vec2? = null, alignment: Align = AlignDefault, visibleSize: Vec2? = null, vararg children: Drawable?) : Block(at, Vec2.Based(base = image.size), alignment, visibleSize, children = children) {
    var image: PolyImage = image
        set(value) {
            field = value
            renderer.initImage(value)
        }

    override val shouldScroll get() = false

    override fun render() {
        renderer.image(image, x, y)
    }

    override fun debugRender() {}

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        renderer.initImage(image)
        super.setup(renderer, polyUI)
    }
}
