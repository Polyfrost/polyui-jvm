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
import cc.polyfrost.polyui.input.PolyText
import cc.polyfrost.polyui.input.PolyTranslator.Companion.localised
import cc.polyfrost.polyui.property.Properties
import cc.polyfrost.polyui.utils.radii

class TextInputProperties(val text: TextProperties) : Properties() {
    @Deprecated("use background color instead", ReplaceWith("backgroundColor"), DeprecationLevel.ERROR)
    override val color: Color = Color.BLACK
    override val padding: Float = 0f
    val paddingFromTextLateral: Float = 12f
    val paddingFromTextVertical: Float = 8f
    val defaultText: PolyText = "polyui.text.default".localised()
    val cornerRadii: FloatArray = 0f.radii()
    val backgroundColor: Color? = null
    val outlineColor: Color? = Color.GRAYf
    val outlineThickness: Float = 1f
}
