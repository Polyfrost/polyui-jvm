/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package cc.polyfrost.polyui.event

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.Focusable
import cc.polyfrost.polyui.input.KeyModifiers
import cc.polyfrost.polyui.input.Keys
import cc.polyfrost.polyui.layout.Layout
import cc.polyfrost.polyui.utils.fastEachReversed
import cc.polyfrost.polyui.utils.fastRemoveIf
import org.jetbrains.annotations.ApiStatus
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * # EventManager
 * Handles all events and passes them to the correct components/layouts.
 * @param polyUI The PolyUI instance to use.
 */
class EventManager(private val polyUI: PolyUI) {
    private val mouseOverObjects = ArrayList<Drawable>(3) // asm: it is not expected to have many of this type under hover at once
    private var mouseOverObj: Drawable? = null
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

    private var clickedCancelled = false
    private var releaseCancelled = false

    /** weather or not the left button/primary click is DOWN (aka repeating) */
    var mouseDown = false
        private set

    /** This method should be called when a printable key is typed. This key should be **mapped to the user's keyboard layout!** */
    fun keyTyped(key: Char) {
        polyUI.focused?.accept(FocusedEvent.KeyTyped(key, keyModifiers))
    }

    /** This method should be called when a non-printable, but representable key is pressed. */
    fun keyDown(key: Keys) {
        val event = FocusedEvent.KeyPressed(key, keyModifiers)
        if (polyUI.keyBinder.accept(event)) return
        polyUI.focused?.accept(event)
    }

    /** This method should be called when a non-printable, but representable key is released. */
    fun keyUp(key: Keys) {
        val event = FocusedEvent.KeyReleased(key, keyModifiers)
        if (polyUI.keyBinder.accept(event)) return
        polyUI.focused?.accept(event)
    }

    /**
     * This method should be called when a non-representable key is pressed.
     *
     * This is used solely for keybinding, and so the key can be any value, as long as it is consistent, and unique to that key.
     */
    fun keyDown(code: Int) {
        if (polyUI.keyBinder.accept(code, true)) return
        polyUI.focused?.accept(code, true)
    }

    /**
     * This method should be called when a non-representable key is released.
     *
     * This is used solely for keybinding, and so the key can be any value, as long as it is consistent, and unique to that key.
     */
    fun keyUp(code: Int) {
        if (polyUI.keyBinder.accept(code, false)) return
        polyUI.focused?.accept(code, false)
    }

    /**
     * Internal function that will force the mouse position to be updated.
     * @since 0.18.5
     */
    @ApiStatus.Internal
    @Suppress("NOTHING_TO_INLINE")
    inline fun recalculateMousePos() = setMousePosAndUpdate(mouseX, mouseY)

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

    /**
     * Clear the current keyModifiers.
     * @see KeyModifiers
     * @see addModifier
     * @since 0.20.1
     */
    fun clearModifiers() {
        keyModifiers = 0
    }

    private fun mouseCalculate(drawable: Drawable, x: Float, y: Float): Boolean {
        if ((!drawable.consumesHover || mouseOverObj == null) && drawable.acceptsInput && drawable.isInside(x, y)) {
            if (!drawable.mouseOver) {
                drawable.mouseOver = true
                if (!drawable.consumesHover) {
                    mouseOverObjects.add(drawable)
                    if (drawable.accept(MouseEntered)) {
                        return true
                    }
                } else {
                    mouseOverObj = drawable
                    drawable.accept(MouseEntered)
                    return true
                }
            }
        }
        return false
    }

    private fun calcMouse(layout: Layout, x: Float, y: Float) {
        if (layout.isInside(x, y)) {
            for (i in layout.children.size - 1 downTo 0) {
                calcMouse(layout.children[i], x, y)
            }
            for (i in layout.components.size - 1 downTo 0) {
                if (mouseCalculate(layout.components[i], x, y)) {
                    return
                }
            }
            mouseCalculate(layout, x, y)
        }
    }

    /** call this function to update the mouse position. It also will update all necessary mouse over flags. */
    fun setMousePosAndUpdate(x: Float, y: Float) {
        if (mouseX == x && mouseY == y) return
        mouseX = x
        mouseY = y
        if (!mouseDown) {
            calcMouse(polyUI.master, x, y)
        }
        if (mouseOverObj != null) {
            val it = mouseOverObj!!
            if (!it.isInside(x, y)) {
                it.accept(MouseExited)
                it.mouseOver = false
                mouseOverObj = null
            } else {
                it.accept(MouseMoved)
            }
        }
        mouseOverObjects.fastRemoveIf {
            (!it.isInside(x, y)).also { b ->
                if (b) {
                    it.accept(MouseExited)
                    it.mouseOver = false
                } else {
                    it.accept(MouseMoved)
                }
            }
        }
    }

    fun onMousePressed(button: Int) {
        if (button == 0) mouseDown = true
        val event = MousePressed(button, mouseX, mouseY, keyModifiers)
        dispatch(event)
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
            if (curr - clickTimer < polyUI.settings.comboMaxInterval) {
                if (clickAmount < polyUI.settings.maxComboSize) {
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
        val event = MouseReleased(button, mouseX, mouseY, keyModifiers)
        releaseCancelled = false
        clickedCancelled = false
        val event2 = MouseClicked(button, mouseX, mouseY, clickAmount, keyModifiers)
        if (polyUI.keyBinder.accept(event)) return
        if (polyUI.keyBinder.accept(event2)) return
        polyUI.master.onAllLayouts(true) {
            if (isInside(mouseX, mouseY)) {
                components.fastEachReversed {
                    if (it.mouseOver) {
                        if (button == 0 && it is Focusable) {
                            if (polyUI.focus(it)) {
                                return@onAllLayouts
                            }
                        }
                        if (!releaseCancelled) {
                            if (it.accept(event)) releaseCancelled = true
                        }
                        if (!clickedCancelled) {
                            if (it.accept(event2)) clickedCancelled = true
                        }
                    }
                }
            }
        }
    }

    /** call this function when the mouse is scrolled. */
    @Suppress("NAME_SHADOWING")
    fun onMouseScrolled(amountX: Int, amountY: Int) {
        var amountX = if (polyUI.settings.naturalScrolling) amountX else -amountX
        var amountY = if (polyUI.settings.naturalScrolling) amountY else -amountY
        if ((keyModifiers and KeyModifiers.LSHIFT.value).toInt() != 0) {
            (amountX to amountY).let {
                amountX = it.second
                amountY = it.first
            }
        }
        val (sx, sy) = polyUI.settings.scrollMultiplier
        val event = MouseScrolled(amountX * sx, amountY * sy, keyModifiers)
        dispatch(event)
    }

    /**
     * Dispatch an event to this PolyUI instance, and it will be given to any drawable that has mouseOver true.
     */
    fun dispatch(event: Event) {
        if (polyUI.keyBinder.accept(event)) return
        dispatchTo(polyUI.master, event)
    }

    /**
     * Dispatch an event to a specific layout, and its children.
     *
     * **Note:** [dispatch] is almost every case should be used instead, as it gives it to all drawables; hence why it is marked as internal.
     * @see dispatch
     */
    @ApiStatus.Internal
    fun dispatchTo(layout: Layout, event: Event): Boolean {
        for (i in layout.components.size - 1 downTo 0) {
            val it = layout.components[i]
            if (it.mouseOver) {
                if (it.accept(event)) {
                    return true
                }
            }
        }
        for (i in layout.children.size - 1 downTo 0) {
            if (dispatchTo(layout.children[i], event)) return true
        }
        if (layout.mouseOver) {
            return layout.accept(event)
        }
        return false
    }
}
