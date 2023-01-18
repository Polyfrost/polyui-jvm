package cc.polyfrost.polyui.events

open class FocusedEvent : Event {
    data class KeyPressed(val key: Int) : FocusedEvent()
    data class KeyReleased(val key: Int) : FocusedEvent()
}