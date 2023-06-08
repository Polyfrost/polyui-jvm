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

package cc.polyfrost.polyui.animate.keyframes

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.seconds

/**
 * # KeyFrames
 *
 * Keyframes are an easy way to create animations on components. PolyUI uses a [Kotlin DSL](https://kotlinlang.org/docs/type-safe-builders.html) to create them in an easy and concise way.
 *
 * They can be created wherever a [Component] is in scope, so everything from [events][cc.polyfrost.polyui.event.Events.Handler]
 * for [mouse clicks][cc.polyfrost.polyui.event.Events.MouseClicked]; to in your initialization block; using the extension function [keyframed].
 *
 * Each keyframe can be added using a number between 0 (representing the start or 0%) to 100, representing the end or 100%.
 * Each keyframe can control the color, rotation, position, skew and size of the component (using this [function][cc.polyfrost.polyui.component.Component.animateTo]).
 * They support different animation curves and durations using the parameters [overNanos] and [animation].
 *
 * The syntax is as follows:
 * ```
 * keyframed(2.seconds, Animations.EaseOutExpo) {
 *      20 {    // at 20% or 0.4 seconds, the rotation will be 25 degrees
 *          rotation = 25.0
 *      }
 *      32.5 {  // at 32.5%, the rotation will be 35 degrees
 *          rotation = 35.0
 *      }
 *      50 {
 *          rotation = 180.0
 *      }
 *      100 {   // when it is finished, we will be fully rotated (equal to 0 degrees)
 *          rotation = 360.0
 *      }
 * }
 * ```
 *
 * @param overNanos duration in nanoseconds of this keyframe set (how long it takes to finish)
 * @param animation the animation curve to use for each keyframe movement
 * @param component the component to apply to.
 */
@KeyFrameDSL
class KeyFrames @JvmOverloads constructor(private val overNanos: Long, private val animation: Animations = Animations.Linear, val component: Component) {
    private var time = 0L
    private val keyframes = ArrayList<KeyFrame>()
    private var i = 0
    private inline val next get() = keyframes.getOrNull(i)

    operator fun Number.invoke(frame: (@KeyFrameDSL KeyFrame).() -> kotlin.Unit) {
        val percent = this.toFloat()
        require(percent in 0f..100f) { "Keyframe number must be between 0 and 100%!" }
        val last: Long
        val t = percentToTime(percent)
        val f = KeyFrame(t, keyframes.lastOrNull().also { last = it?.time ?: 0L })
        if (last > t) {
            PolyUI.LOGGER.warn(
                "Keyframe ordering incorrect: Keyframe at {} is placed after {}, please swap these frames.",
                last,
                t
            )
        }
        frame(f)
        keyframes.add(f)
    }

    fun at(percent: Float, frame: (@KeyFrameDSL KeyFrame).() -> kotlin.Unit) {
        percent { frame() }
    }

    fun frame(percent: Float, frame: (@KeyFrameDSL KeyFrame).() -> kotlin.Unit) {
        percent { frame() }
    }

    private fun percentToTime(percent: Float) = (overNanos.toFloat() * (percent / 100f)).toLong()

    /**
     * Setup this keyframe instance. This method will sort the keyframes so they
     */
    fun setup() {
        keyframes.sortBy { it.time }
    }

    fun update(deltaTimeNanos: Long): Boolean {
        time += deltaTimeNanos
        val n = next ?: return true
        if (time >= n.time) {
            i++
            val nn = next ?: return true
            component.animateTo(
                nn.position, nn.size,
                nn.rotation,
                nn.scaleX, nn.scaleY,
                nn.skewX, nn.skewY,
                nn.color,
                animation, nn.time - n.time
            )
        }
        return false
    }
}

@KeyFrameDSL
class KeyFrame(val time: Long, prev: KeyFrame?) {
    var rotation: Double = prev?.rotation ?: 0.0
    var position: Vec2<Unit>? = prev?.position
    var scaleX: Float = prev?.scaleX ?: 1f
    var scaleY: Float = prev?.scaleY ?: 1f
    var skewX: Double = prev?.skewX ?: 0.0
    var skewY: Double = prev?.skewY ?: 0.0
    var size: Vec2<Unit>? = prev?.size
    var color: Color? = prev?.color

    override fun toString() = "KeyFrame(toRotate $rotation, skews $skewX+$skewY, scales $scaleX+$scaleY, to $position, toSize $size, toColor $color)"
}

/** marker class for preventing illegal nesting. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@DslMarker
annotation class KeyFrameDSL

/** create a keyframe set for this component.
 *
 * # [click here][KeyFrames]
 */
@JvmOverloads
@KeyFrameDSL
fun Component.keyframed(
    durationNanos: Long = 1L.seconds,
    animation: Animations = Animations.Linear,
    frames: KeyFrames.() -> kotlin.Unit
) {
    val k = KeyFrames(durationNanos, animation, this)
    frames(k) // apply
    k.setup()
    this.addKeyframes(k)
}

/** create a keyframe set for this component.
 *
 * # [click here][KeyFrames]
 */
@JvmOverloads
@KeyFrameDSL
fun Component.keyframes(
    durationNanos: Long = 1L.seconds,
    animation: Animations = Animations.Linear,
    frames: KeyFrames.() -> kotlin.Unit
) = keyframed(durationNanos, animation, frames)
