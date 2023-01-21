package cc.polyfrost.polyui.events

import cc.polyfrost.polyui.components.Component

open class ComponentEvent(val type: Type) : Event {
    // imagine this is a rust enum okay
    data class MousePressed(val button: Int) : ComponentEvent(Type.MousePressed)
    data class MouseReleased(val button: Int) : ComponentEvent(Type.MouseReleased)
    data class MouseEntered(val x: Float, val y: Float) : ComponentEvent(Type.MouseEntered)
    data class MouseExited(val x: Float, val y: Float) : ComponentEvent(Type.MouseExited)
    data class MouseScrolled(val amount: Int) : ComponentEvent(Type.MouseScrolled)

    enum class Type : Event.Type {
        MousePressed,
        MouseReleased,
        MouseEntered,
        MouseExited,
        MouseScrolled;
    }

    fun on(action: Component.() -> Unit): Handler {
        return Handler(this.type, action)
    }


    companion object {
        @JvmStatic
        fun events(vararg events: Handler): Array<out Handler> {
            return events
        }
    }
    data class Handler(val type: Type, val handler: Component.() -> Unit)
}

