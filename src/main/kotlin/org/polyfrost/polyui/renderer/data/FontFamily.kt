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

package org.polyfrost.polyui.renderer.data

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.utils.getResourceStreamNullable
import org.polyfrost.polyui.utils.resourceExists
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

/**
 * Represents a font family, which consists of multiple font styles.
 *
 * @property name The name of the font family.
 * @property path The path to the font files or font zip file.
 * @property fallback The fallback font family to use if a particular font style is not found in this font family.
 * If not provided, the [default fallback font family][PolyUI.defaultFonts] will be used.
 * @since 0.22.0
 */
@Suppress("unused")
open class FontFamily(
    val name: String,
    val path: String,
    private val fallback: FontFamily? = null,
) {
    @Transient
    val isZip = path.endsWith(".zip")
    protected val dir: URI by lazy {
        val start = System.nanoTime()
        val f = File(System.getProperty("java.io.tmpdir")).resolve("polyui-fonts-$name")
        f.deleteOnExit()
        PolyUI.LOGGER.info("Extracting font $name to temporary directory... ($f)")
        f.mkdirs()
        val zip =
            ZipInputStream(getResourceStreamNullable(path)?.buffered() ?: throw IllegalArgumentException("Font zip file $path not found!"))
        while (true) {
            val entry = zip.nextEntry ?: break
            val file = f.resolve(entry.name)
            file.createNewFile()
            file.outputStream().buffered().use { out ->
                zip.copyTo(out)
            }
        }
        zip.close()
        PolyUI.LOGGER.info("\t\t> took ${(System.nanoTime() - start) / 1_000_000.0}ms")
        f.toURI()
    }

    open val thin by lazy { fload(Font.Weight.Thin, false) }
    open val thinItalic by lazy { fload(Font.Weight.Thin, true) }

    open val extraLight by lazy { fload(Font.Weight.ExtraLight, false) }
    open val extraLightItalic by lazy { fload(Font.Weight.ExtraLight, true) }

    open val light by lazy { fload(Font.Weight.Light, false) }
    open val lightItalic by lazy { fload(Font.Weight.Light, true) }

    open val regular by lazy { fload(Font.Weight.Regular, false) }
    open val regularItalic by lazy { fload(Font.Weight.Regular, true) }

    open val medium by lazy { fload(Font.Weight.Medium, false) }
    open val mediumItalic by lazy { fload(Font.Weight.Medium, true) }

    open val semiBold by lazy { fload(Font.Weight.SemiBold, false) }
    open val semiBoldItalic by lazy { fload(Font.Weight.SemiBold, true) }

    open val bold by lazy { fload(Font.Weight.Bold, false) }
    open val boldItalic by lazy { fload(Font.Weight.Bold, true) }

    open val extraBold by lazy { fload(Font.Weight.ExtraBold, false) }
    open val extraBoldItalic by lazy { fload(Font.Weight.ExtraBold, true) }

    open val black by lazy { fload(Font.Weight.Black, false) }
    open val blackItalic by lazy { fload(Font.Weight.Black, true) }

    /**
     * Return `true` if the font at the given index exists, or `false` if it is the backup one.
     *
     * Note that this will initialize the given font, and the [PolyUI.defaultFonts] version as well.
     * @since 1.0.7
     * @see get
     */
    fun has(weight: Font.Weight, italic: Boolean): Boolean = get(weight, italic) !== PolyUI.defaultFonts.get(weight, italic)

    /**
     * Return the specified font. This is a programmatic method, and is the same as getting the field.
     * @since 1.0.7
     */
    fun get(weight: Font.Weight, italic: Boolean): Font {
        return if (italic) {
            when (weight) {
                Font.Weight.Thin -> thinItalic
                Font.Weight.ExtraLight -> extraLightItalic
                Font.Weight.Light -> lightItalic
                Font.Weight.Regular -> regularItalic
                Font.Weight.Medium -> mediumItalic
                Font.Weight.SemiBold -> semiBoldItalic
                Font.Weight.Bold -> boldItalic
                Font.Weight.ExtraBold -> extraBoldItalic
                Font.Weight.Black -> blackItalic
            }
        } else {
            when (weight) {
                Font.Weight.Thin -> thin
                Font.Weight.ExtraLight -> extraLight
                Font.Weight.Light -> light
                Font.Weight.Regular -> regular
                Font.Weight.Medium -> medium
                Font.Weight.SemiBold -> semiBold
                Font.Weight.Bold -> bold
                Font.Weight.ExtraBold -> extraBold
                Font.Weight.Black -> black
            }
        }
    }

    fun getStyle(weight: Font.Weight, italic: Boolean): String {
        if (italic) {
            if (weight == Font.Weight.Regular) return "Italic"
            return "${weight}Italic"
        }
        return "$weight"
    }

    /**
     * Load a font file.
     */
    protected open fun fload(weight: Font.Weight, italic: Boolean, origin: Font.Weight? = null): Font {
        val style = getStyle(weight, italic)
        val p = "$name-$style.ttf"
        val address = if (isZip) "$dir$p" else "$path/$p"
        return if (!resourceExists(address)) {
            val fb = weight.fb
            return if (fb == null) {
                PolyUI.LOGGER.warn("Could not find any matching styles for $style, falling back to default font!")
                val w = origin ?: weight
                fallback?.fload(w, italic) ?: PolyUI.defaultFonts.fload(w, italic)
            } else {
                PolyUI.LOGGER.warn("Font $style not found, falling back to weight $weight")
                fload(fb, italic, weight)
            }
        } else {
            Font(address, family = this, italic = italic, weight = weight)
        }
    }
}
