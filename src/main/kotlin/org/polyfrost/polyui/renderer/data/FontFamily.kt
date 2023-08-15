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

package org.polyfrost.polyui.renderer.data

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.utils.getResourceStreamNullable
import java.io.File
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
class FontFamily(
    val name: String,
    val path: String,
    private val fallback: FontFamily? = null
) {
    val isZip = path.endsWith(".zip")
    private val dir by lazy {
        File(System.getProperty("java.io.tmpdir")).resolve("polyui-$name").also {
            it.deleteOnExit()
            PolyUI.LOGGER.info("Extracting font $name to temporary directory... ($it)")
            it.mkdirs()
            val zip = ZipInputStream(getResourceStreamNullable(path) ?: throw IllegalArgumentException("Font zip file $path not found!"))
            while (true) {
                val entry = zip.nextEntry ?: break
                val file = it.resolve(entry.name)
                file.createNewFile()
                file.outputStream().use { out ->
                    zip.copyTo(out)
                }
            }
            zip.close()
        }
    }

    val thin by lazy { get("Thin", false) }
    val thinItalic by lazy { get("ThinItalic", true) }
    val light by lazy { get("Light", false) }
    val lightItalic by lazy { get("LightItalic", true) }
    val regular by lazy { get("Regular", false) }
    val regularItalic by lazy { get("Italic", true) }
    val medium by lazy { get("Medium", false) }
    val mediumItalic by lazy { get("MediumItalic", true) }
    val bold by lazy { get("Bold", false) }
    val boldItalic by lazy { get("BoldItalic", true) }
    val black by lazy { get("Black", false) }
    val blackItalic by lazy { get("BlackItalic", true) }

    private fun get(style: String, italic: Boolean): Font {
        val p = "$name-$style${if (italic) "Italic" else ""}.ttf"
        val address = if (isZip) "${dir.toURI()}/$p" else "$path/$p"
        return if (getResourceStreamNullable(address) == null) {
            PolyUI.LOGGER.warn("Font $address not found! Falling back to ${fallback?.name}")
            if (this === PolyUI.defaultFonts) {
                PolyUI.LOGGER.warn("Default fonts does not contain font $p, using regular.")
                return if (italic) {
                    PolyUI.defaultFonts.regularItalic
                } else {
                    PolyUI.defaultFonts.regular
                }
            }
            fallback?.get(style, italic) ?: PolyUI.defaultFonts.get(style, italic)
        } else {
            Font(address)
        }
    }
}
