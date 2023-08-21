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
import org.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.component.DrawableOp
import org.polyfrost.polyui.component.Focusable
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.input.PolyText
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout.Companion.drawables
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.property.impl.DropdownProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.maxOf
import kotlin.math.max

class Dropdown(
    properties: DropdownProperties? = null,
    at: Point<Unit>,
    size: Size<Unit>? = null,
    heightBeforeScrolls: Unit.Pixel = 300.px,
    val default: Int = 0,
    vararg entries: Entry,
) : ContainingComponent(properties, at, size, false, true, arrayOf()), Focusable {
    private lateinit var borderColor: Color.Animated
    private val chevron = Image(image = PolyImage("/chevron-down.svg", 16f, 16f), at = origin)
    private var ycache = 0f

    init {
        addComponents(chevron)
        entries.forEach {
            it.dropdown = this
            if (size != null) it.size = size.clone()
        }
    }

    override fun accept(event: FocusedEvent): Boolean {
        if (event is FocusedEvent.Gained) {
            active = true
            open()
        }
        if (event is FocusedEvent.Lost) {
            active = false
            close()
        }
        return true
    }

    override val properties: DropdownProperties
        get() = super.properties as DropdownProperties
    var selected: Entry? = null
        private set(value) {
            if (field === value) return
            removeComponents(field)
            value?.clone()?.let {
                field = it
                it.show = true
                it.acceptsInput = false
                addComponents(it)
            }
        }

    val dropdown = FlexLayout(
        at.clone(),
        size = size?.clone()?.also {
            it.b.px = 0f
        },
        gap = Gap(0f.px, 0f.px),
        drawables = drawables(
            *entries,
        ),
    ).scrolling(0.px * heightBeforeScrolls)

    init {
        dropdown.refuseFramebuffer = true
        dropdown.addOperation(object : DrawableOp.Persistent(this) {
            override fun apply(renderer: Renderer) {
                if (openAnimation != null) {
                    renderer.pushScissor(0f, 0f, dropdown.width, dropdown.height * openAnimation!!.value)
                }
                renderer.rect(0f, 0f, dropdown.width, dropdown.height, color, 0f, 0f, this@Dropdown.properties.cornerRadii[2], this@Dropdown.properties.cornerRadii[3])
            }

            override fun unapply(renderer: Renderer) {
                renderer.popScissor()
            }
        })
        dropdown.simpleName = "Dropdown@${Integer.toHexString(this.hashCode())}"
    }

    var active = false
        private set
    private var openAnimation: Animation? = null

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        polyUI.master.add(dropdown)
        borderColor = properties.borderColor.toAnimatable()
    }

    override fun accept(event: Event): Boolean {
        if (event is MouseEntered) {
            polyUI.cursor = Cursor.Clicker
            return true
        }
        if (event is MouseExited) {
            polyUI.cursor = Cursor.Pointer
            return true
        }
        if (event is MouseClicked) {
            if (event.button == 0 && active) {
                polyUI.unfocus()
                return true
            }
        }
        // k2 why?
        return super<ContainingComponent>.accept(event)
    }

    fun close() {
        openAnimation = properties.openAnimation.create(properties.openDuration, openAnimation?.value ?: 1f, 0f)
        chevron.rotateTo(0.0, properties.openAnimation, properties.openDuration)
        color.recolor(properties.palette.normal)
        borderColor.recolor(properties.borderColor)
    }

    fun open() {
        ycache = layout.y
        openAnimation = properties.openAnimation.create(properties.openDuration, openAnimation?.value ?: 0f, 1f)
        chevron.rotateTo(180.0, properties.openAnimation, properties.openDuration)
        color.recolor(properties.activeColor)
        borderColor.recolor(properties.activeBorderColor)
        // asm: assert on top
        if (polyUI.master.children.indexOf(this.getContainingLayout()) > polyUI.master.children.indexOf(dropdown)) {
            if (polyUI.settings.debug) PolyUI.LOGGER.warn("Promoting dropdown to top of stack")
            polyUI.master.removeNow(dropdown)
            polyUI.master.add(dropdown)
        }
        dropdown.exists = true
        dropdown.moveTo((trueX + properties.borderThickness).px * (trueY + height + properties.borderThickness).px)
        dropdown.calculateBounds()
    }

    override fun render() {
        if (openAnimation != null) {
            if (openAnimation!!.isFinished && openAnimation!!.value == 0f) {
                openAnimation = null
            } else {
                openAnimation!!.update(polyUI.delta)
            }
        }
        dropdown.exists = (openAnimation?.value ?: 0f) != 0f
        if (active) {
            // asm: close dropdown when scrolled
            if (ycache != layout.y) {
                polyUI.unfocus()
            }
        }
        renderer.rect(0f, 0f, width, height, color, properties.cornerRadii)
        renderer.hollowRect(0f, 0f, width, height, borderColor, properties.borderThickness, properties.cornerRadii)
        super.render()
    }

    override fun calculateSize(): Size<Unit> {
        val largest = dropdown.components.maxOf { (it as? Entry)?.calculateSize() }
        dropdown.components.fastEach {
            if (it is Entry) {
                it.size = largest.clone()
            }
        }
        largest.a.px = max(largest.a.px, properties.minWidth)
        largest.b.px += properties.verticalPadding * 2f
        return largest
    }

    override fun calculateBounds() {
        if (dropdown.initStage == INIT_NOT_STARTED) {
            dropdown.setup(renderer, polyUI)
        }
        dropdown.calculateBounds()
        chevron.calculateBounds()
        super.calculateBounds()
        chevron.x = width - chevron.width - 12f
        chevron.y = height / 2f - chevron.height / 2f
    }

    override fun onInitComplete() {
        default()
        dropdown.visibleSize = dropdown.size!!.clone()
    }

    fun default() {
        selected = dropdown.components[default] as Entry
    }

    @Suppress("EqualsOrHashCode")
    class Entry @JvmOverloads constructor(private val txt: PolyText, private val icon: PolyImage? = null, private val iconSide: Side = Side.Right, properties: DropdownProperties.Entry? = null, private val onSelected: (() -> kotlin.Unit)? = null) : ContainingComponent(properties, flex(), null, false, true, arrayOf()) {
        @JvmOverloads
        constructor(text: String, icon: PolyImage? = null, iconSide: Side = Side.Right, properties: DropdownProperties.Entry? = null, onSelected: (() -> kotlin.Unit)? = null) : this(text.localised(), icon, iconSide, properties, onSelected)

        override val properties
            get() = super.properties as DropdownProperties.Entry

        val text = Text(text = txt, at = origin, fontSize = 12.px)
        val image: Image? = if (icon != null) Image(image = icon, at = origin) else null
        lateinit var dropdown: Dropdown
        var show = false

        init {
            addComponents(text, image)
        }

        override fun onInitComplete() {
            recolorAll(properties.contentColor.normal)
            super.onInitComplete()
        }

        override fun accept(event: Event): Boolean {
            if (event is MouseExited) {
                recolorAll(properties.contentColor.normal, properties.hoverAnimation, properties.hoverAnimationDuration)
                return true
            }
            if (event is MouseEntered) {
                recolorAll(properties.contentColor.hovered, properties.hoverAnimation, properties.hoverAnimationDuration)
                return true
            }
            if (event is MousePressed) {
                return true
            }
            if (event is MouseReleased) {
                return true
            }
            if (event is MouseClicked) {
                if (event.button != 0) return false
                if (!show) {
                    dropdown.active = false
                    dropdown.close()
                    dropdown.selected = this
                    onSelected?.invoke()
                    return true
                }
            }
            return super.accept(event)
        }

        override fun calculateBounds() {
            super.calculateBounds()
            if (image != null) {
                if (iconSide == Side.Right) {
                    image.x = width - image.width - properties.lateralPadding
                    text.x = properties.lateralPadding
                } else {
                    image.x = properties.lateralPadding
                    text.x = image.width + properties.lateralPadding
                }
                image.y = (height - image.height) / 2f
            } else {
                text.x = properties.lateralPadding
            }
            text.y = (height - text.height) / 2f
        }

        override fun calculateSize(): Size<Unit> {
            val width = text.width + properties.lateralPadding * 2f + if (image != null) image.width + properties.lateralPadding else 0f
            val height = text.height + properties.verticalPadding * 2f
            return Size(max(width, dropdown.properties.minWidth).px, height.px)
        }

        override fun clone() = Entry(txt, image?.image, iconSide, properties, onSelected).also {
            it.size = size!!.clone()
            it.dropdown = this.dropdown
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entry) return false

            if (txt != other.txt) return false
            if (icon !== other.icon) return false
            return iconSide == other.iconSide
        }
    }

    companion object {
        /**
         * Constructs a dropdown from an enum class. This method will look at the first field in the enum entries, and if it is a String it will extract that; and use that as the name.
         *
         * Otherwise, it will use the name of the enum entry as it is declared in the source code.
         *
         * @throws IllegalArgumentException if the class is not an enum
         * @since 0.19.0
         */
        @JvmStatic
        fun from(enumClass: Class<*>): Array<out Entry> {
            require(enumClass.isEnum) { "class must be an enum to create a dropdown" }
            return enumClass.enumConstants.map {
                it as Enum<*>
                Entry(it::class.java.fields[0].get(it) as? String ?: it.name)
            }.toTypedArray()
        }

        /**
         * Constructs a dropdown from an array of values. This method will call [Any.toString] on each value to get the name.
         * @since 0.19.0
         */
        @JvmStatic
        fun from(values: Array<out Any>): Array<out Entry> = values.map { Entry(it.toString()) }.toTypedArray()
    }
}
