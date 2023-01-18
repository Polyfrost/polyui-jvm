package cc.polyfrost.polyui.components

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.renderer.Renderer

/**
 * Class to represent an operation that can be applied on a component that modifies its geometry in some way temporarily.
 */
abstract class TransformOp(val component: Component, val animation: Animation? = null) {
    abstract fun apply(renderer: Renderer)
    abstract fun unapply(renderer: Renderer)

    fun update(deltaTimeMillis: Long) {
        if (animation != null) {
            if (!animation.finished) {
                animation.update(deltaTimeMillis)
            }
        }
    }

    val finished
        get() = animation?.finished ?: true

    class Translate(private val x: Float, private val y: Float, component: Component, animation: Animation?) : TransformOp(component, animation) {
        override fun apply(renderer: Renderer) {
            renderer.translate(x, y)
        }

        override fun unapply(renderer: Renderer) {
            renderer.translate(-x, -y)
        }
    }

    class Scale(private val x: Float, private val y: Float, component: Component, animation: Animation?) : TransformOp(component, animation) {
        override fun apply(renderer: Renderer) {
            renderer.scale(x, y)
        }

        override fun unapply(renderer: Renderer) {
            renderer.scale(1 / x, 1 / y)
        }
    }

    class Rotate(private val angle: Float, component: Component, animation: Animation?) : TransformOp(component, animation) {
        override fun apply(renderer: Renderer) {
            renderer.rotate(angle)
        }

        override fun unapply(renderer: Renderer) {
            renderer.rotate(-angle)
        }
    }
}

