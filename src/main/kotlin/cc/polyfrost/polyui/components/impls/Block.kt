package cc.polyfrost.polyui.components.impls

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.properties.impls.BlockProperties
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2

/**
 * # Block
 *
 * A simple block component, supporting the full PolyUI API.
 */
open class Block(
    properties: Properties = Properties.get<BlockProperties>("cc.polyfrost.polyui.components.impls.Block"),
    at: Vec2<Unit>, size: Size<Unit>,
    acceptInput: Boolean = true,
    vararg events: ComponentEvent.Handler
) : Component(properties, at, size, acceptInput, *events) {
    private val props: BlockProperties = properties as BlockProperties

    override fun render() {
        renderer.drawRoundRectangle(x(), y(), width(), height(), color.getARGB(), props.cornerRadius)
    }


}