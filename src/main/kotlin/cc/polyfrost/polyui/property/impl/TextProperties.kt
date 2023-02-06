/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - simple, easy to use and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property.impl

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.renderer.data.Font

open class TextProperties : Properties() {
    override val color: Color = Color.WHITE
    override val padding: Float = 0F
    val font = Font("/Inter-Regular.ttf")
}