/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
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

/** default animations packaged with PolyUI. */
package org.polyfrost.polyui.animate

import kotlin.math.*

class Linear(durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    override fun getValue(percent: Float): Float {
        return percent
    }

    override fun clone(): Animation = Linear(durationNanos, from, to)
}

abstract class Easing(val type: Type, durationNanos: Long, start: Float, end: Float) :
    Animation(durationNanos, start, end) {
    abstract fun getValueInOut(percent: Float): Float
    abstract fun getValueIn(percent: Float): Float
    abstract fun getValueOut(percent: Float): Float

    final override fun getValue(percent: Float): Float {
        return when (type) {
            Type.In -> getValueIn(percent)
            Type.Out -> getValueOut(percent)
            Type.InOut -> getValueInOut(percent)
        }
    }

    enum class Type {
        In,
        Out,
        InOut
    }

    class Back(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        private inline val overshoot get() = 1.70158f
        private inline val overshoot2 get() = 2.70158f
        private inline val overshoot3 get() = overshoot * 1.525f
        override fun getValueOut(percent: Float): Float {
            return 1f + overshoot2 * (percent - 1f).pow(3f) + overshoot * (percent - 1f).pow(2f)
        }

        override fun getValueIn(percent: Float): Float {
            return overshoot * percent * percent * percent - overshoot2 * percent * percent
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent < 0.5f) {
                (2f * percent).pow(2f) * ((overshoot3 + 1f) * 2f * percent - overshoot3) / 2f
            } else {
                ((2f * percent - 2f).pow(2f) * ((overshoot3 + 1f) * (percent * 2f - 2f) + overshoot3) + 2f) / 2f
            }
        }

        override fun clone(): Animation = Back(type, durationNanos, from, to)
    }

    class Bump(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {

        override fun getValueOut(percent: Float): Float {
            return (3.7 * (percent - 1.0).pow(3.0) + 1.7 * 1.2 * (percent - 1.0).pow(2.0)).toFloat()
        }

        override fun getValueIn(percent: Float): Float {
            return (3.7 * percent.toDouble().pow(3.0) + 1.7 * 1.2 * percent.toDouble().pow(2.0)).toFloat()
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent < 0.5f) {
                getValueIn(percent)
            } else {
                getValueOut(percent)
            }
        }

        override fun clone(): Animation = Bump(type, durationNanos, from, to)
    }

    class Quad(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        override fun getValueOut(percent: Float): Float {
            return 1f - (1f - percent) * (1f - percent)
        }

        override fun getValueIn(percent: Float): Float {
            return percent * percent
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent < 0.5f) {
                2f * percent * percent
            } else {
                1f - (-2.0 * percent.toDouble() + 2.0).pow(2.0).toFloat() / 2f
            }
        }

        override fun clone(): Animation = Quad(type, durationNanos, from, to)
    }

    class Expo(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        override fun getValueOut(percent: Float): Float {
            return if (percent == 1f) {
                1f
            } else {
                1f - 2.0.pow(-10.0 * percent.toDouble()).toFloat()
            }
        }

        override fun getValueIn(percent: Float): Float {
            return if (percent == 0f) {
                0f
            } else {
                2.0.pow(10.0 * percent - 10.0).toFloat()
            }
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent == 0f || percent == 1f) {
                percent
            } else {
                if (percent < 0.5f) {
                    2.0.pow(20.0 * percent.toDouble() - 10.0).toFloat() / 2f
                } else {
                    (2.0 - 2.0.pow(-20.0 * percent + 10)).toFloat() / 2f
                }
            }
        }

        override fun clone(): Animation = Expo(type, durationNanos, from, to)
    }

    class Sine(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        override fun getValueIn(percent: Float): Float {
            return 1f - cos((percent.toDouble() * PI) / 2.0).toFloat()
        }

        override fun getValueOut(percent: Float): Float {
            return sin((percent.toDouble() * PI) / 2.0).toFloat()
        }

        override fun getValueInOut(percent: Float): Float {
            return -(cos(PI * percent.toDouble()) - 1.0).toFloat() / 2f
        }

        override fun clone(): Animation {
            return Sine(type, durationNanos, from, to)
        }
    }

    class Cubic(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        override fun getValueIn(percent: Float): Float {
            return percent * percent * percent
        }

        override fun getValueOut(percent: Float): Float {
            return 1f - (1.0 - percent.toDouble()).pow(3.0).toFloat()
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent < 0.5f) {
                4f * percent * percent * percent
            } else {
                1f - (-2.0 * percent.toDouble() + 2.0).pow(3.0).toFloat() / 2f
            }
        }

        override fun clone(): Animation = Cubic(type, durationNanos, from, to)
    }

    class Quart(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        override fun getValueIn(percent: Float): Float {
            return percent * percent * percent * percent
        }

        override fun getValueOut(percent: Float): Float {
            return 1f - (1.0 - percent.toDouble()).pow(4.0).toFloat()
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent < 0.5f) {
                8f * percent * percent * percent * percent
            } else {
                1f - (-2.0 * percent.toDouble() + 2.0).pow(4.0).toFloat() / 2f
            }
        }

        override fun clone(): Animation = Quart(type, durationNanos, from, to)
    }

    class Quint(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        override fun getValueIn(percent: Float): Float {
            return percent * percent * percent * percent * percent
        }

        override fun getValueOut(percent: Float): Float {
            return 1f - (1.0 - percent.toDouble()).pow(5.0).toFloat()
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent < 0.5f) {
                16f * percent * percent * percent * percent * percent
            } else {
                1f - (-2.0 * percent.toDouble() + 2.0).pow(5.0).toFloat() / 2f
            }
        }

        override fun clone(): Animation = Quint(type, durationNanos, from, to)
    }

    class Circ(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        override fun getValueIn(percent: Float): Float {
            return 1f - sqrt(1.0 - percent.toDouble().pow(2.0)).toFloat()
        }

        override fun getValueOut(percent: Float): Float {
            return sqrt(1.0 - (percent.toDouble() - 1.0).pow(2.0)).toFloat()
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent < 0.5f) {
                (1f - sqrt(1.0 - (2.0 * percent).pow(2.0))).toFloat() / 2f
            } else {
                (sqrt(1.0 - (-2.0 * percent + 2.0).pow(2.0)) + 1).toFloat() / 2f
            }
        }

        override fun clone(): Animation = Circ(type, durationNanos, from, to)
    }

    class Elastic(type: Type, durationNanos: Long, start: Float, end: Float) :
        Easing(type, durationNanos, start, end) {
        private inline val v get() = (2.0 * PI) / 3.0

        override fun getValueIn(percent: Float): Float {
            return if (percent == 0f || percent == 1f) {
                percent
            } else {
                -(2.0.pow(10.0 * percent.toDouble() - 10.0) * sin((percent.toDouble() * 10.0 - 10.75) * v)).toFloat()
            }
        }

        override fun getValueOut(percent: Float): Float {
            return if (percent == 0f || percent == 1f) {
                percent
            } else {
                (2.0.pow(-10.0 * percent.toDouble()) * sin((percent.toDouble() * 10.0 - 0.75) * v) + 1.0).toFloat()
            }
        }

        override fun getValueInOut(percent: Float): Float {
            return if (percent == 0f || percent == 1f) {
                percent
            } else if (percent < 0.5f) {
                -(2.0.pow(20.0 * percent.toDouble() - 10.0) * sin((20.0 * percent - 11.125) * v) / 2.0).toFloat()
            } else {
                (2.0.pow(-20.0 * percent.toDouble() + 10.0) * sin((20.0 * percent - 11.125) * v) / 2.0 + 1.0).toFloat()
            }
        }

        override fun clone(): Animation = Elastic(type, durationNanos, from, to)
    }
}
