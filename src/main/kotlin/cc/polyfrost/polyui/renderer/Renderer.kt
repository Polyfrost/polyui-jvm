package cc.polyfrost.polyui.renderer

interface Renderer {
    fun translate(x: Float, y: Float)
    fun scale(x: Float, y: Float)
    fun rotate(angle: Float)
}