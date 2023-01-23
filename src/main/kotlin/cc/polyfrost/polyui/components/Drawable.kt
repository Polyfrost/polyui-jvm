package cc.polyfrost.polyui.components

import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2
import org.jetbrains.annotations.ApiStatus


/** The most basic component in the PolyUI system. <br>
 * This class is implemented for both [cc.polyfrost.polyui.layouts.Layout] and [Component]. <br>
 */
@ApiStatus.Internal
interface Drawable {
    val at: Point<Unit>
    var sized: Size<Unit>?
    val renderer: Renderer

    /** reference to the layout encapsulating this drawable.
     * For components, this is never null, but for layouts, it can be null (meaning its parent is the polyui)
     */
    val layout: Layout?

    /** pre-render functions, such as applying transforms. */
    fun preRender()

    /** draw script for this drawable. */
    fun render()

    /** post-render functions, such as removing transforms. */
    fun postRender()

    /** calculate the position and size of this drawable.
     *
     * This method is called once the [layout] is populated for children and components, and when a recalculation is requested.
     *
     * The value of [layout]'s bounds will be updated after this method is called, so **do not** use [layout]'s bounds as an updated value in this method.
     */
    fun calculateBounds()

    /** method that is called when the physical size of the total window area changes. */
    fun rescale(scaleX: Float, scaleY: Float) {
        at.scale(scaleX, scaleY)
        sized!!.scale(scaleX, scaleY)
    }


    fun debugRender() {
        TODO("Not yet implemented")
    }

    fun x(): Float = at.x()
    fun y(): Float = at.y()
    fun width(): Float = sized!!.width()
    fun height(): Float = sized!!.height()

    fun isInside(x: Float, y: Float): Boolean {
        return x >= this.x() && x <= this.x() + this.width() && y >= this.y() && y <= this.y() + this.height()
    }

    fun unitType(): Unit.Type {
        if (at.type() != sized!!.type()) {
            throw Exception("Unit type mismatch: at and sized of $this do not use the same unit type.")
        }
        return at.type()
    }

    /** Implement this function to return the size of this drawable, if no size is specified during construction.
     *
     * This should be so that if the component can determine its own size (for example, it is an image), then the size parameter in the constructor can be omitted using:
     *
     * `sized: Vec2<cc.polyfrost.polyui.units.Unit>? = null;` and this method **needs** to be implemented!
     *
     *
     * Otherwise, the size parameter in the constructor must be specified.
     * @throws UnsupportedOperationException if this method is not implemented, and the size parameter in the constructor is not specified. */
    fun getSize(): Vec2<Unit> {
        throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
    }
}