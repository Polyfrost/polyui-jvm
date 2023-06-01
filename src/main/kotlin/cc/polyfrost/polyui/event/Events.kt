/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

@file:Suppress("UNCHECKED_CAST")

package cc.polyfrost.polyui.event

import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.event.EventManager.Companion.insertTrueInsn
import cc.polyfrost.polyui.event.Events.*
import cc.polyfrost.polyui.input.Mouse
import java.util.function.Consumer
import java.util.function.Function

/** Events that components can receive, for example [MouseClicked], [Added], [Removed], and more. */
sealed class Events : Event {
    // imagine this is a rust enum okay
    /** acceptable by component and layout, when the mouse goes down on this drawable.
     * @see MouseReleased
     * @see MouseClicked
     * @see MouseEntered
     */
    data class MousePressed internal constructor(val button: Int, val x: Float, val y: Float, val mods: Short = 0) :
        Events() {
        constructor(button: Int) : this(button, 0f, 0f)

        override fun hashCode(): Int {
            var result = button + 500
            result = 31 * result + mods
            return result
        }

        override fun toString(): String =
            "MousePressed(($x, $y), ${Mouse.toStringPretty(Mouse.fromValue(button), mods)})"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MousePressed

            if (button != other.button) return false
            return mods == other.mods
        }
    }

    /** acceptable by component and layout, when the mouse is released on this component.
     *
     * Note that this event is **not dispatched** if the mouse leaves the drawable while being pressed, as it technically is not released on that drawable.
     * @see MousePressed
     * @see MouseClicked
     * @see MouseEntered
     */
    data class MouseReleased internal constructor(val button: Int, val x: Float, val y: Float, val mods: Short = 0) :
        Events() {
        constructor(button: Int) : this(button, 0f, 0f)

        override fun hashCode(): Int {
            var result = button + 5000 // avoid conflicts with MousePressed
            result = 31 * result + mods
            return result
        }

        override fun toString(): String =
            "MouseReleased(($x, $y), ${Mouse.toStringPretty(Mouse.fromValue(button), mods)})"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MouseReleased

            if (button != other.button) return false
            return mods == other.mods
        }
    }

    /** acceptable by components and layouts, and is dispatched when the mouse is clicked on this component.
     *
     * Note that this event is **not dispatched** if the mouse leaves the drawable while being pressed, as it technically is not released on that drawable.
     * @see MouseReleased
     * @see MouseClicked
     * @see MouseEntered
     */
    data class MouseClicked @JvmOverloads constructor(val button: Int, val amountClicks: Int = 1, val mods: Short = 0) :
        Events() {
        override fun toString(): String = "MouseClicked(${Mouse.toStringPretty(Mouse.fromValue(button), mods)})"
    }

    /** acceptable by component and layout, when the mouse enters this drawable.
     * @see MouseExited */
    data object MouseEntered : Events()

    /** acceptable by component and layout, when the mouse leaves this drawable.
     * @see MouseEntered */
    data object MouseExited : Events()

    /** Dispatched when the mouse is scrolled on this component/layout.
     *
     * acceptable by component and layout */
    data class MouseScrolled internal constructor(val amountX: Int, val amountY: Int, val mods: Short = 0) : Events() {
        constructor() : this(0, 0)

        override fun hashCode() = 0
    }

    /**
     * This event is dispatched when a component/layout is added **after** it has been initialized (i.e. not in the UI creation block), using the [addComponent][cc.polyfrost.polyui.layout.Layout.addComponent] function.
     *
     * acceptable by component and layout. */
    data object Added : Events()

    /**
     * This event is dispatched when a component/layout is removed, using the [removeComponent][cc.polyfrost.polyui.layout.Layout.removeComponent] function.
     *
     * acceptable by component and layout. */
    data object Removed : Events()

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @return return true to consume the event/cancel it, false to pass it on to other handlers.
     * */
    @OverloadResolutionByLambdaReturnType
    operator fun invoke(action: (Component.() -> Boolean)): Handler {
        return Handler(this, action as Drawable.() -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @return return true to consume the event/cancel it, false to pass it on to other handlers.
     * */
    @OverloadResolutionByLambdaReturnType
    @JvmName("Invoke")
    operator fun invoke(action: (Component.() -> Unit)): Handler {
        return Handler(this, insertTrueInsn(action) as Drawable.() -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @return return true to consume the event/cancel it, false to pass it on to other handlers.
     * */
    @OverloadResolutionByLambdaReturnType
    infix fun to(action: (Component.() -> Boolean)): Handler {
        return Handler(this, action as Drawable.() -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @return returns a [Handler] for the event, which will return true when called, meaning it will **consume** the event. Return false to not consume this event.
     * */
    @OverloadResolutionByLambdaReturnType
    @JvmName("To")
    infix fun to(action: (Component.() -> Unit)): Handler {
        return Handler(this, insertTrueInsn(action) as Drawable.() -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @return returns a [Handler] for the event, which will return true when called, meaning it will **consume** the event. Return false to not consume this event.
     * */
    @OverloadResolutionByLambdaReturnType
    infix fun then(action: (Component.() -> Boolean)): Handler {
        return Handler(this, action as Drawable.() -> Boolean)
    }

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotateBy], [Component.recolor], etc.
     *
     * @return returns a [Handler] for the event, which will return true when called, meaning it will **consume** the event. Return false to not consume this event.
     * */
    @OverloadResolutionByLambdaReturnType
    @JvmName("Then")
    infix fun then(action: (Component.() -> Unit)): Handler {
        return Handler(this, insertTrueInsn(action) as Drawable.() -> Boolean)
    }

    /**
     * Java compat version of [then]
     */
    fun then(action: Consumer<Component>): Handler = then { action.accept(this); true }

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
    fun then(action: Function<Component, Boolean>): Handler = then { action.apply(this) }

    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun events(vararg events: Handler): Array<out Handler> = events
    }

    data class Handler(val event: Events, val handler: Drawable.() -> Boolean)
}
