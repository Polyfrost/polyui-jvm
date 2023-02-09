/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.utils

import cc.polyfrost.polyui.color.Color

fun rgba(r: Float, g: Float, b: Float, a: Float): Color {
    return Color(r, g, b, a)
}

fun Int.toColor(): Color {
    return Color(
        ((this shr 16) and 0xFF) / 255f,
        ((this shr 8) and 0xFF) / 255f,
        (this and 0xFF) / 255f,
        ((this shr 24) and 0xFF) / 255f
    )
}

fun java.awt.Color.asPolyColor(): Color {
    return Color(this.red.toFloat(), this.green.toFloat(), this.blue.toFloat(), this.alpha.toFloat())
}
