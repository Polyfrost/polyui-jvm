package cc.polyfrost.polyui

import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.components.Focusable
import cc.polyfrost.polyui.events.EventManager
import cc.polyfrost.polyui.layouts.Layout


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
 *
 */
class PolyUI(vararg layouts: Layout) {
    val layouts = mutableListOf(*layouts)
    val eventManager = EventManager(this)
    var focused: (Focusable)? = null

    fun onResize(newWidth: Int, newHeight: Int) {

    }

    fun render() {
        // todo
    }
    fun cleanup() {
        // todo
    }

    inline fun forEachComponent(crossinline action: (Component) -> Unit) {
        layouts.forEach { layout -> layout.forEachComponent { action(it) } }
    }
}