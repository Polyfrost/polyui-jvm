package cc.polyfrost.polyui.layouts.impls

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.units.*
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.utils.*
import kotlin.Exception
import kotlin.Float
import kotlin.Int
import kotlin.Suppress

@Suppress("unused")

/** # FlexLayout
 *
 * a flex layout that implements all the features of the [Flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-flexbox-properties) system.
 *
 * This layout is very powerful, and is used commonly in web development. If you need to know more, I would recommend checking out the link above.
 */
class FlexLayout(
    at: Point<Unit>, sized: Size<Unit>? = null,
    private val wrap: Unit.Concrete? = null,
    onAdded: (Drawable.() -> kotlin.Unit)? = null,
    onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    val flexDirection: Direction = Direction.Row,
    val wrapDirection: Wrap = Wrap.NoWrap,
    val contentJustify: JustifyContent = JustifyContent.Start,
    val itemAlign: AlignItems = AlignItems.Start,
    val contentAlign: AlignContent = AlignContent.Start,
    val gap: Gap = Gap.Auto,
    vararg items: Drawable,
) : Layout(at, sized, onAdded, onRemoved, *items) {
    private val drawables: ArrayList<FlexDrawable>

    init {
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
        // todo fix this lol
        var mainAxis = 0F
        var crossAxis = 0F

        val list = ArrayList<FlexDrawable>()
        if (wrapDirection != Wrap.NoWrap) {
            val wrap = when (flexDirection) {
                Direction.Row, Direction.RowReverse -> sized?.width() ?: (wrap as Unit?)?.px
                Direction.Column, Direction.ColumnReverse -> sized?.height() ?: (wrap as Unit?)?.px
            } ?: throw Exception("wrap direction is set to wrap, but no wrap size is specified")
            drawables.forEachNoAlloc {
                if (it.drawable.sized == null) it.drawable.sized = getSize()

                if (mainAxis + it.lengthOrMin() + gapMain() > wrap) { // means we need to wrap
                    val spareSpace = wrap - mainAxis
                    mainAxis = 0F

                    // calculate cross stuff
                    val maxCross = list.sumOf { it.crossLengthOrMin() } + (gapCross() * (list.size - 1))
                    var spareCrossSpace = cross() - maxCross
                    if (spareCrossSpace < 0) {
                        PolyUI.LOGGER.warn("FlexLayout: Not enough space to fit all items in the cross axis. (cross: ${cross()}, maxCross: $maxCross")
                        spareCrossSpace = 0F
                    }
                    var cross = 0
                    expandMainIfCan(list, spareSpace)
                    list.forEachNoAlloc {
                        // set cross size and position
                        it.setCrossPos(crossAxis, list.size, spareCrossSpace, cross)
                        if (contentAlign == AlignContent.Stretch) {
                            it.setCrossSize(cross() / list.size)
                        }
                        cross++
                        crossAxis += it.crossLengthOrMin() + gapCross()

                        // set main size and position
                        it.setMainPos(mainAxis, list.size, spareSpace)
                        mainAxis += it.lengthOrMin() + gapMain()
                    }


                    mainAxis = 0F
                }

                list.add(it)
            }
            return
        }
        // no-wrap:
        drawables.forEachNoAlloc {
            //it.flex.px =
        }
        this.sized = Size(
            main().px(),
            crossAxis.px()
        )
        needsRecalculation = false
    }


    inner class FlexDrawable(val drawable: Drawable, val flex: Unit.Flex, val minSize: Size<Unit>? = null) {
        fun lengthOrMin(): Float {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> {
                    drawable.sized?.width() ?: (flex.flexBasis as Unit).px
                }

                Direction.Column, Direction.ColumnReverse -> {
                    drawable.sized?.height() ?: (flex.flexBasis as Unit).px
                }
            }
        }

        fun crossLengthOrMin(): Float {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> {
                    drawable.sized?.height() ?: (flex.flexBasis as Unit).px
                }

                Direction.Column, Direction.ColumnReverse -> {
                    drawable.sized?.width() ?: (flex.flexBasis as Unit).px
                }
            }
        }

        fun setCrossSize(size: Float) {
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> {
                    drawable.sized?.b?.px = size
                }

                Direction.Column, Direction.ColumnReverse -> {
                    drawable.sized?.a?.px = size
                }
            }
        }

        fun crossPos(): Float {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> {
                    drawable.at.b.px
                }

                Direction.Column, Direction.ColumnReverse -> {
                    drawable.at.a.px
                }
            }
        }

        fun mainPos(): Float {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> {
                    drawable.at.a.px
                }

                Direction.Column, Direction.ColumnReverse -> {
                    drawable.at.b.px
                }
            }
        }

        fun setCrossPos(currentCross: Float, amount: Int, spare: Float, row: Int) {
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> {
                    drawable.at.b.px = setCrossPos0(currentCross, amount, spare, row)
                }

                Direction.Column, Direction.ColumnReverse -> {
                    drawable.at.a.px = setCrossPos0(currentCross, amount, spare, row)
                }
            }
        }

        private fun setCrossPos0(currentCross: Float, amount: Int, spare: Float, row: Int): Float {
            return when (contentAlign) {
                AlignContent.Start -> currentCross
                AlignContent.End -> currentCross + spare
                AlignContent.Center -> currentCross + (spare / 2)
                AlignContent.SpaceBetween -> {
                    if (amount == 1) currentCross
                    else currentCross + (spare / (amount - 1)) * row
                }

                AlignContent.SpaceAround -> {
                    if (amount == 1) currentCross
                    else currentCross + (spare / (amount + 1)) * (row + 1)
                }

                AlignContent.SpaceEvenly -> {
                    if (amount == 1) currentCross
                    else currentCross + (spare / (amount + 1)) * (row + 1)
                }

                AlignContent.Stretch -> {
                    if (amount == 1) currentCross
                    else currentCross + (spare / (amount - 1)) * row
                }
            }
        }

        fun setMainPos(currentMain: Float, amount: Int, spare: Float) {
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> {
                    drawable.at.a.px = setMainPos0(currentMain, amount, spare)
                }

                Direction.Column, Direction.ColumnReverse -> {
                    drawable.at.b.px = setMainPos0(currentMain, amount, spare)
                }
            }
        }

        private fun setMainPos0(currentMain: Float, amount: Int, spare: Float): Float {
            return when (contentAlign) {
                AlignContent.Start -> currentMain
                AlignContent.End -> currentMain + spare
                AlignContent.Center -> currentMain + (spare / 2)
                AlignContent.SpaceBetween -> {
                    if (amount == 1) currentMain
                    else currentMain + (spare / (amount - 1))
                }

                AlignContent.SpaceAround -> {
                    if (amount == 1) currentMain
                    else currentMain + (spare / (amount + 1))
                }

                AlignContent.SpaceEvenly -> {
                    if (amount == 1) currentMain
                    else currentMain + (spare / (amount + 1))
                }

                AlignContent.Stretch -> {
                    if (amount == 1) currentMain
                    else currentMain + (spare / (amount - 1))
                }
            }
        }

    }


    private fun expandMainIfCan(list: ArrayList<FlexDrawable>, spare: Float) {
        var i = 0
        list.forEachNoAlloc { i += it.flex.flexGrow }
        if (i == 0) return
        list.forEachNoAlloc {
            val grow = it.flex.flexGrow
            if (grow == 0) return@forEachNoAlloc
            val add = spare * (grow / i)
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> {
                    it.drawable.sized!!.a.px = it.drawable.sized!!.a.px.plus(add)
                }

                Direction.Column, Direction.ColumnReverse -> {
                    it.drawable.sized!!.b.px = it.drawable.sized!!.b.px.plus(add)
                }
            }
        }
    }

    private fun gapMain(): Float {
        return when (flexDirection) {
            Direction.Row, Direction.RowReverse -> {
                gap.mainGap.px
            }

            Direction.Column, Direction.ColumnReverse -> {
                gap.crossGap.px
            }
        }
    }

    private fun gapCross(): Float {
        return when (flexDirection) {
            Direction.Row, Direction.RowReverse -> {
                gap.crossGap.px
            }

            Direction.Column, Direction.ColumnReverse -> {
                gap.mainGap.px
            }
        }
    }

    private fun cross(): Float {
        return when (flexDirection) {
            Direction.Row, Direction.RowReverse -> {
                sized?.height() ?: (wrap as Unit).px
            }

            Direction.Column, Direction.ColumnReverse -> {
                sized?.width() ?: 0F
            }
        }
    }

    fun main(): Float {
        return when (flexDirection) {
            Direction.Row, Direction.RowReverse -> {
                sized?.width() ?: (wrap as Unit).px
            }

            Direction.Column, Direction.ColumnReverse -> {
                sized?.height() ?: 0F
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
        Start, End, Center, SpaceBetween, SpaceAround, SpaceEvenly
    }

    /** [Flex Align Items](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-align-items) */
    enum class AlignItems {
        Start, End, Center, Stretch
    }

    /** [Flex Align Content](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-align-content) */
    enum class AlignContent {
        Start, End, Center, SpaceBetween, SpaceAround, SpaceEvenly, Stretch
    }

    /** [Row Gap Column Gap](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-gap-row-gap-column-gap) */
    data class Gap(val mainGap: Unit.Pixel, val crossGap: Unit.Pixel) {
        companion object {
            @JvmField
            val Auto = Gap(5F.px(), 5F.px())
        }
    }

}