package org.polyfrost.polyui.event

import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.polyui.utils.fastEach

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
    var value: T = value
        set(value) {
            if (field !== value) {
                if (instanceChangeOnlyListener?.invoke(value) == true) return
                if (notifyInternal(value)) return
                field = value
            }
        }

    private var firstListener: ((T) -> Boolean)? = null
    private var secondListener: ((T) -> Boolean)? = null
    private var extraListeners: ArrayList<(T) -> Boolean>? = null
    private var instanceChangeOnlyListener: ((T) -> Boolean)? = null

    fun notify() = notifyInternal(value)

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
}
