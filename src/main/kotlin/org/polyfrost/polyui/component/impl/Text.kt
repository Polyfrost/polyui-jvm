/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.input.PolyText
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.property.impl.TextProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Line
import org.polyfrost.polyui.renderer.data.MultilineText
import org.polyfrost.polyui.renderer.data.SingleText
import org.polyfrost.polyui.renderer.data.Text
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.cl1
import kotlin.math.floor

@Suppress("UNCHECKED_CAST")
open class Text @JvmOverloads constructor(
    properties: TextProperties? = null,
    var initialText: PolyText,
    at: Vec2<Unit>,
    size: Size<Unit>? = null,
    fontSize: Unit? = null,
    val textAlign: TextAlign = TextAlign.Left,
    rawResize: Boolean = false,
    acceptInput: Boolean = false,
    events: EventDSL<org.polyfrost.polyui.component.impl.Text>.() -> kotlin.Unit = {},
) : Component(properties, at, size, rawResize, acceptInput, events as EventDSL<Component>.() -> kotlin.Unit) {
    /** Internally [txt] is stored as a [PolyText] object, which supports localization and object substitution */
    @JvmOverloads
    constructor(
        txt: String,
        at: Vec2<Unit>,
        size: Size<Unit>? = null,
        fontSize: Unit? = null,
        textAlign: TextAlign = TextAlign.Left,
        rawResize: Boolean = false,
        acceptInput: Boolean = false,
        events: EventDSL<org.polyfrost.polyui.component.impl.Text>.() -> kotlin.Unit = {},
    ) : this(null, txt.localised(), at, size, fontSize, textAlign, rawResize, acceptInput, events)

    constructor(properties: TextProperties? = null, text: PolyText, fontSize: Unit, at: Vec2<Unit>) :
        this(properties, text, at, null, fontSize)

    final override val properties: TextProperties
        get() = super.properties as TextProperties

    @Transient
    private val fs = fontSize ?: this.properties.fontSize

    @Transient
    internal lateinit var str: Text
    var fontSize get() = str.fontSize
        set(value) {
            str.fontSize = value
        }
    val stringSize get() = str.size
    val lines get() = str.lines
    val full get() = str.full
    val font get() = str.font
    var string
        get() = str.text.string
        set(value) {
            str.text.string = value
            str.calculate()
        }

    override fun trueX(): Float {
        calculateBounds()
        return super.trueX()
    }

    override fun trueY(): Float {
        calculateBounds()
        return super.trueY()
    }

    override fun render() {
        @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
        if (string.length == 0) return
        val push = if (str.textOffsetY != 0f || str.textOffsetX != 0f) {
            renderer.pushScissor(str.at.x, str.at.y, str.size.width, fontSize)
            true
        } else {
            false
        }
        str.render(color)
        if (push) renderer.popScissor()
    }

    operator fun get(index: Int) = str[index]

    override fun reset() = str.text.reset()

    /**
     * @return the [Line] that encapsulates this character, the index of the character relative to the start of the line, and the index of this line
     * @since 0.18.5
     */
    fun getLineByIndex(index: Int) = str.getLineByIndex(index)

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        if (rawResize) {
            this.scaleX = str.rescale(scaleX, scaleY)
        } else {
            val scale = cl1(scaleX, scaleY)
            this.scaleX = str.rescale(scale, scale)
        }
    }

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        if (fs is Unit.Dynamic) fs.set(size?.b ?: throw IllegalArgumentException("${this.simpleName} has a dynamic font size, but it has no height"))
        str = if (initialText.string.contains("\n") || floor((size?.height ?: 0f) / this.fs.px).toInt() > 1 || (size?.width != 0f && size?.height == 0f)) {
            MultilineText(initialText, this.properties.font, this.fs.px, textAlign, renderer, size!!, truncates = size?.height != 0f)
        } else {
            SingleText(initialText, this.properties.font, this.fs.px, textAlign, renderer, size?.width ?: -1f)
        }
        str.text.translator = polyUI.translator
        str.calculate()
        if (size == null || size!!.height == 0f) {
            size = str.size
        }
    }

    override fun calculateBounds() {
        super.calculateBounds()
        str.at.a.px = x
        str.at.b.px = y
    }

    override fun calculateSize() = str.size
}
