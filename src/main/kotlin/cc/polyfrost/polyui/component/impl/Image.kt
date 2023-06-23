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

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.impl.ImageProperties
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit

open class Image @JvmOverloads constructor(
    properties: ImageProperties? = null,
    val image: PolyImage,
    at: Vec2<Unit>,
    rawResize: Boolean = false,
    acceptInput: Boolean = true,
    vararg events: Events.Handler
) : Component(properties, at, null, rawResize, acceptInput, *events) {
    final override val properties: ImageProperties
        get() = super.properties as ImageProperties

    override fun render() {
        renderer.drawImage(image, x, y, width, height, properties.cornerRadii, properties.color.argb)
    }

    override fun calculateSize(): Vec2<Unit> {
        if (image.width == -1f || image.height == -1f) {
            renderer.initImage(image)
        }
        return Vec2(image.width.px, image.height.px)
    }
}
