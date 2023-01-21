package cc.polyfrost.polyui.renderer.data

import cc.polyfrost.polyui.properties.Settings

data class Framebuffer(val width: Int, val height: Int, val addr: Int, val type: Settings.BufferType) {

    enum class Mode {
        Read, Write, ReadWrite
    }
}