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

package org.polyfrost.polyui.data

import org.polyfrost.polyui.utils.getByName
import org.polyfrost.polyui.utils.names

/**
 * # Font
 *
 * A font used by the rendering implementation. As with other PolyUI resources, the actual
 * loading, management, etc. of the font is NOT managed in this class in any way. This class just contains
 * metadata for usage with PolyUI.
 *
 * @param resourcePath The resource of the font, can be a URL (see [here][org.polyfrost.polyui.utils.getResourceStream], you should use this or an equivalent method)
 * @param letterSpacing The letter spacing of the font, in pixels (e.g. 1 pixel = 1 empty pixel between each letter).
 * @param lineSpacing The line spacing of the font, in proportion to the font size (e.g. 2 means 1 empty line between each line, 1.5 = half a line between...)
 * @param family a reference to the family that encloses this font. Only set if this font was retrieved from a method like [FontFamily.get] or similar.
 */
class Font @JvmOverloads constructor(
    resourcePath: String,
    @Transient
    val letterSpacing: Float = 0f,
    @Transient
    val lineSpacing: Float = 1.2f,
    val family: FontFamily? = null,
    @Transient
    val italic: Boolean = resourcePath.contains("italic", ignoreCase = true),
    @Transient
    val weight: Weight = Weight.entries.getByName(resourcePath.findLastAnyOf(Weight.entries.names(), ignoreCase = true)?.second) ?: Weight.Regular,
) : Resource(resourcePath) {
    private constructor() : this("")

    val name get() = resourcePath.substringAfterLast('/').substringBeforeLast('.')

    fun getAtStyle(weight: Weight, italic: Boolean): Font {
        require(family != null) { "getAtStyle() only works if this font is aware of its family" }
        return family.get(weight, italic)
    }

    // improves memory usage so fonts can use the same data object
    override fun hashCode() = resourcePath.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Font) return false

        return resourcePath == other.resourcePath && weight == other.weight && italic == other.italic
    }

    /**
     * Enum representing weights available to a font in PolyUI.
     * @param value the weight of the font, as specified by the [Google Fonts CSS v2 API](https://fonts.google.com/knowledge/glossary/weight_axis).
     * @param fb in the case that the given weight is unavailable, this will be used instead.
     * @since 1.0.7
     */
    enum class Weight(val value: Int, val fb: Weight?) {
        Regular(400, null),
        Bold(700, null),

        Medium(500, Regular),
        Light(300, Regular),

        SemiBold(600, Bold),
        ExtraBold(800, Bold),
        Black(900, ExtraBold),

        ExtraLight(200, Light),
        Thin(100, ExtraLight),
    }

    companion object {

        /**
         * Get a Weight instance given the integer weight,
         * as specified by the [Google Fonts CSS v2 API](https://fonts.google.com/knowledge/glossary/weight_axis).
         *
         * Weights that are not whole numbers will be floored to the nearest 100.
         * @since 1.0.7
         */
        @JvmStatic
        fun byWeight(weight: Int): Weight {
            return when (weight / 100) {
                1 -> Weight.Thin
                2 -> Weight.ExtraLight
                3 -> Weight.Light
                5 -> Weight.Medium
                6 -> Weight.SemiBold
                7 -> Weight.Bold
                8 -> Weight.ExtraBold
                9 -> Weight.Black
                else -> Weight.Regular
            }
        }
    }
}
