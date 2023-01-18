package cc.polyfrost.polyui.events

import cc.polyfrost.polyui.PolyUI

class EventManager(private val polyUI: PolyUI) {
    fun onKeyPressed(key: Int) {
        polyUI.focused?.accept(FocusedEvent.KeyPressed(key))
    }
    fun onKeyReleased(key: Int) {
        polyUI.focused?.accept(FocusedEvent.KeyReleased(key))
    }
    fun onMouseMove(x: Float, y: Float) {
        polyUI.forEachComponent { if (it.isInside(x, y)) {
            if(it.mouseOver) {
                //it.accept(ComponentEvent.MouseMoved(x, y))
            } else {
                it.mouseOver = true
                it.accept(ComponentEvent.MouseEntered(x, y))
            }
        } else if (it.mouseOver) {
            it.accept(ComponentEvent.MouseExited(x, y))
        }}
    }

    fun onMousePressed(button: Int) {
        polyUI.forEachComponent { if (it.mouseOver) {
            it.accept(ComponentEvent.MousePressed(button))
        }}
    }

    fun onMouseReleased(button: Int) {
        polyUI.forEachComponent { if (it.mouseOver) {
            it.accept(ComponentEvent.MouseReleased(button))
        }}
    }

    fun onMouseScrolled(amount: Int) {
        polyUI.forEachComponent { if (it.mouseOver) {
            it.accept(ComponentEvent.MouseScrolled(amount))
        }}
    }


    fun getCharFromCode(key: Int): Char {
        TODO()
    }
}