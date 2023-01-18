package cc.polyfrost.polyui

import cc.polyfrost.polyui.components.impls.Block
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.events.ComponentEvent.Companion.events
import cc.polyfrost.polyui.layouts.impls.PixelLayout
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.utils.pixels

fun main() {
    val block: Block
    val screen = PolyUI(
        PixelLayout(
            at = Point(0F.pixels(), 0F.pixels()),
            Block(
                at = Point(0.84930.pixels(), 0.pixels()),
                size = Size(0.pixels(), 10.pixels()),
                events = events(
                    ComponentEvent.MousePressed(0).on {
                        println("Mouse pressed!")
                    },
                    ComponentEvent.MouseReleased(0).on {
                        println("Mouse released!")
                    })
            ).also { block = it }
        )
    )
    val window = GLWindow("Test", 800, 600, screen).open()
}