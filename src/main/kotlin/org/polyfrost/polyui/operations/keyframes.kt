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

package org.polyfrost.polyui.operations

import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.animateBy
import org.polyfrost.polyui.unit.Vec2

/**
 * # KeyFrames
 *
 * Keyframes are an easy way to create animations on components. PolyUI uses a [Kotlin DSL](https://kotlinlang.org/docs/type-safe-builders.html) to create them in an easy and concise way.
 *
 * They can be created wherever a [Drawable] is in scope, so everything from [events][org.polyfrost.polyui.event.Event]
 * for [mouse clicks][org.polyfrost.polyui.event.Event.Mouse.Clicked]; to in your initialization block; using the extension function [keyframed].
 *
 * Each keyframe can be added using a number between 0 (representing the start or 0%) to 100, representing the end or 100%.
 * Each keyframe can control the color, rotation, position, skew and size of the component (using this [function][org.polyfrost.polyui.component.animateTo]).
 * They support different animation curves and durations using the [animation] parameter.
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
 * @param animation the animation curve to use for each keyframe movement
 * @param self the component to apply to.
 */
@KeyFrameDSL
class KeyFrames<T : Drawable>(self: T, animation: Animation) : DrawableOp.Animatable<T>(self, animation) {
    private val keyframes = ArrayList<KeyFrame>(5)
    private var i = 0
    private inline val next get() = keyframes.getOrNull(i)

    operator fun Number.invoke(frame: (@KeyFrameDSL KeyFrame).() -> Unit) {
        val percent = this.toFloat()
        require(percent in 0f..100f) { "Keyframe number must be between 0 and 100%!" }
        val f = KeyFrame(percent / 100f, keyframes.lastOrNull())
        frame(f)
        keyframes.add(f)
    }

    fun at(percent: Float, frame: (@KeyFrameDSL KeyFrame).() -> Unit) {
        percent { frame() }
    }

    fun frame(percent: Float, frame: (@KeyFrameDSL KeyFrame).() -> Unit) {
        percent { frame() }
    }

    fun begin() {
        keyframes.sortBy { it.start }
        add()
    }

    override fun apply(value: Float) {
        val n = next ?: return
        if (value >= n.start) {
            i++
            val nn = next ?: return
            self.animateBy(
                nn.position, nn.size,
                nn.rotation,
                nn.scaleX, nn.scaleY,
                nn.skewX, nn.skewY,
                nn.color,
                // todo test animation stuff
                animation,
            )
        }
    }
}

@KeyFrameDSL
class KeyFrame(val start: Float, prev: KeyFrame?) {
    var rotation: Double = prev?.rotation ?: 0.0
    var position: Vec2 = prev?.position ?: Vec2.ZERO
    var scaleX: Float = prev?.scaleX ?: 1f
    var scaleY: Float = prev?.scaleY ?: 1f
    var skewX: Double = prev?.skewX ?: 0.0
    var skewY: Double = prev?.skewY ?: 0.0
    var size: Vec2 = prev?.size ?: Vec2.ZERO
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
@KeyFrameDSL
fun <S : Drawable> S.keyframed(
    animation: Animation,
    frames: KeyFrames<S>.() -> Unit,
) {
    val k = KeyFrames(this, animation)
    k.apply(frames)
    k.begin()
}
