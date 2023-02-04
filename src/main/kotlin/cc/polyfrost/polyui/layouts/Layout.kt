package cc.polyfrost.polyui.layouts

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.events.ComponentEvent
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.utils.forEachNoAlloc
import org.jetbrains.annotations.ApiStatus

abstract class Layout(
    override val at: Point<Unit>,
    override var sized: Size<Unit>? = null,
    override var onAdded: (Drawable.() -> kotlin.Unit)? = null,
    override var onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    final override var acceptInput: Boolean = true,
    vararg items: Drawable
) : Drawable {
    val simpleName = this.toString().substringAfterLast(".")
    val components: ArrayList<Component> = items.filterIsInstance<Component>() as ArrayList<Component>
    val children: ArrayList<Layout> = items.filterIsInstance<Layout>() as ArrayList<Layout>

    // small arraylist because it's only used for removal
    val removeQueue: ArrayList<Drawable> = ArrayList(5)
    var fbo: Framebuffer? = null
    final override lateinit var renderer: Renderer

    /** reference to parent */
    override var layout: Layout? = null

    init {
        if (items.isEmpty()) throw IllegalStateException("Layout cannot be empty!")
        components.forEachNoAlloc { it.layout = this }
        children.forEachNoAlloc { it.layout = this }
    }

    var needsRedraw = true
    var needsRecalculation = true

    fun reRenderIfNecessary() {
        children.forEachNoAlloc { it.reRenderIfNecessary() }
        if (fbo != null) {
            if (needsRedraw) {
                // TODO framebuffer issues
//                renderer.bindFramebuffer(fbo!!)
                preRender()
                render()
                postRender()
                if (renderer.settings.debug) debugRender()
//                renderer.unbindFramebuffer(fbo!!)
//                needsRedraw = false
            } else renderer.drawFramebuffer(fbo!!, x(), y(), width(), height())
        } else {
            preRender()
            render()
            postRender()
            if (renderer.settings.debug) debugRender()
        }
    }

    /**
     * adds the given components to this layout.
     *
     * this will add the component to the [components] list, and invoke its [onAdded] function.
     */
    fun addComponents(components: Collection<Drawable>) {
        components.forEach { addComponent(it) }
    }

    /**
     * adds the given components to this layout.
     *
     * this will add the component to the [components] list, and invoke its [onAdded] function.
     */
    fun addComponents(vararg components: Drawable) {
        components.forEach { addComponent(it) }
    }

    /**
     * adds a component to this layout.
     *
     * this will add the component to the [components] list, and invoke its [onAdded] function.
     */
    open fun addComponent(drawable: Drawable) {
        when (drawable) {
            is Component -> {
                components.add(drawable)
                drawable.renderer = renderer
                drawable.layout = this
                // allows properties' onAdded to be called
                drawable.accept(ComponentEvent.Added)
            }

            is Layout -> {
                children.add(drawable)
                drawable.renderer = renderer
                drawable.layout = this
                drawable.onAdded?.invoke(drawable)
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        drawable.calculateBounds()
        needsRedraw = true
    }

    /**
     * removes a component from this layout.
     *
     * this will add the component to the removal queue, and invoke its [onRemoved] function.
     *
     * This removal queue is used so that components can finish up any animations they're doing before being removed.
     */
    open fun removeComponent(drawable: Drawable) {
        when (drawable) {
            is Component -> {
                removeQueue.add(components[components.indexOf(drawable)])
                drawable.accept(ComponentEvent.Removed)
            }

            is Layout -> {
                removeQueue.add(children[children.indexOf(drawable)])
                drawable.onRemoved?.invoke(drawable)
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
    }

    /** removes a component immediately, without waiting for it to finish up.
     *
     * This is marked as internal because you should be using [removeComponent] for most cases to remove a component, as it waits for it to finish and play any removal animations. */
    @ApiStatus.Internal
    fun removeComponentNow(drawable: Drawable) {
        removeQueue.remove(drawable)
        when (drawable) {
            is Component -> {
                if (!components.remove(drawable)) PolyUI.LOGGER.warn("Tried to remove component $drawable from $this, but it wasn't found!")
            }

            is Layout -> {
                if (!children.remove(drawable)) PolyUI.LOGGER.warn("Tried to remove layout $drawable from $this, but it wasn't found!")
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        needsRedraw = true
    }

    fun getComponents(): List<Component> {
        return components
    }

    fun getChildren(): List<Layout> {
        return children
    }

    override fun calculateBounds() {
        components.forEachNoAlloc {
            it.calculateBounds()
        }
        children.forEachNoAlloc {
            it.calculateBounds()
        }
        if (this.sized == null) this.sized =
            getSize() ?: throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
        needsRecalculation = false
    }

    override fun preRender() {
        removeQueue.forEachNoAlloc { if (it.canBeRemoved()) removeComponentNow(it) }
        components.forEachNoAlloc { it.preRender() }
    }

    override fun render() {
        components.forEachNoAlloc { it.render() }
    }

    override fun postRender() {
        components.forEachNoAlloc { it.postRender() }
    }

    override fun debugPrint() {
        println("Layout: $simpleName")
        println("Children: ${children.size}")
        println("Components: ${components.size}")
        println("At: $at")
        println("Sized: $sized")
        println("Needs redraw: $needsRedraw")
        println("Needs recalculation: $needsRecalculation")
        println("FBO: $fbo")
        println("Layout: $layout")
        println()
        children.forEachNoAlloc { it.debugPrint() }
    }

    override fun debugRender() {
        renderer.drawHollowRect(x(), y(), width(), height(), Color.GRAYf, 2)
        renderer.drawText(renderer.defaultFont, x() + 1, y() + 1, 0f, simpleName, Color.WHITE, 10f)
    }

    /** give this, and all its children, a renderer. */
    fun giveRenderer(renderer: Renderer) {
        if (this::renderer.isInitialized) throw Exception("Renderer already initialized!") // sanity check
        this.renderer = renderer
        components.forEachNoAlloc { it.renderer = renderer }
        children.forEachNoAlloc { it.giveRenderer(renderer) }
    }

    override fun canBeRemoved(): Boolean {
        return !needsRedraw
    }


    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun items(vararg items: Drawable): Array<out Drawable> {
            return items
        }
    }
}
