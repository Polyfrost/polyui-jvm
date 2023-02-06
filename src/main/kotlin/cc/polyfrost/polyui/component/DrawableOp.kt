package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.animate.Animations
import cc.polyfrost.polyui.component.DrawableOp.*
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import org.jetbrains.annotations.ApiStatus

/**
 * Class to represent an operation that can be applied on a component that modifies is applied before and after it renders, for example a [Translate], [Scale], or [Rotate] operation, or a transition.
 */
abstract class DrawableOp(val component: Component) {
    abstract val animation: Animation?
    abstract fun apply(renderer: Renderer)
    open fun unapply(renderer: Renderer) {
        // no-op
    }

    fun update(deltaTimeMillis: Long) {
        animation?.update(deltaTimeMillis)
    }

    open val finished
        get() = animation?.finished ?: true

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.component.Component.scale], [rotate][cc.polyfrost.polyui.component.Component.rotate], or [translate][cc.polyfrost.polyui.component.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Translate(
        private val x: Float, private val y: Float, component: Component,
        type: Animations? = null, durationMillis: Long = 1000L,
    ) : DrawableOp(component) {
        override val animation = type?.create(durationMillis, component.x, component.x + x)
        private val yAnim = type?.create(durationMillis, component.y, component.y + y)
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

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.component.Component.scale], [rotate][cc.polyfrost.polyui.component.Component.rotate], or [translate][cc.polyfrost.polyui.component.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Scale(
        private val x: Float, private val y: Float, component: Component,
        type: Animations? = null, durationMillis: Long = 1000L,
    ) : DrawableOp(component) {
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

    /** Note: if you are making a UI, you should probably be using [scale][cc.polyfrost.polyui.component.Component.scale], [rotate][cc.polyfrost.polyui.component.Component.rotate], or [translate][cc.polyfrost.polyui.component.Component.move] instead of this class.
     */
    @ApiStatus.Internal
    class Rotate(
        private val angle: Double,
        component: Component,
        type: Animations? = null,
        durationMillis: Long = 1000L
    ) :
        DrawableOp(component) {
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

    @ApiStatus.Internal
    class Resize(
        toSize: Vec2<Unit>,
        component: Component,
        animation: Animation.Type?,
        durationMillis: Long = 1000L
    ) :
        DrawableOp(component) {
        override val animation = animation?.create(durationMillis, component.sized!!.a.px, toSize.a.px)
        private val animation2 = animation?.create(durationMillis, component.sized!!.b.px, toSize.b.px)

        override fun apply(renderer: Renderer) {

        }
    }
}

