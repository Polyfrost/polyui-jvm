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

import dev.dediamondpro.minemark.LayoutData
import dev.dediamondpro.minemark.LayoutStyle
import dev.dediamondpro.minemark.elements.Element
import dev.dediamondpro.minemark.elements.impl.list.ListElement
import dev.dediamondpro.minemark.elements.impl.list.ListHolderElement
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.markdown.MarkdownStyle
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Vec2
import org.xml.sax.Attributes

class MarkdownListElement(
    style: MarkdownStyle,
    layoutStyle: LayoutStyle,
    parent: Element<MarkdownStyle, Renderer>?,
    qName: String,
    attributes: Attributes?,
) : ListElement<MarkdownStyle, Renderer>(style, layoutStyle, parent, qName, attributes) {
    private val markerStr: String =
        when (listType) {
            ListHolderElement.ListType.ORDERED -> "${elementIndex + 1}. "
            ListHolderElement.ListType.UNORDERED -> "- "
            else -> "- "
        }
    private lateinit var markerBounds: Vec2

    override fun drawMarker(
        x: Float,
        y: Float,
        renderer: Renderer,
    ) {
        renderer.text(
            style.textStyle.normalFont,
            x,
            y,
            markerStr,
            PolyColor.from(style.textStyle.defaultTextColor),
            style.textStyle.defaultFontSize,
        )
    }

    override fun getListMarkerWidth(
        layoutData: LayoutData,
        renderer: Renderer,
    ): Float {
        if (!this::markerBounds.isInitialized) {
            markerBounds = renderer.textBounds(style.textStyle.normalFont, markerStr, style.textStyle.defaultFontSize)
        }
        return markerBounds.width
    }

    override fun getMarkerHeight(
        layoutData: LayoutData,
        renderer: Renderer,
    ): Float {
        return markerBounds.height
    }
}
