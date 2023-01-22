package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.animate.Animation

data class Color(val r: Byte, val g: Byte, val b: Byte, val a: Byte) {
    constructor(r: Int, g: Int, b: Int, a: Int) : this(r.toByte(), g.toByte(), b.toByte(), a.toByte())
    constructor(r: Float, g: Float, b: Float, a: Float) : this(
        (r * 255).toInt(),
        (g * 255).toInt(),
        (b * 255).toInt(),
        (a * 255).toInt()
    )

    fun getARGB(): Int {
        return (a.toInt() shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }

    fun toMutable(): Mutable {
        return Mutable(r, g, b, a)
    }

    companion object {
        val NONE = Color(0f, 0f, 0f, 0f)
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
    }

    data class Mutable(var r: Byte, var g: Byte, var b: Byte, var a: Byte) {
        private var animation: Animation? = null
        var targetColor: Color? = null
            private set

        fun toImmutable(): Color {
            return Color(r, g, b, a)
        }

        fun recolor(target: Color, animation: Animation? = null) {
            if (animation != null) {
                this.animation = animation
                this.targetColor = target
            } else {
                this.r = target.r
                this.g = target.g
                this.b = target.b
                this.a = target.a
            }
        }

        fun update(deltaTimeMillis: Long) {
            if (animation != null) {
                if (animation!!.finished) {
                    this.r = targetColor!!.r
                    this.g = targetColor!!.g
                    this.b = targetColor!!.b
                    this.a = targetColor!!.a
                    animation = null
                    targetColor = null
                    return
                }

                animation!!.update(deltaTimeMillis)
                val percent = animation!!.getPercentComplete()
                this.r = ((targetColor!!.r - r) * percent + r).toInt().toByte()
                this.g = ((targetColor!!.g - g) * percent + g).toInt().toByte()
                this.b = ((targetColor!!.b - b) * percent + b).toInt().toByte()
                this.a = ((targetColor!!.a - a) * percent + a).toInt().toByte()
            }
        }

        fun getARGB(): Int {
            return (a * 255) shl 24 or ((r * 255) shl 16) or ((g * 255) shl 8) or (b * 255)
        }

        fun isRecoloring(): Boolean {
            return animation != null
        }
    }
}

