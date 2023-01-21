package cc.polyfrost.polyui.components

import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.properties.Properties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.units.Box
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2
import cc.polyfrost.polyui.utils.Clock
import java.util.*

/** A component is a drawable object that can be interacted with. <br>
 * It has a [properties] attached to it, which contains various pieces of information about how this component should look, and its default responses to events. <br>*/
abstract class Component(
    val properties: Properties,
    at: Vec2<Unit>,
    size: Vec2<Unit>? = null,
    vararg events: ComponentEvent.Handler
) : Drawable {
    private val eventHandlers: EnumMap<ComponentEvent.Type, Component.() -> kotlin.Unit> =
        EnumMap(ComponentEvent.Type::class.java)

    // TODO perhaps optimize this to use an array
    private val animations: ArrayList<Animation> = ArrayList()
    private val transforms: ArrayList<TransformOp> = ArrayList()
    private val color: Color.Mutable = properties.color.toMutable()
    private val clock = Clock()
    override lateinit var layout: Layout
    final override val box = run {
        if (size == null) {
            Box(at, getSize())
        } else {
            Box(at, size)
        }
    }
    /** weather or not the mouse is currently over this component. DO NOT modify this value. It is managed automatically by [cc.polyfrost.polyui.events.EventManager]. */
    var mouseOver = false
    final override val boundingBox = box.expand(properties.margins)

    init {
        events.forEach {
            this.eventHandlers[it.type] = it.handler
        }
    }


    /**
     * Called when an event is received by this component.
     *
     * **make sure to call super [Component.accept]!**
     */
    open fun accept(event: ComponentEvent) {
        properties.eventHandlers[event.type]?.let { it(this) }
        eventHandlers[event.type]?.let { it(this) }
    }

    fun addEventHandler(type: ComponentEvent.Type, handler: Component.() -> kotlin.Unit) {
        eventHandlers[type] = handler
    }


    /** Add a [TransformOp] to this component. */
    open fun transform(transformOp: TransformOp) {
        transforms.add(transformOp)
    }

    open fun animate(animation: Animation) {
        animations.add(animation)
    }

    open fun recolor(toColor: Color, animation: Animation? = null) {
        color.recolor(toColor, animation)
    }

    override fun calculateBounds(layout: Layout) {

    }

    /**
     * Called before rendering.
     *
     * **make sure to call super [Component.preRender]!**
     */
    override fun preRender(renderer: Renderer) {
        animations.removeIf { it.finished }
        transforms.removeIf { it.finished }

        val delta = clock.getDelta()
        animations.forEach { it.update(delta) }
        transforms.forEach { it.update(delta) }
        color.update(delta)
        animations.forEach { if (it.finished) layout.needsRedraw = true }
        transforms.forEach { if (it.finished) layout.needsRedraw = true }

        transforms.forEach { it.apply(renderer) }
    }

    /**
     * Called after rendering.
     *
     * **make sure to call super [Component.postRender]!**
     */
    override fun postRender(renderer: Renderer) {
        transforms.forEach { it.unapply(renderer) }
    }

    /** Implement this function to return the size of the component, if no size is specified during construction. <br>
     *
     * This should be so that if the component can determine its own size, then the size parameter in the constructor can be omitted using:
     *
     * `size: Vec2<cc.polyfrost.polyui.units.Unit>? = null;` and this method **needs** to be implemented! <br>
     *
     *
     * Otherwise, the size parameter in the constructor must be specified. <br>
     * @throws UnsupportedOperationException if this method is not implemented, and the size parameter in the constructor is not specified. */
    open fun getSize(): Vec2<Unit> {
        throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
    }
}


