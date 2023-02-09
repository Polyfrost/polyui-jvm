/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.event

import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.event.ComponentEvent.*
import java.util.function.Consumer

/** Events that components can receive, for example [MouseClicked], [Added], [Removed], and more. */
sealed class ComponentEvent : Event {
    // imagine this is a rust enum okay
    data class MousePressed(val button: Int) : ComponentEvent()
    data class MouseReleased(val button: Int) : ComponentEvent()
    data class MouseClicked @JvmOverloads constructor(val button: Int, val amountClicks: Int = 1) : ComponentEvent()
    object MouseEntered : ComponentEvent()
    object MouseExited : ComponentEvent()
    data class MouseScrolled(val amount: Int) : ComponentEvent()
    object Added : ComponentEvent()
    object Removed : ComponentEvent()


    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotate], [Component.recolor], etc.*/
    open infix fun to(action: (Component.() -> Unit)): Handler {
        return Handler(this, action)
    }

    /**
     * Java compat version of [to]
     */
    fun to(action: Consumer<Component>): Handler = to { action.accept(this) }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotate], [Component.recolor], etc.*/
    open infix fun then(action: (Component.() -> Unit)): Handler {
        return Handler(this, action)
    }

    /**
     * Java compat version of [then]
     */
    fun then(action: Consumer<Component>): Handler = then { action.accept(this) }

    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun events(vararg events: Handler): Array<out Handler> {
            return events
        }
    }

    data class Handler(val event: ComponentEvent, val handler: Component.() -> Unit)
}

