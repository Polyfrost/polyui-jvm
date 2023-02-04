package cc.polyfrost.polyui.components.impl

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.properties.impl.BlockProperties
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2

/**
 * # Block
 *
 * A simple block component, supporting the full PolyUI API.
 */
open class Block(
    val props: BlockProperties = Properties.get<_, Block>(),
    at: Vec2<Unit>, size: Size<Unit>,
    acceptInput: Boolean = true,
    vararg events: ComponentEvent.Handler,
) : Component(props, at, size, acceptInput, *events) {

    override fun render() {
        renderer.drawRoundRect(x, y, width, height, color, props.cornerRadius)
    }
}