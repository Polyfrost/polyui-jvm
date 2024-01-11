/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2024 Polyfrost and its contributors.
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

package org.polyfrost.polyui.markdown.elements

import dev.dediamondpro.minemark.LayoutStyle
import dev.dediamondpro.minemark.elements.Element
import dev.dediamondpro.minemark.elements.impl.ImageElement
import dev.dediamondpro.minemark.providers.ImageProvider
import org.polyfrost.polyui.markdown.MarkdownStyle
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.xml.sax.Attributes

class MarkdownImageElement(
    style: MarkdownStyle,
    layoutStyle: LayoutStyle,
    parent: Element<MarkdownStyle, Renderer>?,
    qName: String,
    attributes: Attributes?,
) : ImageElement<MarkdownStyle, Renderer, PolyImage>(style, layoutStyle, parent, qName, attributes) {
    override fun drawImage(
        image: PolyImage,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        renderer: Renderer,
    ) {
        if (!image.init) {
            renderer.initImage(image)
            onDimensionsReceived(ImageProvider.Dimension(image.width, image.height))
            return
        }
        renderer.image(image, x, y, width, height)
    }

    override fun onImageReceived(image: PolyImage) {
        if (imageWidth != -1f && imageHeight != -1f) {
            image.width = imageWidth
            image.height = imageHeight
        }
        super.onImageReceived(image)
    }
}
