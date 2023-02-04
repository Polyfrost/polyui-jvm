package cc.polyfrost.polyui.components.impl

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.properties.impl.ImageBlockProperties
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2
import cc.polyfrost.polyui.units.px

open class ImageBlock(
    private val image: Image,
    properties: ImageBlockProperties = Properties.get<_, ImageBlock>(),
    acceptInput: Boolean = true,
    at: Vec2<Unit>,
    vararg events: ComponentEvent.Handler,
) : Component(properties, at, null, acceptInput, *events) {

    override fun render() {
        renderer.drawImage(image, x, y)
    }

    override fun getSize(): Vec2<Unit> {
        if (image.width == null) {
            renderer.initImage(image)
        }
        return Vec2(image.width!!.px, image.height!!.px)
    }
}