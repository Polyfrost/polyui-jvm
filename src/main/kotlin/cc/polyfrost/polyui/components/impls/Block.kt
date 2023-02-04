package cc.polyfrost.polyui.components.impls

import cc.polyfrost.polyui.color.Color
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
    color: Color? = null,
    acceptInput: Boolean = true,
    vararg events: ComponentEvent.Handler
) : Component(properties, at, size, acceptInput, *events) {
    private val props: BlockProperties = properties as BlockProperties
    override val color: Color.Mutable = color?.toMutable() ?: properties.color.toMutable()

    override fun render() {
        if (color is Color.Gradient) {
            val color = color as Color.Gradient
            renderer.drawGradientRect(x(), y(), width(), height(), color.getARGB1(), color.getARGB2(), color.type)
        } else renderer.drawRoundRect(x(), y(), width(), height(), color.getARGB(), props.cornerRadius)
    }


}