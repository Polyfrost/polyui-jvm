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

package cc.polyfrost.polyui.animate

import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.DrawableOp
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.SlideDirection
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.seconds

/** mother/super class of all transitions. */
abstract class Transition(drawable: Drawable) : DrawableOp(drawable), Cloneable {

    public abstract override fun clone(): Transition

    enum class Type {
        FadeOut, FadeIn, SlideFromLeft, SlideFromRight, SlideFromTop, SlideFromBottom;

        fun create(drawable: Drawable, durationNanos: Long): Transition {
            return when (this) {
                FadeOut -> FadeOut(drawable, Animations.EaseOutQuad, durationNanos)
                FadeIn -> FadeIn(drawable, Animations.EaseOutQuad, durationNanos)
                SlideFromLeft -> Slide(drawable, SlideDirection.FromLeft, Animations.EaseOutQuad, durationNanos)
                SlideFromRight -> Slide(drawable, SlideDirection.FromRight, Animations.EaseOutQuad, durationNanos)
                SlideFromTop -> Slide(drawable, SlideDirection.FromTop, Animations.EaseOutQuad, durationNanos)
                SlideFromBottom -> Slide(drawable, SlideDirection.FromBottom, Animations.EaseOutQuad, durationNanos)
            }
        }
    }
}

open class FadeIn(drawable: Drawable, private val animationType: Animations, durationNanos: Long = 1000L) :
    Transition(drawable) {
    override val animation: Animation = animationType.create(durationNanos, 0f, 1f)
    final override fun apply(renderer: Renderer) {
        renderer.globalAlpha(animation.value)
    }

    final override fun unapply(renderer: Renderer) {
        renderer.globalAlpha(1f)
    }

    override fun clone(): FadeIn {
        return FadeIn(drawable, animationType, animation.durationNanos)
    }
}

class FadeOut(drawable: Drawable, private val animationType: Animations, durationNanos: Long = 1000L) :
    FadeIn(drawable, animationType, durationNanos) {
    override val animation: Animation = animationType.create(durationNanos, 1f, 0f)
    override fun clone(): FadeOut {
        return FadeOut(drawable, animationType, animation.durationNanos)
    }
}

class Slide(
    drawable: Drawable,
    private val direction: SlideDirection = SlideDirection.FromLeft,
    private val animationType: Animations,
    durationNanos: Long = 1L.seconds
) :
    Transition(drawable) {
    override val animation: Animation =
        when (direction) {
            SlideDirection.FromLeft -> animationType.create(durationNanos, -1f - drawable.width, drawable.x)
            SlideDirection.FromRight -> animationType.create(
                durationNanos,
                Unit.VUnits.vWidth + drawable.width,
                drawable.x
            )

            SlideDirection.FromTop -> animationType.create(durationNanos, -1f - drawable.height, drawable.y)
            SlideDirection.FromBottom -> animationType.create(
                durationNanos,
                Unit.VUnits.vHeight + drawable.height,
                drawable.y
            )
        }
    private val movesX = direction == SlideDirection.FromLeft || direction == SlideDirection.FromRight
    override fun apply(renderer: Renderer) {
        if (movesX) {
            renderer.translate(animation.value, 0f)
        } else {
            renderer.translate(0f, animation.value)
        }
    }

    override fun unapply(renderer: Renderer) {
        if (movesX) {
            renderer.translate(-animation.value, 0f)
        } else {
            renderer.translate(0f, -animation.value)
        }
    }

    override fun clone(): Slide {
        return Slide(drawable, direction, animationType, animation.durationNanos)
    }
}

typealias Transitions = Transition.Type
