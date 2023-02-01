package cc.polyfrost.polyui

import cc.polyfrost.polyui.components.Drawable
import cc.polyfrost.polyui.components.Focusable
import cc.polyfrost.polyui.events.EventManager
import cc.polyfrost.polyui.layouts.impls.PixelLayout
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.units.Point
import cc.polyfrost.polyui.units.Size
import cc.polyfrost.polyui.units.Unit
import cc.polyfrost.polyui.utils.forEachNoAlloc
import cc.polyfrost.polyui.utils.px
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * note: in early stages. I have not updated this kdoc.
 *
 * how this is going to work
 * 1. window: an abstract class that is impl for each renderer impl that creates a window.
 * 2. polyui: the screen that is rendered by the window. it handles frame buffers and its layouts.
 * 3. renderer: the renderer that is used to draw to the screen. it handles all the drawing methods.
 * 4. layout: a layout that is used to organize the components on the screen. can contain sub layouts as well.
 * 5. properties: component properties, such as colors, its events, etc.
 * 6. component: a component. need I say more?
 *
 * events:
 * animations will be mostly triggered by events. The only exception is an animation that is constantly happening e.g. a video.
 * these events will be for everything, from mouse enter, key press, etc.
 * events are dispatched to only the component that it is relevant to, like the one the mouse is over or the one that is currently focused.
 *
 * component:
 * a component is a drawable that can be focused and has events. basically everything.
 * it is UNAWARE of surrounding elements. it just has its raw x, y, width, height, draw scripts, and update scripts.
 * it does not need to know its position or parents as it is handled by a layout which DOES know what its relatives are. this prevents circular loops and stuff.
 * if I need to I may make it keep its layout for ease of use.
 *
 * on creation of a window, a screen is created and the matching renderer is created.
 * the window will then calculate all of its layouts sizes, then each layout will calculate its components sizes and sub layouts... -> this can be recalculated e.g. on window resize.
 * everything will be effectively scaled to the window size. This is a fundamental part of how it works, as basically saying 'draw this at this px' is not supported. It will all work on relevancy to the window size.
 * everything is rendered to a framebuffer. this framebuffer will only be redrawn if a component needs to be redrawn, like if there is an animation to do or something.
 * if it isn't redrawn its just handed right back to the renderer to draw statically = mad speed.
 *
 *
 * updated: performance information:
 * I have optimized this to use very little memory allocations. The only allocations that occur are the iterators for the layouts and stuff, and by using Arrays on most things (except component stuff) it rarely allocates; so memory usage is very constant.
 * It uses about 60MB of ram during usage, with 70% of that being OpenGL and the JVM itself, so about 10MB of RAM is used. (not bad?)
 * And it gets around 9000 fps with 8% CPU usage (lol)
 * CPU wise, the most expensive part of the code is OpenGL, with roughly 0.46% of the time being spent in the code itself. The rest is openGL, most expensive being blitFramebuffer and swapBuffers (15% and 68% respectively)
 *
 *
 */
class PolyUI(var width: Int, var height: Int, val renderer: Renderer, vararg items: Drawable) {
    val master = PixelLayout(Point(0.px(), 0.px()), Size(width.px(), height.px()), items = items)
    private var renderHooks = ArrayList<Renderer.() -> kotlin.Unit>(5)
    val eventManager = EventManager(this)
    private val settings = renderer.settings
    internal var focused: (Focusable)? = null

    init {
        master.giveRenderer(renderer)
        master.calculateBounds()
        master.children.forEachNoAlloc {
            if (settings.minItemsForFramebuffer > it.children.size + it.components.size) it.fbo =
                renderer.createFramebuffer(it.width().toInt(), it.height().toInt(), settings.bufferType)
            if (it.width() > width || it.height() > height) {
                LOGGER.warn("Layout $it is larger than the window. This may cause issues.")
            }
        }
        if (settings.masterIsFramebuffer) master.fbo = renderer.createFramebuffer(width, height, settings.bufferType)
        if (this.settings.debug) this.master.debugPrint()
        LOGGER.info("PolyUI initialized")
        Unit.VUnits.vHeight = height.toFloat()
        Unit.VUnits.vWidth = width.toFloat()
    }

    fun onResize(newWidth: Int, newHeight: Int) {
        // todo very amongsus
        println("resize: $newWidth x $newHeight")

        master.sized!!.a.px = newWidth.toFloat()
        master.sized!!.b.px = newHeight.toFloat()
        Unit.VUnits.vHeight = newHeight.toFloat()
        Unit.VUnits.vWidth = newWidth.toFloat()
        master.calculateBounds()
        if (settings.masterIsFramebuffer) {
            renderer.deleteFramebuffer(master.fbo!!)
            master.fbo = renderer.createFramebuffer(newWidth, newHeight, settings.bufferType)
        }

        master.children.forEachNoAlloc {
            it.rescale(
                newWidth.toFloat() / this.width.toFloat(),
                newHeight.toFloat() / this.height.toFloat()
            )
            if (it.fbo != null) {
                renderer.deleteFramebuffer(it.fbo!!)
                it.fbo = renderer.createFramebuffer(it.width().toInt(), it.height().toInt(), settings.bufferType)
            }
        }
        this.width = newWidth
        this.height = newHeight
    }

    fun render() {
        renderer.beginFrame(width, height)
        master.reRenderIfNecessary()
        renderHooks.forEachNoAlloc { it(renderer) }
        renderer.endFrame()
    }

    /** add something to be rendered after each frame. */
    fun addRenderHook(func: Renderer.() -> kotlin.Unit) {
        renderHooks.add(func)
    }

    fun removeComponent(drawable: Drawable) {
        master.removeComponent(drawable)
    }

    fun addComponent(drawable: Drawable) {
        master.addComponent(drawable)
    }

    fun addComponents(vararg drawables: Drawable) {
        master.addComponents(*drawables)
    }

    fun addComponents(drawables: Collection<Drawable>) {
        master.addComponents(drawables)
    }

    fun focus(drawable: Focusable) {
        focused?.unfocus()
        focused = drawable
        focused?.focus()
    }

    fun cleanup() {
        // todo
    }

    companion object {
        @JvmField
        val LOGGER: Logger = LoggerFactory.getLogger("PolyUI").also { it.info("PolyUI initializing") }
    }
}