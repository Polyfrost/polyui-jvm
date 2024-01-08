package org.polyfrost.polyui.markdown

import dev.dediamondpro.minemark.providers.ImageProvider
import org.polyfrost.polyui.renderer.data.PolyImage
import java.util.function.Consumer

object MarkdownImageProvider : ImageProvider<PolyImage> {
    override fun getImage(
        src: String,
        dimensionCallback: Consumer<ImageProvider.Dimension>,
        imageCallback: Consumer<PolyImage>
    ) {
        imageCallback.accept(PolyImage(src))
    }
}
