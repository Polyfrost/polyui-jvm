/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.property.impl.TextProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.px
import org.polyfrost.polyui.unit.times
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.radii
import org.polyfrost.polyui.utils.wrap
import kotlin.jvm.internal.Ref.FloatRef

/**
 * Markdown text, with a simple stupid parser.
 *
 * Supported syntax:
 * - *italic*, **bold**, ***bold italic***
 * - [named links](https://example.com)
 * - images ![alt text](https://example.com/image.png)
 * - lists with + or -
 * - headers with #, in h1, h2, and h3
 */
@Deprecated("Not currently implemented.")
class MarkdownText(properties: MarkdownProperties? = MarkdownProperties, markdown: String, at: Vec2<Unit>, size: Vec2<Unit>? = null) : Component(properties, at, size, acceptInput = false) {
    override val properties
        get() = super.properties as MarkdownProperties
    var markdown = markdown
        set(value) {
            field = value
            parse(value)
        }
    protected val nodes = ArrayList<Node>()

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        val colors = properties.colors
        val fonts = properties.fonts
        properties.bold.init(colors, fonts)
        properties.boldItalic.init(colors, fonts)
        properties.italic.init(colors, fonts)
        properties.regular.init(colors, fonts)
        properties.header1.init(colors, fonts)
        properties.header2.init(colors, fonts)
        properties.header3.init(colors, fonts)
        parse(markdown)
    }

    override fun render() {
        var y = 0f
        nodes.fastEach {
            y = it.y
            it.render(renderer)
        }
    }

    abstract class Node(val x: Float, val y: Float) {
        abstract fun render(renderer: Renderer)
    }

    class TextNode(
        val str: String,
        val properties: TextProperties,
        x: Float,
        y: Float,
        val size: Vec2<Unit.Pixel>,
        val isLink: Boolean,
        val isList: Boolean,
    ) : Node(x, y) {
        val color = properties.palette.normal.toAnimatable()
        override fun render(renderer: Renderer) {
            val xx = x + if (isList) 16f else 0f
            renderer.text(properties.font, xx, y, str, color, properties.fontSize.px)
            if (isLink) renderer.line(xx, y + size.height - 1f, x + size.width, y + size.height - 1f, Color.WHITE, 0.6f)
        }

        override fun toString() = str
    }

    class ImageNode(private val img: PolyImage, x: Float, y: Float, private val radii: FloatArray) : Node(x, y) {
        override fun render(renderer: Renderer) {
            renderer.image(img, x, y, radii = radii)
        }

        override fun toString() = img.toString()
    }

    protected fun parse(str: String) {
        val now = System.nanoTime()
        var h = 0
        var s = 0
        var stars = -1
        var header = -1
        var name = -1
        var nameExit = -1
        var url = -1
        var newLine = -1
        var starStop = -1
        var onlyWhitespace = true
        var listed = -1
        var listing = false
        var starring = false
        var nstars = 0
        var starred: String? = null
        val x = FloatRef()
        val y = FloatRef()
        val sb = StringBuilder()
        for ((i, c) in str.withIndex()) {
            when (c) {
                '#' -> {
                    if (onlyWhitespace || header != -1) {
                        starStop = i
                        starring = false
                        onlyWhitespace = false
                        if (header == -1) header = i
                        h++
                    }
                }

                '[' -> {
                    starStop = i
                    starring = false
                    onlyWhitespace = false
                    name = i
                }

                ']' -> {
                    starStop = i
                    starring = false
                    onlyWhitespace = false
                    nameExit = i
                }

                '(' -> {
                    starStop = i
                    starring = false
                    onlyWhitespace = false
                    url = i
                }

                ')' -> {
                    starStop = i
                    starring = false
                    onlyWhitespace = false
                    // check valid:  ](             [             ]                 (            )
                    if (nameExit + 1 == url && name != -1 && name < nameExit && url != -1 && url < i) {
                        if (sb.isNotEmpty()) {
                            add(sb.toString(), x, y, 0, 0, false, false)
                            sb.clear()
                        }
                        val link = str.substring(url + 1, i)
                        val isImage = str[name - 1] == '!'
                        if (isImage) {
                            y.element += properties.lineBreakSpace.px
                            x.element = 0f
                            val img = PolyImage(link)
                            renderer.initImage(img)
                            nodes.add(ImageNode(img, x.element, y.element, properties.imageRadii))
                            y.element += img.height + properties.lineBreakSpace.px
                        } else {
                            add(starred ?: str.substring(name + 1, nameExit), x, y, nstars, h, true, false)
                        }
//                        println("${starred ?: str.substring(name + 1, nameExit)} -> $link")
                        url = -1
                        name = -1
                        nameExit = -1
                    }
                }

                '*' -> {
                    onlyWhitespace = false
                    if (stars == -1) {
                        stars = i
                        starring = true
                    }
                    if (!starring) {
                        s--
                        if (s == 0) {
                            nstars = (i - starStop).coerceAtMost(3)
                            starred = str.substring(stars + nstars, i - nstars + 1)
                            stars = -1
                        }
                    } else {
                        s++
                    }
                }

                '+', '-' -> {
                    if (onlyWhitespace) {
                        if (!listing) {
                            listing = true
//                            println("list start")
                            y.element += properties.lineBreakSpace.px
                            x.element = 0f
                        }
                        listed = i
                    }
                    onlyWhitespace = false
                }

                '\n' -> {
                    if (listing) {
                        if (listed == -1) {
//                            println("list end")
                            listing = false
                        } else {
                            if (sb.isNotEmpty()) {
                                val o = add(sb.toString(), x, y, nstars, h, false, true)
                                sb.clear()
                                x.element = 0f
                                y.element += properties.lineSpace.px + o.size.height
                            }
//                            println(starred ?: str.substring(listed + 1, i))
                        }
                    }
                    if (header != -1) {
                        if (str[h + header] == ' ') {
                            add(starred ?: str.substring(header + h + 1, i), x, y, nstars, h, false, false)
//                            println((starred ?: str.substring(header + h + 1, i)) + " $h")
                        }
                    }
                    if (newLine == i - 1) {
                        y.element += properties.lineBreakSpace.px
                        x.element = 0f
//                        println("line skip")
                    }
                    if (sb.isNotEmpty()) {
                        add(sb.toString(), x, y, 0, 0, false, false)
                        sb.clear()
                    }
                    h = 0
                    s = 0
                    nstars = 0
                    starred = null
                    starStop = -1
                    starring = false
                    stars = -1
                    header = -1
                    newLine = i
                    listed = -1
                    onlyWhitespace = true
                }

                else -> {
                    if (name == -1 && header == -1) {
                        if (str.getOrNull(i + 1) != '[') sb.append(c)
                    }
                    starStop = i
                    starring = false
                    onlyWhitespace = false
                }
            }
        }
        if (polyUI.settings.debug) {
            PolyUI.LOGGER.info("Markdown parse of ${str.length} characters to ${nodes.size} elements took ${(System.nanoTime() - now) / 1_000_000f}ms")
        }
        if (size == null) size = x.element.px * y.element.px
    }

    fun add(str: String, x: FloatRef, y: FloatRef, nstars: Int, h: Int, isLink: Boolean, isList: Boolean): TextNode {
        val isHeader = h != 0
        val properties = when (h) {
            1 -> properties.header1
            2 -> properties.header2
            3 -> properties.header3
            else -> {
                when (nstars) {
                    1 -> properties.italic
                    2 -> properties.bold
                    3 -> properties.boldItalic
                    else -> properties.regular
                }
            }
        }
        if (isHeader) {
            y.element += this.properties.lineBreakSpace.px
            x.element = 0f
        }
        val bounds = renderer.textBounds(properties.font, str, properties.fontSize.px)
        var out: TextNode? = null
        if (size != null) {
            if (x.element + bounds.width > size!!.width) {
                str.wrap(size!!.width - x.element, renderer, properties.font, properties.fontSize.px).fastEach {
                    val b = renderer.textBounds(properties.font, it, properties.fontSize.px)
                    out = TextNode(it, properties, x.element, y.element, b, isLink, isList)
                    nodes.add(out!!)
                    y.element += this.properties.lineSpace.px + b.height
                    x.element = 0f
                }
            }
        } else {
            out = TextNode(str, properties, x.element, y.element, bounds, isLink, isList)
            nodes.add(out!!)
            x.element += bounds.width
        }
        if (isHeader) {
            y.element += this.properties.lineBreakSpace.px + bounds.height
            x.element = 0f
        }
        return out!!
    }

    object MarkdownProperties : Properties() {
        open val imageRadii = 4f.radii()
        open val lineBreakSpace = 15.px
        open val lineSpace = 4.px
        override val palette get() = colors.text.primary
        open val header1 = TextProperties(32.px) { fonts.medium }
        open val header2 = TextProperties(24.px) { fonts.medium }
        open val header3 = TextProperties(18.px) { fonts.medium }
        open val regular = TextProperties(12.px) { fonts.regular }
        open val italic = TextProperties(12.px) { fonts.regularItalic }
        open val bold = TextProperties(12.px) { fonts.bold }
        open val boldItalic = TextProperties(12.px) { fonts.boldItalic }
    }
}
