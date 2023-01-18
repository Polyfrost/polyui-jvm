package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.renderer.Window

class GLWindow(title: String, width: Int, height: Int, polyUI: PolyUI) : Window(title, width, height, polyUI, NVGRenderer()) {
    override fun open(): Window {
        TODO("Not yet implemented")
    }

    override fun closeWindow() {
        TODO("Not yet implemented")
    }

    override fun renameWindow(title: String) {
        TODO("Not yet implemented")
    }

    override fun fullscreen() {
        TODO("Not yet implemented")
    }

}