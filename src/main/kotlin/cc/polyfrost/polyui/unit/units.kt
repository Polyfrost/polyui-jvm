@file:Suppress("NOTHING_TO_INLINE")

package cc.polyfrost.polyui.unit

operator fun Float.compareTo(x: Unit): Int = compareTo(x.px)

val Number.px get() = Unit.Pixel(this.toFloat())
val Number.percent get() = Unit.Percent(this.toFloat())

val Number.vwidth get() = Unit.VUnits(this.toFloat(), Unit.Type.VWidth)
val Number.vheight get() = Unit.VUnits(this.toFloat(), Unit.Type.VHeight)
val Number.vmin get() = Unit.VUnits(this.toFloat(), Unit.Type.VMin)
val Number.vmax get() = Unit.VUnits(this.toFloat(), Unit.Type.VMax)

val Number.milliseconds get() = toLong()
val Number.seconds get() = (toDouble() * 1000).toLong()
val Number.minutes get() = (toDouble() * 1000 * 60).toLong()
val Number.hours get() = (toDouble() * 1000 * 60 * 60).toLong()

val origin = 0.px * 0.px
val fill = Unit.Percent(100f)
val fillv = fill * fill

fun index(index: Int) = Unit.Flex(index)

/** create a new flex unit, in vec2 form for at property. */
fun flex(index: Int = -1, flexShrink: Int = 0, flexGrow: Int = 0): Vec2<Unit> {
    val u = Unit.Flex(index, flexShrink, flexGrow)
    // i'm not mad.
    return Vec2(u, u.clone())
}

/**
 * Declare a vec2 of unit, for either at or sized property.
 *
 * example:
 * `30.px() x 30.px()`
 * */
inline operator fun Unit.times(other: Unit): Vec2<Unit> = Vec2(this, other)