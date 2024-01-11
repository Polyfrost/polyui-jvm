/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.polyui.markdown

import dev.dediamondpro.minemark.MineMarkCore
import dev.dediamondpro.minemark.MineMarkCoreBuilder
import dev.dediamondpro.minemark.elements.Elements
import dev.dediamondpro.minemark.utils.MouseButton
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.events
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.markdown.elements.*
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
        markdownElement.draw(x, y, width, polyUI.mouseX, polyUI.mouseY, renderer)
    }

    private fun layoutCallback(newHeight: Float) {
        size.height = newHeight
    }

    companion object {
        private val defaultCore = MineMarkCore.builder<MarkdownStyle, Renderer>()
            .addExtension(StrikethroughExtension.create())
            .addExtension(TablesExtension.create())
            .addPolyUIExtensions()
            .build()
    }
}

fun MineMarkCoreBuilder<MarkdownStyle, Renderer>.addPolyUIExtensions(): MineMarkCoreBuilder<MarkdownStyle, Renderer> {
    return this.setTextElement(::MarkdownTextElement)
        .addElement(Elements.IMAGE, ::MarkdownImageElement)
        .addElement(Elements.HEADING, ::MarkdownHeadingElement)
        .addElement(Elements.HORIZONTAL_RULE, ::MarkdownHorizontalRuleElement)
        .addElement(Elements.CODE_BLOCK, ::MarkdownCodeBlockElement)
        .addElement(Elements.BLOCKQUOTE, ::MarkdownBlockquoteElement)
        .addElement(Elements.LIST_ELEMENT, ::MarkdownListElement)
        .addElement(Elements.TABLE_CELL, ::MarkdownTableCellElement)
}
