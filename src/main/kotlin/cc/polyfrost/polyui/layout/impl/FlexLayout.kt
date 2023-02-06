package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.*
import kotlin.Exception
import kotlin.Float
import kotlin.Int
import kotlin.Suppress
import kotlin.math.max

@Suppress("unused")

/**
 * # FlexLayout
 *
 * A flex layout that implements all the features of the [Flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-flexbox-property) system.
 *
 * This layout is very powerful, and is used commonly in web development. If you need to know more, I would recommend checking out the link above.
 *
 * @param [sized] The size of this layout. This is a 'hard' limit for the wrap. If the next item would exceed the size, it will not be added. If you want a hard limit, but auto cross size, use 0 as the cross size.
 * @param [wrap] The wrap size of this layout. If this is set, the layout will automatically wrap to the next line if the next item would exceed the wrap size. This is a 'soft' limit, so that if it needs to exceed, it can. Takes precedence over [sized].
 */
class FlexLayout(
    at: Point<Unit>,
    sized: Size<Unit>? = null,
    wrap: Unit? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    private val flexDirection: Direction = Direction.Row,
    wrapDirection: Wrap = Wrap.Wrap,
    private val contentJustify: JustifyContent = JustifyContent.Start,
    private val itemAlign: AlignItems = AlignItems.Start,
    private val contentAlign: AlignContent = AlignContent.Start,
    gap: Gap = Gap.AUTO,
    vararg items: Drawable,
) : Layout(at, sized, onAdded, onRemoved, true, *items) {
    private val drawables: ArrayList<FlexDrawable>

    private val mainGap = when (flexDirection) {
        Direction.Row, Direction.RowReverse -> gap.mainGap.px
        Direction.Column, Direction.ColumnReverse -> gap.crossGap.px
    }

    private val crossGap = when (flexDirection) {
        Direction.Row, Direction.RowReverse -> gap.crossGap.px
        Direction.Column, Direction.ColumnReverse -> gap.mainGap.px
    }
    private val mainPos: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> at.a.px
                Direction.Column, Direction.ColumnReverse -> at.b.px
            }
        }
    private val crossPos: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> at.b.px
                Direction.Column, Direction.ColumnReverse -> at.a.px
            }
        }
    private val wrapDirection: Wrap
    private val strictSize = sized != null && wrap == null


    init {
        if (this.sized == null) this.sized = origin
        if (wrapDirection != Wrap.NoWrap) {
            if (wrap != null) {
                PolyUI.LOGGER.warn("wrap is set, but wrap direction is set to NoWrap. Defaulting to Wrap.")
                this.wrapDirection = Wrap.Wrap
                when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> this.sized = Size(wrap, sized?.b ?: 0.px)
                    Direction.Column, Direction.ColumnReverse -> this.sized = Size(sized?.a ?: 0.px, wrap)
                }
            } else if (sized != null) {
                PolyUI.LOGGER.warn("sized is set, but wrap direction is set to NoWrap. Defaulting to Wrap.")
                this.wrapDirection = Wrap.Wrap
            } else {
                this.wrapDirection = wrapDirection
            }
        } else {
            this.wrapDirection = wrapDirection
        }
        items.forEachIndexed { i, it ->
            if (it.atUnitType() != Unit.Type.Flex) {
                throw Exception("Unit type mismatch: Drawable $it needs to be placed using a Flex unit for a flex layout.")
            }
            if (it.sizedUnitType() == Units.Flex) {
                throw Exception("A flex layout's sized property is used to specify the minimum size of the component, please use the at property for your flex data.")
            }
            @Suppress("UNCHECKED_CAST") // already type-checked
            if ((it.at as Point<Unit.Flex>).a.index >= 0) {
                items.moveElement(i, (it.at.a as Unit.Flex).index)
            }
        }
        drawables = items.map { FlexDrawable(it, it.at.a as Unit.Flex, it.sized) } as ArrayList<FlexDrawable>
        if (wrapDirection == Wrap.WrapReverse) drawables.reverse()
    }

    private var crossSize: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> sized!!.b.px
                Direction.Column, Direction.ColumnReverse -> sized!!.a.px
            }
        }
        set(value) {
            if (strictSize) return
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> sized!!.b.px = value
                Direction.Column, Direction.ColumnReverse -> sized!!.a.px = value
            }
        }
    private var mainSize: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> sized!!.a.px
                Direction.Column, Direction.ColumnReverse -> sized!!.b.px
            }
        }
        set(value) {
            if (strictSize) return
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> sized!!.a.px = value
                Direction.Column, Direction.ColumnReverse -> sized!!.b.px = value
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
                    (mainAxis + (
                            if (strictSize) (drawables.getOrNull(i + 1)?.mainSize ?: 0F)
                            else 0F
                            ) >= this.width)
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
        kotlin.run {
            rows.fastEach {
                maxCrossSizeNoGaps += it.maxCrossSize
                minIndex += it.drawables.size
                if (strictSize) {
                    if (maxCrossSizeNoGaps > crossSize) {
                        PolyUI.LOGGER.warn("Cross size is too small for the content. (Cross size: $crossSize, content size: $maxCrossSizeNoGaps). Excess removed.")
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
            if (contentAlign == AlignContent.End) cross -= row.maxCrossSize + crossGap
            else cross += row.maxCrossSize + crossGap
        }
        rows.clear()
        needsRecalculation = false
    }

    fun trySetMainSize(new: Float) {
        mainSize = max(mainSize, new)
    }


    private inner class FlexDrawable(val drawable: Drawable, val flex: Unit.Flex, minSize: Size<Unit>? = null) {
        val size: Size<Unit>

        init {
            if (drawable.sized == null) drawable.sized = getSize() ?: minSize ?: origin
            // just so I don't have to !! everywhere
            size = drawable.sized!!
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
            if (wrapDirection != Wrap.NoWrap) PolyUI.LOGGER.warn("Drawable ${this.drawable} has a main size of 0. This may lead to odd things on wrapped layouts.")
            false
        }
        val isSizedCross = crossSize != 0F
    }

    private inner class FlexRow(
        val drawables: ArrayList<FlexDrawable>,
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
            val startCross = crossPos + thisRowCrossPos
            when (contentAlign) {
                AlignContent.Start -> {
                    drawables.fastEach { it.crossPos = startCross }
                }

                AlignContent.End -> {
                    drawables.fastEach { it.crossPos = startCross + (crossSize - it.crossSize) }
                }

                AlignContent.Center -> {
                    drawables.fastEach { it.crossPos = startCross + (crossSize - it.crossSize) / 2 }
                }

                AlignContent.SpaceBetween -> {
                    val gap = (crossSize - allMaxCrossSize) / (numRows - 1)
                    drawables.fastEach {
                        it.crossPos = startCross + gap
                    }
                }

                AlignContent.SpaceEvenly -> {
                    val gap = (crossSize - allMaxCrossSize) / (numRows + 1)
                    drawables.fastEach {
                        it.crossPos = startCross + gap
                    }
                }

                AlignContent.Stretch -> {
                    drawables.fastEach {
                        it.crossPos = startCross
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
                        it.mainPos = pos + mainPos
                        pos += it.mainSize + mainGap
                    }
                }

                JustifyContent.End -> {
                    var pos = mainPos + mainSize
                    drawables.fastEach {
                        pos -= it.mainSize
                        it.mainPos = pos
                        pos -= mainGap
                    }
                }

                JustifyContent.Center -> {
                    var pos = (mainSize - thisMainSizeWithGaps) / 2
                    drawables.fastEach {
                        it.mainPos = pos + mainPos
                        pos += it.mainSize + mainGap
                    }
                }

                JustifyContent.SpaceBetween -> {
                    if (drawables.size == 1) {
                        drawables[0].mainPos = mainPos + (mainSize - drawables[0].mainSize) / 2
                        return
                    }
                    val gap = (mainSize - thisMainSize) / (drawables.size - 1)
                    var pos = mainPos
                    drawables.fastEach {
                        it.mainPos = pos
                        pos += it.mainSize + gap
                    }
                }

                JustifyContent.SpaceEvenly -> {
                    if (drawables.size == 1) {
                        drawables[0].mainPos = mainPos + (mainSize - drawables[0].mainSize) / 2
                        return
                    }
                    val gap = (mainSize - thisMainSize) / drawables.size
                    var pos = mainPos + gap
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

    /** [Row Gap Column Gap](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-gap-row-gap-column-gap) */
    data class Gap(val mainGap: Unit.Pixel, val crossGap: Unit.Pixel) {
        companion object {
            @JvmField
            val AUTO = Gap(5.px, 5.px)
        }
    }

}