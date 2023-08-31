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
import org.polyfrost.polyui.utils.resourceExists
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
@Suppress("unused")
open class FontFamily(
    val name: String,
    val path: String,
    private val fallback: FontFamily? = null,
) {
    @Transient
    val isZip = path.endsWith(".zip")
    protected val dir by lazy {
        val f = File(System.getProperty("java.io.tmpdir")).resolve("polyui-$name")
        f.deleteOnExit()
        PolyUI.LOGGER.info("Extracting font $name to temporary directory... ($f)")
        f.mkdirs()
        val zip = ZipInputStream(getResourceStreamNullable(path) ?: throw IllegalArgumentException("Font zip file $path not found!"))
        while (true) {
            val entry = zip.nextEntry ?: break
            val file = f.resolve(entry.name)
            file.createNewFile()
            file.outputStream().buffered().use { out ->
                zip.copyTo(out)
            }
        }
        zip.close()
        f.toURI()
    }

    open val thin by lazy { get("Thin") }
    open val thinItalic by lazy { get("ThinItalic") }

    open val light by lazy { get("Light") }
    open val lightItalic by lazy { get("LightItalic") }

    open val regular by lazy { get("Regular") }
    open val regularItalic by lazy { get("Italic") }

    open val medium by lazy { get("Medium") }
    open val mediumItalic by lazy { get("MediumItalic") }

    open val bold by lazy { get("Bold") }
    open val boldItalic by lazy { get("BoldItalic") }

    open val black by lazy { get("Black") }
    open val blackItalic by lazy { get("BlackItalic") }

    fun get(style: String): Font {
        val p = "$name-$style.ttf"
        val address = if (isZip) "$dir$p" else "$path/$p"
        return if (!resourceExists(address)) {
            PolyUI.LOGGER.warn("Font $address not found! Falling back to ${fallback?.name}")
            return if (this === PolyUI.defaultFonts) {
                PolyUI.LOGGER.warn("Default fonts does not contain font $p, using regular.")
                PolyUI.defaultFonts.regular
            } else {
                fallback?.get(style) ?: PolyUI.defaultFonts.get(style)
            }
        } else {
            Font(address)
        }
    }
}
