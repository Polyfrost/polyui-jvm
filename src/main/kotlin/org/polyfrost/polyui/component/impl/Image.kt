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
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2

open class Image(
    image: PolyImage,
    at: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2.ZERO,
    var backgroundColor: PolyColor? = null,
    alignment: Align = AlignDefault,
    radii: FloatArray? = null,
    vararg children: Component?
) : Block(children = children, at, size, alignment, Vec2.ZERO, false, null, radii) {
    constructor(image: String) : this(PolyImage(image))

    var image: PolyImage = image
        set(value) {
            field = value
            if (initialized) renderer.initImage(value, size)
            else image.onInit { needsRedraw = true }
        }

    init {
        image.onInit { needsRedraw = true }
    }

    override fun render() {
        val radii = this.radii
        when {
            radii == null -> {
                renderer.image(image, x, y, width, height, 0f, color.argb)
                backgroundColor?.let { renderer.rect(x, y, width, height, it) }
                borderColor?.let { renderer.hollowRect(x, y, width, height, it, borderWidth) }
            }

            radii.size < 4 -> {
                renderer.image(image, x, y, width, height, radii[0], color.argb)
                backgroundColor?.let { renderer.rect(x, y, width, height, it, radii[0]) }
                borderColor?.let { renderer.hollowRect(x, y, width, height, it, borderWidth, radii[0]) }
            }

            else -> {
                renderer.image(image, x, y, width, height, radii, color.argb)
                backgroundColor?.let { renderer.rect(x, y, width, height, it, radii) }
                borderColor?.let { renderer.hollowRect(x, y, width, height, it, borderWidth, radii) }
            }
        }
    }

    override fun setup(polyUI: PolyUI): Boolean {
        if (initialized) return false
        polyUI.renderer.initImage(image, size)
        if (!sizeValid) size = image.size
        if (image.type == PolyImage.Type.Vector) {
            palette = polyUI.colors.text.primary
        } else color = Color.WHITE
        return super.setup(polyUI)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("calculateSize")
    override fun calculateSize() = image.size

    override fun debugString() = "image: ${image.resourcePath.substringAfterLast('/')} (${image.size})"

    override fun extraToString() = image.resourcePath.substringAfterLast('/')
}
