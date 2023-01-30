package cc.polyfrost.polyui.events

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.layouts.Layout

class EventManager(private val polyUI: PolyUI) {
    var mouseX: Float = 0F
        private set
    var mouseY: Float = 0F
        private set
    fun onKeyPressed(key: Int) {
        polyUI.focused?.accept(FocusedEvent.KeyPressed(key))
    }

    fun onKeyReleased(key: Int) {
        polyUI.focused?.accept(FocusedEvent.KeyReleased(key))
    }

    fun setMousePosAndUpdate(x: Float, y: Float) {
        mouseX = x
        mouseY = y
        onApplicableLayouts(x, y) {
            components.forEach {
                if (it.isInside(x, y)) {
                    if (it.mouseOver) {
                        //it.accept(ComponentEvent.MouseMoved(x, y))
                    } else {
                        it.mouseOver = true
                        it.accept(ComponentEvent.MouseEntered(x, y))
                    }
                } else if (it.mouseOver) {
                    it.accept(ComponentEvent.MouseExited(x, y))
                    it.mouseOver = false
                }
            }
        }
    }

    private inline fun onApplicableLayouts(x: Float, y: Float, func: Layout.() -> Unit) {
        return polyUI.master.children.forEach { if (it.isInside(x, y)) func(it) }
    }

    private inline fun onApplicableComponents(x: Float, y: Float, func: Component.() -> Unit) {
        return onApplicableLayouts(x, y) { components.forEach { if (it.isInside(x, y)) func(it) } }
    }

    fun onMousePressed(button: Int) {
        onApplicableComponents(mouseX, mouseY) {
            if (mouseOver) {
                accept(ComponentEvent.MousePressed(button))
            }
        }
    }

    fun onMouseReleased(button: Int) {
        onApplicableComponents(mouseX, mouseY) {
            if (mouseOver) {
                accept(ComponentEvent.MouseReleased(button))
            }
        }
    }

    fun onMouseScrolled(amount: Int) {
        onApplicableComponents(mouseX, mouseY) {
            if (mouseOver) {
                accept(ComponentEvent.MouseScrolled(amount))
            }
        }
    }


    fun getCharFromCode(key: Int): Char {
        TODO()
    }
}