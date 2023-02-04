package cc.polyfrost.polyui.components

import cc.polyfrost.polyui.events.FocusedEvent

/** implement this in order to receive focus events, such as key presses */
interface Focusable {
    /** called when this component is focused */
    fun focus()

    /** called when this component is unfocused */
    fun unfocus()

    fun accept(event: FocusedEvent)
}