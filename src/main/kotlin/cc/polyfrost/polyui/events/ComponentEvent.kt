package cc.polyfrost.polyui.events

import cc.polyfrost.polyui.components.Component

open class ComponentEvent : Event {
    // imagine this is a rust enum okay
    data class MousePressed(val button: Int) : ComponentEvent()
    data class MouseReleased(val button: Int) : ComponentEvent()
    data class MouseClicked(val button: Int, val amountClicks: Int = 1) : ComponentEvent()
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

    /** specify a handler for this event.
     *
     * in the given [action], you can perform things on this component, such as [Component.rotate], [Component.recolor], etc.*/
    open infix fun then(action: (Component.() -> Unit)): Handler {
        return Handler(this, action)
    }


    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun events(vararg events: Handler): Array<out Handler> {
            return events
        }
    }

    data class Handler(val event: ComponentEvent, val handler: Component.() -> Unit)
}

