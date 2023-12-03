/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
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

package org.polyfrost.polyui.component

import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.makeRelative
import org.polyfrost.polyui.utils.printInfo
import kotlin.math.abs
import kotlin.math.max

/**
 * Positioning strategies are the methods used in PolyUI to place both components across the screen,
 * and components inside a layout.
 */
fun interface Positioner {
    fun position(drawable: Drawable)

    class Default : Positioner {
        override fun position(drawable: Drawable) {
            if (drawable.size.isZero) drawable.size = sizeCalculate(drawable)
            if (drawable.children == null) return

            val align = drawable.alignment
            var maxMain = 0f
            var ignored = 0
            val horizontal = drawable.alignment.mode == Align.Mode.Horizontal
            drawable.children!!.fastEach {
                val at = it.at
                if (it.size.isZero) it.size = sizeCalculate(it)
                val sz = it.visibleSize
                if (at is Vec2.Sourced && !at.sourced) at.source = drawable.size
                if (sz is Vec2.Sourced && !sz.sourced) sz.source = drawable.size
                it.at = it.at.makeRelative(drawable.at)
//                if (sz > drawable.size) throw IllegalArgumentException("object is larger than container")

                if (it.size.isNegative) {
                    ignored++
                    return@fastEach
                }
                maxMain += if (horizontal) sz.x else sz.y
            }
            if (horizontal) maxMain += align.padding.x * (drawable.children!!.size - ignored)
            align(drawable, maxMain)
        }

        fun align(drawable: Drawable, maxMain: Float) {
            val size = drawable.size
            val align: Align = drawable.alignment
            val pad = align.padding
            val crs = if (align.mode == Align.Mode.Horizontal) 1 else 0
            val main = abs(crs - 1)
            val children = drawable.children!!

            when (align.main) {
                Align.Main.Left -> {
                    var current = pad[main]
                    children.fastEach {
                        if (it.at.isNegative) return@fastEach
                        it.at[main] = current
                        current += it.visibleSize[main] + pad[main]
                        alignCross(align, it, crs, size, pad)
                    }
                }

                Align.Main.Center -> {
                    var current = (size[main] + pad[main]) / 2f - maxMain / 2f
                    children.fastEach {
                        if (it.at.isNegative) return@fastEach
                        it.at[main] = current
                        current += it.visibleSize[main] + pad[main]
                        alignCross(align, it, crs, size, pad)
                    }
                }

                Align.Main.Right -> {
                    var current = size[main]
                    children.fastEach {
                        if (it.at.isNegative) return@fastEach
                        it.at[main] = current
                        current -= it.visibleSize[main]
                        alignCross(align, it, crs, size, pad)
                    }
                }

                Align.Main.Spread -> {
                    val gapWidth = (size[main] - maxMain) / (children.size + 1)
                    var current = 0f
                    children.fastEach {
                        if (it.at.isNegative) return@fastEach
                        it.at[main] = current
                        current += it.visibleSize[main] + gapWidth
                        alignCross(align, it, crs, size, pad)
                    }
                }
            }
        }

        fun alignCross(align: Align, it: Drawable, crs: Int, size: Vec2, pad: Vec2) {
            when (align.cross) {
                Align.Cross.Top -> {}
                Align.Cross.Middle -> {
                    it.at[crs] = (size[crs]) / 2f - (it.visibleSize[crs] / 2f)
                }

                Align.Cross.Bottom -> {
                    it.at[crs] = size[crs] - (it.visibleSize[crs]) - pad[crs]
                }
            }
        }

        private fun sizeFromChildren(drawable: Drawable) {
            val pad = drawable.alignment.padding
            val crs = if (drawable.alignment.mode == Align.Mode.Horizontal) 1 else 0
            val main = abs(crs - 1)
            val totalSize = Vec2(pad[main], pad[crs])
            drawable.children?.fastEach {
                val sz = sizeCalculate(it)
                if (it.at.isNegative) return@fastEach
                val at = it.at
                // should be safe:tm:
                if (at.isZero) {
                    totalSize[main] += sz[main] + pad[main]
                } else {
                    totalSize[main] = max(totalSize[main], at[main] + sz[main] + pad[main])
                }
                totalSize[crs] = max(totalSize[crs], sz[crs] + pad[crs])
            } ?: throw IllegalArgumentException("no children")
            drawable.size = totalSize
        }

        private fun sizeCalculate(it: Drawable): Vec2 {
            if (it.size.isZero) {
                val sz = it.calculateSize()
                if (sz == null && it.children?.isNotEmpty() == true) {
                    sizeFromChildren(it)
                    // todo backlog: change this: allow other children to provide a size for it??
                } else {
                    if (sz == null) {
                        printInfo(it)
                        throw IllegalArgumentException("Cannot infer size of $it: does not implement calculateSize(), please specify a size")
                    }
                    it.size = sz
                }
            }
            return it.size
        }
    }
}
