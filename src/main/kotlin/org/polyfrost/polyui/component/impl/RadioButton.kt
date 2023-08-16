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
import org.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import org.polyfrost.polyui.color.LightTheme
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.property.impl.BlockProperties
import org.polyfrost.polyui.property.impl.RadioButtonProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.fastEachIndexed
import org.polyfrost.polyui.utils.truncate
import kotlin.math.max

/**
 * # Radiobutton
 *
 * A radiobutton component in PolyUI. Supports an array of generic values as options.
 *
 * @since 0.23.0
 */
class RadioButton<T>(
    properties: RadioButtonProperties? = null,
    at: Vec2<Unit>,
    size: Vec2<Unit>? = null,
    private val toStringFunc: (T) -> String = { it.toString() },
    val onChange: RadioButton<T>.(T) -> kotlin.Unit = {},
    defaultIndex: Int = 0,
    fontSize: Unit = 12f.px,
    values: List<T>
) : ContainingComponent(properties, at, size, false, true, arrayOf()) {
    val block = Block(properties = BlockProperties(paletteGet = { colors.brand.fg }), at = origin, size = origin, acceptInput = false)

    constructor(
        properties: RadioButtonProperties? = null,
        at: Vec2<Unit>,
        size: Vec2<Unit>? = null,
        toStringFunc: (T) -> String = { it.toString() },
        onChange: RadioButton<T>.(T) -> kotlin.Unit = {},
        defaultIndex: Int = 0,
        fontSize: Unit = 12f.px,
        vararg values: T
    ) : this(properties, at, size, toStringFunc, onChange, defaultIndex, fontSize, values.toList())

    init {
        addComponents(block, *values.map { Text(properties = properties?.textProperties, at = origin, initialText = toStringFunc(it).localised(), acceptInput = false, fontSize = fontSize) }.toTypedArray())
        require(values.size >= 2) { "Radiobutton must have at least 2 values" }
        require(defaultIndex in values.indices) { "Default index must be in range 0..${values.size - 1}" }
    }
    override val properties
        get() = super.properties as RadioButtonProperties

    var values = values
        set(value) {
            require(value.size == field.size) { "Resizing radiobutton is not currently supported!" }
            field = value
            children.fastEachIndexed { i, it ->
                (it as? Text)?.text = toStringFunc(value[i]).localised()
            }
        }
    var selectedIndex: Int = defaultIndex
        set(value) {
            if (field == value) return
            if (initStage != INIT_COMPLETE) {
                field = value
                return
            }
            val x = value * (width / values.size) + this.x + properties.edgePadding.px
            block.moveTo(x.px * block.at.b, properties.moveAnimation, properties.moveAnimationDuration)
            if (properties.colors is LightTheme) {
                children[field + 1].color.recolor(properties.textProperties.palette.normal)
                children[value + 1].color.recolor(properties.colors.onBrand.fg.normal)
            }
            field = value
            onChange(this, values[value])
        }

    fun get() = values[selectedIndex]

    fun set(value: T) {
        selectedIndex = values.indexOf(value)
    }

    override fun render() {
        if (properties.outlineThickness != 0f) {
            renderer.hollowRect(x, y, width, height, properties.outlineColor, properties.outlineThickness, properties.cornerRadii)
        }
        renderer.rect(x, y, width, height, color, properties.cornerRadii)
        super.render()
    }

    override fun placeChildren() {
        super.placeChildren()
        block.width = width / values.size - (properties.edgePadding.px * 2f)
        block.height = height - (properties.edgePadding.px * 2f)
        block.cornerRadii = properties.cornerRadii - properties.edgePadding.px
        block.x = x + selectedIndex * (width / values.size) + properties.edgePadding.px
        block.y += properties.edgePadding.px
        val blockSize = width / values.size
        children.fastEachIndexed { i, it ->
            if (it !is Text) return@fastEachIndexed
            if (!autoSized) it.string = it.string.truncate(renderer, it.font, it.fontSize, blockSize - 12f)
            it.x = x + blockSize * (i - 1) + blockSize / 2f - it.width / 2f
            it.y = y + (height - it.height) / 2f
        }
    }

    override fun accept(event: Event): Boolean {
        if (event is MouseClicked) {
            val index = ((event.mouseX - trueX) / (width / values.size)).toInt()
            selectedIndex = index
        }
        if (block.isInside(polyUI.mouseX, polyUI.mouseY)) {
            return block.accept(event)
        }
        return false
    }
    override fun calculateSize(): Size<Unit> {
        var mw = 0f
        var mh = 0f
        children.fastEach {
            if (it !is Text) return@fastEach
            mw = max(mw, it.width)
            mh = max(mh, it.height)
        }
        children.fastEach {
            if (it !is Text) return@fastEach
            it.width = mw
            it.height = mh
        }
        mw += properties.lateralPadding.px * values.size * 2f
        mh += properties.verticalPadding.px * 2f
        return mw.px * mh.px
    }

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        properties.textProperties.colors = properties.colors
    }
}

private operator fun FloatArray.minus(px: Float): FloatArray {
    return FloatArray(size) { this[it] - px }
}
