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

import dev.dediamondpro.minemark.LayoutData.MarkDownElementPosition
import dev.dediamondpro.minemark.LayoutStyle
import dev.dediamondpro.minemark.elements.Element
import dev.dediamondpro.minemark.elements.impl.TextElement
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.markdown.MarkdownStyle
import org.polyfrost.polyui.renderer.Renderer
import org.xml.sax.Attributes
import java.awt.Color
import kotlin.math.floor

class MarkdownTextElement(
    style: MarkdownStyle,
    layoutStyle: LayoutStyle,
    parent: Element<MarkdownStyle, Renderer>?,
    qName: String,
    attributes: Attributes?,
) : TextElement<MarkdownStyle, Renderer>(style, layoutStyle, parent, qName, attributes) {
    private val textColor: PolyColor = PolyColor.from(layoutStyle.textColor)
    private val codeBlockColor: PolyColor = PolyColor.from(style.codeBlockStyle.color)
    private val font =
        when {
            layoutStyle.isPartOfCodeBlock -> style.codeBlockStyle.codeFont
            layoutStyle.isBold && layoutStyle.isItalic -> style.textStyle.italicBoldFont
            layoutStyle.isBold -> style.textStyle.boldFont
            layoutStyle.isItalic -> style.textStyle.italicNormalFont
            else -> style.textStyle.normalFont
        }
    private var lineHeight = -1f

    override fun drawText(
        text: String,
        x: Float,
        y: Float,
        fontSize: Float,
        color: Color,
        hovered: Boolean,
        position: MarkDownElementPosition,
        renderer: Renderer,
    ) {
        if (layoutStyle.isUnderlined || layoutStyle.isPartOfLink && hovered) {
            renderer.rect(x, position.bottomY, position.width, floor(layoutStyle.fontSize / 8), textColor)
        }
        if (layoutStyle.isStrikethrough) {
            renderer.rect(
                x,
                y + position.height / 2f - fontSize / 8f,
                position.width,
                floor(layoutStyle.fontSize / 8),
                textColor,
            )
        }
        renderer.text(font, x, y, text, textColor, fontSize)
    }

    override fun drawInlineCodeBlock(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        renderer: Renderer,
    ) {
        renderer.rect(x, y, width, height, codeBlockColor, radius = 2f)
    }

    override fun getTextWidth(
        text: String,
        fontSize: Float,
        renderer: Renderer,
    ): Float {
        return renderer.textBounds(font, text, fontSize).width
    }

    override fun getBaselineHeight(
        fontSize: Float,
        renderer: Renderer,
    ): Float {
        if (lineHeight == -1f) {
            lineHeight = renderer.textBounds(font, text, fontSize).height
        }
        return lineHeight
    }
}
