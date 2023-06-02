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

class DividerProperties(val thickness: Float = 1f) : Properties() {
    override val color: Color get() = colors.onBrand.fg
    override val padding: Float = 0F
}
