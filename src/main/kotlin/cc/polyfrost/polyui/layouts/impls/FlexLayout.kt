package cc.polyfrost.polyui.layouts.impls

import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Units
import cc.polyfrost.polyui.utils.UnitUtils
import cc.polyfrost.polyui.utils.moveElement
import cc.polyfrost.polyui.utils.px

@Suppress("unused")

/** # FlexLayout
 *
 * a flex layout that implements all the features of the [Flexbox](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-flexbox-properties) system.
 *
 * This layout is very powerful, and is used commonly in web development. If you need to know more, I would recommend checking out the link above.
 */
class FlexLayout(
    at: Point<Unit>, sized: Size<Unit>? = null,
    val flexDirection: Direction = Direction.Row,
    val flexWrap: Wrap = Wrap.NoWrap,
    val contentJustify: JustifyContent = JustifyContent.Start,
    val itemAlign: AlignItems = AlignItems.Start,
    val contentAlign: AlignContent = AlignContent.Normal,
    val gap: Gap = Gap.Auto,
    vararg things: Drawable,
) : Layout(at, sized, *things) {
    override val atUnitType: Unit.Type = Units.Flex

    // list of ROWS.
    var matrix: Array<Array<Drawable>>

    init {
        things.forEachIndexed { i, it ->
            if (it.sizedUnitType() == Units.Flex) {
                throw Exception("A flex layout's size cannot be an index flex type. Use a pixel type.")
            }
            if (it.at != UnitUtils.FlexAuto) {
                things.moveElement(i, (it.at.a as Unit.Flex).index)
            }
        }
        // just put everything in there for now.
        matrix = Array(1) { things.asList().toTypedArray() }
    }

    override fun calculateBounds() {
        components.forEach {
            it.calculateBounds()
        }
        children.forEach {
            it.calculateBounds()
        }
        if (flexWrap != Wrap.NoWrap) {
            val things = matrix[0]
            if (flexWrap == Wrap.WrapReverse) things.reversedArray()

        }

        needsRecalculation = false
    }

    fun put(thing: Drawable) {

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
        Start, End, Center, Baseline, Stretch
    }

    /** [Flex Align Content](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-align-content) */
    enum class AlignContent {
        Normal, FlexStart, FlexEnd, Center, SpaceBetween, SpaceAround, Stretch
    }

    /** [Row Gap Column Gap](https://css-tricks.com/snippets/css/a-guide-to-flexbox/#aa-gap-row-gap-column-gap) */
    data class Gap(val rowGap: Unit.Pixel, val columnGap: Unit.Pixel) {
        companion object {
            @JvmField
            val Auto = Gap(5F.px(), 5F.px())
        }
    }

}