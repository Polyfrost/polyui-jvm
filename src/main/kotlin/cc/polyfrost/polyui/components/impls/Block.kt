package cc.polyfrost.polyui.components.impls

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.properties.PropertiesRegistry
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2

class Block(
    properties: Properties = PropertiesRegistry.forNew("cc.polyfrost.polyui.components.impls.Block"),
    at: Vec2<Unit>, size: Vec2<Unit>,
    vararg events: ComponentEvent.Handler
) : Component(properties, at, size, *events) {



    override fun render(renderer: Renderer) {
        TODO("Not yet implemented")
    }


    override fun calculateBounds(layout: Layout) {
        TODO("Not yet implemented")
    }

    override fun needsRecalculation(): Boolean {
        TODO("Not yet implemented")
    }


}