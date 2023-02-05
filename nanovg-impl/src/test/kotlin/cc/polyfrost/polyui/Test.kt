package cc.polyfrost.polyui

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.component.impl.ImageBlock
import cc.polyfrost.polyui.component.impl.Text
import cc.polyfrost.polyui.event.ComponentEvent
import cc.polyfrost.polyui.event.ComponentEvent.Companion.events
import cc.polyfrost.polyui.layout.Layout.Companion.items
import cc.polyfrost.polyui.layout.impl.FlexLayout
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.renderer.impl.NVGRenderer
import cc.polyfrost.polyui.unit.*
import java.lang.Math.random

fun main() {
    val window = GLWindow("Test", 800, 800)
    val things = Array<Drawable>(50) {
        Block(
            at = flex(),
            size = Size((random() * 40 + 40).px, (random() * 40 + 40).px),
            events = events(
                ComponentEvent.MouseClicked(0) to {
                    println("Mouse clicked! $it")
                },
                ComponentEvent.MouseClicked(0, 2) to {
                    println("Mouse double-clicked!")
                },
                ComponentEvent.MouseClicked(1) to {
                    println("Mouse right-clicked!")
                },
            )
        )
    }
    val polyUI = PolyUI(
        window.width, window.height, NVGRenderer(),
        items = items(
            Text(
                text = "Kotlin...       rainbow!      and image",
                fontSize = 32.px,
                at = 20.px * 570.px,
            ),
            Block(
                props = BlockProperties(Color.Gradient(Color(1f, 0f, 1f, 1f), Color(0f, 1f, 1f, 1f))),
                at = 20.px * 600.px,
                size = 120.px * 120.px,
            ),
            Block(
                props = BlockProperties(Color.Chroma(5.seconds, 255)),
                at = 200.px * 600.px,
                size = 120.px * 120.px,
            ),
            ImageBlock(
                Image("/s.png", 120, 120),
                at = 380.px * 600.px,
            ),
            FlexLayout(
                at = 20.px * 30.px,
                wrap = 80.percent,
                items = things
            )
        )
    )

    window.open(polyUI)
}