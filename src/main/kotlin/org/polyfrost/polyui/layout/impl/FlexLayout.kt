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

package org.polyfrost.polyui.layout.impl

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import org.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.event.Added
import org.polyfrost.polyui.event.Removed
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.property.PropertyManager
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.sortedByDescending
import kotlin.Boolean
import kotlin.Exception
import kotlin.Float
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.Suppress
import kotlin.math.max
import kotlin.run

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
    onAdded: (Drawable.(Added) -> kotlin.Unit)? = null,
    onRemoved: (Drawable.(Removed) -> kotlin.Unit)? = null,
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
    val flexDrawables = drawables.filter { it.at.a is Unit.Flex } as ArrayList

    private val isSizedCross get() = crossSize != 0f

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
        drawables.forEach {
            if (it.atType != Unit.Type.Flex) {
                throw Exception("Unit type mismatch: Drawable $it needs to be placed using a Flex unit for a flex layout.")
            }
            if (it.sizeType == Units.Flex) {
                throw Exception("A flex layout's size property is used to specify the minimum size of the component, please use the at property for your flex data.")
            }
        }
    }

    private var crossSize: Float
        get() {
            return when (flexDirection) {
                Direction.Row, Direction.RowReverse -> height
                Direction.Column, Direction.ColumnReverse -> width
            }
        }
        set(value) {
            if (strictSize && isSizedCross) return
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
        flexDrawables.shuffle()
        calculateBounds()
    }

    override fun addComponent(drawable: Drawable) {
        super.addComponent(drawable)
        if (drawable.at.a is Unit.Flex) flexDrawables.add(drawable)
        if (initStage != INIT_NOT_STARTED) calculateBounds()
    }

    override fun removeComponent(index: Int) {
        val c = components[index]
        if (c.at.a is Unit.Flex) {
            super.removeComponent(c)
            flexDrawables.remove(c)
        }
    }

    override fun removeComponentNow(index: Int) {
        val c = components[index]
        if (c.at.a is Unit.Flex) {
            removeComponentNow(c)
            flexDrawables.remove(c)
        }
    }

    override fun removeComponent(drawable: Drawable) {
        super.removeComponent(drawable)
        calculateBounds()
    }

    override fun removeComponentNow(drawable: Drawable?) {
        super.removeComponentNow(drawable)
        flexDrawables.remove(drawable)
        calculateBounds()
    }

    override fun calculateBounds() {
        if (initStage == INIT_NOT_STARTED) throw IllegalStateException("${this.simpleName} has not been setup, but calculateBounds() was called!")
        doDynamicSize()
        var mainAxis = 0f
        val rows = arrayListOf<FlexRow>()

        if (wrapDirection != Wrap.NoWrap) {
            var row = arrayListOf<Drawable>()
            flexDrawables.forEachIndexed { i, it ->
                mainAxis += getMainSize(it) + mainGap
                if (getFlex(it).endRowAfter || mainAxis + getMainSize(flexDrawables.getOrNull(i + 1)) >= mainSize) { // means we need to wrap
                    rows.add(FlexRow(row))
                    mainAxis = 0f
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
            rows.add(FlexRow(flexDrawables))
        }

        var maxCrossSizeNoGaps = 0f
        var minIndex = 0
        var err = false
        run {
            rows.fastEach {
                maxCrossSizeNoGaps += it.maxCrossSize
                minIndex += it.drawables.size
                if (strictSize && isSizedCross) {
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
            for (i in minIndex until flexDrawables.size) {
                val d = flexDrawables[i]
                if (d is Component) {
                    components.remove(d)
                } else {
                    children.remove(d)
                }
            }
        }
        crossSize = maxCrossSizeNoGaps + (rows.size - 1) * crossGap
        var cross = 0f

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
        components.fastEach {
            it.calculateBounds()
        }
        children.fastEach {
            it.calculateBounds()
        }
        if (initStage != INIT_COMPLETE) {
            initStage = INIT_COMPLETE
            onInitComplete()
        }
        if (fbo != null) {
            renderer.delete(fbo)
            fbo = renderer.createFramebuffer(width, height)
        }
        needsRedraw = true
    }

    fun trySetMainSize(new: Float) {
        mainSize = max(mainSize, new)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun init(drawable: Drawable) {
        if (drawable.size == null) drawable.size = calculateSize() ?: origin
    }

    fun getMainPos(drawable: Drawable): Float {
        return when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.x
            Direction.Column, Direction.ColumnReverse -> drawable.y
        }
    }

    fun setMainPos(drawable: Drawable, value: Float) {
        when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.x = value
            Direction.Column, Direction.ColumnReverse -> drawable.y = value
        }
    }

    fun getCrossPos(drawable: Drawable?): Float {
        if (drawable == null) return 0f
        return when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.y
            Direction.Column, Direction.ColumnReverse -> drawable.x
        }
    }

    fun setCrossPos(drawable: Drawable, value: Float) {
        when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.y = value
            Direction.Column, Direction.ColumnReverse -> drawable.x = value
        }
    }

    fun addCrossPos(drawable: Drawable, value: Float) {
        when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.y += value
            Direction.Column, Direction.ColumnReverse -> drawable.x += value
        }
    }

    fun getMainSize(drawable: Drawable?): Float {
        if (drawable == null) return 0f
        return when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.width
            Direction.Column, Direction.ColumnReverse -> drawable.height
        }
    }

    fun setMainSize(drawable: Drawable, value: Float) {
        when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.width = value
            Direction.Column, Direction.ColumnReverse -> drawable.height = value
        }
    }

    fun addMainSize(drawable: Drawable, value: Float) {
        when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.width += value
            Direction.Column, Direction.ColumnReverse -> drawable.height += value
        }
    }

    fun getCrossSize(drawable: Drawable): Float {
        return when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.height
            Direction.Column, Direction.ColumnReverse -> drawable.width
        }
    }

    fun setCrossSize(drawable: Drawable, value: Float) {
        when (flexDirection) {
            Direction.Row, Direction.RowReverse -> drawable.height = value
            Direction.Column, Direction.ColumnReverse -> drawable.width = value
        }
    }

    fun isMainSized(drawable: Drawable): Boolean {
        return if (getMainSize(drawable) != 0f) {
            true
        } else {
            if (wrapDirection != Wrap.NoWrap) {
                PolyUI.LOGGER.warn(
                    "[Flex] Drawable {} has a main size of 0. This may lead to odd things on wrapped layouts.",
                    drawable
                )
            }
            false
        }
    }

    fun getFlex(drawable: Drawable): Unit.Flex {
        return drawable.at.a as Unit.Flex
    }

    private inner class FlexRow(
        val drawables: ArrayList<Drawable>
    ) {
        val rowMainSize: Float
        val maxCrossSize: Float
        val rowMainSizeWithGaps: Float

        init {
            var main = 0f
            drawables.fastEach { main += getMainSize(it) }
            this.rowMainSize = main
            this.rowMainSizeWithGaps = main + (drawables.size - 1) * mainGap
            resizeMainIfCan()

            var cross = 0f
            drawables.fastEach { cross = max(cross, getCrossSize(it)) }
            this.maxCrossSize = cross
        }

        fun resizeMainIfCan() {
            val spare = mainSize - rowMainSize
            var i = 0
            if (spare > 0) {
                drawables.fastEach { i += getFlex(it).flexGrow }
            } else {
                drawables.fastEach { i += getFlex(it).flexShrink }
            }
            if (i == 0) return
            drawables.fastEach {
                val grow = getFlex(it).flexGrow
                if (grow == 0) return@fastEach
                addMainSize(it, spare * (grow / i))
            }
        }

        fun align(numRows: Int, thisRowCrossPos: Float, allMaxCrossSize: Float) {
            when (contentAlign) {
                AlignContent.Start -> {
                    drawables.fastEach { setCrossPos(it, thisRowCrossPos) }
                }

                AlignContent.End -> {
                    drawables.fastEach { setCrossPos(it, thisRowCrossPos + (crossSize - getCrossSize(it))) }
                }

                AlignContent.Center -> {
                    drawables.fastEach { setCrossPos(it, thisRowCrossPos + (crossSize - getCrossSize(it)) / 2) }
                }

                AlignContent.SpaceBetween -> {
                    val gap = (crossSize - allMaxCrossSize) / (numRows - 1)
                    drawables.fastEach {
                        setCrossPos(it, thisRowCrossPos + gap)
                    }
                }

                AlignContent.SpaceEvenly -> {
                    val gap = (crossSize - allMaxCrossSize) / (numRows + 1)
                    drawables.fastEach {
                        setCrossPos(it, thisRowCrossPos + gap)
                    }
                }

                AlignContent.Stretch -> {
                    drawables.fastEach {
                        setCrossPos(it, thisRowCrossPos)
                        setCrossSize(it, maxCrossSize)
                    }
                }
            }
            when (itemAlign) {
                AlignItems.Start -> {
                }

                AlignItems.End -> {
                    drawables.fastEach { addCrossPos(it, maxCrossSize - getCrossSize(it)) }
                }

                AlignItems.Center -> {
                    drawables.fastEach { addCrossPos(it, (maxCrossSize - getCrossSize(it)) / 2f) }
                }
            }
        }

        fun justify() {
            trySetMainSize(rowMainSizeWithGaps)
            when (contentJustify) {
                JustifyContent.Start -> {
                    var pos = 0f
                    drawables.fastEach {
                        setMainPos(it, pos)
                        pos += getMainSize(it) + mainGap
                    }
                }

                JustifyContent.End -> {
                    var pos = mainSize
                    drawables.fastEach {
                        pos -= getMainSize(it)
                        setMainPos(it, pos)
                        pos -= mainGap
                    }
                }

                JustifyContent.Center -> {
                    var pos = (mainSize - rowMainSizeWithGaps) / 2
                    drawables.fastEach {
                        setMainPos(it, pos)
                        pos += getMainSize(it) + mainGap
                    }
                }

                JustifyContent.SpaceBetween -> {
                    if (drawables.size == 1) {
                        setMainPos(drawables[0], (mainSize - getMainSize(drawables[0])) / 2f)
                        return
                    }
                    val gap = (mainSize - rowMainSize) / (drawables.size - 1)
                    var pos = 0f
                    drawables.fastEach {
                        setMainPos(it, pos)
                        pos += getMainSize(it) + gap
                    }
                }

                JustifyContent.SpaceEvenly -> {
                    if (drawables.size == 1) {
                        setMainPos(drawables[0], (mainSize - getMainSize(drawables[0])) / 2f)
                        return
                    }
                    val gap = (mainSize - rowMainSize) / drawables.size
                    var pos = gap
                    drawables.fastEach {
                        setMainPos(it, pos)
                        pos += getMainSize(it) + gap
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
