package cc.polyfrost.polyui.components.impls

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.properties.impls.TextProperties
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2
import cc.polyfrost.polyui.utils.px

open class Text(
    properties: Properties = Properties.get("cc.polyfrost.polyui.components.impls.Text"),
    acceptInput: Boolean = false,
    var text: String, val fontSize: Unit.Pixel = 12.px(),
    at: Vec2<Unit>, size: Size<Unit>? = null,
    vararg events: ComponentEvent.Handler
) : Component(properties, at, size, acceptInput, *events) {
    val props: TextProperties = properties as TextProperties
    var autoSized = false

    override fun render() {
        renderer.drawText(props.font, x(), y(), if (autoSized) 0f else width(), text, props.color, fontSize.get())
    }

    override fun getSize(): Vec2<Unit> {
        autoSized = true
        return renderer.textBounds(props.font, text, fontSize.get()) as Vec2<Unit>
    }

}