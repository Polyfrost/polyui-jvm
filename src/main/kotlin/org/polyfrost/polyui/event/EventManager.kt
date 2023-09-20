/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/polui-jvm>
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

package org.polyfrost.polyui.event

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Focusable
import org.polyfrost.polyui.input.KeyModifiers
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.fastEachReversed
import org.polyfrost.polyui.utils.fastRemoveIfReversed
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

/**
 * # EventManager
 * Handles all events and passes them to the correct components/layouts.
 * @param polyUI The PolyUI instance to use.
 */
class EventManager(private val polyUI: PolyUI) {
    private val mouseOvers = ArrayList<Drawable>(3) // asm: it is not expected to have many of this type under hover at once
    var mouseOver: Drawable? = null
        private set
    val primaryCandidate get() = mouseOvers.lastOrNull()
    var mouseX: Float = 0f
        private set
    var mouseY: Float = 0f
        private set
    private var clickTimer: Long = 0L

    /** @see org.polyfrost.polyui.input.Modifiers */
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
    fun recalculateMousePos() {
        // force update
        val mouseX = mouseX
        this.mouseX = 0f
        mouseMoved(mouseX, mouseY)
    }

    /**
     * Internal function that will forcefully drop the given drawable from event tracking.
     */
    @ApiStatus.Internal
    fun drop(drawable: Drawable, withChildren: Boolean = false) {
        if (drawable === mouseOver) {
            drawable.mouseOver = false
            drawable.accept(MouseExited)
            mouseOver = null
        }
        mouseOvers.fastRemoveIfReversed {
            if (it === drawable) {
                it.mouseOver = false
                it.accept(MouseExited)
                true
            } else {
                false
            }
        }
        if (withChildren) {
            if (drawable is Layout) {
                drawable.children.fastEach { drop(it) }
            }
        }
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
        keyModifiers = keyModifiers and modifier.inv()
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

    /**
     * Internal method for drawables, tracking it if it does not consume the events.
     *
     * This is used for event dispatching.
     */
    @ApiStatus.Internal
    fun processCandidate(drawable: Drawable, x: Float, y: Float): Drawable? {
        var candidate: Drawable? = null
        if (drawable.acceptsInput && drawable.isInside(x, y)) {
            if (!drawable.consumesHover) {
                if (!drawable.mouseOver) {
                    drawable.mouseOver = true
                    mouseOvers.add(drawable)
                    drawable.accept(MouseEntered)
                }
                return null
            } else {
                candidate = drawable
            }
        }
        return candidate
    }

    private fun processCandidates(layout: Layout, x: Float, y: Float): Drawable? {
        var candidate: Drawable? = null
        if (layout.isInside(x, y)) {
            candidate = processCandidate(layout, x, y)
            for (i in layout.components.size - 1 downTo 0) {
                val cc = processCandidate(layout.components[i], x, y)
                if (cc != null) candidate = cc
            }
            for (i in layout.children.indices) {
                val ccc = processCandidates(layout.children[i], x, y)
                if (ccc != null) candidate = ccc
            }
        }
        return candidate
    }

    /** call this function to update the mouse position. */
    fun mouseMoved(x: Float, y: Float) {
        if (mouseX == x && mouseY == y) return
        mouseX = x
        mouseY = y
        if (!mouseDown) {
            val candidate = processCandidates(polyUI.master, x, y)
            if (candidate != null && candidate !== mouseOver) {
                candidate.mouseOver = true
                mouseOver?.let {
                    it.accept(MouseExited)
                    it.mouseOver = false
                }
                mouseOver = candidate
                candidate.accept(MouseEntered)
            }
        }
        mouseOver?.let {
            if (!it.isInside(x, y)) {
                it.accept(MouseExited)
                it.mouseOver = false
                mouseOver = null
            } else {
                it.accept(MouseMoved)
            }
        }
        mouseOvers.fastRemoveIfReversed {
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

    fun mousePressed(button: Int) {
        if (button == 0) mouseDown = true
        val event = MousePressed(button, mouseX, mouseY, keyModifiers)
        dispatch(event)
    }

    /** call this function when a mouse button is released. */
    fun mouseReleased(button: Int) {
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
                if (!(polyUI.focused as Drawable).mouseOver) {
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
        if (button == 0) {
            mouseOvers.fastEach {
                if (tryFocus(it)) {
                    return
                }
            }
            if (mouseOver != null && tryFocus(mouseOver!!)) {
                return
            }
        }
        dispatch(event)
        dispatch(event2)
    }

    /**
     * try to focus a drawable, and return true if it was successful.
     */
    fun tryFocus(drawable: Drawable): Boolean {
        if (drawable is Focusable && drawable.mouseOver) {
            return polyUI.focus(drawable)
        }
        return false
    }

    /** call this function when the mouse is scrolled. */
    @Suppress("NAME_SHADOWING")
    fun mouseScrolled(amountX: Float, amountY: Float) {
        var amountX = if (polyUI.settings.naturalScrolling) amountX else -amountX
        var amountY = if (polyUI.settings.naturalScrolling) amountY else -amountY
        if ((keyModifiers and KeyModifiers.LSHIFT.value).toInt() != 0) {
            val t = amountX
            amountX = amountY
            amountY = t
        }
        val (sx, sy) = polyUI.settings.scrollMultiplier
        val event = MouseScrolled(amountX * sx, amountY * sy, keyModifiers)
        dispatch(event)
    }

    /**
     * Dispatch an event to this PolyUI instance, and it will be given to any tracked drawable.
     */
    fun dispatch(event: Event): Boolean {
        if (polyUI.keyBinder.accept(event)) return true
        if (mouseOver?.accept(event) == true) {
            return true
        }
        mouseOvers.fastEachReversed {
            if (it.accept(event)) {
                return true
            }
        }
        return false
    }
}
