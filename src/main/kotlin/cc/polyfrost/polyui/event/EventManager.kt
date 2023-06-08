/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package cc.polyfrost.polyui.event

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.Focusable
import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.input.Keys
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.utils.fastEach
import cc.polyfrost.polyui.utils.fastRemoveIf
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * # EventManager
 * Handles all events and passes them to the correct components/layouts.
 * @param polyUI The PolyUI instance to use.
 */
class EventManager(private val polyUI: PolyUI) {
    private val mouseOverDrawables = ArrayList<Drawable>()
    var mouseX: Float = 0f
        private set
    var mouseY: Float = 0f
        private set
    private var clickTimer: Long = 0L

    /** @see cc.polyfrost.polyui.input.Modifiers */
    var keyModifiers: Short = 0
        private set

    /** amount of clicks in the current combo */
    private var clickAmount = 0

    /** tracker for the combo */
    private var clickedButton: Int = 0

    /** weather or not the left button/primary click is DOWN (aka repeating) */
    var mouseDown = false
        private set

    /** This method should be called when a printable key is typed. This key should be **mapped to the user's keyboard layout!** */
    fun onKeyTyped(key: Char, isRepeat: Boolean) {
        val event = FocusedEvents.KeyTyped(key, keyModifiers, isRepeat)
        if (!isRepeat) {
            if (polyUI.keyBinder.accept(event)) return
        }
        polyUI.focused?.accept(event)
    }

    /** This method should be called when a non-printable key is pressed. */
    fun onUnprintableKeyTyped(key: Keys, isRepeat: Boolean) {
        val event = FocusedEvents.KeyPressed(key, keyModifiers, isRepeat)
        if (!isRepeat) {
            if (polyUI.keyBinder.accept(event)) return
        }
        polyUI.focused?.accept(event)
    }

    /**
     * add a modifier to the current keyModifiers.
     * @see KeyModifiers
     */
    fun addModifier(modifier: Short) {
        keyModifiers = keyModifiers or modifier
    }

    /**
     * remove a modifier from the current keyModifiers.
     * @see KeyModifiers
     */
    fun removeModifier(modifier: Short) {
        keyModifiers = keyModifiers xor modifier
    }

    /** call this function to update the mouse position. It also will update all necessary mouse over flags. */
    fun setMousePosAndUpdate(x: Float, y: Float) {
        mouseX = x
        mouseY = y
        if (!mouseDown) {
            onApplicableDrawablesUnsafe(x, y) {
                if (isInside(x, y) && acceptsInput) {
                    if (!mouseOver) {
                        accept(Events.MouseEntered)
                        mouseOverDrawables.add(this)
                        mouseOver = true
                    }
                }
            }
        }
        mouseOverDrawables.fastRemoveIf {
            (!it.isInside(x, y)).also { b ->
                if (b) {
                    it.accept(Events.MouseExited)
                    it.mouseOver = false
                }
            }
        }
    }

    private fun onApplicableLayouts(x: Float, y: Float, func: Layout.() -> Unit) {
        polyUI.master.onAllLayouts {
            if (isInside(x, y)) {
                func(this)
            }
        }
    }

    private fun onApplicableDrawables(x: Float, y: Float, func: Drawable.() -> Unit) {
        return onApplicableLayouts(x, y) {
            if (acceptsInput) func()
            components.fastEach { if (it.isInside(x, y)) func(it) }
        }
    }

    // don't check if the drawable is inside the mouse, this is unsafe and should only be used in setMousePosAndUpdate
    private fun onApplicableDrawablesUnsafe(x: Float, y: Float, func: Drawable.() -> Unit) {
        return onApplicableLayouts(x, y) {
            components.fastEach { func(it) }
            if (acceptsInput) func()
        }
    }

    fun onMousePressed(button: Int) {
        if (button == 0) mouseDown = true
        val event = Events.MousePressed(button, mouseX, mouseY, keyModifiers)
        if (polyUI.keyBinder.accept(event)) return
        onApplicableDrawables(mouseX, mouseY) {
            if (mouseOver) {
                if (button == 0 && this is Focusable) {
                    polyUI.focus(this)
                    return@onApplicableDrawables
                }
                if (accept(event)) return@onApplicableDrawables
            }
        }
    }

    /** call this function when a mouse button is released. */
    fun onMouseReleased(button: Int) {
        if (button == 0) {
            mouseDown = false
        }
        if (clickedButton != button) {
            clickedButton = button
            clickAmount = 1
        } else {
            val curr = System.nanoTime()
            if (curr - clickTimer < polyUI.renderer.settings.comboMaxInterval) {
                if (clickAmount < polyUI.renderer.settings.maxComboSize) {
                    clickAmount++
                } else if (polyUI.settings.clearComboWhenMaxed) {
                    clickAmount = 1
                }
            } else {
                clickAmount = 1
            }
            clickTimer = curr
            if (polyUI.focused != null) {
                if (!(polyUI.focused as Drawable).isInside(mouseX, mouseY)) {
                    polyUI.unfocus()
                }
            }
        }
        val event = Events.MouseReleased(button, mouseX, mouseY, keyModifiers)
        var releaseCancelled = false
        var clickedCancelled = false
        val event2 = Events.MouseClicked(button, clickAmount, keyModifiers)
        if (polyUI.keyBinder.accept(event)) return
        if (polyUI.keyBinder.accept(event2)) return
        onApplicableDrawables(mouseX, mouseY) {
            if (mouseOver) {
                if (!releaseCancelled) {
                    if (accept(event)) releaseCancelled = true
                }
                if (!clickedCancelled) {
                    if (accept(event2)) clickedCancelled = true
                }
            }
        }
    }

    /** call this function when the mouse is scrolled. */
    @Suppress("NAME_SHADOWING")
    fun onMouseScrolled(amountX: Int, amountY: Int) {
        var amountX = if (polyUI.settings.naturalScrolling) amountX else -amountX
        var amountY = if (polyUI.settings.naturalScrolling) amountY else -amountY
        if ((keyModifiers and KeyModifiers.LSHIFT.value) != 0.toShort()) {
            (amountX to amountY).let {
                amountX = it.second
                amountY = it.first
            }
        }
        val (sx, sy) = polyUI.settings.scrollMultiplier
        val event = Events.MouseScrolled(amountX * sx, amountY * sy, keyModifiers)
        if (polyUI.keyBinder.accept(event)) return
        onApplicableDrawables(mouseX, mouseY) {
            if (mouseOver) {
                if (accept(event)) return@onApplicableDrawables
            }
        }
    }

    companion object {
        /** insert true return instruction on the end of the method */
        @JvmStatic
        fun insertTrueInsn(action: (Component.() -> Unit)): (Component.() -> Boolean) {
            return {
                action(this)
                true
            }
        }
    }
}
