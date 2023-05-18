/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.unit

enum class SlideDirection {
    FromLeft, FromRight, FromTop, FromBottom
}

enum class TextAlign {
    Left, Right, Center
}

enum class Direction {
    Horizontal, Vertical
}

/**
 * Represents a fixed-size gap between rows or columns, used in flex and grid layouts.
 *
 * @see [cc.polyfrost.polyui.layout.impl.FlexLayout]
 * @see [cc.polyfrost.polyui.layout.impl.GridLayout]
 */
data class Gap(val mainGap: Unit.Pixel, val crossGap: Unit.Pixel) {

    companion object {
        @JvmField
        val Default = Gap(5.px, 5.px)
    }
}
