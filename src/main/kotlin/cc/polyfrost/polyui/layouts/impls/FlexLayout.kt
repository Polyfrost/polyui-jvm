package cc.polyfrost.polyui.layouts.impls

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.units.*
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.utils.*
import cc.polyfrost.polyui.utils.UnitUtils.origin
import kotlin.Exception
import kotlin.Float
import kotlin.Int
import kotlin.Suppress
import kotlin.math.max

@Suppress("unused")

/** # FlexLayout
 *
 * a flex layout that implements all the features of the [Flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-flexbox-properties) system.
 *
 * This layout is very powerful, and is used commonly in web development. If you need to know more, I would recommend checking out the link above.
 */
class FlexLayout(
    at: Point<Unit>, sized: Size<Unit>? = null,
    wrap: Unit? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    private val flexDirection: Direction = Direction.Row,
    wrapDirection: Wrap = Wrap.Wrap,
    // todo test these modes and make sure they all work
    private val contentJustify: JustifyContent = JustifyContent.Start,
    private val itemAlign: AlignItems = AlignItems.Start,
    private val contentAlign: AlignContent = AlignContent.Start,
    gap: Gap = Gap.Auto,
    vararg items: Drawable,
) : Layout(at, sized, onAdded, onRemoved, true, *items) {
    private val drawables: ArrayList<FlexDrawable>
    private var crossSize: Float = 0F
    private var mainSize: Float = 0F
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


    init {
        if (wrapDirection == Wrap.NoWrap) {
            if (wrap != null) {
                PolyUI.LOGGER.warn("wrap is set, but wrap direction is set to NoWrap. Defaulting to Wrap.")
                this.wrapDirection = Wrap.Wrap
                when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> this.sized = Size(wrap, sized?.b ?: 0F.px())
                    Direction.Column, Direction.ColumnReverse -> this.sized = Size(sized?.a ?: 0F.px(), wrap)
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

    override fun calculateBounds() {
        doDynamicSize()
        mainSize = when (flexDirection) {
            Direction.Row, Direction.RowReverse -> sized!!.a.px
            Direction.Column, Direction.ColumnReverse -> sized!!.b.px
        }
        crossSize = when (flexDirection) {
            Direction.Row, Direction.RowReverse -> sized!!.b.px
            Direction.Column, Direction.ColumnReverse -> sized!!.a.px
        }
        var mainAxis = 0F
        var mainAxisMax = 0F

        val rows = ArrayList<FlexRow>()
        if (wrapDirection != Wrap.NoWrap) {
            var row = ArrayList<FlexDrawable>()
            drawables.forEachNoAlloc {
                mainAxis += it.mainSize + mainGap
                if (mainAxis > width()) { // means we need to wrap
                    rows.add(FlexRow(row, mainAxis, mainAxisMax))
                    mainAxisMax = max(mainAxisMax, mainAxis)
                    mainAxis = 0F
                    row = ArrayList()
                }
                row.add(it)
            }
            // do last row
            if (row.isNotEmpty()) {
                rows.add(FlexRow(row, mainAxis, mainAxisMax))
            }
            if (wrapDirection == Wrap.WrapReverse) {
                rows.reverse()
                rows.forEachNoAlloc { it.drawables.reverse() }
            }
            row = ArrayList()
        } else {
            rows.add(FlexRow(drawables, 0F, width()))
        }

        val crossAxis = rows.sumOf { it.maxCrossSize } + (rows.size - 1) * crossGap
        var cross = 0F
        var i = 0
        rows.forEachNoAlloc {
            it.justifyContent()
            if (contentAlign == AlignContent.End) cross -= (it.alignItemsAndContent(
                rows.size, cross, crossAxis
            ) + crossGap)
            else cross += (it.alignItemsAndContent(rows.size, cross, crossAxis) + crossGap)
            println("ROW: $i (${it.drawables.size} items)")
            it.drawables.forEachNoAlloc { println(it.drawable.at) }
            println("END ROW $i")
            println("")
            i++
        }

        rows.clear()
        this.sized = Size(
            max(mainAxisMax, width()).px(),
            crossAxis.px()
        )
        needsRecalculation = false
    }


    private inner class FlexDrawable(val drawable: Drawable, val flex: Unit.Flex, minSize: Size<Unit>? = null) {
        val size: Size<Unit>

        init {
            if (drawable.sized == null) drawable.sized = getSize() ?: minSize ?: origin()
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
        thisMainSize: Float = 0f,
        maxMainSize: Float
    ) {
        val thisMainSize: Float
        val maxCrossSize: Float
        val maxMainSize: Float

        init {
            if (thisMainSize == 0f) {
                var main = 0F
                drawables.forEachNoAlloc { main += it.mainSize + mainGap }
                this.thisMainSize = main
            } else this.thisMainSize = thisMainSize
            if (maxMainSize == 0f) {
                this.maxMainSize = thisMainSize
            } else this.maxMainSize = maxMainSize
            //expandMainIfCan()

            var cross = 0F
            drawables.forEachNoAlloc { cross = max(cross, it.crossSize) }
            this.maxCrossSize = cross
        }

        fun expandMainIfCan() {
            var i = 0
            drawables.forEachNoAlloc { i += it.flex.flexGrow }
            if (i == 0) return
            val spare = maxMainSize - thisMainSize
            drawables.forEachNoAlloc {
                val grow = it.flex.flexGrow
                if (grow == 0) return@forEachNoAlloc
                it.mainSize += spare * (grow / i)
            }
        }

        fun alignItemsAndContent(numRows: Int, thisRow: Float, crossSize: Float): Float {
            when (contentAlign) {
                AlignContent.Start -> {
                    drawables.forEachNoAlloc { it.crossPos = crossPos + thisRow }
                }

                AlignContent.End -> {
                    drawables.forEachNoAlloc { it.crossPos = crossPos + thisRow + (crossSize - it.crossSize) }
                }

                AlignContent.Center -> {
                    drawables.forEachNoAlloc { it.crossPos = crossPos + thisRow + (crossSize - it.crossSize) / 2 }
                }

                AlignContent.SpaceBetween -> {
                    val gap = (crossSize - drawables.sumOf { it.crossSize })
                    drawables.forEachIndexedNoAlloc { i, it ->
                        it.crossPos = crossPos + thisRow + ((i - 2) * gap + i * it.crossSize)
                    }
                }

                AlignContent.SpaceEvenly -> {
                    val gap = (crossSize - drawables.sumOf { it.crossSize })
                    drawables.forEachIndexedNoAlloc { i, it ->
                        it.crossPos = crossPos + thisRow + ((i - 1) * gap + i * it.crossSize)
                    }
                }

                AlignContent.Stretch -> {
                    drawables.forEachNoAlloc {
                        it.crossPos = 0F
                        it.crossSize = crossSize / numRows
                    }
                }
            }
            when (itemAlign) {
                AlignItems.Start -> {
                    drawables.forEachNoAlloc { it.crossPos += 0F }
                }

                AlignItems.End -> {
                    drawables.forEachNoAlloc { it.crossPos += crossSize - it.crossSize }
                }

                AlignItems.Center -> {
                    drawables.forEachNoAlloc { it.crossPos += (crossSize - it.crossSize) / 2 }
                }

                AlignItems.Stretch -> {
                    drawables.forEachNoAlloc { it.crossSize = crossSize / numRows }
                }
            }
            return maxCrossSize
        }

        fun justifyContent() {
            when (contentJustify) {
                JustifyContent.Start -> {
                    var pos = 0F
                    drawables.forEachNoAlloc {
                        it.mainPos = pos + mainPos
                        pos += it.mainSize + mainGap
                    }
                }

                JustifyContent.End -> {
                    var pos = mainPos + mainSize
                    drawables.forEachNoAlloc {
                        pos -= it.mainSize
                        it.mainPos = pos
                        pos -= mainGap
                    }
                }

                JustifyContent.Center -> {
                    var pos = (mainSize - thisMainSize) / 2
                    drawables.forEachNoAlloc {
                        it.mainPos = pos + mainPos
                        pos += it.mainSize + mainGap
                    }
                }

                JustifyContent.SpaceBetween -> {
                    if (drawables.size == 1) {
                        drawables.forEachNoAlloc { it.mainPos = 0F }
                    } else {
                        val gap = drawables.sumOf { it.mainSize } / (drawables.size - 1)
                        var pos = mainPos
                        drawables.forEachNoAlloc {
                            it.mainPos = pos
                            pos += it.mainSize + gap
                        }
                    }
                }

                JustifyContent.SpaceEvenly -> {
                    if (drawables.size == 1) {
                        drawables.forEachNoAlloc { it.mainPos = 0F }
                    } else {
                        val gap = (mainSize - drawables.sumOf { it.mainSize }) / drawables.size
                        var pos = gap + mainPos
                        drawables.forEachNoAlloc {
                            it.mainPos = pos
                            pos += it.mainSize + gap
                        }
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
        Start, End, Center, Stretch
    }

    /** [Flex Align Content](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-align-content) */
    enum class AlignContent {
        Start, End, Center, SpaceBetween, SpaceEvenly, Stretch
    }

    /** [Row Gap Column Gap](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-gap-row-gap-column-gap) */
    data class Gap(val mainGap: Unit.Pixel, val crossGap: Unit.Pixel) {
        companion object {
            @JvmField
            val Auto = Gap(5F.px(), 5F.px())
        }
    }

}