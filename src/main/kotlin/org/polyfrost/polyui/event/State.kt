package org.polyfrost.polyui.event

import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.polyui.event.State.Companion.map
import org.polyfrost.polyui.utils.fastEach
import java.lang.ref.WeakReference

/**
 * Simple and efficient state class that can be used to hold a value and notify listeners when the value changes.
 *
 * Listeners can be added using the [listen] method, which will be called with the new value when it changes.
 * They may cancel the change by returning `true` from the listener, in which case the value will not be updated.
 *
 * For mutable objects, the [notify] method can be used to notify listeners of changes to the object too.
 *
 * @param T the type of the value held by this state
 * @since 1.12.0
 */
open class State<T>(value: T) {
    private var v: T = value

    var value: T
        get() = v
        set(value) {
            set(value)
        }

    private var firstListener: ((T) -> Boolean)? = null
    private var secondListener: ((T) -> Boolean)? = null
    private var extraListeners: ArrayList<(T) -> Boolean>? = null
    private var instanceChangeOnlyListener: ((T) -> Boolean)? = null

    fun notify() = notifyInternal(v)

    /**
     * Set the value of this state, notifying listeners if the instance has changed.
     * @return `true` if the change was cancelled by a listener, `false` otherwise.
     */
    fun set(value: T): Boolean {
        if (this.v != value) {
            if (instanceChangeOnlyListener?.invoke(value) == true) return true
            if (notifyInternal(value)) return true
            v = value
        }
        return false
    }

    @MustBeInvokedByOverriders
    protected open fun notifyInternal(value: T): Boolean {
        firstListener?.let {
            if (it(value)) return true
        }
        secondListener?.let {
            if (it(value)) return true
        }
        extraListeners?.fastEach {
            if (it(value)) return true
        }
        return false
    }

    /**
     * Add a listener that will **only be called when the instance of the value changes**, so when [notify] is called, it will **NOT** be called.
     *
     * Note that only one of these is supported, and that the other one will be replaced and returned.
     */
    fun listenToInstanceChange(listener: (T) -> Boolean): ((T) -> Boolean)? {
        val old = instanceChangeOnlyListener
        instanceChangeOnlyListener = listener
        return old
    }

    @OverloadResolutionByLambdaReturnType
    @JvmName("listenZ")
    fun listen(listener: (T) -> Boolean): State<T> {
        if (firstListener == null) {
            firstListener = listener
        } else if (secondListener == null) {
            secondListener = listener
        } else {
            val multi = extraListeners
            if (multi == null) this.extraListeners = ArrayList<(T) -> Boolean>(5).also { it.add(listener) }
            else multi.add(listener)
        }
        return this
    }

    @OverloadResolutionByLambdaReturnType
    fun listen(listener: (T) -> Unit) = listen { listener(it); false }

    fun removeListener(listener: (T) -> Boolean): State<T> {
        if (firstListener === listener) {
            firstListener = null
        } else if (secondListener === listener) {
            secondListener = null
        } else {
            extraListeners?.remove(listener)
        }
        return this
    }

    /**
     * Create a new derived state from this state using the given [map] function.
     */
    fun <U> derive(map: (T) -> U): State<U> {
        val out = State(map(v))
        val ref = WeakReference(out)

        var listener: ((T) -> Boolean)? = null

        listener = {
            ref.get()?.set(map(it)) ?: run {
                this.removeListener(listener!!)
                false
            }
        }
        this.listen(listener)
        return out
    }

    /**
     * Make this state a direct copy of another [other] state, keeping both in sync.
     */
    fun copyFrom(other: State<T>): State<T> {
        this.set(other.value)
        val weak = WeakReference(this)
        var listener: ((T) -> Boolean)? = null
        var lock = false

        listener = listener@{
            if (lock) return@listener false
            lock = true
            val ret = weak.get()?.set(it) ?: run {
                other.removeListener(listener!!)
                false
            }
            lock = false
            ret
        }
        other.listen(listener)
        this.listen {
            if (lock) return@listen false
            lock = true
            val ret = other.set(this.value)
            lock = false
            ret
        }
        return this
    }

    companion object {
        /**
         * Map this state to another state using the given [map] function.
         * The function will be called whenever the value of this state changes, and the resulting state will be set on the new state.
         *
         * **Note that this is one-way and changes to the resulting state will not affect this state.**
         * @since 1.15.3
         */
        @JvmStatic
        fun <T, U> State<T>.map(map: (T) -> U): State<U> {
            val out = State(map(v))
            // weak reference is used as this would otherwise be a memory leak
            val ref = WeakReference(out)
            var listener: ((T) -> Boolean)? = null
            listener = {
                ref.get()?.set(map(it)) ?: run {
                    this.removeListener(listener!!)
                    false
                }
            }
            this.listen(listener)

            return out
        }

    }
}
