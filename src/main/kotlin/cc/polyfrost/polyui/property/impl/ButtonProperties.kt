/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.property.impl

/**
 * Button properties.
 *
 * @since 0.17.3
 */
class ButtonProperties : BlockProperties() {
    /** This is the padding from the top to the items. */
    val topEdgePadding: Float = 10f

    /** padding between the icons and the text. */
    val iconTextSpacing: Float = 10f

    /** padding from the left/right edges. */
    val edgePadding: Float = 10f
}
