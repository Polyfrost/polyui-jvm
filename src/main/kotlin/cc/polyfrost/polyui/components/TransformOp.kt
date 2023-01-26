package cc.polyfrost.polyui.components

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.renderer.Renderer
import org.jetbrains.annotations.ApiStatus

/**
 * Class to represent an operation that can be applied on a component that modifies its geometry in some way temporarily.
 */
abstract class TransformOp(val component: Component, val animation: Animation? = null) {
    abstract fun apply(renderer: Renderer)
    abstract fun unapply(renderer: Renderer)

    fun update(deltaTimeMillis: Long) {
        animation?.update(deltaTimeMillis)
    }

    protected fun byAnimation(value: Float): Float {
        return if (animation != null) {
            animation.value * value
        } else {
            value
        }
    }

    val finished
        get() = animation?.finished ?: true

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.components.Component.scale], [rotate][cc.polyfrost.polyui.components.Component.rotate], or [translate][cc.polyfrost.polyui.components.Component.translate] instead of this class.
     */
    @ApiStatus.Internal
    class Translate(private val x: Float, private val y: Float, component: Component, animation: Animation?) :
        TransformOp(component, animation) {
        override fun apply(renderer: Renderer) {
            renderer.translate(byAnimation(x), byAnimation(y))
        }

        override fun unapply(renderer: Renderer) {
            renderer.translate(-byAnimation(x), -byAnimation(y))
        }
    }

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.components.Component.scale], [rotate][cc.polyfrost.polyui.components.Component.rotate], or [translate][cc.polyfrost.polyui.components.Component.translate] instead of this class.
     */
    @ApiStatus.Internal
    class Scale(private val x: Float, private val y: Float, component: Component, animation: Animation?) :
        TransformOp(component, animation) {
        override fun apply(renderer: Renderer) {
            renderer.scale(1 / byAnimation(x), 1 / byAnimation(y))
        }

        override fun unapply(renderer: Renderer) {
            renderer.scale(1 / byAnimation(x), 1 / byAnimation(y))
        }
    }

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.components.Component.scale], [rotate][cc.polyfrost.polyui.components.Component.rotate], or [translate][cc.polyfrost.polyui.components.Component.translate] instead of this class.
     */
    @ApiStatus.Internal
    class Rotate(private val angle: Double, component: Component, animation: Animation?) :
        TransformOp(component, animation) {
        override fun apply(renderer: Renderer) {
            renderer.rotate(byAnimationD(angle))
        }

        override fun unapply(renderer: Renderer) {
            renderer.rotate(-byAnimationD(angle))
        }

        private fun byAnimationD(value: Double): Double {
            return if (animation != null) {
                animation.value * value
            } else {
                value
            }
        }
    }
}

