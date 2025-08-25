/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023-2024 Polyfrost and its contributors.
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

package org.polyfrost.polyui.input

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Contract
import org.polyfrost.polyui.PolyUI.Companion.INPUT_HOVERED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.Settings
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.extensions.isRelatedTo
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.utils.Clock
import org.polyfrost.polyui.utils.fastEach
import java.nio.file.Path
import kotlin.math.abs

/**
 * # InputManager
 * Handles input events and passes them to the correct components/layouts.
 * @param master the layout to create this event manager for. marked as internal as should not be accessed.
 * @param keyBinder the key binder to use for this event manager. marked as internal as should not be accessed.
 */
class InputManager(
    private var master: Inputtable?,
    var keyBinder: KeyBinder?,
    private val settings: Settings,
) {
    constructor(settings: Settings) : this(null, KeyBinder(settings), settings)

    var mouseOver: Inputtable? = null
        private set(value) {
            if (field === value) return
            field?.let {
                if (it.inputState == INPUT_PRESSED) {
                    // safeguard to stop the component from getting stuck in a pressed state
                    it.accept(Event.Mouse.Companion.Released.set(mouseX, mouseY))
                }
                it.inputState = INPUT_NONE
            }
            value?.inputState = INPUT_HOVERED
            field = value
        }

    var mouseX: Float = 0f
        private set
    var mouseY: Float = 0f
        private set

    private var pressX = 0f
    private var pressY = 0f

    /**
     * `true` if the mouse is currently dragging.
     * @see Settings.dragThreshold
     * @see Event.Mouse.Drag
     * @since 1.6.1
     */
    var dragging = false
        private set

    private var clickTimer: Long = 0L

    /** @see org.polyfrost.polyui.input.Modifiers */
    @get:JvmName("getKeyModifiers")
    val keyModifiers get() = Modifiers(mods)

    /** the current key modifiers (raw). */
    @ApiStatus.Internal
    var mods: Byte = 0
        private set

    /** amount of clicks in the current combo */
    private var clickAmount = 0

    /** tracker for the combo */
    private var clickedButton: Int = 0

    var focused: Inputtable? = null
        private set

    /** weather or not the left button/primary click is DOWN (aka repeating) */
    var mouseDown = false
        private set

    fun with(master: Inputtable?): InputManager {
        this.master = master
        return this
    }

    /**
     * Call this method when files are dropped onto this window. The [Event.Focused.FileDrop] is then dispatched to the currently focused component.
     * @see focus
     * @since 1.0.3
     */
    fun filesDropped(files: Array<Path>) {
        focused?.accept(Event.Focused.FileDrop(files))
    }

    /** This method should be called when a printable key is typed. This key should be **mapped to the user's keyboard layout!** */
    fun keyTyped(key: Char) {
        focused?.accept(Event.Focused.KeyTyped(key, keyModifiers))
    }

    /** This method should be called when a non-printable, but representable key is pressed. */
    fun keyDown(key: Keys) {
        if (key == Keys.ESCAPE && focused != null) {
            focus(null)
            return
        }
        val event = Event.Focused.KeyPressed(key, keyModifiers)
        if (keyBinder?.accept(event, mods) == true) return
        focused?.accept(event)
    }

    /** This method should be called when a non-printable, but representable key is released. */
    fun keyUp(key: Keys) {
        val event = Event.Focused.KeyReleased(key, keyModifiers)
        if (keyBinder?.accept(event, mods) == true) return
        focused?.accept(event)
    }

    /**
     * This method should be called when a non-representable key is pressed.
     *
     * This is used solely for keybinding, and so the key can be any value, as long as it is consistent, and unique to that key.
     */
    fun keyDown(code: Int) {
        if (keyBinder?.accept(code, true, mods) == true) return
        focused?.accept(Event.Focused.UnmappedInput(code, true, keyModifiers))
    }

    /**
     * This method should be called when a non-representable key is released.
     *
     * This is used solely for keybinding, and so the key can be any value, as long as it is consistent, and unique to that key.
     */
    fun keyUp(code: Int) {
        if (keyBinder?.accept(code, false, mods) == true) return
        focused?.accept(Event.Focused.UnmappedInput(code, false, keyModifiers))
    }

    /**
     * force the mouse position to be updated.
     * @since 0.18.5
     */
    fun recalculate() {
        master?.let { mouseOver = rayCheck(it, mouseX, mouseY) }
    }

    /**
     * If [it] is the current component which the mouse is over, it will be dropped.
     *
     * If [it] is `null`, the current mouseOver component will be dropped regardless of what it is.
     */
    @ApiStatus.Internal
    fun drop(it: Inputtable? = null) {
        if (it != null) {
            if (it === mouseOver) mouseOver = null
        } else mouseOver = null
    }

    /**
     * add a modifier to the current keyModifiers.
     * @see Modifiers
     */
    fun addModifier(modifier: Byte) {
        mods = (mods.toInt() or modifier.toInt()).toByte()
        keyBinder?.update(0L, mods, true)
    }

    /**
     * remove a modifier from the current keyModifiers.
     * @see Modifiers
     */
    fun removeModifier(modifier: Byte) {
        keyBinder?.modifierRemoved(mods)
        mods = (mods.toInt() and modifier.toInt().inv()).toByte()
        keyBinder?.update(0L, mods, false)
    }

    /**
     * Clear the current keyModifiers.
     * @see Modifiers
     * @see addModifier
     * @since 0.20.1
     */
    fun clearModifiers() {
        mods = 0
    }

    /**
     * perform a recursive ray check on the component.
     * Each component will be checked with [Component.isEnabled], then [Component.isInside], then finally [Inputtable.acceptsInput]
     * before it is considered a valid candidate.
     *
     * Note that this functions inclusion in public API is experimental.
     *
     * @since 1.0.7
     */
    @ApiStatus.Experimental
    @Contract(pure = true)
    fun rayCheck(it: Inputtable, x: Float, y: Float): Inputtable? {
        var c: Inputtable? = null
        if (it.isEnabled && it.isInside(x, y)) {
            if (it.acceptsInput) c = it
            it.children?.fastEach {
                if (it !is Inputtable) return@fastEach
                val n = rayCheck(it, x, y)
                if (n != null) c = n
            }
        }
        return c
    }

    /** same as [rayCheck], but does not check for [Inputtable.acceptsInput]. */
    @ApiStatus.Experimental
    @Contract(pure = true)
    fun rayCheckUnsafe(it: Component, x: Float, y: Float): Component? {
        var c: Component? = null
        if (it.isEnabled && it.isInside(x, y)) {
            c = it
            it.children?.fastEach {
                val n = rayCheckUnsafe(it, x, y)
                if (n != null) c = n
            }
        }
        return c
    }


    /** call this function to update the mouse position. */
    fun mouseMoved(x: Float, y: Float) {
        if (mouseX == x && mouseY == y) return
        mouseX = x
        mouseY = y
        if (mouseDown) {
            val threshold = settings.dragThreshold
            if (abs(pressX - x) > threshold || abs(pressY - y) > threshold) {
                if (!dragging) {
                    dragging = true
                    if (dispatch(Event.Mouse.Drag.Started)) {
                        // dragging was cancelled.
                        dragging = false
                        return
                    }
                }
            }
            if (dragging) dispatch(Event.Mouse.Drag)
            return
        }
        master?.let { mouseOver = rayCheck(it, x, y) }
        dispatch(Event.Mouse.Moved)
    }

    fun mousePressed(button: Int) {
        recalculate()
        if (button == 0) {
            mouseDown = true
            mouseOver?.inputState = INPUT_PRESSED
            pressX = mouseX
            pressY = mouseY
        }
        val event =
            if (button == 0 && keyModifiers.isEmpty) Event.Mouse.Companion.Pressed.set(mouseX, mouseY)
            else Event.Mouse.Pressed(button, mouseX, mouseY, keyModifiers)
        dispatch(event, true)
    }

    /** call this function when a mouse button is released. */
    fun mouseReleased(button: Int) {
        if (clickedButton != button) {
            clickedButton = button
            clickAmount = 1
        } else {
            val curr = Clock.time
            if (curr - clickTimer < settings.comboMaxInterval) {
                if (clickAmount < settings.maxComboSize) {
                    clickAmount++
                } else if (settings.clearComboWhenMaxed) {
                    clickAmount = 1
                }
            } else {
                clickAmount = 1
            }
            clickTimer = curr
        }
        mouseOver?.inputState = INPUT_HOVERED

        // fast path: no need to alloc or check for anything on simple
        if (button == 0) {
            mouseDown = false
            if (dragging) {
                dragging = false
                if (dispatch(Event.Mouse.Drag.Ended)) return
            }
            mouseOver?.let {
                if (!it.isInside(mouseX, mouseY)) {
                    mouseOver = null
                    return
                }
            }
            focused?.let {
                if (!it.isInside(mouseX, mouseY)) unfocus()
            }
            if (clickAmount == 1 && keyModifiers.isEmpty) {
                dispatch(Event.Mouse.Companion.Released.set(mouseX, mouseY), true)
                if (!dispatch(Event.Mouse.Companion.Clicked.set(mouseX, mouseY))) safeFocus(mouseOver)
                return
            }
        }

        dispatch(Event.Mouse.Released(button, mouseX, mouseY, keyModifiers), true)
        val click = Event.Mouse.Clicked(button, mouseX, mouseY, clickAmount, keyModifiers)
        // asm: not many components will actually have double click listeners, so, we check and see if we can skip the dispatch.
        val mouseOver = mouseOver ?: return
        if (!mouseOver.hasListenersFor(click)) {
            val click1 = Event.Mouse.Clicked(button, mouseX, mouseY, 1, keyModifiers)
            dispatch(click1)
        } else dispatch(click)
    }

    /**
     * try to focus a component, without throwing an exception.
     * @return `true` if the component was focused successfully, and `false` if it is not [Inputtable.focusable], or if it is already focused.
     * @see focus
     * @since 1.0.4
     */
    fun safeFocus(component: Inputtable?): Boolean {
        if (component == null) return false
        if (component.focusable) {
            return focus(component)
        }
        return false
    }

    /** call this function when the mouse is scrolled. */
    @Suppress("NAME_SHADOWING")
    fun mouseScrolled(amountX: Float, amountY: Float) {
        var amountX = if (settings.naturalScrolling) amountX else -amountX
        var amountY = if (settings.naturalScrolling) amountY else -amountY
        if (keyModifiers.hasShift) {
            val t = amountX
            amountX = amountY
            amountY = t
        }
        val (sx, sy) = settings.scrollMultiplier
        val event = Event.Mouse.Scrolled(amountX * sx, amountY * sy, keyModifiers)
        dispatch(event)
    }

    /**
     * Dispatch an event to the provided component.
     *
     * If the candidate component does not accept the event (returns `false`), it will be given to the parent, and so on.
     *
     * **Note that changing of the [to] parameter is experimental.**
     * @param bindable (*mostly internal parameter*) if `true`, the event will be given to the [keyBinder] for processing of keybindings,
     * **before** being given to the candidate component. It will consume the event if a keybinding matches and accepts it.
     *
     */
    fun dispatch(event: Event, bindable: Boolean = false, to: Inputtable? = mouseOver): Boolean {
        if (bindable && keyBinder?.accept(event, mods) == true) return true
        val cf = event is Event.Focused
        var candidate = to
        while (candidate != null) {
            if (cf && candidate.focusable) continue
            if (candidate.accept(event)) return true
            candidate = candidate._parent as? Inputtable
        }
        return false
    }

    fun unfocus() {
        focused?.let {
            it.focused = false
            var p = it._parent as? Inputtable
            while (p != null) {
                if (p.focused) {
                    focused = p
                    return
                }
                p = p._parent as? Inputtable
            }
            focused = null
        }
    }

    /**
     * Unfocus the [target] if it currently is the [focused] drawable.
     * @since 1.6.1
     */
    fun unfocus(target: Inputtable?) {
        if (target === focused) unfocus()
    }

    /**
     * Synthetically drop all the current input states.
     *
     * Useful for some window platforms which don't correctly create release events when the window loses focus.
     * @since 1.7.4
     */
    fun drop() {
        dragging = false
        clickTimer = 0L
        mods = 0
        clickAmount = 0
        clickedButton = 0
        focused = null
        mouseOver = null
        mouseDown = false
    }

    /**
     * Sets the focus to the specified focusable element, throwing an exception if the provided element is not focusable.
     *
     * @throws IllegalArgumentException if the provided component is not [Inputtable.focusable]
     * @param focusable the element to set focus on
     * @return true if focus was successfully set, false if the provided focusable is already focused
     * @see safeFocus
     */
    fun focus(focusable: Inputtable?): Boolean {
        if (focusable === focused) return false
        if (focusable == null || !focusable.isRelatedTo(focused)) {
            var p = focused
            while (p != null) {
                p.focused = false
                p = p._parent as? Inputtable
            }
            focused = null
            if (focusable == null) return true
        }
        if (!focusable.initialized) {
            val polyUI = master?.polyUI ?: throw IllegalArgumentException("Cannot focus uninitialized component")
            focusable.setup(polyUI)
            focusable.rescaleToPolyUIInstance()
        }
        require(focusable.focusable) { "Cannot focus un-focusable component" }
        focused = focusable
        focusable.focused = true
        return true
    }
}
