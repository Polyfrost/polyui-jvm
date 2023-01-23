package cc.polyfrost.polyui.components.impls

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.properties.impls.BlockProperties
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2
import cc.polyfrost.polyui.utils.px

class ImageBlock(
    private val image: Image,
    properties: Properties = Properties.get<BlockProperties>("cc.polyfrost.polyui.components.impls.Block"),
    at: Vec2<Unit>, size: Size<Unit>? = null,
    vararg events: ComponentEvent.Handler
) : Component(properties, at, size, *events) {


    override fun render() {
        renderer.drawImage(image, x(), y(), properties.color)
    }

    override fun getSize(): Vec2<Unit> {
        return Vec2(image.width.px(), image.height.px())
    }

}