package cc.polyfrost.polyui.layouts

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.units.Vec2
import cc.polyfrost.polyui.utils.px
import kotlin.math.max

abstract class Layout(override val at: Point<Unit>, override var sized: Size<Unit>? = null, vararg items: Drawable) :
    Drawable {
    val components: Array<Component> = items.filterIsInstance<Component>().toTypedArray()
    val children: Array<Layout> = items.filterIsInstance<Layout>().toTypedArray()
    lateinit var fbo: Framebuffer
    final override lateinit var renderer: Renderer
    abstract val atUnitType: Unit.Type

    /** reference to parent */
    override var layout: Layout? = null

    init {
        items.forEach {
            if (it.atUnitType() != atUnitType) {
                // todo make special exceptions that can tell you more verbosely which component is at fault
                throw Exception("Unit type mismatch: Drawable $it does not a valid unit type for layout: $atUnitType")
            }
        }
        components.forEach { it.layout = this }
        children.forEach { it.layout = this }
    }

    var needsRedraw = true
    var needsRecalculation = true

    fun reRenderIfNecessary() {
        for (it in children) {
            it.reRenderIfNecessary()
        }
        if (needsRedraw) {
            // todo framebuffer issues
//            renderer.bindFramebuffer(fbo)
            preRender()
            render()
            postRender()
//            renderer.unbindFramebuffer(fbo)
//            needsRedraw = false
        }
    }

    override fun calculateBounds() {
        components.forEach {
            it.calculateBounds()
        }
        children.forEach {
            it.calculateBounds()
        }
        if (this.sized == null) this.sized = getSize()
        needsRecalculation = false
    }

    override fun preRender() {
        components.forEach { it.preRender() }
    }

    override fun render() {
        components.forEach { it.render() }
    }

    override fun postRender() {
        components.forEach { it.postRender() }
    }

    override fun getSize(): Vec2<Unit> {
        var width = children.maxOfOrNull { it.x() + it.width() } ?: 0f
        width = max(width, components.maxOfOrNull { it.x() + it.width() } ?: 0f)
        var height = children.maxOfOrNull { it.y() + it.height() } ?: 0f
        height = max(height, components.maxOfOrNull { it.y() + it.height() } ?: 0f)
        if (width == 0f) throw Exception("unable to infer width of $layout: no sized children or components, please specify a size")
        if (height == 0f) throw Exception("unable to infer height of $layout: no sized children or components, please specify a size")
        return Vec2(width.px(), height.px())
    }

    fun debugPrint() {
        println("Layout: $this")
        println("Children: ${children.size}")
        println("Components: ${components.size}")
        println("At: $at")
        println("Sized: $sized")
        println("Needs redraw: $needsRedraw")
        println("Needs recalculation: $needsRecalculation")
        println("FBO: $fbo")
        println("Layout: $layout")
        println()
        children.forEach { it.debugPrint() }
    }

    /** give this, and all its children, a renderer. */
    fun giveRenderer(renderer: Renderer) {
        if (this::renderer.isInitialized) throw Exception("Renderer already initialized!") // sanity check
        this.renderer = renderer
        components.forEach { it.renderer = renderer }
        children.forEach { it.giveRenderer(renderer) }
    }
}