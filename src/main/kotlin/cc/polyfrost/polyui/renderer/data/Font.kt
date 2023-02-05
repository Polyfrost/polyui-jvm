package cc.polyfrost.polyui.renderer.data

data class Font(val fileName: String) {
    val name: String = fileName.substringAfterLast("/")
        .substringBeforeLast(".")
}