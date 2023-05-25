/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.fastEachIndexed
import cc.polyfrost.polyui.utils.moveElement
import cc.polyfrost.polyui.utils.sortedByDescending
import kotlin.math.max

/**
 * # FlexLayout
 *
 * A flex layout that implements all the features of the [Flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-flexbox-property) system.
 *
 * This layout is very powerful, and is used commonly in web development. If you need to know more, I would recommend checking out the link above.
 *
 * @param [size] The size of this layout. This is a 'hard' limit for the wrap. If the next item would exceed the size, it will not be added. If you want a hard limit, but auto cross size, use 0 as the cross size.
 * @param [wrap] The wrap size of this layout. If this is set, the layout will automatically wrap to the next line if the next item would exceed the wrap size. This is a 'soft' limit, so that if it needs to exceed, it can. Takes precedence over [size].
 */
@Suppress("unused")
class FlexLayout @JvmOverloads constructor(
    at: Point<Unit>,
    size: Size<Unit>? = null,
    wrap: Unit? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    private val flexDirection: Direction = Direction.Row,
    wrapDirection: Wrap = Wrap.Wrap,
    private val contentJustify: JustifyContent = JustifyContent.Start,
    private val itemAlign: AlignItems = AlignItems.Start,
    private val contentAlign: AlignContent = AlignContent.Start,
    gap: Gap = Gap.Default,
    resizesChildren: Boolean = true,
    vararg items: Drawable
) : Layout(at, size, onAdded, onRemoved, false, resizesChildren, *items) {
    constructor(at: Point<Unit>, wrap: Unit.Percent, vararg items: Drawable) : this(
        at,
        null,
        wrap,
        null,
        null,
        items = items
    )

    private val drawables: ArrayList<FlexDrawable>

    private val mainGap = when (flexDirection) {
        Direction.Row, Direction.RowReverse -> gap.mainGap.px
        Direction.Column, Direction.ColumnReverse -> gap.crossGap.px
    }

    private val crossGap = when (flexDirection) {
        Direction.Row, Direction.RowReverse -> gap.crossGap.px
        Direction.Column, Direction.ColumnReverse -> gap.mainGap.px
    }
    private val wrapDirection: Wrap
    private val strictSize = size != null && wrap == null

    init {
        if (this.size == null) this.size = origin
        if (wrapDirection != Wrap.NoWrap) {
            if (wrap != null) {
                PolyUI.LOGGER.warn("[Flex] wrap is set, but wrap direction is set to NoWrap. Defaulting to Wrap.")
                this.wrapDirection = Wrap.Wrap
                when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> this.size = Size(wrap, size?.b ?: 0.px)
                    Direction.Column, Direction.ColumnReverse -> this.size = Size(size?.a ?: 0.px, wrap)
                }
            } else if (size != null) {
                PolyUI.LOGGER.warn("[Flex] size is set, but wrap direction is set to NoWrap. Defaulting to Wrap.")
                this.wrapDirection = Wrap.Wrap
            } else {
                this.wrapDirection = wrapDirection
            }
        } else {
            this.wrapDirection = wrapDirection
        }
        items.forEachIndexed { i, it ->
            if (it.atType != Unit.Type.Flex) {
                throw Exception("Unit type mismatch: Drawable $it needs to be placed using a Flex unit for a flex layout.")
            }
            if (it.sizeType == Units.Flex) {
                throw Exception("A flex layout's size property is used to specify the minimum size of the component, please use the at property for your flex data.")
            }
            @Suppress("UNCHECKED_CAST") // already type-checked
            if ((it.at as Point<Unit.Flex>).a.index >= 0) {
                items.moveElement(i, (it.at.a as Unit.Flex).index)
            }
            if (it is Component) it.layout = this // why are smart casts so goofy? like I seriously have to do this?
            if (it is Layout) it.layout = this
        }
        drawables = items.map { FlexDrawable(it, it.at.a as Unit.Flex, it.size) } as ArrayList<FlexDrawable>
        if (wrapDirection == Wrap.WrapReverse) drawables.reverse()
    }

    private var crossSize: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> size!!.b.px
                Direction.Column, Direction.ColumnReverse -> size!!.a.px
            }
        }
        set(value) {
            if (strictSize) return
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> size!!.b.px = value
                Direction.Column, Direction.ColumnReverse -> size!!.a.px = value
            }
        }
    private var mainSize: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> size!!.a.px
                Direction.Column, Direction.ColumnReverse -> size!!.b.px
            }
        }
        set(value) {
            if (strictSize) return
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> size!!.a.px = value
                Direction.Column, Direction.ColumnReverse -> size!!.b.px = value
            }
        }

    override fun calculateBounds() {
        doDynamicSize()
        var mainAxis = 0F
        val rows = arrayListOf<FlexRow>()

        if (wrapDirection != Wrap.NoWrap) {
            var row = arrayListOf<FlexDrawable>()
            drawables.fastEachIndexed { i, it ->
                mainAxis += it.mainSize + mainGap
                if (it.flex.endRowAfter ||
                    (
                        mainAxis + (
                            if (strictSize) {
                                (drawables.getOrNull(i + 1)?.mainSize ?: 0F)
                            } else {
                                0F
                            }
                            ) >= this.width
                        )
                ) { // means we need to wrap
                    rows.add(FlexRow(row))
                    mainAxis = 0F
                    row = arrayListOf()
                }
                row.add(it)
            }

            // do last row
            if (row.isNotEmpty()) {
                rows.add(FlexRow(row))
            }
            if (wrapDirection == Wrap.WrapReverse) {
                rows.reverse()
                rows.fastEach { it.drawables.reverse() }
            }
            row = ArrayList()
        } else {
            // add all to the row if wrap is off
            rows.add(FlexRow(drawables))
        }

        var maxCrossSizeNoGaps = 0F
        var minIndex = 0
        var err = false
        run {
            rows.fastEach {
                maxCrossSizeNoGaps += it.maxCrossSize
                minIndex += it.drawables.size
                if (strictSize) {
                    if (maxCrossSizeNoGaps > crossSize) {
                        PolyUI.LOGGER.warn(
                            "[Flex] Cross size is too small for the content. (Cross size: {}, content size: {}). Excess removed.",
                            crossSize,
                            maxCrossSizeNoGaps
                        )
                        err = true
                        return@run // break https://kotlinlang.org/docs/returns.html#return-to-labels
                    }
                }
            }
        }
        if (err) {
            for (i in minIndex until drawables.size) {
                removeComponentNow(drawables[i].drawable)
            }
        }
        crossSize = maxCrossSizeNoGaps + (rows.size - 1) * crossGap
        var cross = 0F

        // justify, with the largest row first.
        (rows.sortedByDescending { it.thisMainSizeWithGaps }).fastEach {
            it.justify()
        }
        rows.fastEach { row ->
            row.align(rows.size, cross, maxCrossSizeNoGaps)
            if (contentAlign == AlignContent.End) {
                cross -= row.maxCrossSize + crossGap
            } else {
                cross += row.maxCrossSize + crossGap
            }
        }
        rows.clear()
    }

    fun trySetMainSize(new: Float) {
        mainSize = max(mainSize, new)
    }

    private inner class FlexDrawable(val drawable: Drawable, val flex: Unit.Flex, minSize: Size<Unit>? = null) {
        val size get() = drawable.size!!

        init {
            if (drawable.size == null) drawable.size = calculateSize() ?: minSize ?: origin
        }

        /** x */
        var mainPos: Float
            get() {
                return when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> drawable.at.a.px
                    Direction.Column, Direction.ColumnReverse -> drawable.at.b.px
                }
            }
            set(value) = when (flexDirection) {
                Direction.Row, Direction.RowReverse -> drawable.at.a.px = value
                Direction.Column, Direction.ColumnReverse -> drawable.at.b.px = value
            }

        /** y */
        var crossPos: Float
            get() {
                return when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> drawable.at.b.px
                    Direction.Column, Direction.ColumnReverse -> drawable.at.a.px
                }
            }
            set(value) = when (flexDirection) {
                Direction.Row, Direction.RowReverse -> drawable.at.b.px = value
                Direction.Column, Direction.ColumnReverse -> drawable.at.a.px = value
            }

        /** height */
        var crossSize: Float
            get() {
                return when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> size.b.px
                    Direction.Column, Direction.ColumnReverse -> size.a.px
                }
            }
            set(value) = when (flexDirection) {
                Direction.Row, Direction.RowReverse -> size.b.px = value
                Direction.Column, Direction.ColumnReverse -> size.a.px = value
            }

        /** width */
        var mainSize: Float
            get() {
                return when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> size.a.px
                    Direction.Column, Direction.ColumnReverse -> size.b.px
                }
            }
            set(value) = when (flexDirection) {
                Direction.Row, Direction.RowReverse -> size.a.px = value
                Direction.Column, Direction.ColumnReverse -> size.b.px = value
            }

        val isSizedMain = if (mainSize != 0F) {
            true
        } else {
            if (wrapDirection != Wrap.NoWrap) {
                PolyUI.LOGGER.warn(
                    "[Flex] Drawable {} has a main size of 0. This may lead to odd things on wrapped layouts.",
                    this.drawable
                )
            }
            false
        }
        val isSizedCross = crossSize != 0F
    }

    private inner class FlexRow(
        val drawables: ArrayList<FlexDrawable>
    ) {
        val thisMainSize: Float
        val maxCrossSize: Float
        val thisMainSizeWithGaps: Float

        init {
            var main = 0F
            drawables.fastEach { main += it.mainSize }
            this.thisMainSize = main
            this.thisMainSizeWithGaps = main + (drawables.size - 1) * mainGap
            resizeMainIfCan()

            var cross = 0F
            drawables.fastEach { cross = max(cross, it.crossSize) }
            this.maxCrossSize = cross
        }

        fun resizeMainIfCan() {
            val spare = mainSize - thisMainSize
            var i = 0
            if (spare > 0) {
                drawables.fastEach { i += it.flex.flexGrow }
            } else {
                drawables.fastEach { i += it.flex.flexShrink }
            }
            if (i == 0) return
            drawables.fastEach {
                val grow = it.flex.flexGrow
                if (grow == 0) return@fastEach
                it.mainSize += spare * (grow / i)
            }
        }

        fun align(numRows: Int, thisRowCrossPos: Float, allMaxCrossSize: Float) {
            when (contentAlign) {
                AlignContent.Start -> {
                    drawables.fastEach { it.crossPos = thisRowCrossPos }
                }

                AlignContent.End -> {
                    drawables.fastEach { it.crossPos = thisRowCrossPos + (crossSize - it.crossSize) }
                }

                AlignContent.Center -> {
                    drawables.fastEach { it.crossPos = thisRowCrossPos + (crossSize - it.crossSize) / 2 }
                }

                AlignContent.SpaceBetween -> {
                    val gap = (crossSize - allMaxCrossSize) / (numRows - 1)
                    drawables.fastEach {
                        it.crossPos = thisRowCrossPos + gap
                    }
                }

                AlignContent.SpaceEvenly -> {
                    val gap = (crossSize - allMaxCrossSize) / (numRows + 1)
                    drawables.fastEach {
                        it.crossPos = thisRowCrossPos + gap
                    }
                }

                AlignContent.Stretch -> {
                    drawables.fastEach {
                        it.crossPos = thisRowCrossPos
                        it.crossSize = maxCrossSize
                    }
                }
            }
            when (itemAlign) {
                AlignItems.Start -> {
                    drawables.fastEach { it.crossPos += 0F }
                }

                AlignItems.End -> {
                    drawables.fastEach { it.crossPos += maxCrossSize - it.crossSize }
                }

                AlignItems.Center -> {
                    drawables.fastEach { it.crossPos += (maxCrossSize - it.crossSize) / 2 }
                }
            }
        }

        fun justify() {
            trySetMainSize(thisMainSizeWithGaps)
            when (contentJustify) {
                JustifyContent.Start -> {
                    var pos = 0F
                    drawables.fastEach {
                        it.mainPos = pos
                        pos += it.mainSize + mainGap
                    }
                }

                JustifyContent.End -> {
                    var pos = mainSize
                    drawables.fastEach {
                        pos -= it.mainSize
                        it.mainPos = pos
                        pos -= mainGap
                    }
                }

                JustifyContent.Center -> {
                    var pos = (mainSize - thisMainSizeWithGaps) / 2
                    drawables.fastEach {
                        it.mainPos = pos
                        pos += it.mainSize + mainGap
                    }
                }

                JustifyContent.SpaceBetween -> {
                    if (drawables.size == 1) {
                        drawables[0].mainPos = (mainSize - drawables[0].mainSize) / 2
                        return
                    }
                    val gap = (mainSize - thisMainSize) / (drawables.size - 1)
                    var pos = 0f
                    drawables.fastEach {
                        it.mainPos = pos
                        pos += it.mainSize + gap
                    }
                }

                JustifyContent.SpaceEvenly -> {
                    if (drawables.size == 1) {
                        drawables[0].mainPos = (mainSize - drawables[0].mainSize) / 2
                        return
                    }
                    val gap = (mainSize - thisMainSize) / drawables.size
                    var pos = gap
                    drawables.fastEach {
                        it.mainPos = pos
                        pos += it.mainSize + gap
                    }
                }
            }
        }
    }

    //  --- data ---

    /** [Flex Direction](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-flex-direction) */
    enum class Direction {
        Row, Column, RowReverse, ColumnReverse
    }

    /** [Flex Wrap](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-flex-wrap) */
    enum class Wrap {
        NoWrap, Wrap, WrapReverse
    }

    /** [Flex Justify Content](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-justify-content) */
    enum class JustifyContent {
        Start, End, Center, SpaceBetween, SpaceEvenly
    }

    /** [Flex Align Items](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-align-items) */
    enum class AlignItems {
        Start, End, Center
    }

    /** [Flex Align Content](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-align-content) */
    enum class AlignContent {
        Start, End, Center, SpaceBetween, SpaceEvenly, Stretch
    }
}
