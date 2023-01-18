package cc.polyfrost.polyui.renderer

import cc.polyfrost.polyui.PolyUI

abstract class Window(var title: String, var width: Int, var height: Int, val polyUI: PolyUI, val renderer: Renderer) {
    abstract fun open(): Window
    protected abstract fun closeWindow()
    protected abstract fun renameWindow(title: String)
    abstract fun fullscreen()


    fun onResize(newWidth: Int, newHeight: Int) {
        this.width = newWidth
        this.height = newHeight
        polyUI.onResize(newWidth, newHeight)
    }
    fun close() {
        polyUI.cleanup()
        closeWindow()
    }
    fun render() {
        polyUI.render()
    }
    fun rename(title: String): Window {
        this.title = title
        this.renameWindow(title)
        return this
    }
}