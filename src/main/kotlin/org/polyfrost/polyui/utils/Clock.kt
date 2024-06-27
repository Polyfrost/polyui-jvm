/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.polyui.utils

import org.jetbrains.annotations.ApiStatus

/**
 * A simple class for timing of animations and things.
 * Literally a delta function.
 *
 * PolyUI internally uses nanoseconds as the time unit. There are a few reasons for this:
 * - animations will be smoother as frames are usually shorter than 1ms, around 0.007ms. This means that the delta will be a decimal, which is not possible with longs, so some accuracy is lost with milliseconds.
 * - [Clock,getTime] (the function we use to measure time) ignores changes to the system clock, so if the user changes their clock PolyUI won't crash or go very crazy because of a negative time.
 * - 9,223,372,036,854,775,808 is max for a long, so the program can run for 2,562,047.78802 hours, or 106,751.991167 days, or 292.471 years. So we can use these.
 *
 * @see Clock.FixedTimeExecutor
 * @see org.polyfrost.polyui.PolyUI.every
 */
class Clock {
    @PublishedApi
    internal var lastTime: Long = time

    val delta: Long
        get() {
            val currentTime = time
            val delta = currentTime - lastTime
            lastTime = currentTime
            return delta
        }

    /**
     * this function is rather wasteful and usage should be avoided.
     */
    @ApiStatus.Internal
    fun peek(): Long = time - lastTime

    companion object {
        /**
         * Get the current time in nanoseconds.
         */
        @JvmStatic
        inline val time get() = System.nanoTime()
    }

    fun interface Executor {
        /**
         * Update this Executor.
         * @return the time remaining until the next execution, or a negative number if it no longer needs to be executed and can be removed.
         */
        fun tick(deltaTimeNanos: Long): Long
    }

    /**
     * An executor that will execute a function after a certain amount of time.
     * @since 1.5.0
     */
    class Bomb(val timerNanos: Long, val func: () -> Unit) : Executor {
        private var time = 0L
        override fun tick(deltaTimeNanos: Long): Long {
            time += deltaTimeNanos
            if (time >= timerNanos) func()
            return timerNanos - time
        }

        /**
         * Re-fuse the bomb, resetting the timer.
         */
        fun refuse() {
            time = 0L
        }
    }

    /**
     * Create a new FixedTimeExecutor.
     * @param repeats how many times to repeat before stopping. If it is 0 (default) then it will last forever (see [finished])
     * @since 0.18.1
     */
    open class FixedTimeExecutor @JvmOverloads constructor(val executeEveryNanos: Long, val repeats: Int = 0, protected val func: () -> Unit) : Executor {
        protected var time = 0L

        /** amount of times this has executed. Only incremented when [repeats] is not 0 (i.e. it does not last forever) to prevent numerical overflow and keep method short. */
        var cycles = 0
            protected set

        override fun tick(deltaTimeNanos: Long): Long {
            if (cycles > repeats) return -1L
            time += deltaTimeNanos
            if (time >= executeEveryNanos) {
                func()
                if (repeats != 0) cycles++
                time = 0L
            }
            return executeEveryNanos - time
        }
    }
}
