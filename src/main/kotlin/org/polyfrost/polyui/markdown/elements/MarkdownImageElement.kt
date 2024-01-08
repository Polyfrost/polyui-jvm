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
    attributes: Attributes?
) : ImageElement<MarkdownStyle, Renderer, PolyImage>(style, layoutStyle, parent, qName, attributes) {
    override fun drawImage(
        image: PolyImage,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        renderer: Renderer
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
