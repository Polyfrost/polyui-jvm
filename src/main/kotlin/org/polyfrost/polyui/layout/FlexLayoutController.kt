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
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.fastEach
import kotlin.math.max

object FlexLayoutController : LayoutController {
    override fun layout(component: Component) {
        val children = component.children
        val polyUI = component.polyUI
        val iKnowHowBigIAm = component.createdWithSetSize
        // step 1: query if we know our size. if we don't try calculateSize(), if not we are going to calculate it based on our children.
        if (!component.sizeValid) {
            val out = component.calculateSize()
            if (out.isPositive) {
                component.width = out.x
                component.height = out.y
            }
        }

        if (!component.sizeValid) {
            // we don't know our size still, so we need to calculate it based on our children.
            // hope they know what they are doing!
            if (component.layoutIgnored) return
            if (children.isNullOrEmpty()) {
                PolyUI.LOGGER.error("failed to initialize $component: was skipped as it has no size and no children")
                return
            }
        } else if (children.isNullOrEmpty()) {
            // no children, so nothing to position. just ensure the size isn't larger than itself and return.
            component.fixVisibleSize()
            return
        }

        // step 2: we might have a size, and we have children to place. lets calculate the padding.
        val align = component.alignment
        val main = if (align.mode == Align.Mode.Horizontal) 0 else 1
        val crs = if (main == 0) 1 else 0

        // asm: if we have already been positioned, we need to scale the padding
        val mainPadEdges = align.padEdges[main].let { if (component.positioned) it * (polyUI.size[main] / polyUI.iSize[main]) else it }
        val crossPadEdges = align.padEdges[crs].let { if (component.positioned) it * (polyUI.size[crs] / polyUI.iSize[crs]) else it }
        val mainPadBetween = align.padBetween[main].let { if (component.positioned) it * (polyUI.size[main] / polyUI.iSize[main]) else it }
        val crossPadBetween = align.padBetween[crs].let { if (component.positioned) it * (polyUI.size[crs] / polyUI.iSize[crs]) else it }
        // figure out how many 'useful' children we have.
        var sizeWithoutIgnored = 0
        children.fastEach { if (!it.layoutIgnored) sizeWithoutIgnored++ }

        if (sizeWithoutIgnored == 1) {
            // we only have 1 child, so as a fast path, we can set our size
            val child = children.first()
            child._parent = component
            val ipad = child.padding
            if (!child.sizeValid) {
                // our child might not know how big it is, but as we know how big we are, we can suggest a size with confidence.
                val suggestedSize = if (iKnowHowBigIAm) {
                    val mySize = component.visibleSize
                    Vec2(
                        mySize[main] - (ipad[main] + ipad[main + 2] + mainPadEdges * 2f),
                        mySize[crs] - (ipad[crs] + ipad[crs + 2] + crossPadEdges * 2f)
                    )
                } else Vec2.ZERO
                handleInvalidSize(child, suggestedSize, polyUI)
            }

            // lets try and assign ourself our size based on the child.
            val ivs = child.visibleSize
            assignAndCheckSize(
                component, main, crs,
                ivs[main] + ipad[main] + ipad[main + 2] + (mainPadEdges * 2f),
                ivs[crs] + ipad[crs] + ipad[crs + 2] + (crossPadEdges * 2f)
            )
            if (child.layoutIgnored || child.createdWithSetPosition) return
            // now lets place the child in the middle.
            val vs = component.visibleSize
            child.at(
                main,
                component.at[main] + when (align.main) {
                    Align.Content.Start -> mainPadEdges + ipad[main]
                    Align.Content.End -> vs[main] - ivs[main] - mainPadEdges - ipad[main]
                    else -> (vs[main] - ivs[main]) / 2f
                },
            )
            child.at(
                crs,
                component.at[crs] + when (align.line) {
                    Align.Line.Start -> crossPadEdges + ipad[crs]
                    Align.Line.End -> vs[crs] - ivs[crs] - crossPadEdges - ipad[crs]
                    else -> (vs[crs] - ivs[crs]) / 2f
                },
            )
            if (child is Scrollable) child.resetScroll()
            return
        }

        // OK, we have atleast 2 children, so we need to do a proper layout.
        val rows = ArrayList<WrappingRow>(when(align.wrap) {
            Align.Wrap.NEVER -> 1
            Align.Wrap.ALWAYS -> sizeWithoutIgnored
            Align.Wrap.AUTO -> 5
        })

        // step 1: lets calculate the maximum size of the main axis, our wrap capacity.
        val wrapCap = component.visibleSize[main].let { mySize ->
            // asm: we do the layout at full (original) size so we need to undo it for this calculation
            val invScalingFactor = if (component.positioned) 1f else polyUI.iSize[main] / polyUI.size[main]
            val screenSize = polyUI.master.size[main] * invScalingFactor
            // use our own size if we have it.
            if (mySize != 0f) mySize.coerceAtMost(screenSize)
            else {
                val parent = component._parent
                if (parent != null && parent.sizeValid) {
                    // our parent knows how big it is, so we can use that
                    (parent.visibleSize[main] * invScalingFactor).coerceAtMost(screenSize)
                } else screenSize // if not, we use the screen size
            }
        } + 1f - (mainPadEdges * 2f) // asm: add 1 to avoid rounding errors

        var maxMain = 0f
        var maxCross = crossPadEdges * 2f
        var rowMain = 0f
        var rowCross = 0f
        val rowInitialSize = if (align.wrap == Align.Wrap.ALWAYS) 1 else children.size.coerceAtMost(10)
        var currentRow = ArrayList<Component>(rowInitialSize)
        // step 2: lets calculate our size, and calculate our rows of children, based on how many we can fit in the main axis.
        children.fastEach {
            it._parent = component
            if (it.layoutIgnored) return@fastEach
            // todo: maybe figure out if we can supply something plausible here as a suggested size?
            if (!it.sizeValid) handleInvalidSize(it, Vec2.ZERO, polyUI)
            // todo: removed as it introduced bugs with added UIs to a resized instance. wasn't very useful anyway
//            if (it.visibleSize[main] > wrapCap && !iKnowHowBigIAm) {
                // object is too large for this in its entirety (it is bigger than our wrap capacity), so we
                // ask it to recalculate its size (hopefully it will shrink, if not then oh well)
//                it.setup(polyUI)
//                it.recalculate()
//            }
            val itSize = it.visibleSize
            val itPad = it.padding
            val itMain = itPad[main] + itPad[main + 2] + itSize[main]
            if (align.wrap != Align.Wrap.NEVER && currentRow.isNotEmpty() && (rowMain + itMain > wrapCap || align.wrap == Align.Wrap.ALWAYS)) {
                // the row cannot accommodate this item, so we need to finish the current row and start a new one, then continue.
                rowMain = rowMain - mainPadBetween + mainPadEdges * 2f
                rows.add(WrappingRow(rowMain, rowCross, currentRow))
                currentRow = ArrayList(rowInitialSize)
                maxMain = max(maxMain, rowMain)
                maxCross += rowCross + crossPadBetween
                rowMain = 0f
                rowCross = 0f
            }
            rowMain += itMain + mainPadBetween
            rowCross = max(rowCross, itPad[crs] + itPad[crs + 2] + itSize[crs])
            currentRow.add(it)
        }

        // now we have calculated all the rows, we need to finalize the last row.
        rowMain = rowMain - mainPadBetween + mainPadEdges * 2f // remove the last padding, add the edges.
        if (currentRow.isNotEmpty()) {
            rows.add(WrappingRow(rowMain, rowCross, currentRow))
            maxMain = max(maxMain, rowMain)
            // this is added then removed in the case that there wasn't a last row (otherwise we would never have added it above).
            maxCross += rowCross + crossPadBetween
        }
        // remove the last padding, as we don't need it (there is no next row).
        maxCross -= crossPadBetween

        // we now know how big we are, so lets assign our size (if we can)
        assignAndCheckSize(component, main, crs, maxMain, maxCross)
        val mySize = component.visibleSize

        // lets calculate where to place our rows on the cross axis.
        val gap = when (align.cross) {
            Align.Content.SpaceBetween -> {
                val totalCross = maxCross - (rows.size - 1) * crossPadBetween
                (mySize[crs] - totalCross) / (rows.size - 1)
            }

            Align.Content.SpaceEvenly -> {
                val totalCross = maxCross - (rows.size + 1) * crossPadBetween + crossPadEdges
                (mySize[crs] - totalCross) / (rows.size + 1)
            }

            else -> 0f
        }

        rowCross = component.at[crs] + when (align.cross) {
            Align.Content.Start, Align.Content.SpaceBetween -> crossPadEdges
            Align.Content.End -> mySize[crs] - crossPadEdges
            Align.Content.Center -> (mySize[crs] - maxCross) / 2f + crossPadEdges
            Align.Content.SpaceEvenly -> gap
        }

        // lets place the rows.
        rows.fastEach { (theRowMain, theRowCross, row) ->
            if (align.cross == Align.Content.End) rowCross -= theRowCross
            place(
                align, row,
                theRowCross, rowCross, crs,
                theRowMain, component.at[main], mySize[main], mainPadEdges, mainPadBetween, main,
            )
            when (align.cross) {
                Align.Content.Start, Align.Content.Center -> rowCross += theRowCross + crossPadBetween
                Align.Content.End -> rowCross -= crossPadBetween
                Align.Content.SpaceBetween, Align.Content.SpaceEvenly -> rowCross += theRowCross + gap
//                        Align.Content.SpaceEvenly -> rowCross += (theRowCross + crossPadBetween) / 2f

            }
        }
    }

    private fun handleInvalidSize(child: Component, suggestedSize: Vec2, polyUI: PolyUI) {
        if (suggestedSize != Vec2.ZERO) {
            if (child is Scrollable && child.hasVisibleSize) child.visibleSize = suggestedSize
            child.size = suggestedSize
        }
        child.setup(polyUI)
    }

    private data class WrappingRow(val rowMain: Float, val rowCross: Float, val row: ArrayList<out Component>)

    private fun place(
        align: Align, row: ArrayList<out Component>,
        rowCross: Float, minCross: Float, crs: Int,
        rowMain: Float, minMain: Float, maxMain: Float, mainPadEdges: Float, mainPadBetween: Float, main: Int,
    ) {
        val aligner = when (align.line) {
            Align.Line.Start -> cStart
            Align.Line.Center -> cCenter
            Align.Line.End -> cEnd
        }
        val justifier = when (align.main) {
            Align.Content.Start, Align.Content.Center -> mProgressive
            Align.Content.End -> mBackwards
            Align.Content.SpaceBetween, Align.Content.SpaceEvenly -> mSpace
        }
        val gap = when (align.main) {
            Align.Content.SpaceBetween -> (maxMain - rowMain) / (row.size - 1)
            Align.Content.SpaceEvenly -> (maxMain - rowMain) / (row.size + 1)
            else -> 0f
        }
        var current = minMain + when (align.main) {
            Align.Content.Start, Align.Content.SpaceBetween -> mainPadEdges
            Align.Content.End -> maxMain - mainPadEdges
            Align.Content.Center -> (maxMain / 2f) - (rowMain / 2f) + mainPadEdges
            Align.Content.SpaceEvenly -> mainPadEdges + gap
        }
        row.fastEach {
            if (it.createdWithSetPosition || it.layoutIgnored) return@fastEach
            aligner.align(rowCross, it, minCross, crs)
            current = justifier.justify(current, it, mainPadBetween, main, gap)
            if (it is Scrollable) it.resetScroll()
        }
    }

    /**
     * assign the size of the component if it is not set.
     */
    private fun assignAndCheckSize(component: Component, main: Int, crs: Int, mainValue: Float, crossValue: Float) {
        if (component.size[main] <= 0f) component.size(main, mainValue)
        if (component.size[crs] <= 0f) component.size(crs, crossValue)
        component.fixVisibleSize()
    }

    private fun interface Aligner {
        fun align(rowCross: Float, it: Component, min: Float, crs: Int)
    }

    private fun interface Justifier {
        fun justify(current: Float, it: Component, padBetween: Float, main: Int, gap: Float): Float
    }

    private val cStart = Aligner { _, it, min, crs ->
        it.at(crs, min + it.padding[crs])
    }

    private val cCenter = Aligner { rowCross, it, min, crs ->
        it.at(crs, min + (rowCross / 2f) - (it.visibleSize[crs] / 2f))
    }

    private val cEnd = Aligner { rowCross, it, min, crs ->
        val ipad = it.padding
        it.at(crs, min + rowCross - (it.visibleSize[crs] + ipad[crs] + ipad[crs + 2]))
    }

    private val mProgressive = Justifier { current, it, padBetween, main, _ ->
        val ipad = it.padding
        it.at(main, current + ipad[main])
        return@Justifier current + it.visibleSize[main] + padBetween + ipad[main] + ipad[main + 2]
    }

    private val mBackwards = Justifier { current, it, padBetween, main, _ ->
        val ipad = it.padding
        val cur = current - it.visibleSize[main] - ipad[main + 2]
        it.at(main, cur)
        return@Justifier cur - ipad[main] - padBetween
    }

    private val mSpace = Justifier { current, it, padBetween, main, gap ->
        val ipad = it.padding
        it.at(main, current + ipad[main])
        return@Justifier current + it.visibleSize[main] + padBetween + gap + ipad[main] + ipad[main + 2]
    }
}
