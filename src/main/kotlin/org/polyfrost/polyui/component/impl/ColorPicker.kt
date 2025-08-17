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
import org.polyfrost.polyui.color.asMutable
import org.polyfrost.polyui.color.toColor
import org.polyfrost.polyui.color.toHex
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.State
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.image

fun ColorPicker(color: State<PolyColor.Mutable>, faves: MutableList<PolyColor>?, recents: MutableList<PolyColor>?, polyUI: PolyUI?, openNow: Boolean = true, position: SpawnPos = SpawnPos.AtMouse, attachedDrawable: Drawable? = null): Block {
    attachedDrawable?.onChange(color, instanceOnly = true) {
        this.color = it
    }
    val p = PopupMenu(
        Group(
            Dropdown("polyui.color.solid", "polyui.color.chroma", size = 200f by 32f).onChange { index: Int ->
                val theColor = color.value
                when (index) {
                    0 -> {
                        if (theColor is PolyColor.Chroma) {
                            color.value = PolyColor.Mutable(theColor.hue, theColor.saturation, theColor.brightness, theColor.alpha)
                        }
                        val it = parent.siblings
                        it[it.lastIndex] = StandardColorTypedOptions(color)
                    }

                    1 -> {
                        if (theColor !is PolyColor.Chroma) {
                            color.value = PolyColor.Chroma(theColor.hue, theColor.saturation, theColor.brightness, theColor.alpha, 2.seconds)
                        }
                        val it = parent.siblings
                        it[it.lastIndex] = ChromaSliderUnit(color)
                    }

                }
            },
            Image("polyui/close.svg".image()).setDestructivePalette().withHoverStates().onClick {
                this.polyUI.inputManager.focus(null)
            }.padded(0f, 1f, 8f, 0f),
            size = 264f by 32f,
            alignment = Align(pad = Vec2.ZERO, main = Align.Content.SpaceBetween),
        ),
        ColorPickingBox(color).onPress {
            val picker = this[0]
            picker.x = it.x - picker.width / 2f
            picker.y = it.y - picker.height / 2f
            this.polyUI.inputManager.recalculate()
            picker.inputState = INPUT_PRESSED
            picker.accept(it)
        },
        Image(
            PolyImage("polyui/color/hue.png"),
            size = 16f by 200f,
            children = arrayOf(
                Block(size = 12f by 12f, color = PolyColor.TRANSPARENT).draggable(withX = false).onDrag {
                    y = y.coerceIn(parent.y, parent.y + parent.height - height)
                    color.value.hue = (y - parent.y) / (parent.height - height)
                    color.notify()
                }.withBorder(PolyColor.WHITE, 2f).onChange(color) {
                    y = (color.value.hue * (parent.height - height)) + parent.y
                },
            ),
            alignment = Align(pad = 2f by 2f)
        ).radius(8f).padded(2f, 0f).onPress {
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
                Block(size = 12f by 12f, color = PolyColor.TRANSPARENT).draggable(withX = false).onDrag {
                    y = y.coerceIn(parent.y, parent.y + parent.height - height)
                    color.value.alpha = 1f - (y - parent.y) / (parent.height - height)
                    color.notify()
                }.radius(6f).withBorder(PolyColor.WHITE, 2f).onChange(color) {
                    y = (1f - color.value.alpha) * (parent.height - height) + parent.y
                },
            ),
            alignment = Align(pad = 2f by 2f)
        ).radius(8f).padded(2f, 0f).onPress {
            val picker = this[0]
            picker.y = it.y - picker.height / 2f
            this.polyUI.inputManager.recalculate()
            picker.inputState = INPUT_PRESSED
            picker.accept(it)
        },
        StandardColorTypedOptions(color),
        size = 288f by 0f,
        align = Align(pad = 12f by 12f, line = Align.Line.Start),
        polyUI = polyUI,
        openNow = openNow,
        spawnPos = position,
    ).namedId("ColorPicker")

    if (openNow) assign(p, color.value)
    else p.afterInit { assign(p, color.value) }
    return p
}

private fun StandardColorTypedOptions(color: State<PolyColor.Mutable>) = Group(
    Dropdown(
        "polyui.color.hex",
        "polyui.color.rgb",
        padding = 10f,
    ).onChange { index: Int ->
        when (index) {
            0 -> parent[1] = HexOptions(color)
            1 -> parent[1] = RGBOptions(color)
        }
    },
    HexOptions(color),
    Image(
        "info.svg".image(),
        size = 18f by 18f,
    ).padded(0f, 7f),
    alignment = Align(padEdges = Vec2.ZERO, padBetween = Vec2(12f, 12f))
).named("StandardColorTypedOptions")

private fun ChromaSliderUnit(color: State<PolyColor.Mutable>) = Group(
    Slider(initialValue = ((color.value as PolyColor.Chroma).speedNanos / 1_000_000_000L).toFloat(), max = 60f)
)


private fun HexOptions(color: State<PolyColor.Mutable>): Group {
    var dodge = false
    return Group(
        BoxedTextInput(
            placeholder = "#FFFFFF",
            initialValue = color.value.toHex(alpha = false),
            center = true,
            size = 78f by 32f,
        ).onChange(color) {
            if (dodge) {
                dodge = false
            } else this.getTextFromBoxedTextInput().text = color.value.toHex(alpha = false)
        }.onChange { text: String ->
            dodge = true
            if (text.isEmpty()) return@onChange false
            if (text.startsWith('-')) return@onChange true
            if (text.length > 8) return@onChange true
            if (text == "#") return@onChange false
            try {
                color.value.recolor(text.toColor(color.value.alpha))
                color.notify()
                false
            } catch (_: Exception) {
                shake(); true
            }
        },
        BoxedNumericInput(
            initialValue = color.value.alpha * 100f,
            post = " % ",
            center = true,
            arrows = false,
            size = Vec2(73f, 32f)
        ).onChange { it: Float ->
            color.value.alpha = it / 100f
            color.notify()
            false
        }.onChange(color) {
            this.getTextFromBoxedTextInput().text = (color.value.alpha * 100f).toInt().toString()
        },
        alignment = Align(padEdges = Vec2.ZERO, padBetween = Vec2(12f, 12f))
    )
}

private fun RGBOptions(color: State<PolyColor.Mutable>): Group {
    return Group(
        DraggingNumericTextInput(
            initialValue = color.value.r.toFloat(),
            pre = "R: ",
            size = Vec2(50f, 32f),
            integral = true,
            max = 255f,
        ).onChange { it: Int ->
            color.value.r = it
            color.notify()
            false
        }.onChange(color) {
            (this[1] as TextInput).text = color.value.r.toString()
        },
        DraggingNumericTextInput(
            initialValue = color.value.g.toFloat(),
            pre = "G: ",
            size = Vec2(50f, 32f),
            integral = true,
            max = 255f,
        ).onChange { it: Int ->
            color.value.g = it
            color.notify()
            false
        }.onChange(color) {
            (this[1] as TextInput).text = color.value.g.toString()
        },
        DraggingNumericTextInput(
            initialValue = color.value.b.toFloat(),
            pre = "B: ",
            size = Vec2(50f, 32f),
            integral = true,
            max = 255f,
        ).onChange { it: Int ->
            color.value.b = it
            color.notify()
            false
        }.onChange(color) {
            (this[1] as TextInput).text = color.value.b.toString()
        },
        alignment = Align(padEdges = Vec2.ZERO, padBetween = Vec2(12f, 12f))
    )
}


//fun ColorPicker2(color: State<PolyColor.Mutable>, polyUI: PolyUI?, openNow: Boolean = true, position: SpawnPos = SpawnPos.AtMouse): Block {
//    Block(size = 288f by 0f, focusable = true, alignment = Align(pad = Vec2(12f, 12f))).children {
//
//    }
//

//
//}

private fun assign(p: Block, col: PolyColor.Mutable) {
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
    val theColor: State<PolyColor.Mutable>,
) : Block(
    Block(size = 12f by 12f, color = PolyColor.TRANSPARENT).draggable().onChange(theColor) {
        val hf = width / 2f
        x = (theColor.value.saturation * parent.width) + parent.x - hf
        y = ((1f - theColor.value.brightness) * parent.height) + parent.y - hf
    }.onDrag {
        val hf = width / 2f
        x = x.coerceIn(parent.x - hf, parent.x + parent.width - hf)
        y = y.coerceIn(parent.y - hf, parent.y + parent.height - hf)
        theColor.value.saturation = (x - parent.x + hf) / parent.width
        theColor.value.brightness = 1f - (y - parent.y + hf) / parent.height
        (parent as? Inputtable)?.accept(Event.Mouse.Drag)
        theColor.notify()
        Unit
    }.withBorder(PolyColor.WHITE, 2f),
    size = 200f by 200f,
) {
    val grad1 = PolyColor.Gradient.Mutable(
        PolyColor.WHITE.asMutable(),
        theColor.value,
        PolyColor.Gradient.Type.LeftToRight,
    )
    val grad2 = PolyColor.Gradient(
        PolyColor.TRANSPARENT,
        PolyColor.BLACK,
        PolyColor.Gradient.Type.TopToBottom,
    )

    override fun render() {
        if (theColor.value is PolyColor.Chroma) theColor.notify()
        grad1.color2.recolor(theColor.value)
        val col = theColor.value
        val os = col.saturation
        val ob = col.brightness
        val oa = col.alpha
        col.alpha = 1f
        col.saturation = 1f
        col.brightness = 1f
        renderer.rect(x, y, width, height, grad1, 6f)
        col.saturation = os
        col.brightness = ob
        col.alpha = oa

        renderer.rect(x, y, width, height, grad2, 6f)
    }
}
