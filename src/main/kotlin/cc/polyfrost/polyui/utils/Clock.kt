/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - simple, easy to use and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.utils

/**
 * A simple class for timing of animations and things.
 * Literally a delta function.
 */
class Clock {
    private var lastTime: Long = System.currentTimeMillis()

    fun getDelta(): Long {
        val currentTime = System.currentTimeMillis()
        val delta = currentTime - lastTime
        lastTime = currentTime
        return delta
    }
}