package cc.polyfrost.polyui

import cc.polyfrost.polyui.components.impls.Block
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.events.ComponentEvent.Companion.events
import cc.polyfrost.polyui.layouts.impls.PixelLayout
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.renderer.impl.NVGRenderer
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.utils.px

fun main() {
    val window = GLWindow("Test", 800, 600)
    val polyUI = PolyUI(
        window.width, window.height, NVGRenderer(),
        PixelLayout(
            at = Point(0F.px(), 0F.px()),
            sized = null,
            Block(
                at = Point(0.px(), 0.px()),
                size = Size(20.px(), 35.px()),
                events = events(
                    ComponentEvent.MousePressed(0).on {
                        println("Mouse pressed!")
                    },
                    ComponentEvent.MouseReleased(0).on {
                        println("Mouse released!")
                    })
            )
        )
    )
    window.open(polyUI)

}