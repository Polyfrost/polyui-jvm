/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.renderer.Window

class NoOpWindow(title: String, width: Int, height: Int) : Window(width, height) {
    override fun open(polyUI: PolyUI): Window {
        while (true) {
            polyUI.render()
        }
        return this
    }

    override fun close() {
    }

    override fun createCallbacks() {
    }

    override fun videoSettingsChanged() {
    }
}
