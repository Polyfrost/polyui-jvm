package cc.polyfrost.polyui.components.impls

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.properties.impls.BlockProperties
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2

open class Block(
    properties: Properties = Properties.get<BlockProperties>("cc.polyfrost.polyui.components.impls.Block"),
    at: Vec2<Unit>, size: Size<Unit>,
    vararg events: ComponentEvent.Handler
) : Component(properties, at, size, *events) {


    override fun render() {
        renderer.drawRect(x(), y(), width(), height(), properties.color)
    }


}