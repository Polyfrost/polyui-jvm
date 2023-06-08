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
    val type: Type = from(resourcePath)
) {

    override fun toString(): String {
        return "$type Image(file=$resourcePath, ${width}x$height)"
    }

    override fun hashCode(): Int {
        return resourcePath.hashCode()
    }

    override fun equals(other: Any?): Boolean = resourcePath.equals(other)

    enum class Type {
        PNG, JPEG, BMP, SVG;
    }

    /**
     * Styles for [Google Material icons](https://github.com/google/material-design-icons)
     * @see getMaterialIcon
     */
    enum class MaterialStyle(val style: String) {
        OUTLINED("materialiconsoutlined"),
        ROUND("materialiconsround"),
        SHARP("materialiconssharp"),
        TWOTONE("materialiconstwotone"),
        NORMAL("materialicons")
    }

    companion object {
        @JvmStatic
        fun from(fileName: String): Type {
            return when (fileName.substringAfterLast('.')) {
                "png" -> Type.PNG
                "svg" -> Type.SVG
                "jpg", "jpeg", "jpe", "jif", "jfif", "jfi" -> Type.JPEG
                "bmp" -> Type.BMP
                else -> throw IllegalArgumentException(
                    "Unknown image type for file $fileName"
                )
            }
        }

        /**
         * Get a font from the [Google Material Icons repository](https://github.com/google/material-design-icons) on GitHub.
         *
         * Note that these icons are licensed under the [Apache 2.0 License](https://github.com/google/material-design-icons/blob/master/LICENSE).
         * @param icon the name of the icon, as a path as it is on the repo, using either dots or slashes, for example `camera.11mp` or `actions.abc`.
         * @param style the Material icon [style][MaterialStyle] to use.
         *
         * @since 0.16.0
         */
        @JvmStatic
        @JvmOverloads
        fun getMaterialIcon(
            icon: String,
            width: Float = -1f,
            height: Float = -1f,
            style: MaterialStyle = MaterialStyle.NORMAL
        ) = PolyImage(
            "https://raw.githubusercontent.com/google/material-design-icons/master/src/" +
                "${icon.replace('.', '/')}/${style.style}/24px.svg",
            width,
            height,
            Type.SVG
        )
    }
}
