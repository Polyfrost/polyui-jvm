package cc.polyfrost.polyui

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.component.impl.Text
import cc.polyfrost.polyui.event.ComponentEvent
import cc.polyfrost.polyui.event.ComponentEvent.Companion.events
import cc.polyfrost.polyui.layout.Layout.Companion.items
import cc.polyfrost.polyui.layout.impl.FlexLayout
import cc.polyfrost.polyui.property.impl.BlockProperties
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
                    println("Mouse clicked!")
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
                text = "Kotlin...       rainbow!",
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
//            Block(
//                at = 80.percent x 0.px,
//                size = Size(1.3.percent, fill()),
//            ),
//            ImageBlock(
//                Image("/test.png", 100, 100),
//                at = 50.px x 10.px,
//            ),
//            Text(
//                text = "Hello, world!",
//                at = 0.px x 0.px,
//            ),
            FlexLayout(
                at = 20.px * 30.px,
                sized = 80.percent * 80.percent,
                items = things
            )
        )
    )

    window.open(polyUI)
}