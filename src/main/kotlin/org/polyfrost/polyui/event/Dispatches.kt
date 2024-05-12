package org.polyfrost.polyui.event

import org.jetbrains.annotations.ApiStatus

/**
 * An annotation to mark a function or property as dispatching events.
 * @param `when` condition under which the events are dispatched.
 * @param event the events that are dispatched.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@ApiStatus.Experimental
annotation class Dispatches(val event: String = "", val `when`: String = "")
