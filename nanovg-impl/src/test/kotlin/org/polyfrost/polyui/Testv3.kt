package org.polyfrost.polyui

import org.polyfrost.polyui.color.DarkTheme
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Group
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.dsl.polyUI
import org.polyfrost.polyui.renderer.impl.GLFWWindow
import org.polyfrost.polyui.renderer.impl.GLRenderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.open

fun main() {
    val window = GLFWWindow("PolyUI Test v3", 1600, 970)
    val theme = DarkTheme()

    polyUI {
        size = 1600f by 970f
        renderer = GLRenderer
        settings.debug = true
        colors = theme
        backgroundColor = theme.page.bg.normal
        alignment = Align(main = Align.Content.SpaceBetween)


        TestAlignment(Align(padEdges = 2f by 2f)).add()
        TestAlignment(Align(padBetween = 2f by 2f)).add()
        TestAlignment(Align(pad = 8f by 8f)).add()
        Block(size = 1588f by 20f).add()

        for(mode in Align.Mode.entries) {
            for (mType in Align.Content.entries) {
                for (cType in Align.Content.entries) {
                    for (lType in Align.Line.entries) {
                        TestAlignment(Align(mode = mode, main = mType, cross = cType, line = lType)).add()
                    }
                }
                Block(size = 1588f by 20f).add()
            }
            Block(size = 1588f by 40f).add()
        }


    }.also { it.master.rawRescaleSize = true }.open(window)
}

private fun TestAlignment(align: Align) = Group(
    Text("${align.mode}, M: ${align.main}, C: ${align.cross}, L: ${align.line}; ${if (align.uniformPadding) align.pad else "E: ${align.padEdges}, B: ${align.padBetween}"}", font = PolyUI.monospaceFont, fontSize = 10f),
    Group(
        Block(size = 40f by 30f),
        Block(size = 60f by 30f),
        Block(size = 40f by 40f),
        Block(size = 50f by 20f),
        Block(size = 20f by 30f),
        Block(size = 40f by 35f),
        Block(size = 60f by 30f),
        Block(size = 40f by 40f),
        Block(size = 50f by 20f),
        Block(size = 20f by 30f),
        size = 400f by 120f,
        alignment = align
    ),
    alignment = Align(pad = Vec2.ZERO, wrap = Align.Wrap.ALWAYS)
)
