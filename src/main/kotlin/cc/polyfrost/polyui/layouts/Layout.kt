package cc.polyfrost.polyui.layouts

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit

abstract class Layout(override val at: Point<Unit>, override var sized: Size<Unit>? = null, vararg items: Drawable) :
    Drawable {
    val components: Array<Component> = items.filterIsInstance<Component>().toTypedArray()
    val children: Array<Layout> = items.filterIsInstance<Layout>().toTypedArray()
    lateinit var fbo: Framebuffer
    final override lateinit var renderer: Renderer

    /** reference to parent */
    override var layout: Layout? = null

    init {
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
        if (this.sized == null) this.sized =
            getSize() ?: throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
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


    companion object {
        @JvmStatic
        fun items(vararg items: Drawable): Array<out Drawable> {
            return items
        }
    }
}