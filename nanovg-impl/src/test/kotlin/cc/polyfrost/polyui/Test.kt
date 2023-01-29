package cc.polyfrost.polyui

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.components.impls.Block
import cc.polyfrost.polyui.components.impls.ImageBlock
import cc.polyfrost.polyui.components.impls.Text
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.events.ComponentEvent.Companion.events
import cc.polyfrost.polyui.layouts.Layout.Companion.items
import cc.polyfrost.polyui.layouts.impls.FlexLayout
import cc.polyfrost.polyui.layouts.impls.PixelLayout
import cc.polyfrost.polyui.renderer.data.Image
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.renderer.impl.NVGRenderer
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.utils.UnitUtils.flex
import cc.polyfrost.polyui.utils.px
import cc.polyfrost.polyui.utils.seconds

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
                        recolor(Color(0.5f, 0f, 1f, 1f), Animations.EaseInOutCubic, 1.seconds())
                    },
                    ComponentEvent.MouseReleased(0).on {
                        println("Mouse released!")
                    })
            ),
            Block(
                at = Point(100.px(), 0.px()),
                size = Size(50.px(), 50.px()),
            ),
            ImageBlock(
                Image("/test.png", 100, 100),
                at = Point(50.px(), 10.px()),
            ),
            Text(
                text = "Hello, world!",
                at = Point(0.px(), 0.px()),
            )
        ),
        FlexLayout(
            at = Point(0.px(), 0.px()),
            wrap = 100.px(),
            items = items(
                Block(
                    at = flex(),
                    size = Size(50.px(), 50.px()),
                ),
                Block(
                    at = flex(),
                    size = Size(50.px(), 50.px()),
                )
            )
        )
    )
    window.open(polyUI)

}