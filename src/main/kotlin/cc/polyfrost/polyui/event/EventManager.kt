/*
 * This file is part of PolyUI.
 * Copyright (C) 2022-2023 Polyfrost and its contributors.
 * All rights reserved.
 * PolyUI - Fast and lightweight UI framework https://polyfrost.cc https://github.com/Polyfrost/polui-jvm
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.event

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.fastRemoveIf

/** # EventManager
 * Handles all events and passes them to the correct components/layouts.
 * @param polyUI The PolyUI instance to use.
 */
class EventManager(private val polyUI: PolyUI) {
    private val mouseOverDrawables = ArrayList<Drawable>()
    var mouseX: Float = 0F
        private set
    var mouseY: Float = 0F
        private set
    private var clickTimer: Long = 0L

    /** amount of left clicks in the current combo */
    var clickAmount = 0
        private set

    /** weather or not the left button/primary click is DOWN (aka repeating) */
    var mouseDown = false
        private set

    fun onKeyPressed(key: Int) {
        polyUI.focused?.accept(FocusedEvents.KeyPressed(key))
    }

    fun onKeyReleased(key: Int) {
        polyUI.focused?.accept(FocusedEvents.KeyReleased(key))
    }

    fun setMousePosAndUpdate(x: Float, y: Float) {
        mouseX = x
        mouseY = y
        onApplicableDrawables_dontCheckChildren(x, y) {
            // we only need to check acceptsInput here as it controls the mouseOver flag, so if it doesn't accept input, mouseOver is always false
            if (isInside(x, y) && acceptsInput) {
                if (mouseOver) {
                    //it.accept(ComponentEvent.MouseMoved(x, y))
                } else {
                    accept(Events.MouseEntered)
                    mouseOverDrawables.add(this)
                    mouseOver = true
                }
            }
        }
        mouseOverDrawables.fastRemoveIf {
            (!it.isInside(x, y) && it.mouseOver).also { b ->
                if (b) {
                    it.accept(Events.MouseExited)
                    it.mouseOver = false
                }
            }
        }
    }

    private inline fun onApplicableLayouts(x: Float, y: Float, func: Layout.() -> Unit) {
        polyUI.master.children.fastEach {
            if (it.isInside(x, y)) {
                func(it)
            }
        }
        if (polyUI.master.isInside(x, y)) {
            func(polyUI.master)
        }
    }

    private inline fun onApplicableDrawables(x: Float, y: Float, func: Drawable.() -> Unit) {
        return onApplicableLayouts(x, y) {
            if (acceptsInput) func()
            components.fastEach { if (it.isInside(x, y)) func(it) }
        }
    }

    // don't check if the drawable is inside the mouse, this is unsafe and should only be used in setMousePosAndUpdate
    private inline fun onApplicableDrawables_dontCheckChildren(x: Float, y: Float, func: Drawable.() -> Unit) {
        return onApplicableLayouts(x, y) {
            if (acceptsInput) func()
            components.fastEach { func(it) }
        }
    }

    fun onMousePressed(button: Int) {
        if (button == 0) mouseDown = true
        val event = Events.MousePressed(button, mouseX, mouseY)
        onApplicableDrawables(mouseX, mouseY) {
            if (mouseOver) {
                if (accept(event)) return
            }
        }
    }

    fun onMouseReleased(button: Int) {
        if (button == 0) {
            mouseDown = false
            val curr = System.currentTimeMillis()
            if (curr - clickTimer < polyUI.renderer.settings.multiClickInterval) {
                if (clickAmount < polyUI.renderer.settings.maxClicksThatCanCombo) clickAmount++
                else clickAmount = 1
            } else {
                clickAmount = 1
            }
            clickTimer = curr
        }
        val event = Events.MouseReleased(button, mouseX, mouseY)
        var flagReleased = false
        var flagClicked = false
        val event2 = Events.MouseClicked(button, clickAmount)
        onApplicableDrawables(mouseX, mouseY) {
            if (mouseOver) {
                if (!flagReleased)
                    if (accept(event)) flagReleased = true
                if (!flagClicked)
                    if (accept(event2)) flagClicked = true
            }
        }
    }

    fun onMouseScrolled(amount: Int) {
        val event = Events.MouseScrolled(amount)
        onApplicableDrawables(mouseX, mouseY) {
            if (mouseOver) {
                if (accept(event)) return
            }
        }
    }

    fun getCharFromCode(key: Int): Char {
        TODO()
    }

    companion object {
        /** insert false return instruction on the end of the method */
        fun insertFalseInsn(action: (Component.() -> Unit)): (Component.() -> Boolean) {
            return {
                action(this)
                false
            }
        }
    }
}