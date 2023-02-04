package cc.polyfrost.polyui.events

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.components.Component
import cc.polyfrost.polyui.layouts.Layout
import cc.polyfrost.polyui.utils.forEachNoAlloc
import cc.polyfrost.polyui.utils.removeIfNoAlloc

class EventManager(private val polyUI: PolyUI) {
    private val mouseOverComponents = ArrayList<Component>()
    var mouseX: Float = 0F
        private set
    var mouseY: Float = 0F
        private set
    private var clickTimer: Long = 0L
    var clickAmount = 0
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
            components.forEachNoAlloc {
                if (it.isInside(x, y)) {
                    if (it.mouseOver) {
                        //it.accept(ComponentEvent.MouseMoved(x, y))
                    } else {
                        it.accept(ComponentEvent.MouseEntered)
                        mouseOverComponents.add(it)
                        it.mouseOver = true
                    }
                }
            }
        }
        mouseOverComponents.removeIfNoAlloc {
            (!it.isInside(x, y) && it.mouseOver).also { b ->
                if (b) {
                    it.accept(ComponentEvent.MouseExited)
                    it.mouseOver = false
                }
            }
        }
    }

    private inline fun onApplicableLayouts(x: Float, y: Float, func: Layout.() -> Unit) {
        polyUI.master.children.forEachNoAlloc {
            if (it.isInside(x, y)) {
                func(it)
            }
        }
        if (polyUI.master.isInside(x, y)) {
            func(polyUI.master)
        }
    }

    private inline fun onApplicableComponents(x: Float, y: Float, func: Component.() -> Unit) {
        return onApplicableLayouts(x, y) { components.forEachNoAlloc { if (it.isInside(x, y)) func(it) } }
    }

    fun onMousePressed(button: Int) {
        onApplicableComponents(mouseX, mouseY) {
            if (mouseOver) {
                accept(ComponentEvent.MousePressed(button))
            }
        }
    }

    fun onMouseReleased(button: Int) {
        val curr = System.currentTimeMillis()
        if (curr - clickTimer < polyUI.renderer.settings.multiClickInterval) {
            if (clickAmount < polyUI.renderer.settings.maxClicksThatCanCombo) clickAmount++
            else clickAmount = 1
        } else {
            clickAmount = 1
        }
        clickTimer = curr
        onApplicableComponents(mouseX, mouseY) {
            if (mouseOver) {
                accept(ComponentEvent.MouseReleased(button))
                accept(ComponentEvent.MouseClicked(button, clickAmount))
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