package org.polyfrost.polyui.input

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.utils.IntArraySet
import org.polyfrost.polyui.utils.codepointToString
import org.polyfrost.polyui.utils.nullIfEmpty

open class PolyBind(unmappedKeys: IntArray? = null, keys: Array<Keys>? = null, mouse: IntArray? = null, @get:JvmName("getMods") mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, @Transient @set:ApiStatus.Internal var action: (Boolean) -> Boolean) {
    constructor(unmappedKeys: IntArray? = null, keys: Array<Keys>? = null, mouse: Array<Mouse>? = null, mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, action: (Boolean) -> Boolean) : this(
        unmappedKeys, keys,
        mouse?.map {
            it.value.toInt()
        }?.toIntArray(),
        mods, durationNanos, action,
    )

    constructor(unmappedKeys: IntArray? = null, keys: Array<Keys>? = null, mouse: Mouse? = null, mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, action: (Boolean) -> Boolean) : this(
        unmappedKeys, keys,
        mouse?.value?.let {
            intArrayOf(it.toInt())
        },
        mods, durationNanos, action,
    )

    constructor(unmappedKeys: IntArray? = null, key: Keys? = null, mouse: Array<Mouse>? = null, mods: Modifiers = Modifiers(0), durationNanos: Long = 0L, action: (Boolean) -> Boolean) : this(
        unmappedKeys,
        key?.let {
            arrayOf(it)
        },
        mouse, mods, durationNanos, action,
    )

    constructor(mods: Modifiers = Modifiers(0), action: (Boolean) -> Boolean) : this(unmappedKeys = null, key = null, mouse = null, mods = mods, durationNanos = 0L, action = action)

    var unmappedKeys = unmappedKeys.nullIfEmpty()
        internal set
    var keys = keys?.nullIfEmpty()
        internal set
    var mouse = mouse?.nullIfEmpty()
        internal set

    @get:JvmName("getMods")
    var mods = mods
        internal set
    var durationNanos = durationNanos
        internal set

    @Transient
    var muted = false

    @Transient
    private var time = 0L

    @Transient
    private var ran = false

    val size get() = (unmappedKeys?.size ?: 0) + (keys?.size ?: 0) + (mouse?.size ?: 0) + mods.size
    val isBound get() = size > 0

    internal fun update(c: IntArraySet, k: ArrayList<Keys>, m: IntArraySet, mods: Byte, deltaTimeNanos: Long, down: Boolean): Boolean {
        if (muted) return false
        if (durationNanos == 0L && deltaTimeNanos > 0L) return false // asm: we are not time-sensitive, so ignore
        if (!test(c, k, m, mods, down)) {
            time = 0L
            if (ran) {
                ran = false
                return action(false)
            }
            return false
        }
        if (durationNanos != 0L) {
            time += deltaTimeNanos
            if (time >= durationNanos && !ran) {
                ran = true
                return action(true)
            }
        } else if (down && !ran) {
            ran = true
            return action(true)
        }
        return false
    }

    /**
     * Reset the state of this keybind, meaning that it will call [action] with `false` if it was previously ran (i.e. is actively being held down),
     * and reset its internal state so that it can be used again. To be used in conjunction with [KeyBinder.release].
     */
    internal fun resetState() {
        if (ran) action(false)
        time = 0L
        ran = false
    }

    protected open fun test(c: IntArraySet, k: ArrayList<Keys>, m: IntArraySet, mods: Byte, down: Boolean): Boolean {
        if (!isBound) return false
        if (!unmappedKeys.matches(c)) return false
        if (!keys.matches(k)) return false
        if (!mouse.matches(m)) return false
        return this.mods.containedBy(Modifiers(mods))
    }

    final override fun toString(): String {
        val sb = StringBuilder()
        sb.append("KeyBind(")
        sb.append(keysToString())
        sb.append(')')
        return sb.toString()
    }

    fun keysToString(ifNotBound: String = "", getKeyName: ((Int) -> String) = { it.codepointToString() }): String {
        if (!isBound) return ifNotBound
        val sb = StringBuilder()
        val s = mods.fullName
        sb.append(s)
        var hasp = false
        if (unmappedKeys != null) {
            if (s.isNotEmpty()) sb.append(" + ")
            for (c in unmappedKeys) {
                sb.append(getKeyName(c))
                sb.append(" + ")
                hasp = true
            }
        }
        if (keys != null) {
            if (s.isNotEmpty()) sb.append(" + ")
            for (k in keys) {
                sb.append(Keys.toStringPretty(k))
                sb.append(" + ")
                hasp = true
            }
        }
        if (mouse != null) {
            if (s.isNotEmpty()) sb.append(" + ")
            for (m in mouse) {
                sb.append(Mouse.toStringPretty(Mouse.fromValue(m)))
                sb.append(" + ")
                hasp = true
            }
        }
        return if (hasp) sb.substring(0, sb.length - 3) else sb.toString()
    }

    final override fun equals(other: Any?): Boolean {
        if (other !is PolyBind) return false
        if (other.unmappedKeys?.contentEquals(unmappedKeys) == false) return false
        if (other.keys?.contentEquals(keys) == false) return false
        if (other.mouse?.contentEquals(mouse) == false) return false
        if (other.mods != mods) return false
        if (other.durationNanos != durationNanos) return false
        return true
    }

    final override fun hashCode(): Int {
        var result = unmappedKeys?.contentHashCode() ?: 0
        result = 31 * result + (keys?.contentHashCode() ?: 0)
        result = 31 * result + (mouse?.contentHashCode() ?: 0)
        result = 31 * result + mods.hashCode()
        result = 31 * result + durationNanos.hashCode()
        return result
    }

    protected fun <T> Array<T>?.matches(other: ArrayList<T>): Boolean {
        if (this == null) return true
        for (i in this) {
            if (i !in other) return false
        }
        return true
    }

    protected fun IntArray?.matches(other: IntArraySet): Boolean {
        if (this == null) return true
        for (i in this) {
            if (i !in other) return false
        }
        return true
    }
}
