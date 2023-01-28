package cc.polyfrost.polyui.components

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.renderer.Renderer
import org.jetbrains.annotations.ApiStatus

/**
 * Class to represent an operation that can be applied on a component that modifies its geometry in some way.
 */
abstract class TransformOp(val component: Component) {
    abstract val animation: Animation?
    abstract fun apply(renderer: Renderer)

    fun update(deltaTimeMillis: Long) {
        animation?.update(deltaTimeMillis)
    }

    open val finished
        get() = animation?.finished ?: true

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.components.Component.scale], [rotate][cc.polyfrost.polyui.components.Component.rotate], or [translate][cc.polyfrost.polyui.components.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Translate(
        private val x: Float, private val y: Float, component: Component,
        type: Animations? = null, durationMillis: Long = 0L
    ) : TransformOp(component) {
        override val animation = type?.create(durationMillis, component.x(), component.x() + x)
        private val yAnim = type?.create(durationMillis, component.y(), component.y() + y)
        override fun apply(renderer: Renderer) {
            if (animation != null) {
                component.at.a.px += animation.value
                component.at.b.px += yAnim!!.value
            } else {
                component.at.a.px += x
                component.at.b.px += y
            }
        }
    }

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.components.Component.scale], [rotate][cc.polyfrost.polyui.components.Component.rotate], or [translate][cc.polyfrost.polyui.components.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Scale(
        private val x: Float, private val y: Float, component: Component,
        type: Animations? = null, durationMillis: Long = 0L
    ) : TransformOp(component) {
        override val animation = type?.create(durationMillis, component.scaleX, component.scaleX + x)
        private val yAnim = type?.create(durationMillis, component.scaleY, component.scaleY + y)
        override fun apply(renderer: Renderer) {
            if (animation != null) {
                component.scaleX = animation.value
                component.scaleY = yAnim!!.value
            } else {
                component.scaleX = x
                component.scaleY = y
            }
        }
    }

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.components.Component.scale], [rotate][cc.polyfrost.polyui.components.Component.rotate], or [translate][cc.polyfrost.polyui.components.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Rotate(private val angle: Double, component: Component, type: Animations? = null, durationMillis: Long = 0L) :
        TransformOp(component) {
        override val animation =
            type?.create(durationMillis, component.rotation.toFloat(), (component.rotation + angle).toFloat())

        override fun apply(renderer: Renderer) {
            if (animation != null) {
                component.rotation = animation.value.toDouble()
            } else {
                component.rotation += angle
            }
        }
    }
}

