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
 * Image representation in PolyUI.
 * @param resourcePath the path to the [resource][cc.polyfrost.polyui.utils.getResourceStream]
 * @param width the width of the image. Specify one to respect the aspect ratio of the image.
 * @param height the width of the image. Specify one to respect the aspect ratio of the image.
 * @param type the [image type][Type]. This is automatically inferred from the file extension normally, but you can manually select it.
 */
data class PolyImage @JvmOverloads constructor(
    val resourcePath: String,
    var width: Float = -1f, // uninitialized
    var height: Float = -1f,
    val type: Type = Type.from(resourcePath)
) {

    override fun toString(): String {
        return "$type Image(file=$resourcePath, ${width}x$height)"
    }

    override fun hashCode(): Int {
        return resourcePath.hashCode()
    }

    enum class Type {
        PNG, JPEG, BMP, SVG;

        companion object {
            @JvmStatic
            fun from(fileName: String): Type {
                return when (fileName.substringAfterLast(".")) {
                    "png" -> PNG
                    "svg" -> SVG
                    "jpg", "jpeg", "jpe", "jif", "jfif", "jfi" -> JPEG
                    "bmp" -> BMP
                    else -> throw IllegalArgumentException(
                        "Unknown image type for file $fileName"
                    )
                }
            }
        }
    }
}
