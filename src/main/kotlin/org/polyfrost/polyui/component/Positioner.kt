/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
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

import org.polyfrost.polyui.component.Positioner.Default.Aligner
import org.polyfrost.polyui.component.Positioner.Default.Justifier
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.utils.fastEach
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
            val polyUI = drawable.polyUI
            if (!drawable.sizeValid) {
                val out = drawable.calculateSize()
                if (out != null) {
                    drawable.width = out.x
                    drawable.height = out.y
                }
            }
            val needsToCalcSize = !drawable.sizeValid
            if (needsToCalcSize) {
                require(!children.isNullOrEmpty()) { "Drawable $drawable has no size and no children\nBacktrace: ${polyUI.debugger.debugString()}" }
            } else if (children.isNullOrEmpty()) {
                fixVisibleSize(drawable)
                return
            }

            // asm: there are definitely children at this point, so we need to place them
            // we are unsure if there is a size at this point though
            val align = drawable.alignment
            val main = if (align.mode == Align.Mode.Horizontal) 0 else 1
            val crs = if (main == 0) 1 else 0
            val padding = align.padding
            val vs = drawable.visibleSize
            val totalSm = polyUI.size[main] / polyUI.iSize[main]
            val totalSc = polyUI.size[crs] / polyUI.iSize[crs]
            val mainPad = padding[main] * totalSm
            val crossPad = padding[crs] * totalSc

            if (children.size == 1) {
                // asm: fast path: set a square size with the object centered
                val it = children.first()
                val ivs = it.visibleSize
                if (!it.sizeValid) position(it)
                if (needsToCalcSize) {
                    drawable.size.x = ivs.x + mainPad * 2f
                    drawable.size.y = ivs.y + crossPad * 2f
                }
                fixVisibleSize(drawable)
                it.at(
                    main,
                    drawable.x + when (align.main) {
                        Align.Main.Start -> mainPad
                        Align.Main.End -> vs[main] - ivs[main] - mainPad
                        else -> (vs[main] - ivs[main]) / 2f
                    },
                )
                it.at(
                    crs,
                    drawable.y + when (align.cross) {
                        Align.Cross.Start -> crossPad
                        Align.Cross.End -> vs[crs] - ivs[crs] - crossPad
                        else -> (vs[crs] - ivs[crs]) / 2f
                    },
                )
                return
            }
            val willWrap = align.maxRowSize != 0 && (vs[main] != 0f || align.maxRowSize != AlignDefault.maxRowSize)
            if (willWrap) {
                val rows = ArrayList<Pair<Pair<Float, Float>, ArrayList<Drawable>>>()
                val maxRowSize = align.maxRowSize
                require(maxRowSize > 0) { "Drawable $drawable has max row size of $maxRowSize, needs to be greater than 0" }
                var maxMain = 0f
                var maxCross = crossPad
                var rowMain = mainPad
                var rowCross = 0f
                var currentRow = ArrayList<Drawable>()
                children.fastEach {
                    if (!it.enabled) return@fastEach
                    if (!it.sizeValid) position(it)
                    val ivs = it.visibleSize
                    if (currentRow.isNotEmpty() && (rowMain + ivs[main] + mainPad > vs[main] || currentRow.size == maxRowSize)) {
                        rows.add((rowMain to rowCross) to currentRow)
                        currentRow = ArrayList(maxRowSize.coerceAtMost(10))
                        maxMain = max(maxMain, rowMain)
                        maxCross += rowCross + crossPad
                        rowMain = mainPad
                        rowCross = 0f
                    }
                    rowMain += ivs[main] + mainPad
                    rowCross = max(rowCross, ivs[crs])
                    currentRow.add(it)
                }
                if (currentRow.isNotEmpty()) {
                    rows.add((rowMain to rowCross) to currentRow)
                    maxMain = max(maxMain, rowMain)
                    maxCross += rowCross + crossPad
                }
                if (needsToCalcSize) {
                    drawable.size[main] = maxMain
                    drawable.size[crs] = maxCross
                }
                fixVisibleSize(drawable)
                rowCross = drawable.at(crs)
                if (rows.size == 1) {
                    // asm: in this situation, the user specified a size, and as there is only 1 row, so we should
                    // make it so the actual cross limit is the size of the drawable
                    val (rowData, row) = rows[0]
                    place(
                        align, row,
                        vs[crs], drawable.at(crs), crossPad, crs,
                        rowData.first, drawable.at(main), vs[main], mainPad, main,
                    )
                } else {
                    rows.fastEach { (rowData, row) ->
                        val (theRowMain, theRowCross) = rowData
                        place(
                            align, row,
                            theRowCross, rowCross, crossPad, crs,
                            theRowMain, drawable.at(main), vs[main], mainPad, main,
                        )
                        rowCross += theRowCross + crossPad
                    }
                }
            } else {
                var rowMain = mainPad
                var rowCross = 0f
                val pad = crossPad * 2f
                children.fastEach {
                    if (!it.renders) return@fastEach
                    if (!it.sizeValid) position(it)
                    if (it.atValid) {
                        rowCross = max(rowCross, it.at(crs) + it.visibleSize[crs] + pad)
                        rowMain = max(rowMain, it.at(main) + it.visibleSize[main] + mainPad)
                    } else {
                        rowCross = max(rowCross, it.size[crs] + pad)
                        rowMain += it.size[main] + mainPad
                    }
                }
                if (needsToCalcSize) {
                    drawable.size[main] = rowMain
                    drawable.size[crs] = rowCross
                }
                fixVisibleSize(drawable)
                place(
                    align, children,
                    rowCross, drawable.at(crs), crossPad, crs,
                    rowMain, drawable.at(main), drawable.size[main], mainPad, main,
                )
            }
        }

        private fun place(
            align: Align, row: ArrayList<Drawable>,
            rowCross: Float, minCross: Float, padCross: Float, crs: Int,
            rowMain: Float, minMain: Float, maxMain: Float, padMain: Float, main: Int,
        ) {
            val aligner = when (align.cross) {
                Align.Cross.Start -> cStart
                Align.Cross.Center -> cCenter
                Align.Cross.End -> cEnd
            }
            val justifier = when (align.main) {
                Align.Main.Start, Align.Main.Center -> mProgressive
                Align.Main.End -> mBackwards
                Align.Main.SpaceBetween, Align.Main.SpaceEvenly -> mSpace
            }
            val gap = when (align.main) {
                Align.Main.SpaceBetween -> (maxMain - rowMain) / (row.size - 1)
                Align.Main.SpaceEvenly -> (maxMain - rowMain) / (row.size + 1)
                else -> 0f
            }
            var current = minMain + when (align.main) {
                Align.Main.Start, Align.Main.End, Align.Main.SpaceBetween -> padMain
                Align.Main.Center -> (maxMain / 2f) - (rowMain / 2f) + padMain
                Align.Main.SpaceEvenly -> padMain + gap
            }
            row.fastEach {
                if (it.atValid) return@fastEach
                aligner.align(rowCross, it, minCross, padCross, crs)
                current = justifier.justify(current, it, padMain, main, gap)
            }
        }

        private fun interface Aligner {
            fun align(rowCross: Float, it: Drawable, min: Float, padding: Float, crs: Int)
        }

        private fun interface Justifier {
            fun justify(current: Float, it: Drawable, padding: Float, main: Int, gap: Float): Float
        }

        private val cStart = Aligner { _, it, min, padding, crs ->
            it.at(crs, min + padding)
        }

        private val cCenter = Aligner { rowCross, it, min, _, crs ->
            it.at(crs, min + (rowCross / 2f) - (it.visibleSize[crs] / 2f))
        }

        private val cEnd = Aligner { rowCross, it, min, padding, crs ->
            it.at(crs, min + rowCross - it.visibleSize[crs] - padding)
        }

        private val mProgressive = Justifier { current, it, padding, main, _ ->
            it.at(main, current)
            return@Justifier current + it.visibleSize[main] + padding
        }

        private val mBackwards = Justifier { current, it, padding, main, _ ->
            val cur = current - it.visibleSize[main] - padding
            it.at(main, cur)
            return@Justifier cur
        }

        private val mSpace = Justifier { current, it, padding, main, gap ->
            it.at(main, current)
            return@Justifier current + it.visibleSize[main] + padding + gap
        }

        private fun fixVisibleSize(drawable: Drawable): Drawable {
            if (!drawable.hasVisibleSize) return drawable
            val vs = drawable.visibleSize
            if (vs.x > drawable.size.x) vs.x = drawable.size.x
            if (vs.y > drawable.size.y) vs.y = drawable.size.y
            return drawable
        }
    }
}
