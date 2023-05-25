/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastEach
import kotlin.math.max

/**
 * # GridLayout
 * A grid layout is a simplistic layout that places components in a grid.
 *
 * It is **fully** automatic, meaning that all components will be sized and placed automatically, based on two flags: [cellSize] and [contentStretch].
 *
 * It is recommended to use [scrolling] on this layout to set a viewport size for it, and so the user can scroll through the grid.
 */
class GridLayout @JvmOverloads constructor(
    at: Point<Unit>,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    private val cellSize: CellSize = CellSize.AllSame,
    private val contentStretch: ContentStretch = ContentStretch.FillCell,
    private val gap: Gap = Gap.Default,
    resizesChildren: Boolean = true,
    vararg items: Drawable
) : Layout(at, origin, onAdded, onRemoved, false, resizesChildren, *items) {

    /** list of rows */
    private var grid: Array<Array<Drawable?>>

    init {
        var nrows = 0
        var ncols = 0
        items.forEach {
            if (it.atType != Unit.Type.Grid) {
                throw Exception("Unit type mismatch: Drawable $it needs to be placed using a Grid unit for a grid layout.")
            }
            val u = it.at.a as Unit.Grid
            nrows = max(nrows, u.row + u.rs)
            ncols = max(ncols, u.column + u.cs)
        }
        grid = Array(nrows) { Array(ncols) { null } }
        items.forEach {
            val u = it.at.a as Unit.Grid
            for (row in u.row until u.row + u.rs) {
                for (col in u.column until u.column + u.cs) {
                    grid[row][col] = it
                }
            }
        }
    }

    private fun getColumns(): Array<Array<Drawable?>> {
        return grid.map { it }.toTypedArray()
    }

    override fun calculateBounds() {
        doDynamicSize()
        components.fastEach {
            it.layout = this
            it.calculateBounds()
        }
        children.fastEach {
            it.layout = this
            it.calculateBounds()
        }

        var atX = 0f
        var atY = 0f
        var width = atX
        var height = atY
        when (cellSize) {
            CellSize.AllSame -> {
                var cellw = 0f
                var cellh = 0f
                grid.forEach { row ->
                    row.forEach {
                        if (it != null) {
                            cellw = max(cellw, it.width)
                            cellh = max(cellh, it.height)
                        }
                    }
                }
                grid.forEach { row ->
                    row.forEach {
                        if (it != null) placeItem(atX, atY, cellw, cellh, it)
                        atX += cellw + gap.mainGap.px
                    }
                    width = max(width, atX)
                    atX = at.x
                    atY += cellh + gap.crossGap.px
                }
                height = atY
            }

            CellSize.DependsOnRow -> {
                grid.forEachIndexed { i, row ->
                    val cellw = row.maxOfOrNull { it?.width ?: 0f } ?: 0f
                    val cellh = row.maxOfOrNull { it?.height ?: 0f } ?: 0f
                    if (cellw == 0f) throw Exception("Row $i has no sized items in it. Please specify at least one's width.")
                    if (cellh == 0f) throw Exception("Row $i has no sized items in it. Please specify at least one's height.")
                    row.forEach {
                        if (it != null) placeItem(atX, atY, cellw, cellh, it)
                        atX += cellw + gap.mainGap.px
                    }
                    width = max(width, atX)
                    atX = at.x
                    atY += cellh + gap.crossGap.px
                }
                height = atY
            }

            CellSize.DependsOnColumn -> {
                getColumns().forEachIndexed { i, col ->
                    val cellw = col.maxOfOrNull { it?.width ?: 0f } ?: 0f
                    val cellh = col.maxOfOrNull { it?.height ?: 0f } ?: 0f
                    if (cellw == 0f) throw Exception("Column $i has no sized items in it. Please specify at least one's sized width.")
                    if (cellh == 0f) throw Exception("Column $i has no sized items in it. Please specify at least one's sized height.")
                    col.forEach {
                        if (it != null) placeItem(atX, atY, cellw, cellh, it)
                        atY += cellh + gap.mainGap.px
                    }
                    height = max(height, atY)
                    atY = at.y
                    atX += cellw + gap.crossGap.px
                }
                width = atX
            }
        }

        this.size = Size(width.px, height.px)
    }

    private fun placeItem(atX: Float, atY: Float, cellw: Float, cellh: Float, it: Drawable) {
        it.at.a.px = atX
        it.at.b.px = atY
        if (it.size == null) {
            it.size = Size(cellw.px, cellh.px)
            return
        }
        when (contentStretch) {
            ContentStretch.FillCell -> {
                it.size!!.a.px = cellw
                it.size!!.b.px = cellh
            }

            ContentStretch.DontStretch -> {
                // Ok!
            }

            ContentStretch.FillRespectAspectRatio -> {
                val ratio = it.width / it.height
                if (ratio > 1) {
                    it.size!!.a.px = cellw
                    it.size!!.b.px = cellw / ratio
                } else {
                    it.size!!.a.px = cellh * ratio
                    it.size!!.b.px = cellh
                }
            }
        }
    }

    enum class CellSize {
        /** All cells have the same size. */
        AllSame,

        /** Each row has its own size (the largest of that row) */
        DependsOnRow,

        /** Each column has its own size (the largest of that column). */
        DependsOnColumn;
    }

    enum class ContentStretch {
        /** The content is stretched to fill the cell. */
        FillCell,

        /** the content is stretched to fill the cell, but respects the aspect ratio. */
        FillRespectAspectRatio,

        /** The content is not stretched. */
        DontStretch;
    }
}
