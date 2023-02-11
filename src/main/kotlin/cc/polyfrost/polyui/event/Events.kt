/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.event

import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.EventManager.Companion.insertFalseInsn
import cc.polyfrost.polyui.event.Events.*
import java.util.function.Consumer
import java.util.function.Function

/** Events that components can receive, for example [MouseClicked], [Added], [Removed], and more. */
sealed class Events : Event {
    // imagine this is a rust enum okay
    /** acceptable by component and layout */
    data class MousePressed internal constructor(val button: Int, val x: Float, val y: Float) : Events() {
        constructor(button: Int) : this(button, 0f, 0f)

        override fun hashCode(): Int {
            return button.hashCode()
        }
    }

    /** acceptable by component and layout */
    data class MouseReleased internal constructor(val button: Int, val x: Float, val y: Float) : Events() {
        constructor(button: Int) : this(button, 0f, 0f)

        override fun hashCode(): Int {
            return button.hashCode()
        }
    }

    data class MouseClicked @JvmOverloads constructor(val button: Int, val amountClicks: Int = 1) : Events()

    /** acceptable by component and layout */
    object MouseEntered : Events()

    /** acceptable by component and layout */
    object MouseExited : Events()

    /** acceptable by component and layout */
    data class MouseScrolled internal constructor(val amountX: Int, val amountY: Int) : Events() {
        constructor() : this(0, 0)

        override fun hashCode(): Int {
            return 0
        }
    }

    /** acceptable by component and layout */
    object Added : Events()

    /** acceptable by component and layout */
    object Removed : Events()


    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotate], [Component.recolor], etc.
     *
     * @return return true to consume the event/cancel it, false to pass it on to other handlers.
     * */
    @OverloadResolutionByLambdaReturnType
    infix fun to(action: (Component.() -> Boolean)): Handler {
        return Handler(this, action as Drawable.() -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotate], [Component.recolor], etc.
     *
     * @return returns a [Handler] for the event, which will return false when called, meaning it will not cancel the event. Return true to cancel the event.
     * */
    @OverloadResolutionByLambdaReturnType
    @JvmName("To")
    infix fun to(action: (Component.() -> Unit)): Handler {
        return Handler(this, insertFalseInsn(action) as Drawable.() -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotate], [Component.recolor], etc.
     *
     * @return return true to consume the event/cancel it, false to pass it on to other handlers.
     * */
    @OverloadResolutionByLambdaReturnType
    infix fun then(action: (Component.() -> Boolean)): Handler {
        return Handler(this, action as Drawable.() -> Boolean)
    }


    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotate], [Component.recolor], etc.
     *
     * @return returns a [Handler] for the event, which will return false when called, meaning it will not cancel the event. Return true to cancel the event.
     * */
    @OverloadResolutionByLambdaReturnType
    @JvmName("Then")
    infix fun then(action: (Component.() -> Unit)): Handler {
        return Handler(this, insertFalseInsn(action) as Drawable.() -> Boolean)
    }

    /**
     * Java compat version of [then]
     */
    fun then(action: Consumer<Component>): Handler = then { action.accept(this) }

    /**
     * Java compat version of [to]
     */
    fun to(action: Consumer<Component>): Handler = to { action.accept(this) }

    fun to(action: Function<Component, Boolean>): Handler = to { action.apply(this) }

    fun then(action: Function<Component, Boolean>): Handler = then { action.apply(this) }

    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun events(vararg events: Handler): Array<out Handler> {
            return events
        }
    }

    data class Handler(val event: Events, val handler: Drawable.() -> Boolean)
}

