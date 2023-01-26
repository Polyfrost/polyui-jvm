package cc.polyfrost.polyui.renderer.data

data class Image(
    val fileName: String,
    var width: Int? = null,
    var height: Int? = null,
    val type: Type = Type.from(fileName)
) {
    enum class Type {
        PNG, SVG;

        companion object {
            @JvmStatic
            fun from(fileName: String): Type {
                return when (fileName.substringAfterLast(".")) {
                    "png" -> PNG
                    "svg" -> SVG
                    else -> throw IllegalArgumentException("Unknown image type for file $fileName")
                }
            }
        }
    }
}