/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
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

package org.polyfrost.polyui.component

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INPUT_HOVERED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.data.Cursor
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.Recolor
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.annotations.Dispatches
import org.polyfrost.polyui.utils.fastEach

/**
 * Extensions to [Component] which allow it to send and receive events.
 *
 * @since 1.6.0
 */
abstract class Inputtable(
    at: Vec2,
    size: Vec2,
    alignment: Align,
    @get:JvmName("isFocusable")
    val focusable: Boolean
) : Component(at, size, alignment) {
    private var eventHandlers: MutableMap<Any, EventListener<Inputtable, Event>>? = null


    @get:JvmName("acceptsInput")
    var acceptsInput = false
        get() = field && isEnabled

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            if (super.isEnabled == value) return
            if (value) {
                accept(Event.Lifetime.Enabled)
            } else {
                if (initialized) polyUI.inputManager.drop(this)
                accept(Event.Lifetime.Disabled)
            }
            super.isEnabled = value
        }

    /**
     * The current input state of this component. Note that this value is **not responsible** for dispatching events
     * such as [Event.Mouse.Exited] or [Event.Mouse.Pressed]. It is just here for tracking purposes and for your benefit.
     *
     * This value will be of [INPUT_NONE] (0), [INPUT_HOVERED] (1), or [INPUT_PRESSED] (2).
     *
     * **Do not** modify this value!
     * @since 1.0.0
     */
    @Dispatches("Mouse.Entered", "value > INPUT_NONE")
    @Dispatches("Mouse.Exited", "value == INPUT_NONE")
    @set:ApiStatus.Internal
    var inputState = INPUT_NONE
        set(value) {
            when {
                value == field -> return
                value == INPUT_NONE && field > INPUT_NONE -> {
                    accept(Event.Mouse.Exited)
                    var p = _parent
                    while (p is Inputtable) {
                        if (!p.isInside(polyUI.mouseX, polyUI.mouseY)) p.accept(Event.Mouse.Exited)
                        p = p._parent ?: break
                    }
                }

                field == INPUT_NONE && value > INPUT_NONE -> accept(Event.Mouse.Entered)
            }
            field = value
        }

    /**
     * focused flag for this component. Only ever true if [focusable] is true.
     * @since 1.4.2
     */
    @set:ApiStatus.Internal
    @get:JvmName("isFocused")
    var focused = false
        internal set(value) {
            if (!focusable) return
            if (field == value) return
            if (value) accept(Event.Focused.Gained)
            else accept(Event.Focused.Lost)
            field = value
        }

    override fun setup(polyUI: PolyUI): Boolean {
        if (initialized) return false
        this.polyUI = polyUI

        children?.fastEach { it.setup(polyUI) }
        removeHandlers(Event.Lifetime.Init)?.accept(this, Event.Lifetime.Init)
        position()
        removeHandlers(Event.Lifetime.PostInit)?.accept(this, Event.Lifetime.PostInit)
        return true
    }

    /**
     * called when this component receives an event.
     *
     * @return true if the event should be consumed (cancelled so no more handlers are called), false otherwise.
     */
    @MustBeInvokedByOverriders
    open fun accept(event: Event): Boolean {
        if (!isEnabled) return false
        val eh = eventHandlers ?: return false
        val handlers = eh[event::class.java] ?: eh[event] ?: return false
        return handlers.accept(this, event)
    }

    private fun removeHandlers(event: Event): EventListener<Inputtable, Event>? {
        val eh = eventHandlers ?: return null
        val key: Any = if (System.identityHashCode(event) == event.hashCode()) event::class.java else event
        val out = eh.remove(key)
        if (eh.isEmpty()) eventHandlers = null
        return out
    }

    /**
     * Clear all event handlers for this component.
     * @since 1.1.61
     */
    @ApiStatus.Experimental
    fun clearHandlers() {
        eventHandlers = null
    }

    @OverloadResolutionByLambdaReturnType
    fun <E : Event, S : Inputtable> S.on(event: E, handler: S.(E) -> Boolean): S {
        if (eventHandlers === EventListener.stated) eventHandlers = EventListener.makeStated()
        if (!acceptsInput && event !is Event.Lifetime) acceptsInput = true
        val ev = eventHandlers ?: HashMap(8, 1f)
        // asm: non-specific events will not override hashCode, so identityHashCode will return the same
        val ls = if (event.hashCode() == System.identityHashCode(event)) ev.getOrPut(event::class.java) { EventListener() }
        else ev.getOrPut(event) { EventListener() }
        @Suppress("UNCHECKED_CAST")
        ls.add(handler as Inputtable.(Event) -> Boolean)
        eventHandlers = ev
        return this
    }

    @JvmName("addEventhandler")
    @OverloadResolutionByLambdaReturnType
    inline fun <E : Event, S : Inputtable> S.on(event: E, crossinline handler: S.(E) -> Unit): S = on(event) { handler(this, it); false }

    // asm: uses java class because bootstrapping reflect for this is not worth the slightly better syntax tbh
    /**
     * returns `true` if this component has any [non-specific][Event] event handlers registered for it.
     *
     * pass an event instance to this method for [specific][Event] events.
     * @since 1.1.61
     */
    fun hasListenersFor(event: Class<out Event>) = eventHandlers?.containsKey(event) == true

    /**
     * returns `true` if this component has any [specific][Event] event handlers registered for it.
     *
     * pass a class instance to this method for [non-specific][Event] events.
     * @since 1.1.71
     */
    fun hasListenersFor(event: Event): Boolean {
        val k: Any = if (System.identityHashCode(event) == event.hashCode()) event::class.java else event
        return eventHandlers?.containsKey(k) == true
    }

    internal fun <S : Drawable> S.withStatesCached(): S {
        eventHandlers?.putAll(EventListener.makeStated()) ?: run { eventHandlers = EventListener.stated }
        acceptsInput = true
        return this
    }

    private class EventListener<S : Inputtable, E : Event> {
        private var first: (S.(E) -> Boolean)? = null
        private var second: (S.(E) -> Boolean)? = null
        private var multi: ArrayList<(S.(E) -> Boolean)>? = null

        fun accept(self: S, event: E): Boolean {
            if (first?.invoke(self, event) == true) return true
            if (second?.invoke(self, event) == true) return true
            multi?.fastEach { if (it(self, event)) return true }
            return false
        }

        fun add(handler: S.(E) -> Boolean) {
            if (first == null) first = handler
            else if (second == null) second = handler
            else {
                val multi = multi ?: ArrayList<S.(E) -> Boolean>(2).also { multi = it }
                multi.add(handler)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EventListener<*, *>) return false
            if (first != other.first) return false
            if (second != other.second) return false
            if (multi != other.multi) return false
            return true
        }

        override fun hashCode(): Int {
            var result = first?.hashCode() ?: 0
            result = 31 * result + (second?.hashCode() ?: 0)
            result = 31 * result + (multi?.hashCode() ?: 0)
            return result
        }

        companion object Cache {
            private val defEnter: Drawable.(Event.Mouse.Entered) -> Boolean = {
                Recolor(this, this.palette.hovered, Animations.Default.create(0.08.seconds)).add()
                polyUI.cursor = Cursor.Clicker
                false
            }

            private val defExit: Drawable.(Event.Mouse.Exited) -> Boolean = {
                Recolor(this, this.palette.normal, Animations.Default.create(0.08.seconds)).add()
                polyUI.cursor = Cursor.Pointer
                false
            }

            private val defPressed: Drawable.(Event.Mouse.Pressed) -> Boolean = {
                Recolor(this, this.palette.pressed, Animations.Default.create(0.08.seconds)).add()
                false
            }

            private val defReleased: Drawable.(Event.Mouse.Released) -> Boolean = {
                Recolor(this, this.palette.hovered, Animations.Default.create(0.08.seconds)).add()
                false
            }
            val stated = makeStated<Inputtable>()

            @Suppress("UNCHECKED_CAST")
            fun <S : Inputtable> makeStated(): MutableMap<Any, EventListener<S, Event>> {
                val out = HashMap<Any, EventListener<S, Event>>(8, 1f)
                out[Event.Mouse.Entered::class.java] = EventListener<S, Event>().also {
                    it.add(defEnter as S.(Event) -> Boolean)
                }
                out[Event.Mouse.Exited::class.java] = EventListener<S, Event>().also {
                    it.add(defExit as S.(Event) -> Boolean)
                }
                out[Event.Mouse.Pressed] = EventListener<S, Event>().also {
                    it.add(defPressed as S.(Event) -> Boolean)
                }
                out[Event.Mouse.Released] = EventListener<S, Event>().also {
                    it.add(defReleased as S.(Event) -> Boolean)
                }
                return out
            }
        }
    }
}
