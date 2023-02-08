/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.animate.transitions

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.DrawableOp
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.SlideDirection
import cc.polyfrost.polyui.unit.Unit

/** mother/super class of all transitions. */
abstract class Transition(drawable: Drawable) : DrawableOp(drawable), Cloneable {

    // why, kotlin, why?
    public abstract override fun clone(): Transition

    enum class Type {
        FadeOut, FadeIn, Slide;

        fun create(drawable: Drawable, durationMillis: Long): Transition {
            return when (this) {
                FadeOut -> FadeOut(drawable, Animations.EaseOutQuad, durationMillis)
                FadeIn -> FadeIn(drawable, Animations.EaseOutQuad, durationMillis)
                Slide -> Slide(drawable, SlideDirection.FromLeft, Animations.EaseOutQuad, durationMillis)
            }
        }
    }
}

open class FadeIn(drawable: Drawable, private val animationType: Animations, durationMillis: Long = 1000L) :
    Transition(drawable) {
    override val animation: Animation = animationType.create(durationMillis, 0f, 1f)
    final override fun apply(renderer: Renderer) {
        renderer.globalAlpha(animation.value)
    }

    final override fun unapply(renderer: Renderer) {
        renderer.globalAlpha(1f)
    }

    override fun clone(): FadeIn {
        return FadeIn(drawable, animationType, animation.durationMillis)
    }
}

class FadeOut(drawable: Drawable, private val animationType: Animations, durationMillis: Long = 1000L) :
    FadeIn(drawable, animationType, durationMillis) {
    override val animation: Animation = animationType.create(durationMillis, 1f, 0f)
    override fun clone(): FadeOut {
        return FadeOut(drawable, animationType, animation.durationMillis)
    }
}

class Slide(
    drawable: Drawable,
    private val direction: SlideDirection = SlideDirection.FromLeft, private val animationType: Animations,
    durationMillis: Long = 1000L
) :
    Transition(drawable) {
    override val animation: Animation =
        when (direction) {
            SlideDirection.FromLeft -> animationType.create(durationMillis, -1f - drawable.width, drawable.x)
            SlideDirection.FromRight -> animationType.create(
                durationMillis,
                Unit.VUnits.vWidth + drawable.width,
                drawable.x
            )

            SlideDirection.FromTop -> animationType.create(durationMillis, -1f - drawable.height, drawable.y)
            SlideDirection.FromBottom -> animationType.create(
                durationMillis,
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
        return Slide(drawable, direction, animationType, animation.durationMillis)
    }
}


typealias Transitions = Transition.Type