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

package org.polyfrost.polyui.component.extensions

import org.polyfrost.polyui.PolyUI.Companion.DANGER
import org.polyfrost.polyui.PolyUI.Companion.SUCCESS
import org.polyfrost.polyui.PolyUI.Companion.WARNING
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.Inputtable
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.data.Cursor
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.FontFamily
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.State
import org.polyfrost.polyui.operations.Fade
import org.polyfrost.polyui.operations.Recolor
import org.polyfrost.polyui.operations.ShakeOp
import org.polyfrost.polyui.operations.Skew
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.set

fun <S : Drawable> S.withHoverStates() = withHoverStatesCached()

fun <S : Drawable> S.withHoverStates(
    consume: Boolean = false, showClicker: Boolean = true,
    animation: (() -> Animation)? = {
        Animations.Default.create(0.08.seconds)
    },
): S {
    on(Event.Mouse.Entered) {
        Recolor(this, this.palette.hovered, animation?.invoke()).add()
        if (showClicker) polyUI.cursor = Cursor.Clicker
        consume
    }
    on(Event.Mouse.Exited) {
        Recolor(this, this.palette.normal, animation?.invoke()).add()
        polyUI.cursor = Cursor.Pointer
        consume
    }
    on(Event.Mouse.Pressed) {
        Recolor(this, this.palette.pressed, animation?.invoke()).add()
        consume
    }
    on(Event.Mouse.Released) {
        Recolor(this, this.palette.hovered, animation?.invoke()).add()
        consume
    }
    return this
}

fun <S : Drawable> S.withCursor(cursor: Cursor = Cursor.Clicker): S {
    on(Event.Mouse.Entered) {
        polyUI.cursor = cursor
    }
    on(Event.Mouse.Exited) {
        polyUI.cursor = Cursor.Pointer
    }
    return this
}

fun <S : Block> S.withBorder(color: PolyColor, width: Float = 1f): S {
    this.borderColor = color
    this.borderWidth = width
    return this
}

fun <S : Block> S.withBorder(width: Float = 1f, color: (Colors.() -> PolyColor) = { page.border5 }): S {
    onInit {
        this.borderColor = polyUI.colors.color()
        this.borderWidth = width
    }
    return this
}

/**
 * Fade this drawable in or out, with a given duration.
 *
 * If it is not initialized, it applies the fade instantly.
 * @since 1.5.0
 */
fun <S : Drawable> S.fade(`in`: Boolean, durationNanos: Long = 0.1.seconds): S {
    if (`in`) {
        renders = true
        if (!initialized) {
            alpha = 1f
            return this
        }
        Fade(this, 1f, false, Animations.Default.create(durationNanos)).add()
    } else {
        if (!initialized) {
            alpha = 0f
            renders = false
            return this
        }
        Fade(this, 0f, false, Animations.Default.create(durationNanos)) {
            renders = false
        }.add()
    }
    return this
}


/**
 * Alias for [fade].
 */
fun <S : Drawable> S.fadeIn(durationNanos: Long = 0.1.seconds) = fade(true, durationNanos)

/**
 * Alias for [fade].
 */
fun <S : Drawable> S.fadeOut(durationNanos: Long = 0.1.seconds) = fade(false, durationNanos)

fun <S : Component> S.shake(): S {
    ShakeOp(this, 0.2.seconds, 2).add()
    return this
}

/**
 * Make this drawable toggleable, meaning that, when clicked:
 * - it will change its palette to the brand foreground color if it is toggled on, and to the component background color if it is toggled off.
 * @since 1.5.0
 */
fun <S : Drawable> S.toggleable(state: State<Boolean>): S {
    withHoverStates()
    onClick {
        state.set(!state.value)
    }
    state.listen {
        palette = if (!state.value) polyUI.colors.brand.fg else polyUI.colors.component.bg
    }
    if (state.value) setPalette { brand.fg }
    return this
}

/**
 * Set the palette of this drawable according to one of the three possible component states in PolyUI.
 * @param state the state to set. One of [DANGER]/0 (red), [WARNING]/1 (yellow), [SUCCESS]/2 (green).
 */
fun <S : Drawable> S.setState(state: Byte): S {
    palette = when (state) {
        DANGER -> polyUI.colors.state.danger
        WARNING -> polyUI.colors.state.warning
        SUCCESS -> polyUI.colors.state.success
        else -> throw IllegalArgumentException("Invalid state: $state")
    }
    return this
}

fun <S : Drawable> S.setPalette(palette: Colors.Palette, animate: Boolean = false, check: Boolean = true): S {
    if (this._palette == palette || (this.ignoreThemeChanges && check)) return this
    this._palette = palette
    if (animate) {
        Recolor(this, palette.get(inputState), Animations.Default.create(0.5.seconds)).add()
    } else {
        (_color as? PolyColor.Mut)?.recolor(palette.get(inputState)) ?: run { _color = palette.get(inputState) }
    }
    return this
}

/**
 * Set the color palette of this drawable during initialization, using the PolyUI colors instance.
 */
fun <S : Drawable> S.setPalette(palette: Colors.() -> Colors.Palette): S {
    onInit {
        this.palette = polyUI.colors.palette()
    }
    return this
}

fun <S : Text> S.secondary(): S {
    setPalette { text.secondary }
    return this
}

fun <S : Drawable> S.setDestructivePalette() = setPalette {
    Colors.Palette(text.primary.normal, state.danger.hovered, state.danger.pressed, text.primary.disabled)
}


/**
 * Set the font of this text component during initialization, using the PolyUI fonts instance.
 * @since 1.1.3
 */
fun <S : Text> S.setFont(font: FontFamily.() -> Font): S {
    onInit {
        this.font = polyUI.fonts.font()
    }
    return this
}


fun <S : Block> S.radius(radius: Float): S {
    when (val radii = this.radii) {
        null -> this.radii = floatArrayOf(radius)
        else -> radii.set(radius)
    }
    return this
}

fun <S : Block> S.radii(radii: FloatArray): S {
    this.radii = radii
    return this
}

fun <S : Block> S.radii(topLeftRadius: Float, topRightRadius: Float, bottomLeftRadius: Float, bottomRightRadius: Float): S {
    val radii = this.radii
    when {
        radii == null || radii.size < 4 -> this.radii = floatArrayOf(topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius)
        else -> {
            radii[0] = topLeftRadius
            radii[1] = topRightRadius
            radii[2] = bottomLeftRadius
            radii[3] = bottomRightRadius
        }
    }
    return this
}


/**
 * Add a 3D hover effect to the specified drawable which slightly skews the object as the mouse moves across it,
 * giving the appearance of it popping off the screen.
 *
 * @since 1.6.01
 */
fun <S : Drawable> S.add3dEffect(xIntensity: Double = 0.05, yIntensity: Double = 0.05): S {
    val func: S.(Event) -> Boolean = {
        val pw = (polyUI.mouseX - this.x) / this.width
        val ph = (polyUI.mouseY - this.y) / this.height
        this.skewX = (pw - 0.5f) * xIntensity
        this.skewY = (ph - 0.5f) * (pw * 2f - 1f) * yIntensity
        false
    }
    on(Event.Mouse.Moved, func)
    on(Event.Mouse.Drag, func)
    on(Event.Mouse.Exited) {
        Skew(this, 0.0, 0.0, false, Animations.Default.create(0.1.seconds)).add()
    }
    return this
}

/**
 * Use this method on your component if you plan on it being removed from the component tree and re-added later, to guard against the theming system
 * not updating the component's palette as it was not referenced when the colors were changed.
 *
 * Why is this behavior not default? Because it is quite an unlikely scenario that a component will be removed and re-added,
 * and having this run unnecessarily introduces a performance overhead.
 *
 * @since 1.8.3
 * @see Colors
 * @see org.polyfrost.polyui.PolyUI.colors
 * @see org.polyfrost.polyui.PolyUI.fonts
 * @see addRescalingListeners
 */
fun <T : Inputtable> T.addRethemingListeners(): T {
    var oldColors: Colors? = null
    var oldFonts: FontFamily? = null
    onInit {
        oldColors = polyUI.colors
        oldFonts = polyUI.fonts
    }
    on(Event.Lifetime.Added) {
        if (oldColors !== polyUI.colors) {
            oldColors?.let { retheme(it, polyUI.colors) }
            oldColors = polyUI.colors
        }
        if (oldFonts !== polyUI.fonts) {
            oldFonts?.let { refont(it, polyUI.fonts) }
            oldFonts = polyUI.fonts
        }
        false
    }
    return this
}

/**
 * Perform a retheme operation on this component, changing its palette from the old [Colors] to the new [Colors], and all its children.
 * @since 1.8.3
 * @see refont
 */
fun Component.retheme(old: Colors, new: Colors, animate: Boolean = false): Component {
    onAll<Drawable> {
        val palette = new.getNewPalette(it._palette, old) ?: run {
            // asm: no valid palette was found, so we check for a color change. if still nothing found, just don't do anything
            it.color = new.getNewColor(it.color, old) ?: it.color
            it._palette
        }
        // asm: dont animate on components that are not currently shown (as otherwise they would do the animation later which looks weird)
        if (palette != null) {
            it.setPalette(palette, animate && it.renders)
            if (it is Block) {
                val oldBorder = it.borderColor
                if (oldBorder != null) {
                    it.borderColor = new.getNewColor(oldBorder, old) ?: oldBorder
                }
            }
        }
    }
    return this
}

/**
 * Perform a refont operation on this component, changing its font from the old [FontFamily] to the new [FontFamily], and all its children.
 * @since 1.8.3
 * @see retheme
 */
fun Component.refont(old: FontFamily, new: FontFamily): Component {
    onAll<Text> {
        if (it.font.family === old) {
            it.font = new.get(it.fontWeight, it.italic)
        }
    }
    return this
}
