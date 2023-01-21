package cc.polyfrost.polyui.renderer

import cc.polyfrost.polyui.PolyUI

/** # Window
 * This class represents the physical window that PolyUI will render to.
 * As a rendering implementation, you must:
 *  - implement all the methods
 *  - create callbacks for the event methods in PolyUI e.g. [PolyUI.onResize]
 *  - create callbacks for all the event-related methods in [cc.polyfrost.polyui.events.EventManager]
 *  - call [open] to start the rendering loop; this can be blocking or non-blocking. Please note that after open is called, the rendering implementation will be created. This means that in a thread-based system such as LWJGL's OpenGL, you **must** ensure that it is fully setup before exiting `init {}`.
 */
abstract class Window(var title: String, var width: Int, var height: Int) {
    abstract fun open(polyUI: PolyUI): Window
    abstract fun setIcon(icon: String)
    protected abstract fun closeWindow()
    protected abstract fun renameWindow(title: String)
    abstract fun fullscreen()

    /** Create the callbacks for window events.
     * @see Window */
    abstract fun createCallbacks()

    fun close() {
        closeWindow()
    }

    fun rename(title: String): Window {
        this.title = title
        this.renameWindow(title)
        return this
    }
}