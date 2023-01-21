package cc.polyfrost.polyui.color

import cc.polyfrost.polyui.animate.Animation

data class Color(val r: Float, val g: Float, val b: Float, val a: Float) {
    fun getARGB(): Int {
        return (a * 255).toInt() shl 24 or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }

    fun toMutable(): Mutable {
        return Mutable(r, g, b, a)
    }

    companion object {
        val NONE = Color(0f, 0f, 0f, 0f)
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
    }

    data class Mutable(var r: Float, var g: Float, var b: Float, var a: Float) {
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
                this.r = (targetColor!!.r - r) * percent + r
                this.g = (targetColor!!.g - g) * percent + g
                this.b = (targetColor!!.b - b) * percent + b
                this.a = (targetColor!!.a - a) * percent + a
            }
        }

        fun getARGB(): Int {
            return (a * 255).toInt() shl 24 or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }

        fun isRecoloring(): Boolean {
            return animation != null
        }
    }
}

