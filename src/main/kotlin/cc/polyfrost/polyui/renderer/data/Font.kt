/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer.data

/**
 * # Font
 *
 * A font used by the rendering implementation.
 *
 * @param fileName The file name of the font, relative to the classpath.
 * @param letterSpacing The letter spacing of the font, in pixels (e.g. 1 pixel = 1 empty pixel between each letter).
 * @param lineSpacing The line spacing of the font, in proportion to the font size (e.g. 2 means 1 empty line between each line, 1.5 = half a line between...)
 */
data class Font @JvmOverloads constructor(val fileName: String, @Transient val letterSpacing: Float = 0f, @Transient val lineSpacing: Float = 1f) {
    val name: String = fileName.substringAfterLast("/")
        .substringBeforeLast(".")

    // improves memory usage so fonts can use the same data object
    override fun hashCode(): Int {
        return fileName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        other as Font
        if (fileName != other.fileName) return false
        if (letterSpacing != other.letterSpacing) return false
        return lineSpacing == other.lineSpacing
    }
}
