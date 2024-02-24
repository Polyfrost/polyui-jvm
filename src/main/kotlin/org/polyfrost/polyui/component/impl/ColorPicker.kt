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
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.*
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Point
import org.polyfrost.polyui.unit.by
import org.polyfrost.polyui.utils.ensureSize
import org.polyfrost.polyui.utils.image
import org.polyfrost.polyui.utils.mapToArray
import org.polyfrost.polyui.utils.radii
import kotlin.jvm.internal.Ref

fun ColorPicker(color: Ref.ObjectRef<PolyColor.Animated>, faves: MutableList<PolyColor>, recents: MutableList<PolyColor>, polyUI: PolyUI?, openNow: Boolean = true, position: Point = Point.At): Block {
    faves.ensureSize(6) {
        PolyColor.TRANSPARENT
    }
    recents.ensureSize(6) {
        PolyColor.TRANSPARENT
    }
    val p = PopupMenu(
        Group(
            Dropdown("polyui.color.solid", "polyui.color.gradient", "polyui.color.chroma"),
            Image("close.svg".image()).setDestructivePalette().withStates().onClick {
                this.polyUI.unfocus()
            },
            size = 280f by 40f,
            alignment = Align(main = Align.Main.SpaceBetween),
        ),
        Group(
            ColorPickingBox(color),
            Image(
                PolyImage("hue.png"),
                radii = 8f.radii(),
                children = arrayOf(
                    Block(at = 3f by 0f, size = 10f by 10f, radii = 5f.radii(), color = PolyColor.TRANSPARENT).draggable(withX = false, onDrag = {
                        val parent = parent!!
                        y = y.coerceIn(parent.y, parent.y + parent.height - height)
                        color.element.hue = (y - parent.y) / (parent.height - height)
                    }).withBoarder(PolyColor.WHITE, 2f),
                ),
            ),
            Image(
                PolyImage("alpha.png"),
                size = 16f by 200f,
                radii = 8f.radii(),
                children = arrayOf(
                    Block(at = 3f by 0f, size = 10f by 10f, radii = 5f.radii(), color = PolyColor.TRANSPARENT).draggable(withX = false, onDrag = {
                        val parent = parent!!
                        y = y.coerceIn(parent.y, parent.y + parent.height - height)
                        color.element.alpha = 1f - (y - parent.y) / (parent.height - height)
                    }).withBoarder(PolyColor.WHITE, 2f),
                ),
            ),
            alignment = Align(padding = 16f by 8f),
        ),
        Block(
            Text("polyui.color.hex"),
            size = 48f by 32f,
        ),
        BoxedTextInput(
            placeholder = "#FFFFFF",
            initialValue = PolyColor.hexOf(color.element, alpha = false),
            size = 82f by 32f,
        ),
        BoxedTextInput(
            placeholder = "100%",
            initialValue = "${(color.element.alpha * 100f).toInt()}%",
            size = 72f by 32f,
        ),
        Block(Image("info.svg".image()), size = 48f by 32f),
        Block(Image("info.svg".image()), size = 32f by 32f),
        *faves.mapToArray { Block(size = 32f by 32f, color = it).withBoarder() },
        Block(Image("info.svg".image()), size = 32f by 32f),
        *recents.mapToArray { Block(size = 32f by 32f, color = it).withBoarder() },
        size = 296f by 0f,
        align = Align(padding = 8f by 8f),
        polyUI = polyUI,
        openNow = openNow,
        position = position,
    ).namedId("ColorPicker")
    if (openNow) assign(p, color.element)
    else p.afterInit { assign(p, color.element) }
    return p
}

private fun assign(p: Block, col: PolyColor) {
    val box = p[1][0]
    val boxBlob = box[0]
    val hf = boxBlob.width / 2f
    boxBlob.x = (col.saturation * box.width) + box.x - hf
    boxBlob.y = (col.brightness * box.height) + box.y - hf

    val s1 = p[1][1]
    val s1Blob = s1[0]
    s1Blob.y = col.hue * (s1.height - s1Blob.height) + s1.y

    val s2 = p[1][2]
    val s2Blob = s2[0]
    s2Blob.y = (1f - col.alpha) * (s2.height - s2Blob.height) + s2.y
}

private class ColorPickingBox(
    val theColor: Ref.ObjectRef<PolyColor.Animated>,
) : Block(
    Block(size = 10f by 10f, radii = 5f.radii(), color = PolyColor.TRANSPARENT).draggable(
        onDrag = {
            val parent = parent ?: return@draggable
            val hf = width / 2f
            x = x.coerceIn(parent.x - hf, parent.x + parent.width - hf)
            y = y.coerceIn(parent.y - hf, parent.y + parent.height - hf)
            theColor.element.saturation = (x - parent.x + hf) / parent.width
            theColor.element.brightness = (y - parent.y + hf) / parent.height
        },
    ).withBoarder(PolyColor.WHITE, 2f),
    size = 200f by 200f,
) {
    val grad1 = PolyColor.Gradient(
        PolyColor.WHITE,
        theColor.element,
        PolyColor.Gradient.Type.LeftToRight,
    )
    val grad2 = PolyColor.Gradient(
        PolyColor.TRANSPARENT,
        PolyColor.BLACK,
        PolyColor.Gradient.Type.TopToBottom,
    )

    override fun render() {
        grad1.color2 = theColor.element
        val col = theColor.element
        val os = col.saturation
        val ob = col.brightness
        val oa = col.alpha
        col.alpha = 1f
        col.saturation = 1f
        col.brightness = 1f
        renderer.rect(x, y, width, height, grad1, radii)
        col.saturation = os
        col.brightness = ob
        col.alpha = oa

        renderer.rect(x, y, width, height, grad2, radii)
    }
}
