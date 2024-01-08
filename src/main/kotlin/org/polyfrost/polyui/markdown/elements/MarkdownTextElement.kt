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
    attributes: Attributes?
) : TextElement<MarkdownStyle, Renderer>(style, layoutStyle, parent, qName, attributes) {
    private val font = when {
        layoutStyle.isBold && layoutStyle.isItalic -> style.textStyle.italicBoldFont
        layoutStyle.isBold -> style.textStyle.boldFont
        layoutStyle.isItalic -> style.textStyle.italicNormalFont
        else -> style.textStyle.normalFont
    }

    override fun drawText(
        text: String,
        x: Float,
        y: Float,
        fontSize: Float,
        color: Color,
        hovered: Boolean,
        position: MarkDownElementPosition,
        renderer: Renderer
    ) {
        val polyColor = PolyColor.from(color)
        if (layoutStyle.isUnderlined || layoutStyle.isPartOfLink && hovered) {
            renderer.rect(x, position.bottomY, position.width, floor(layoutStyle.fontSize / 8), polyColor)
        }
        if (layoutStyle.isStrikethrough) {
            renderer.rect(
                x, y + position.height / 2f - fontSize / 8f,
                position.width, floor(layoutStyle.fontSize / 8), polyColor
            )
        }
        renderer.text(font, x, y, text, polyColor, fontSize)
    }

    override fun drawInlineCodeBlock(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        renderer: Renderer
    ) {
        renderer.rect(x, y, width, height, PolyColor.from(color), radius = 2f)
    }

    override fun getTextWidth(text: String, fontSize: Float, renderer: Renderer): Float {
        return renderer.textBounds(font, text, fontSize).width
    }

    override fun getBaselineHeight(fontSize: Float, renderer: Renderer): Float {
        return renderer.textBounds(font, text, fontSize).height
    }
}
