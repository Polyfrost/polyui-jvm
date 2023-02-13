/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

    fun peekDelta(): Long {
        return System.currentTimeMillis() - lastTime
    }

    class FixedTimeExecutor(private val millis: Long, private val func: () -> Unit) {
        private val clock = Clock()

        init {
            clock.getDelta()
        }

        fun tick() {
            if (clock.peekDelta() >= millis) {
                func()
                clock.getDelta()
            }
        }
    }
}
