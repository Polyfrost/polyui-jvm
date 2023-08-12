/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.polyfrost.polyui.event

import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

/**
 * An event in the PolyUI system.
 * @see EventManager
 */
@Suppress("UNCHECKED_CAST", "INAPPLICABLE_JVM_NAME")
interface Event {

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @see then
     * @return return true to consume the event/cancel it, false to pass it on to other handlers.
     * */
    @OverloadResolutionByLambdaReturnType
    infix fun to(action: (Component.(Event) -> Boolean)): Handler {
        return Handler(this, action as Drawable.(Event) -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @see then
     * @return returns a [Handler] for the event, which will return true when called, meaning it will **consume** the event. Return false to not consume this event.
     * */
    @OverloadResolutionByLambdaReturnType
    @JvmName("To")
    infix fun to(action: (Component.(Event) -> Unit)): Handler {
        return Handler(this, insertTrueInsn(action) as Drawable.(Event) -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @return return true to consume the event/cancel it, false to pass it on to other handlers.
     * @see to
     * @since 0.19.2
     * */
    @OverloadResolutionByLambdaReturnType
    infix fun then(action: (Component.(Event) -> Boolean)): Handler {
        return Handler(this, action as Drawable.(Event) -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @return returns a [Handler] for the event, which will return true when called, meaning it will **consume** the event. Return false to not consume this event.
     * @see to
     * @since 0.19.2
     * */
    @OverloadResolutionByLambdaReturnType
    @JvmName("Then")
    infix fun then(action: (Component.(Event) -> Unit)): Handler {
        return Handler(this, insertTrueInsn(action) as Drawable.(Event) -> Boolean)
    }

    /**
     * Java compat version of [to]
     */
    fun to(action: Consumer<Component>): Handler = to { action.accept(this); true }

    /**
     * Java compat version of [to]
     */
    fun to(action: Function<Component, Boolean>): Handler = to { action.apply(this) }

    /**
     * Java compat version of [then]
     */
    fun then(action: BiConsumer<Component, Event>): Handler = then { action.accept(this, it); true }

    /**
     * Java compat version of [then]
     */
    fun then(action: BiFunction<Component, Event, Boolean>): Handler = then { action.apply(this, it) }

    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        @EventDSL
        fun events(vararg events: @EventDSL Handler): Array<out Handler> = events

        @JvmStatic
        fun insertTrueInsn(action: (Component.(Event) -> Unit)): (Component.(Event) -> Boolean) {
            return {
                action(this, it)
                true
            }
        }
    }

    class Handler(val event: Event, val handler: (Drawable.(Event) -> Boolean))
}

/** marker class for preventing illegal nesting. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@DslMarker
annotation class EventDSL
