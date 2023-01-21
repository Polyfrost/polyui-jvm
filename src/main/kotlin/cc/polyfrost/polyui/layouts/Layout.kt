package cc.polyfrost.polyui.layouts

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Framebuffer

abstract class Layout(vararg items: Drawable) : Drawable {
    val components: Array<Component> = items.filterIsInstance<Component>().toTypedArray()
    private val children: Array<Layout> = items.filterIsInstance<Layout>().toTypedArray()
    lateinit var fbo: Framebuffer

    init {
        components.forEach { it.layout = this }
    }

    var needsRedraw = true
    var needsRecalculation = true

    fun reRenderIfNecessary(renderer: Renderer) {
        for (it in children) {
            it.reRenderIfNecessary(renderer)
        }
        if (needsRedraw) {
            renderer.bindFramebuffer(fbo)
            preRender(renderer)
            render(renderer)
            postRender(renderer)
            renderer.unbindFramebuffer(fbo)
            needsRedraw = false
        }
    }

    override fun calculateBounds(layout: Layout) {
        children.forEach { it.calculateBounds(this) }
        components.forEach { it.calculateBounds(this) }
        box.width.v = children.maxOfOrNull { it.box.width() } ?: box.width()
        box.height.v = children.maxOfOrNull { it.box.height() } ?: box.height()
    }

    override fun preRender(renderer: Renderer) {
        components.forEach { it.preRender(renderer) }
    }

    override fun render(renderer: Renderer) {
        components.forEach { it.render(renderer) }
    }

    override fun postRender(renderer: Renderer) {
        components.forEach { it.postRender(renderer) }
    }
}