package org.polyfrost.polyui.markdown.elements

import dev.dediamondpro.minemark.LayoutData
import dev.dediamondpro.minemark.LayoutStyle
import dev.dediamondpro.minemark.elements.Element
import dev.dediamondpro.minemark.elements.impl.BlockQuoteElement
import dev.dediamondpro.minemark.elements.impl.list.ListElement
import dev.dediamondpro.minemark.elements.impl.list.ListHolderElement
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.markdown.MarkdownStyle
import org.polyfrost.polyui.renderer.Renderer
import org.xml.sax.Attributes
import java.awt.Color

class MarkdownListElement(
    style: MarkdownStyle,
    layoutStyle: LayoutStyle,
    parent: Element<MarkdownStyle, Renderer>?,
    qName: String,
    attributes: Attributes?
) : ListElement<MarkdownStyle, Renderer>(style, layoutStyle, parent, qName, attributes) {
    private var markerStr: String = when (listType) {
        ListHolderElement.ListType.ORDERED -> "${elementIndex + 1}. "
        ListHolderElement.ListType.UNORDERED -> "- "
        else -> "- "
    }

    override fun drawMarker(x: Float, y: Float, renderer: Renderer) {
        renderer.text(
            style.textStyle.normalFont, x, y, markerStr,
            PolyColor.from(style.textStyle.defaultTextColor),
            style.textStyle.defaultFontSize
        )
    }

    override fun getListMarkerWidth(layoutData: LayoutData, renderer: Renderer): Float {
        return renderer.textBounds(style.textStyle.normalFont, markerStr, style.textStyle.defaultFontSize).width
    }

    override fun getMarkerHeight(layoutData: LayoutData, renderer: Renderer): Float {
        return renderer.textBounds(style.textStyle.normalFont, markerStr, style.textStyle.defaultFontSize).height
    }
}
