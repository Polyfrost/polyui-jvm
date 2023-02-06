/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer.data

import cc.polyfrost.polyui.property.Settings

data class Framebuffer(val width: Float, val height: Float, val type: Settings.BufferType) {
    enum class Mode {
        Read, Write, ReadWrite
    }
}