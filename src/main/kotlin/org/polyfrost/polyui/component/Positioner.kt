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

package org.polyfrost.polyui.component

import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.makeRelative
import org.polyfrost.polyui.utils.LinkedList
import kotlin.math.abs
import kotlin.math.max

/**
 * Positioning strategies are the methods used in PolyUI to place both components across the screen,
 * and components inside a layout.
 */
fun interface Positioner {
    fun position(drawable: Drawable)

    class Default : Positioner {
        override fun position(drawable: Drawable) {
            val children = drawable.children
            if (drawable.size.hasZero) {
                val out = drawable.calculateSize()
                if (out != null) drawable.size = out
            }
            val needsToCalcSize = drawable.size.hasZero
            if (needsToCalcSize) {
                require(!children.isNullOrEmpty()) { "Drawable $drawable has no size and no children" }
            } else if (children.isNullOrEmpty()) {
                return
            }

            // asm: there are definitely children at this point, so we need to place them
            // we are unsure if there is a size at this point though
            val main = if (drawable.alignment.mode == Align.Mode.Horizontal) 0 else 1
            val crs = abs(main - 1)
            val padding = drawable.alignment.padding

            if (children.size == 1) {
                // asm: fast path: set a square size with the object centered
                val it = children.first()
                it.at = it.at.makeRelative(drawable.at)

                if (it.size.hasZero) position(it)
                it.at.x = padding[main]
                it.at.y = padding[main]
                if (drawable.size.isZero) {
                    drawable.size.x = it.visibleSize.x + padding[main] * 2f
                    drawable.size.y = it.visibleSize.y + padding[main] * 2f
                }
                return
            }
            val willWrap = drawable.visibleSize[main] != 0f
            if (willWrap) {
                val rows = LinkedList<Pair<Pair<Float, Float>, LinkedList<Drawable>>>()
                var maxMain = 0f
                var maxCross = 0f
                var rowMain = 0f
                var rowCross = 0f
                var currentRow = LinkedList<Drawable>()
                children.fastEach {
                    it.at = it.at.makeRelative(drawable.at)
                    if (it.size.isNegative) return@fastEach
                    if (it.size.hasZero) position(it)
                    rowCross = max(rowCross, it.visibleSize[crs])
                    if (rowMain + it.visibleSize[main] > drawable.visibleSize[main]) {
                        rows.add((rowMain to rowCross) to currentRow)
                        currentRow = LinkedList()
                        maxMain = max(maxMain, rowMain)
                        maxCross += rowCross + padding[crs]
                        rowMain = 0f
                        rowCross = 0f
                    }
                    rowMain += it.visibleSize[main] + padding[main]
                    currentRow.add(it)
                }
                if (currentRow.isNotEmpty()) {
                    rows.add((rowMain to rowCross) to currentRow)
                    maxMain = max(maxMain, rowMain)
                    maxCross += rowCross + padding[crs]
                }
                if (needsToCalcSize) {
                    drawable.size[main] = maxMain
                    drawable.size[crs] = maxCross
                }
                rowCross = drawable.at[crs]
                rows.fastEach { (rowData, row) ->
                    val (theRowMain, theRowCross) = rowData
                    align(drawable.alignment.cross, theRowCross, row, rowCross, padding[crs], crs)
                    justify(drawable.alignment.main, theRowMain, row, drawable.at[main], drawable.visibleSize[main], padding[main], main)
                    rowCross += theRowCross + padding[crs]
                }
            } else {
                var rowMain = padding[main]
                var rowCross = 0f
                val pad = padding[crs] * 2f
                children.fastEach {
                    it.at = it.at.makeRelative(drawable.at)
                    if (it.size.isNegative) return@fastEach
                    if (it.size.hasZero) position(it)
                    rowCross = max(rowCross, it.size[crs] + pad)
                    rowMain += it.size[main] + padding[main]
                }
                if (needsToCalcSize) {
                    drawable.size[main] = rowMain
                    drawable.size[crs] = rowCross
                }
                align(drawable.alignment.cross, rowCross, children, 0f, padding[crs], crs)
                justify(drawable.alignment.main, rowMain, children, drawable.at[main], drawable.visibleSize[main], padding[main], main)
            }
        }

        fun align(mode: Align.Cross, rowCross: Float, drawables: LinkedList<Drawable>, min: Float, padding: Float, crs: Int) {
            when (mode) {
                Align.Cross.Start -> {
                    drawables.fastEach {
                        it.at[crs] = min + padding
                    }
                }

                Align.Cross.Center -> {
                    drawables.fastEach {
                        it.at[crs] = min + (rowCross / 2f) - (it.visibleSize[crs] / 2f)
                    }
                }

                Align.Cross.End -> {
                    val max = min + rowCross
                    drawables.fastEach {
                        it.at[crs] = min + (max - it.visibleSize[crs] - padding)
                    }
                }
            }
        }
    }

    fun justify(mode: Align.Main, rowMain: Float, drawables: LinkedList<Drawable>, min: Float, max: Float, padding: Float, main: Int) {
        when (mode) {
            Align.Main.Start -> {
                var current = min + padding
                drawables.fastEach {
                    if(it.at.isNegative) return@fastEach
                    it.at[main] = current
                    current += it.visibleSize[main] + padding
                }
            }

            Align.Main.Center -> {
                var current = min + (max / 2f) - (rowMain / 2f)
                drawables.fastEach {
                    if(it.at.isNegative) return@fastEach
                    it.at[main] = current
                    current += it.visibleSize[main] + padding
                }
            }

            Align.Main.End -> {
                var current = min + (max - padding)
                drawables.fastEach {
                    if(it.at.isNegative) return@fastEach
                    current -= it.visibleSize[main]
                    it.at[main] = current
                    current -= padding
                }
            }

            Align.Main.SpaceBetween -> {
                val gapWidth = max / (drawables.size - 1)
                var current = min + padding
                drawables.fastEach {
                    if(it.at.isNegative) return@fastEach
                    it.at[main] = current
                    current += gapWidth
                }
            }

            Align.Main.SpaceEvenly -> {
                val gapWidth = (max - rowMain) / (drawables.size + 1)
                var current = min + gapWidth
                drawables.fastEach {
                    if(it.at.isNegative) return@fastEach
                    it.at[main] = current
                    current += it.size[main] + gapWidth
                }
            }
        }
    }
}
