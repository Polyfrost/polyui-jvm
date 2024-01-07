package org.polyfrost.polyui.markdown

import dev.dediamondpro.minemark.MineMarkCore
import dev.dediamondpro.minemark.elements.Elements
import dev.dediamondpro.minemark.utils.MouseButton
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.polyfrost.polyui.markdown.elements.MarkdownTextElement
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.events
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2

class MarkdownComponent(
    markdown: String,
    at: Vec2? = null,
    size: Vec2? = null,
    style: MarkdownStyle = MarkdownStyle(),
    core: MineMarkCore<MarkdownStyle, Renderer> = defaultCore,
    alignment: Align = AlignDefault,
    visibleSize: Vec2? = null,
    focusable: Boolean = false,
    vararg children: Drawable?,
) : Drawable(at, alignment, size, visibleSize, focusable = focusable, children = children) {
    private val markdownElement = core.parse(style, markdown).apply {
        addLayoutCallback(this@MarkdownComponent::layoutCallback)
    }

    init {
        events {
            Event.Mouse.Clicked(0).then {
                markdownElement.onMouseClicked(x, y, MouseButton.LEFT, it.mouseX, it.mouseY)
            }
            Event.Mouse.Clicked(1).then {
                markdownElement.onMouseClicked(x, y, MouseButton.RIGHT, it.mouseX, it.mouseY)
            }
            Event.Mouse.Clicked(2).then {
                markdownElement.onMouseClicked(x, y, MouseButton.MIDDLE, it.mouseX, it.mouseY)
            }
            Event.Mouse.Entered
        }
    }

    override fun preRender() {
        markdownElement.beforeDraw(x, y, width, polyUI.mouseX, polyUI.mouseY, renderer)
    }

    override fun render() {
        markdownElement.draw(x, y, width,  polyUI.mouseX, polyUI.mouseY, renderer)
    }

    private fun layoutCallback(newHeight: Float) {
        size.height = newHeight
    }

    companion object {
        private val defaultCore = MineMarkCore.builder<MarkdownStyle, Renderer>()
            .addExtension(StrikethroughExtension.create())
            .addExtension(TablesExtension.create())
            .addElement(Elements.TEXT, ::MarkdownTextElement)
            .build()
    }
}
