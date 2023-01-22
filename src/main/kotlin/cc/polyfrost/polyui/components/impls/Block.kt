package cc.polyfrost.polyui.components.impls

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.properties.impls.BlockProperties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2

class Block(
    properties: Properties = Properties.new<BlockProperties>("cc.polyfrost.polyui.components.impls.Block"),
    at: Vec2<Unit>, size: Size<Unit>,
    vararg events: ComponentEvent.Handler
) : Component(properties, at, size, *events) {



    override fun render(renderer: Renderer) {
        renderer.drawRectangle(x(), y(), width(), height(), properties.color)
    }


}