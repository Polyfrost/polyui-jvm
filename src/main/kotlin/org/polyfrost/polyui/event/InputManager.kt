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

package org.polyfrost.polyui.event

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Contract
import org.polyfrost.polyui.PolyUI.Companion.INPUT_HOVERED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.PolyUI.Companion.INPUT_PRESSED
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.input.KeyBinder
import org.polyfrost.polyui.input.Keys
import org.polyfrost.polyui.input.Modifiers
import org.polyfrost.polyui.property.Settings
import org.polyfrost.polyui.utils.Clock
import java.nio.file.Path

/**
 * # InputManager
 * Handles input events and passes them to the correct components/layouts.
 * @param master the layout to create this event manager for. marked as internal as should not be accessed.
 * @param keyBinder the key binder to use for this event manager. marked as internal as should not be accessed.
 */
class InputManager(
    private var master: Drawable?,
    var keyBinder: KeyBinder?,
    private val settings: Settings,
) {
    var mouseOver: Drawable? = null
        private set(value) {
            if (field === value) return
            field?.inputState = INPUT_NONE
            value?.inputState = INPUT_HOVERED
            field = value
        }

    var mouseX: Float = 0f
        private set
    var mouseY: Float = 0f
        private set
    private var clickTimer: Long = 0L

    /** @see org.polyfrost.polyui.input.Modifiers */
    val keyModifiers get() = Modifiers(mods)

    /** the current key modifiers (raw). */
    @ApiStatus.Internal
    var mods: Byte = 0
        private set

    /** amount of clicks in the current combo */
    private var clickAmount = 0

    /** tracker for the combo */
    private var clickedButton: Int = 0

    var focused: Drawable? = null
        private set

    /** weather or not the left button/primary click is DOWN (aka repeating) */
    var mouseDown = false
        private set

    fun with(master: Drawable?): InputManager {
        this.master = master
        return this
    }

    /**
     * Call this method when files are dropped onto this window. The [Event.Focused.FileDrop] is then dispatched to the currently focused drawable.
     * @see focus
     * @since 1.0.3
     */
    fun filesDropped(files: Array<Path>) {
        if (focused?.hasListenersFor(Event.Focused.FileDrop::class.java) == true) {
            focused?.accept(Event.Focused.FileDrop(files))
        }
    }

    /** This method should be called when a printable key is typed. This key should be **mapped to the user's keyboard layout!** */
    fun keyTyped(key: Char) {
        focused?.accept(Event.Focused.KeyTyped(key, keyModifiers))
    }

    /** This method should be called when a non-printable, but representable key is pressed. */
    fun keyDown(key: Keys) {
        val event = Event.Focused.KeyPressed(key, keyModifiers)
        if (keyBinder?.accept(event) == true) return
        focused?.accept(event)
    }

    /** This method should be called when a non-printable, but representable key is released. */
    fun keyUp(key: Keys) {
        val event = Event.Focused.KeyReleased(key, keyModifiers)
        if (keyBinder?.accept(event) == true) return
        focused?.accept(event)
    }

    /**
     * This method should be called when a non-representable key is pressed.
     *
     * This is used solely for keybinding, and so the key can be any value, as long as it is consistent, and unique to that key.
     */
    fun keyDown(code: Int) {
        if (keyBinder?.accept(code, true) == true) return
        if(focused?.hasListenersFor(Event.Focused.UnmappedInput::class.java) == true) {
            focused?.accept(Event.Focused.UnmappedInput(code, true, keyModifiers))
        }
    }

    /**
     * This method should be called when a non-representable key is released.
     *
     * This is used solely for keybinding, and so the key can be any value, as long as it is consistent, and unique to that key.
     */
    fun keyUp(code: Int) {
        if (keyBinder?.accept(code, false) == true) return
        if (focused?.hasListenersFor(Event.Focused.UnmappedInput::class.java) == true) {
            focused?.accept(Event.Focused.UnmappedInput(code, false, keyModifiers))
        }
    }

    /**
     * force the mouse position to be updated.
     * @since 0.18.5
     */
    fun recalculate() = mouseMoved(mouseX, mouseY)

    /**
     * Drop the current mouse over drawable.
     */
    @ApiStatus.Internal
    fun drop(it: Drawable? = null) {
        if (it != null) {
            if (it === mouseOver) {
                mouseOver = null
            }
        } else mouseOver = null
    }

    /**
     * add a modifier to the current keyModifiers.
     * @see Modifiers
     */
    fun addModifier(modifier: Byte) {
        mods = (mods.toInt() or modifier.toInt()).toByte()
    }

    /**
     * remove a modifier from the current keyModifiers.
     * @see Modifiers
     */
    fun removeModifier(modifier: Byte) {
        mods = (mods.toInt() and modifier.toInt().inv()).toByte()
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
     * perform a recursive ray check on the drawable.
     * Each drawable will be checked with [Drawable.enabled], then [Drawable.isInside], then finally [Drawable.acceptsInput]
     * before it is considered a valid candidate.
     *
     * Note that this functions inclusion in public API is experimental.
     *
     * @since 1.0.7
     */
    @ApiStatus.Experimental
    @Contract(pure = true)
    fun rayCheck(it: Drawable, x: Float, y: Float): Drawable? {
        var c: Drawable? = null
        if (it.enabled && it.isInside(x, y)) {
            if (it.acceptsInput) c = it
            it.children?.fastEach {
                val n = rayCheck(it, x, y)
                if (n != null) c = n
            }
        }
        return c
    }

    /** same as [rayCheck], but does not check for [Drawable.acceptsInput] or [Drawable.enabled]. */
    @ApiStatus.Experimental
    @Contract(pure = true)
    fun rayCheckUnsafe(it: Drawable, x: Float, y: Float): Drawable? {
        var c: Drawable? = null
        if (it.isInside(x, y)) {
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
        mouseX = x
        mouseY = y
        if (mouseDown) {
            dispatch(Event.Mouse.Dragged)
            return
        }
        master?.let { mouseOver = rayCheck(it, x, y) }
    }

    fun mousePressed(button: Int) {
        if (button == 0) mouseDown = true
        val event = Event.Mouse.Pressed(button, mouseX, mouseY, keyModifiers)
        mouseOver?.inputState = INPUT_PRESSED
        dispatch(event, true)
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
        if (focused?.isInside(mouseX, mouseY) != true) {
            unfocus()
        }
        val release = Event.Mouse.Released(button, mouseX, mouseY, keyModifiers)
        val click = Event.Mouse.Clicked(button, mouseX, mouseY, clickAmount, keyModifiers)
        mouseOver?.inputState = INPUT_HOVERED
        dispatch(release, true)
        if (!dispatch(click) && button == 0) {
            safeFocus(mouseOver)
        }
    }

    /**
     * try to focus a drawable, without throwing an exception.
     * @return `true` if the drawable was focused successfully, and `false` if it is not [Drawable.focusable], or if it is already focused.
     * @see focus
     * @since 1.0.4
     */
    fun safeFocus(drawable: Drawable?): Boolean {
        if (drawable == null) return false
        if (drawable.focusable) {
            return focus(drawable)
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
     * Dispatch an event to the provided drawable.
     *
     * If the candidate drawable does not accept the event (returns `false`), it will be given to the parent, and so on.
     *
     * **Note that changing of the [to] parameter is experimental.**
     * @param bindable (*mostly internal parameter*) if `true`, the event will be given to the [keyBinder] for processing of keybindings,
     * **before** being given to the candidate drawable. It will consume the event if a keybinding matches and accepts it.
     *
     */
    fun dispatch(event: Event, bindable: Boolean = false, to: Drawable? = mouseOver): Boolean {
        if (bindable && keyBinder?.accept(event) == true) return true
        var candidate = to
        while (candidate != null) {
            if (candidate.accept(event)) return true
            candidate = candidate._parent
        }
        return false
    }

    /**
     * Sets the focus to the specified focusable element, throwing an exception if the provided element is not focusable.
     *
     * @throws IllegalArgumentException if the provided drawable is not [Drawable.focusable]
     * @param focusable the element to set focus on
     * @return true if focus was successfully set, false if the provided focusable is already focused
     * @see safeFocus
     */
    fun focus(focusable: Drawable?): Boolean {
        if (focusable === focused) return false
        require(focusable?.focusable != false) { "Cannot focus un-focusable drawable!" }
        focused?.accept(Event.Focused.Lost)
        focused = focusable
        return focused?.accept(Event.Focused.Gained) == true
    }

    fun unfocus() = focus(null)
}
