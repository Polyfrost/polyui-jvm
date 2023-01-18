package cc.polyfrost.polyui.components

import cc.polyfrost.polyui.events.FocusedEvent

/** implement this in order to receive focus events, such as key presses */
interface Focusable {
        /** called when this component is focused */
        fun onFocus()
        /** called when this component is unfocused */
        fun onUnfocus()

        /** accept an event. <br>
         * use the <pre>{@code when(event)  }</pre>
         * */
        fun accept(event: FocusedEvent)
}