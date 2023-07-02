/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

package cc.polyfrost.polyui.layout.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import cc.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.property.PropertyManager
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
    propertyManager: PropertyManager? = null,
    private val flexDirection: Direction = Direction.Row,
    wrapDirection: Wrap = Wrap.Wrap,
    private val contentJustify: JustifyContent = JustifyContent.Start,
    private val itemAlign: AlignItems = AlignItems.Start,
    private val contentAlign: AlignContent = AlignContent.Start,
    gap: Gap = Gap.Default,
    vararg drawables: Drawable
) : Layout(at, size, onAdded, onRemoved, propertyManager, false, false, false, *drawables) {
    constructor(at: Point<Unit>, wrap: Unit.Percent, vararg drawables: Drawable) : this(
        at,
        null,
        wrap,
        null,
        null,
        drawables = drawables
    )

    private val drawables: ArrayList<FlexDrawable>

    private var mainGap = when (flexDirection) {
        Direction.Row, Direction.RowReverse -> gap.mainGap.px
        Direction.Column, Direction.ColumnReverse -> gap.crossGap.px
    }

    private var crossGap = when (flexDirection) {
        Direction.Row, Direction.RowReverse -> gap.crossGap.px
        Direction.Column, Direction.ColumnReverse -> gap.mainGap.px
    }
    private val wrapDirection: Wrap
    private val strictSize = size != null && wrap == null

    init {
        if (this.size == null) this.size = origin
        if (wrapDirection != Wrap.NoWrap) {
            if (wrap != null) {
                PolyUI.LOGGER.debug("[Flex] wrap is set, but wrap direction is set to NoWrap. Defaulting to Wrap.")
                this.wrapDirection = Wrap.Wrap
                when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> this.size = Size(wrap, size?.b ?: 0.px)
                    Direction.Column, Direction.ColumnReverse -> this.size = Size(size?.a ?: 0.px, wrap)
                }
            } else if (size != null) {
                PolyUI.LOGGER.debug("[Flex] size is set, but wrap direction is set to NoWrap. Defaulting to Wrap.")
                this.wrapDirection = Wrap.Wrap
            } else {
                this.wrapDirection = wrapDirection
            }
        } else {
            this.wrapDirection = wrapDirection
        }
        drawables.forEachIndexed { i, it ->
            if (it.atType != Unit.Type.Flex) {
                throw Exception("Unit type mismatch: Drawable $it needs to be placed using a Flex unit for a flex layout.")
            }
            if (it.sizeType == Units.Flex) {
                throw Exception("A flex layout's size property is used to specify the minimum size of the component, please use the at property for your flex data.")
            }
            @Suppress("UNCHECKED_CAST") // already type-checked
            if ((it.at as Point<Unit.Flex>).a.index >= 0) {
                drawables.moveElement(i, (it.at.a as Unit.Flex).index)
            }
            if (it is Component) it.layout = this
            if (it is Layout) it.layout = this
        }
        this.drawables = drawables.map { FlexDrawable(it, it.at.a as Unit.Flex, it.size) } as ArrayList<FlexDrawable>
        if (wrapDirection == Wrap.WrapReverse) this.drawables.reverse()
    }

    private var crossSize: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> height
                Direction.Column, Direction.ColumnReverse -> width
            }
        }
        set(value) {
            if (strictSize) return
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> height = value
                Direction.Column, Direction.ColumnReverse -> width = value
            }
        }
    private var mainSize: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> width
                Direction.Column, Direction.ColumnReverse -> height
            }
        }
        set(value) {
            if (strictSize) return
            when (flexDirection) {
                Direction.Row, Direction.RowReverse -> width = value
                Direction.Column, Direction.ColumnReverse -> height = value
            }
        }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        if (resizesChildren) {
            mainGap *= scaleX
            crossGap *= scaleY
        }
    }

    /**
     * Shuffles the drawables in this layout.
     */
    fun shuffle() {
        drawables.shuffle()
        calculateBounds()
        if (fbo != null) {
            renderer.deleteFramebuffer(fbo)
            fbo = renderer.createFramebuffer(width, height)
        }
        needsRedraw = true
    }

    override fun calculateBounds() {
        if (initStage == INIT_NOT_STARTED) throw IllegalStateException("${this.simpleName} has not been setup, but calculateBounds() was called!")
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
                        return@run
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
        (rows.sortedByDescending { it.rowMainSizeWithGaps }).fastEach {
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
        drawables.fastEach {
            it.drawable.calculateBounds()
        }
        if (initStage != INIT_COMPLETE) {
            initStage = INIT_COMPLETE
            onInitComplete()
        }
    }

    fun trySetMainSize(new: Float) {
        mainSize = max(mainSize, new)
    }

    private inner class FlexDrawable(val drawable: Drawable, val flex: Unit.Flex, minSize: Size<Unit>? = null) {
        inline val size get() = drawable.size!!

        init {
            if (drawable.size == null) drawable.size = calculateSize() ?: minSize ?: origin
        }

        /** x */
        var mainPos: Float
            get() {
                return when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> drawable.x
                    Direction.Column, Direction.ColumnReverse -> drawable.y
                }
            }
            set(value) = when (flexDirection) {
                Direction.Row, Direction.RowReverse -> drawable.x = value
                Direction.Column, Direction.ColumnReverse -> drawable.y = value
            }

        /** y */
        var crossPos: Float
            get() {
                return when (flexDirection) {
                    Direction.Row, Direction.RowReverse -> drawable.y
                    Direction.Column, Direction.ColumnReverse -> drawable.x
                }
            }
            set(value) = when (flexDirection) {
                Direction.Row, Direction.RowReverse -> drawable.y = value
                Direction.Column, Direction.ColumnReverse -> drawable.x = value
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
        val rowMainSize: Float
        val maxCrossSize: Float
        val rowMainSizeWithGaps: Float

        init {
            var main = 0F
            drawables.fastEach { main += it.mainSize }
            this.rowMainSize = main
            this.rowMainSizeWithGaps = main + (drawables.size - 1) * mainGap
            resizeMainIfCan()

            var cross = 0F
            drawables.fastEach { cross = max(cross, it.crossSize) }
            this.maxCrossSize = cross
        }

        fun resizeMainIfCan() {
            val spare = mainSize - rowMainSize
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
            trySetMainSize(rowMainSizeWithGaps)
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
                    var pos = (mainSize - rowMainSizeWithGaps) / 2
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
                    val gap = (mainSize - rowMainSize) / (drawables.size - 1)
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
                    val gap = (mainSize - rowMainSize) / drawables.size
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
