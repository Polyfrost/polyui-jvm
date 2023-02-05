package cc.polyfrost.polyui.component

import cc.polyfrost.polyui.event.FocusedEvent

/** implement this in order to receive focus event, such as key presses */
interface Focusable {
    /** called when this component is focused */
    fun focus()

    /** called when this component is unfocused */
    fun unfocus()

    fun accept(event: FocusedEvent)
}