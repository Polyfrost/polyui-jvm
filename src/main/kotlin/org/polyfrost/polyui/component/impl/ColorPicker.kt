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

package org.polyfrost.polyui.component.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.ShakeOp
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.mutable
import org.polyfrost.polyui.utils.toColor
import org.polyfrost.polyui.utils.toHex
import kotlin.jvm.internal.Ref
import kotlin.math.roundToInt

fun ColorPicker(color: Ref.ObjectRef<PolyColor.Mutable>, faves: MutableList<PolyColor>?, recents: MutableList<PolyColor>?, polyUI: PolyUI?, openNow: Boolean = true, position: Point = Point.At): Block {
    val p = PopupMenu(
        Group(
            Dropdown("polyui.color.solid", "polyui.color.gradient", "polyui.color.chroma", textLength = 152f),
            Image("polyui/close.svg".image()).setDestructivePalette().withStates().onClick {
                this.polyUI.unfocus()
            },
            size = 264f by 32f,
            alignment = Align(pad = Vec2.ZERO, main = Align.Main.SpaceBetween),
        ),
        ColorPickingBox(color).onPress {
            val picker = this[0]
            picker.x = it.x - picker.width / 2f
            picker.y = it.y - picker.height / 2f
            this.polyUI.inputManager.recalculate()
            picker.inputState = INPUT_PRESSED
            picker.accept(it)
        }.onDrag {
            (this.parent[5][0][0] as TextInput).text = color.element.toHex(alpha = false)
        },
        Image(
            PolyImage("polyui/color/hue.png"),
            size = 16f by 200f,
            children = arrayOf(
                Block(size = 12f by 12f, color = PolyColor.TRANSPARENT).draggable(withX = false, onDrag = {
                    y = y.coerceIn(parent.y, parent.y + parent.height - height)
                    color.element.hue = (y - parent.y) / (parent.height - height)
                    (this.parent.parent[5][0][0] as TextInput).text = color.element.toHex(alpha = false)
                }).withBoarder(PolyColor.WHITE, 2f),
            ),
            alignment = Align(pad = 2f by 2f)
        ).padded(2f, 0f).onPress {
            val picker = this[0]
            picker.y = it.y - picker.height / 2f
            this.polyUI.inputManager.recalculate()
            picker.inputState = INPUT_PRESSED
            picker.accept(it)
        },
        Image(
            PolyImage("polyui/color/alpha.png"),
            size = 16f by 200f,
            children = arrayOf(
                Block(size = 12f by 12f, color = PolyColor.TRANSPARENT).draggable(withX = false, onDrag = {
                    y = y.coerceIn(parent.y, parent.y + parent.height - height)
                    color.element.alpha = 1f - (y - parent.y) / (parent.height - height)
                    (this.parent.parent[6][0][0] as TextInput).text = "${(color.element.alpha * 100f).roundToInt()}"
                }).radius(6f).withBoarder(PolyColor.WHITE, 2f),
            ),
            alignment = Align(pad = 2f by 2f)
        ).padded(2f, 0f).onPress {
            val picker = this[0]
            picker.y = it.y - picker.height / 2f
            this.polyUI.inputManager.recalculate()
            picker.inputState = INPUT_PRESSED
            picker.accept(it)
        },
        Dropdown(
            "polyui.color.hex",
            "polyui.color.rgb",
        ),
        BoxedTextInput(
            placeholder = "#FFFFFF",
            initialValue = color.element.toHex(alpha = false),
            center = true,
            size = 78f by 32f,
        ).onChange { text: String ->
            if (text.isEmpty()) return@onChange false
            if (text.startsWith('-')) return@onChange true
            if (text.length > 8) return@onChange true
            if (text == "#") return@onChange false
            try {
                color.element.recolor(text.toColor())
                false
            } catch (e: Exception) {
                ShakeOp(this, 0.2.seconds, 2).add()
                true
            }
        },
        BoxedTextInput(
            placeholder = "100",
            initialValue = "${(color.element.alpha * 100f).toInt()}",
            post = " % ",
            center = true,
            size = 42f by 32f
        ),
        Image("info.svg"),
        size = 288f by 0f,
        align = Align(pad = 12f by 12f),
        polyUI = polyUI,
        openNow = openNow,
        position = position,
    ).namedId("ColorPicker")

    if (openNow) assign(p, color.element)
    else p.afterInit { assign(p, color.element) }
    return p
}

private fun assign(p: Block, col: PolyColor) {
    val box = p[1]
    val boxBlob = box[0]
    val hf = boxBlob.width / 2f
    boxBlob.x = (col.saturation * box.width) + box.x - hf
    boxBlob.y = ((1f - col.brightness) * box.height) + box.y - hf

    val s1 = p[2]
    val s1Blob = s1[0]
    s1Blob.y = col.hue * (s1.height - s1Blob.height) + s1.y

    val s2 = p[3]
    val s2Blob = s2[0]
    s2Blob.y = (1f - col.alpha) * (s2.height - s2Blob.height) + s2.y
}

private class ColorPickingBox(
    val theColor: Ref.ObjectRef<PolyColor.Mutable>,
) : Block(
    Block(size = 12f by 12f, color = PolyColor.TRANSPARENT).draggable(
        onDrag = {
            val hf = width / 2f
            x = x.coerceIn(parent.x - hf, parent.x + parent.width - hf)
            y = y.coerceIn(parent.y - hf, parent.y + parent.height - hf)
            theColor.element.saturation = (x - parent.x + hf) / parent.width
            theColor.element.brightness = 1f - (y - parent.y + hf) / parent.height
            (parent as? Inputtable)?.accept(Event.Mouse.Dragged)
        },
    ).withBoarder(PolyColor.WHITE, 2f),
    size = 200f by 200f,
) {
    init {
        radii = floatArrayOf(6f)
    }
    val grad1 = PolyColor.Gradient.Mutable(
        PolyColor.WHITE.mutable(),
        theColor.element,
        PolyColor.Gradient.Type.LeftToRight,
    )
    val grad2 = PolyColor.Gradient(
        PolyColor.TRANSPARENT,
        PolyColor.BLACK,
        PolyColor.Gradient.Type.TopToBottom,
    )

    override fun render() {
        grad1.color2.recolor(theColor.element)
        val col = theColor.element
        val os = col.saturation
        val ob = col.brightness
        val oa = col.alpha
        col.alpha = 1f
        col.saturation = 1f
        col.brightness = 1f
        renderer.rect(x, y, width, height, grad1, radii!!)
        col.saturation = os
        col.brightness = ob
        col.alpha = oa

        renderer.rect(x, y, width, height, grad2, radii!!)
    }
}
