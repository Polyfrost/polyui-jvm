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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.radii

open class Image(
    image: PolyImage,
    at: Vec2? = null,
    size: Vec2? = null,
    radii: FloatArray = 0f.radii(),
    var backgroundColor: PolyColor? = null,
    alignment: Align = AlignDefault,
    vararg children: Drawable?,
) : Block(children = children, at, size, alignment, null, false, null, radii) {
    constructor(image: String) : this(PolyImage(image))

    var image: PolyImage = image
        set(value) {
            field = value
            renderer.initImage(value)
        }

    override val shouldScroll get() = false

    override fun render() {
        renderer.image(image, x, y, width, height, radii, color.argb)
        backgroundColor?.let { renderer.rect(x, y, width, height, color, radii) }
        val outlineColor = boarderColor ?: return
        renderer.hollowRect(x, y, width, height, outlineColor, boarderWidth, radii)
    }

    override fun setup(polyUI: PolyUI): Boolean {
        if (initialized) return false
        polyUI.renderer.initImage(image)
        if (!sizeValid) size.set(image.size)
        palette = polyUI.colors.text.primary
        return super.setup(polyUI)
    }

    override fun debugString() = "image: ${image.resourcePath.substringAfterLast('/')} (${image.size})"
}
