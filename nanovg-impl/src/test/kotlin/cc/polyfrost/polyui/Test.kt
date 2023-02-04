package cc.polyfrost.polyui

import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.components.impls.Block
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.events.ComponentEvent.Companion.events
import cc.polyfrost.polyui.layouts.Layout.Companion.items
import cc.polyfrost.polyui.layouts.impls.FlexLayout
import cc.polyfrost.polyui.renderer.impl.GLWindow
import cc.polyfrost.polyui.renderer.impl.NVGRenderer
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.utils.UnitUtils.flex
import cc.polyfrost.polyui.utils.UnitUtils.x
import cc.polyfrost.polyui.utils.percent
import cc.polyfrost.polyui.utils.px
import cc.polyfrost.polyui.utils.seconds
import java.lang.Math.random

fun main() {
    val window = GLWindow("Test", 800, 800)
    val things = Array<Drawable>(8) {
        Block(
            at = flex(),
            size = Size((random() * 40 + 40).px(), (random() * 40 + 40).px()),
            events = events(
                ComponentEvent.MousePressed(0) to {
                    println("Mouse pressed!")
                    recolor(Color(0.5f, 0f, 1f, 1f), Animations.EaseInOutCubic, 1.seconds())
                },
                ComponentEvent.MouseReleased(0) to {
                    println("Mouse released!")
                },
                ComponentEvent.Added() to { })
        )
    }
    val polyUI = PolyUI(
        window.width, window.height, NVGRenderer(),
        items = items(
            Block(
                at = 20.px() x 600.px(),
                size = 120.px() x 120.px(),
                color = Color.Gradient(
                    Color(0.5f, 0f, 0f, 1f),
                    Color(0f, 0.5f, 0f, 1f),
                    Color.Gradient.Type.BottomLeftToTopRight
                ),
                events = events(
                    ComponentEvent.MousePressed(0) to {
                        println("Mouse pressed!")
                        recolor(Color(0.5f, 0f, 1f, 1f), Animations.EaseInOutCubic, 1.seconds())
                    },
                    ComponentEvent.MouseReleased(0) to {
                        println("Mouse released!")
                    },
                    ComponentEvent.Added().then { })
            ),
//            Block(
//                at = Point(80.percent(), 0.px()),
//                size = Size(1.3.percent(), fill()),
//            ),
//            ImageBlock(
//                Image("/test.png", 100, 100),
//                at = Point(50.px(), 10.px()),
//            ),
//            Text(
//                text = "Hello, world!",
//                at = Point(0.px(), 0.px()),
//            ),
            FlexLayout(
                at = 20.px() x 30.px(),
                sized = 80.percent() x 80.percent(),
                items = things
            )
        )
    )

    window.open(polyUI)
}