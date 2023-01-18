package cc.polyfrost.polyui.layouts

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.renderer.Renderer

abstract class Layout(vararg items: Drawable) : Drawable {
    private val components: List<Component> = items.filterIsInstance<Component>()
    private val children: List<Layout> = items.filterIsInstance<Layout>()
    var dirty = false
        private set

    fun forEachComponent(action: (Component) -> Unit) {
        components.forEach(action)
        children.forEach { it.forEachComponent(action) }
    }

    override fun calculateBounds(layout: Layout) {
        forEachComponent { it.calculateBounds(this) }
    }

    override fun preRender(renderer: Renderer) {
        forEachComponent { it.preRender(renderer) }
    }

    override fun render(renderer: Renderer) {
        forEachComponent { it.render(renderer) }
    }

    override fun postRender(renderer: Renderer) {
        forEachComponent { it.postRender(renderer) }
    }

    override fun needsRedraw(): Boolean {
        return components.any { it.needsRedraw() } || children.any { it.needsRedraw() }
    }

    fun markDirty() {
        dirty = true
    }

    override fun needsRecalculation(): Boolean {
        return dirty || components.any { it.needsRecalculation() } || children.any { it.needsRecalculation() }
    }
}