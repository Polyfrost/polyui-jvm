/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property.impl

import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.unit.TextAlign
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.px

open class TextProperties(val font: Font = Font("/Inter-Regular.ttf")) : Properties() {
    val fontSize: Unit.Pixel = 12.px
    override val color: Color = Color.WHITE
    override val padding: Float = 0F
    val alignment = TextAlign.Left
}
