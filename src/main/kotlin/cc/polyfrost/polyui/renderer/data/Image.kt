/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer.data

data class Image @JvmOverloads constructor(
    val fileName: String,
    var width: Int? = null,
    var height: Int? = null,
    val type: Type = Type.from(fileName),
) {
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