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
 *
 * PolyUI internally uses nanoseconds as the time unit. There are a few reasons for this:
 * - animations will be smoother as frames are usually shorter than 1ms, around 0.02ms. This means that the delta will be a decimal, which is not possible with longs, so some accuracy is lost with milliseconds.
 * - [System.nanoTime] (the function we use to measure time) ignores changes to the system clock, so if the user changes their clock PolyUI won't crash or go very crazy because of a negative time.
 * - 9,223,372,036,854,775,808 is max for a long, so the program can run for 2,562,047.78802 hours, or 106,751.991167 days, or 292.471 years. So we can use these.
 */
class Clock {
    @PublishedApi
    internal var lastTime: Long = System.nanoTime()

    inline val now get() = lastTime
    val delta: Long
        get() {
            val currentTime = System.nanoTime()
            val delta = currentTime - lastTime
            lastTime = currentTime
            return delta
        }

    inline fun peek(): Long = System.nanoTime() - lastTime
    class FixedTimeExecutor(private val executeEveryNanos: Long, private val func: () -> Unit) {
        private var time = 0L

        fun tick(deltaTimeNanos: Long) {
            time += deltaTimeNanos
            if (time >= executeEveryNanos) {
                func()
                time = 0L
            }
        }
    }
}
