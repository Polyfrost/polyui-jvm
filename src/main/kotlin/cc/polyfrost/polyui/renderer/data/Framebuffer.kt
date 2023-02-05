package cc.polyfrost.polyui.renderer.data

import cc.polyfrost.polyui.property.Settings

data class Framebuffer(val width: Float, val height: Float, val type: Settings.BufferType) {
    enum class Mode {
        Read, Write, ReadWrite
    }
}