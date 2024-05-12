package org.polyfrost.polyui.utils

import org.jetbrains.annotations.ApiStatus

/**
 * An annotation to mark a function or property as having side effects.
 * @param `when` condition under which the side effects occur.
 * @param values the side effects that occur.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@MustBeDocumented
@Repeatable
@Retention(AnnotationRetention.SOURCE)
@ApiStatus.Experimental
annotation class SideEffects(val values: Array<String> = [], val `when`: String = "")
