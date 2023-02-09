/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer.data

data class Image @JvmOverloads constructor(
    val fileName: String,
    var width: Int? = null,
    var height: Int? = null,
    val type: Type = Type.from(fileName)
) {

    override fun toString(): String {
        return "$type Image(file=$fileName, ${width}x$height)"
    }

    enum class Type {
        PNG, SVG;

        companion object {
            @JvmStatic
            fun from(fileName: String): Type {
                return when (fileName.substringAfterLast(".")) {
                    "png" -> PNG
                    "svg" -> SVG
                    else -> throw IllegalArgumentException(
                        "Unknown image type for file $fileName"
                    )
                }
            }
        }
    }
}
