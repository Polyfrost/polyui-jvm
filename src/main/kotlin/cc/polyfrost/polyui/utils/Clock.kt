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
 * - animations will be smoother as frames are usually shorter than 1ms, around 0.007ms. This means that the delta will be a decimal, which is not possible with longs, so some accuracy is lost with milliseconds.
 * - [System.nanoTime] (the function we use to measure time) ignores changes to the system clock, so if the user changes their clock PolyUI won't crash or go very crazy because of a negative time.
 * - 9,223,372,036,854,775,808 is max for a long, so the program can run for 2,562,047.78802 hours, or 106,751.991167 days, or 292.471 years. So we can use these.
 *
 * @see FixedTimeExecutor
 * @see cc.polyfrost.polyui.PolyUI.every
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

    /**
     * Create a new FixedTimeExecutor.
     * @param executeEveryNanos how often (in nanoseconds) to execute the given [function][func].
     * @param repeats how many times to repeat before stopping. If it is 0 (default) then it will last forever (see [finished])
     * @param func the function to execute after the given time.
     */
    class FixedTimeExecutor @JvmOverloads constructor(val executeEveryNanos: Long, val repeats: Int = 0, private val func: () -> Unit) {
        /** amount of times this has executed. Only incremented when [repeats] is not 0 (i.e. it does not last forever) to prevent numerical overflow and keep method short. */
        var cycles: Int = 0
            private set
        private var time = 0L

        /** returns true if this FixedTimeExecutor has finished (it has cycled more than [repeats] times)
         *
         * Returns false if [repeats] is 0 (it lasts forever) */
        val finished get() = cycles > repeats

        /**
         * Update this FixedTimeExecutor, and execute it if the given amount of [time][deltaTimeNanos] has passed.
         * @return the same as [finished]
         */
        fun tick(deltaTimeNanos: Long): Boolean {
            if (cycles > repeats) return true
            time += deltaTimeNanos
            if (time >= executeEveryNanos) {
                func()
                if (repeats != 0) cycles++
                time = 0L
            }
            return false
        }
    }
}
