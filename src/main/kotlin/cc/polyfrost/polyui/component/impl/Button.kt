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

package cc.polyfrost.polyui.component.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.ContainingComponent
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.input.PolyText
import cc.polyfrost.polyui.property.impl.ButtonProperties
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import kotlin.math.max

/**
 * A button, that supports two images and a text object.
 * @param properties The properties of the button (inherits from [Block]). There are various padding properties here you may want to tweak.
 * @param maxHeight the maximum height for this icon. You can specify this if you want to have more control over the height of this component, as if it is `null` it is controlled by the largest image or text.
 * @param leftIcon The image to be displayed on the left side of the button
 * @param text The text to be displayed in the button
 * @param fontSize The font size of the text
 *
 * @since 0.17.3
 */
class Button(
    properties: ButtonProperties? = null,
    at: Vec2<Unit>,
    private var maxHeight: Unit? = null,
    leftIcon: PolyImage? = null,
    text: PolyText? = null,
    fontSize: Unit.Pixel = 12.px,
    rightIcon: PolyImage? = null,
    acceptsInput: Boolean = true,
    vararg events: Events.Handler
) : ContainingComponent(properties, at, null, false, acceptsInput, arrayOf(), *events) {
    override val properties get() = super.properties as ButtonProperties
    val leftImage: Image? = if (leftIcon != null) Image(image = leftIcon, at = origin, acceptInput = false) else null
    val rightImage: Image? = if (rightIcon != null) Image(image = rightIcon, at = origin, acceptInput = false) else null
    val text: Text? =
        if (text != null) Text(initialText = text, fontSize = fontSize, at = origin, acceptInput = false) else null

    init {
        if (text == null) {
            if (leftIcon != null && rightIcon != null) {
                throw IllegalArgumentException("${this.simpleName} cannot have both left and right icons and no text!")
            } else if (leftIcon == null && rightIcon == null) throw IllegalArgumentException("${this.simpleName} cannot have no text or icons!")
        }
        addComponents(leftImage, rightImage, this.text)
    }

    override fun render() {
        if (properties.outlineThickness != 0f) {
            renderer.hollowRect(x, y, width, height, color, properties.outlineThickness, properties.cornerRadii)
        }
        renderer.rect(x, y, width, height, color, properties.cornerRadii)
        super.render()
    }

    override fun calculateSize(): Size<Unit> {
        (maxHeight as? Unit.Dynamic)?.set(layout.size?.b ?: throw IllegalArgumentException("${this.simpleName} cannot use maxHeight as dynamic unit when it has no parent layout!"))
        text?.size = text?.calculateSize()
        leftImage?.size = leftImage?.calculateSize()
        rightImage?.size = rightImage?.calculateSize()

        if (maxHeight != null) {
            maxHeight!!.px -= properties.topEdgePadding * 2f
        } else {
            maxHeight = getLargestComponent(text, leftImage, rightImage).also {
                if (text != null) {
                    if (text.height != it) {
                        text.height = it
                        text.str.fontSize = it
                    }
                }
            }.px
        }
        img(leftImage)
        img(rightImage)
        var contentWidth: Float = properties.edgePadding * 2f
        contentWidth += leftImage?.size?.a?.px ?: 0f
        contentWidth += text?.size?.a?.px ?: 0f
        contentWidth += rightImage?.size?.a?.px ?: 0f
        if (text != null) {
            if (leftImage != null) contentWidth += properties.iconTextSpacing
            if (rightImage != null) contentWidth += properties.iconTextSpacing
        }
        rightImage?.at?.a?.px = x - properties.edgePadding + contentWidth - rightImage!!.width
        return Size(contentWidth.px, (maxHeight!!.px + properties.topEdgePadding * 2f).px)
    }

    override fun placeChildren() {
        super.placeChildren()
        if (leftImage != null) {
            leftImage.x += properties.edgePadding
            leftImage.y += properties.topEdgePadding
            if (text != null) text.x += leftImage.width + properties.iconTextSpacing
        }
        if (rightImage != null) {
            rightImage.y += properties.topEdgePadding
        }
        if (text != null) {
            text.x += properties.edgePadding
            text.y += height / 2f - text.height / 2f
        }
    }

    private fun getLargestComponent(vararg cs: Component?): Float {
        var largest = 0f
        cs.forEach { if (it == null) return@forEach else largest = max(largest, it.height) }
        return largest
    }

    /**
     * do size, and y position of this image
     */
    private fun img(icon: Image?) {
        if (icon == null) return
        val ratio = icon.height / (maxHeight?.px ?: return)
        if (ratio != 1f) PolyUI.LOGGER.warn("${this.simpleName} icon heights are not the same! The icon has been resized, but it may look blurry or distorted!")
        icon.height = maxHeight!!.px
        icon.width /= ratio
    }
}
