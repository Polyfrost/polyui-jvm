package org.polyfrost.polyui.markdown.elements

import dev.dediamondpro.minemark.LayoutStyle
import dev.dediamondpro.minemark.elements.Element
import dev.dediamondpro.minemark.elements.impl.table.TableCellElement
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.markdown.MarkdownStyle
import org.polyfrost.polyui.renderer.Renderer
import org.xml.sax.Attributes
import java.awt.Color

class MarkdownTableCellElement(
    style: MarkdownStyle,
    layoutStyle: LayoutStyle,
    parent: Element<MarkdownStyle, Renderer>?,
    qName: String,
    attributes: Attributes?,
) : TableCellElement<MarkdownStyle, Renderer>(style, layoutStyle, parent, qName, attributes) {
    private lateinit var fillPolyColor: PolyColor
    private val borderPolyColor = PolyColor.from(style.tableStyle.borderColor)

    override fun drawCellBackground(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        renderer: Renderer
    ) {
        if (!this::fillPolyColor.isInitialized) {
            fillPolyColor = PolyColor.from(color)
        }
        renderer.rect(x, y, width, height, fillPolyColor)
    }

    override fun drawBorderLine(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        renderer: Renderer
    ) {
        renderer.rect(x, y, width, height, borderPolyColor)
    }
}
