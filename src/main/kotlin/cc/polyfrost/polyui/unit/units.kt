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
val Number.seconds get() = (toDouble() * 1000.0).toLong()
val Number.minutes get() = (toDouble() * 60_000.0).toLong()
val Number.hours get() = (toDouble() * 3_600_000.0).toLong()

val origin = 0.px * 0.px
val fill = Unit.Percent(100f)
val fillv = fill * fill

fun index(index: Int) = Unit.Flex(index)

/** create a new flex unit, in vec2 form for at property. */
fun flex(index: Int = -1, flexShrink: Int = 0, flexGrow: Int = 0, endRowAfter: Boolean = false): Vec2<Unit> {
    val u = Unit.Flex(index, flexShrink, flexGrow, endRowAfter)
    return Vec2(u, u.clone())
}

/**
 * Declare a vec2 of unit, for either at or sized property.
 *
 * example:
 * `30.px * 30.px`
 * */
inline operator fun Unit.times(other: Unit): Vec2<Unit> = Vec2(this, other)