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

package org.polyfrost.polyui.renderer.data

import java.net.URL

/**
 * # Font
 *
 * A font used by the rendering implementation. The font is lazily loaded by the renderer.
 *
 * @param resourcePath The resource of the font, can be a URL (see [here][org.polyfrost.polyui.utils.getResourceStream], you should use this or an equivalent method)
 * @param letterSpacing The letter spacing of the font, in pixels (e.g. 1 pixel = 1 empty pixel between each letter).
 * @param lineSpacing The line spacing of the font, in proportion to the font size (e.g. 2 means 1 empty line between each line, 1.5 = half a line between...)
 */
class Font @JvmOverloads constructor(
    resourcePath: String,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 1.4f,
) : Resource(resourcePath) {
    @Transient
    val name: String = resourcePath.substringAfterLast('/')
        .substringBeforeLast('.')

    // improves memory usage so fonts can use the same data object
    override fun hashCode(): Int {
        return resourcePath.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Font) return false

        return resourcePath == other.resourcePath
    }

    companion object {
        /**
         * Get a font from the [Google Fonts repository](https://github.com/google/fonts) on GitHub.
         *
         * Note that [each font has a different licence,](https://github.com/google/fonts#license) so it's best to check the licence for your usage.
         *
         * Set [verify] to `true` to attempt to connect to the mirror in this method for a better error message.
         * @param family the name of the font, for example `poppins` or `roboto`.
         * @param style the style of the font. These are normally standard, but please check individually if this method fails. Normal ones are `Light, Regular, Bold`, and `Italic`.
         *
         * @since 0.16.0
         */
        @JvmStatic
        @JvmOverloads
        @Suppress("DEPRECATION", "NAME_SHADOWING")
        fun getGoogleFont(
            family: String,
            style: String,
            letterSpacing: Float = 0f,
            lineSpacing: Float = 1f,
            verify: Boolean = false,
        ): Font {
            val dir = family.lowercase()
            val family = family.capitalize()
            val style = style.capitalize()
            val base = "https://raw.githubusercontent.com/google/fonts/main/ofl"
            val link = when (dir) {
                "inter" -> "$base/inter/Inter%5Bslnt%2Cwght%5D.ttf" // inter doesn't support styles?
                "roboto" -> "https://raw.githubusercontent.com/google/fonts/main/apache/roboto/static/$family-$style.ttf" // roboto is in static directory?
                "comicsans" -> "$base/comicneue/ComicNeue-$style.ttf" // https://github.com/google/fonts/blob/main/ofl/comicneue/DESCRIPTION.en_us.html
                else -> "$base/$dir/$family-$style.ttf"
            }
            if (verify) {
                var url = URL(link)
                try {
                    url.openStream()
                } catch (_: Exception) {
                    // check apache base
                    url = URL("https://raw.githubusercontent.com/google/fonts/main/apache/$dir/$family-$style.ttf")
                    try {
                        url.openStream()
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Failed to get font ($family-$style), check if the font exists on https://github.com/google/fonts in either apache or ofl directory.")
                    }
                }
            }

            return Font(
                link,
                letterSpacing,
                lineSpacing,
            )
        }
    }
}
