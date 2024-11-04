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

package org.polyfrost.polyui.layout

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Scrollable
import org.polyfrost.polyui.layout.FlexLayoutController.Aligner
import org.polyfrost.polyui.layout.FlexLayoutController.Justifier
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.utils.fastEach
import kotlin.math.max

object FlexLayoutController : LayoutController {
    override fun layout(component: Component) {
        val children = component.children
        val polyUI = component.polyUI
        if (!component.sizeValid) {
            val out = component.calculateSize()
            if (out.isPositive) {
                component.width = out.x
                component.height = out.y
            }
        }

        if (!component.sizeValid) {
            // hope they know what they are doing!
            if (component.layoutIgnored) return
            if(children.isNullOrEmpty()) {
                PolyUI.LOGGER.error("failed to initialize $component: was skipped as it has no size and no children")
                return
            }
        } else if (children.isNullOrEmpty()) {
            component.fixVisibleSize()
            return
        }

        // asm: there are definitely children at this point, so we need to place them
        // we are unsure if there is a size at this point though
        val align = component.alignment
        val main = if (align.mode == Align.Mode.Horizontal) 0 else 1
        val crs = if (main == 0) 1 else 0
        val padding = align.pad
        val totalSx = polyUI.size.x / polyUI.iSize.x
        val totalSy = polyUI.size.y / polyUI.iSize.y
        val mainPad = padding[main] * totalSx
        val crossPad = padding[crs] * totalSy
        if (!component.positioned) {
            component.rescale0(totalSx, totalSy, false)
        }

        if (children.size == 1) {
            // asm: fast path: set a square size with the object centered
            val it = children.first()
            val ivs = it.visibleSize
            val ipad = it.padding
            if (!it.sizeValid) layout(it)
            assignAndCheckSize(
                component, main, crs,
                ivs[main] + ipad[main] + ipad[main + 2] + (mainPad * 2f),
                ivs[crs] + ipad[crs] + ipad[crs + 2] + (crossPad * 2f)
            )
            val vs = component.visibleSize
            it.at(
                main,
                component.at[main] + when (align.main) {
                    Align.Main.Start -> mainPad + ipad[main]
                    Align.Main.End -> vs[main] - ivs[main] - mainPad - ipad[main]
                    else -> (vs[main] - ivs[main]) / 2f
                },
            )
            it.at(
                crs,
                component.at[crs] + when (align.cross) {
                    Align.Cross.Start -> crossPad + ipad[crs]
                    Align.Cross.End -> vs[crs] - ivs[crs] - crossPad - ipad[crs]
                    else -> (vs[crs] - ivs[crs]) / 2f
                },
            )
            if (it is Scrollable) it.resetScroll()
            return
        }
        val willWrap = align.maxRowSize != 0 && (component.visibleSize[main] != 0f || align.maxRowSize != AlignDefault.maxRowSize)
        if (willWrap) {
            val rows = ArrayList<WrappingRow>()
            val maxRowSize = align.maxRowSize
            val wrapCap = component.visibleSize[main]
            require(maxRowSize > 0) { "Component $component has max row size of $maxRowSize, needs to be greater than 0" }
            var maxMain = 0f
            var maxCross = crossPad
            var rowMain = mainPad
            var rowCross = 0f
            var currentRow = ArrayList<Component>()
            // measure and create rows
            children.fastEach {
                if (it.layoutIgnored) return@fastEach
                if (!it.sizeValid) layout(it)
                val ivs = it.visibleSize
                val ipad = it.padding
                val mainS = ipad[main] + ipad[main + 2] + ivs[main] + mainPad
                if (currentRow.isNotEmpty() && (rowMain + mainS > wrapCap || currentRow.size == maxRowSize)) {
                    rows.add(WrappingRow(rowMain, rowCross, currentRow))
                    currentRow = ArrayList(maxRowSize.coerceAtMost(10))
                    maxMain = max(maxMain, rowMain)
                    maxCross += rowCross + crossPad
                    rowMain = mainPad
                    rowCross = 0f
                }
                rowMain += mainS
                rowCross = max(rowCross, ipad[crs] + ipad[crs + 2] + ivs[crs])
                currentRow.add(it)
            }
            if (currentRow.isNotEmpty()) {
                rows.add(WrappingRow(rowMain, rowCross, currentRow))
                maxMain = max(maxMain, rowMain)
                maxCross += rowCross + crossPad
            }
            assignAndCheckSize(component, main, crs, maxMain, maxCross)
            val vs = component.visibleSize
            rowCross = component.at[crs]
            if (rows.size == 1) {
                // asm: in this situation, the user specified a size, and as there is only 1 row, so we should
                // make it so the actual cross limit is the size of the component
                val (theRowMain, _, row) = rows[0]
                place(
                    align, row,
                    vs[crs], rowCross, crossPad, crs,
                    theRowMain, component.at[main], vs[main], mainPad, main,
                )
            } else {
                rows.fastEach { (theRowMain, theRowCross, row) ->
                    place(
                        align, row,
                        theRowCross, rowCross, crossPad, crs,
                        theRowMain, component.at[main], vs[main], mainPad, main,
                    )
                    rowCross += theRowCross + crossPad
                }
            }
        } else {
            var rowMain = mainPad
            var rowCross = 0f
            val cpad2 = crossPad * 2f
            children.fastEach {
                if (it.layoutIgnored) return@fastEach
                if (!it.sizeValid) layout(it)
                if (!it.sizeValid) layout(it)
                val ivs = it.visibleSize
                val ipad = it.padding
                val itMain = ipad[main] + ivs[main] + ipad[main + 2] + mainPad
                val itCross = cpad2 + ipad[crs] + ivs[crs] + ipad[crs + 2]
                if (it.createdWithSetPosition) {
                    rowMain = max(rowMain, it.at[main] + itMain)
                    rowCross = max(rowCross, it.at[crs] + itCross)
                } else {
                    rowMain += itMain
                    rowCross = max(rowCross, itCross)
                }
            }
            assignAndCheckSize(component, main, crs, rowMain, rowCross)
            place(
                align, children,
                component.size[crs], component.at[crs], crossPad, crs,
                rowMain, component.at[main], component.size[main], mainPad, main,
            )
        }
    }

    private data class WrappingRow(val rowMain: Float, val rowCross: Float, val row: ArrayList<out Component>)

    private fun place(
        align: Align, row: ArrayList<out Component>,
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
            if (it.createdWithSetPosition) return@fastEach
            aligner.align(rowCross, it, minCross, padCross, crs)
            current = justifier.justify(current, it, padMain, main, gap)
            if (it is Scrollable) it.resetScroll()
        }
    }

    private fun assignAndCheckSize(component: Component, main: Int, crs: Int, mainValue: Float, crossValue: Float) {
        if (component.size[main] <= 0f) component.size(main, mainValue)
        if (component.size[crs] <= 0f) component.size(crs, crossValue)
        component.fixVisibleSize()
    }

    private fun interface Aligner {
        fun align(rowCross: Float, it: Component, min: Float, padding: Float, crs: Int)
    }

    private fun interface Justifier {
        fun justify(current: Float, it: Component, padding: Float, main: Int, gap: Float): Float
    }

    private val cStart = Aligner { _, it, min, padding, crs ->
        it.at(crs, min + padding + it.padding[crs])
    }

    private val cCenter = Aligner { rowCross, it, min, _, crs ->
        it.at(crs, min + (rowCross / 2f) - (it.visibleSize[crs] / 2f))
    }

    private val cEnd = Aligner { rowCross, it, min, padding, crs ->
        val ipad = it.padding
        it.at(crs, min + rowCross - (it.visibleSize[crs] + ipad[crs] + ipad[crs + 2]) - padding)
    }

    private val mProgressive = Justifier { current, it, padding, main, _ ->
        val ipad = it.padding
        it.at(main, current + ipad[main])
        return@Justifier current + it.visibleSize[main] + padding + ipad[main] + it.padding[main + 2]
    }

    private val mBackwards = Justifier { current, it, padding, main, _ ->
        val ipad = it.padding
        val cur = current - it.visibleSize[main] - padding - ipad[main + 2]
        it.at(main, cur)
        return@Justifier cur - ipad[main]
    }

    private val mSpace = Justifier { current, it, padding, main, gap ->
        val ipad = it.padding
        it.at(main, current + ipad[main])
        return@Justifier current + it.visibleSize[main] + padding + gap + ipad[main] + ipad[main + 2]
    }
}
