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

@file:Suppress("UNUSED")

package org.polyfrost.polyui.component

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI.Companion.DANGER
import org.polyfrost.polyui.PolyUI.Companion.SUCCESS
import org.polyfrost.polyui.PolyUI.Companion.WARNING
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.PopupMenu
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.component.impl.TextInput
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.event.EventDSL
import org.polyfrost.polyui.operations.*
import org.polyfrost.polyui.renderer.data.Cursor
import org.polyfrost.polyui.renderer.data.Font
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.Clock
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.fastEachIndexed
import org.polyfrost.polyui.utils.set
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.abs

/**
 * Make this component draggable by the user with their mouse.
 *
 * @param free if this is true, the component will be able to be dragged outside its parent.
 * This is achieved by briefly removing this from its parent and adding it to the master.
 */
fun <S : Inputtable> S.draggable(withX: Boolean = true, withY: Boolean = true, free: Boolean = false): S {
    var px = 0f
    var py = 0f
    on(Event.Mouse.Drag.Started) {
        px = polyUI.inputManager.mouseX - x
        py = polyUI.inputManager.mouseY - y
        if (free && _parent !== polyUI.master) {
            parent.children!!.remove(this)
            polyUI.master.children!!.add(this)
        }
        false
    }
    on(Event.Mouse.Drag) {
        val mx = polyUI.inputManager.mouseX
        val my = polyUI.inputManager.mouseY
        if (this is Drawable) needsRedraw = true
        if (withX) x = mx - px
        if (withY) y = my - py
        false
    }
    on(Event.Mouse.Drag.Ended) {
        if (free && _parent !== polyUI.master) {
            polyUI.master.children!!.remove(this)
            parent.children!!.add(this)
        }
        false
    }
    return this
}

/**
 * Turn this component into a 'rearrangeable grid', meaning that the children in it
 * can be dragged around and rearranged in any order by the user. It will automatically adapt and insert the children in
 * the correct order.
 *
 * @since 1.6.1
 */
fun <S : Inputtable> S.makeRearrangeableGrid(): S {
    children?.fastEach { cmp ->
        val self = cmp as? Inputtable ?: return@fastEach
        self.draggable().onDrag { _ ->
            val px = x
            val py = y
            val pw = width
            val siblings = parent.children ?: return@onDrag
            siblings.fastEachIndexed { i, it ->
                if (it === this) return@fastEachIndexed
                if (it.intersects(px, py, pw, height)) {
                    siblings.remove(this)
                    val middleX = px + pw / 2f
                    val itMiddleX = it.x + it.width / 2f
                    siblings.add(if (middleX > itMiddleX) i else (i - 1).coerceAtLeast(0), this)
                }
            }
            parent.position()
            x = px
            y = py
        }.onDragEnd {
            parent.position()
        }
    }
    return this
}

/**
 * Add some text that is shown when the mouse is left still over this drawable
 * for 1 second or more.
 * @since 1.0.3
 */
@JvmName("addHoverInfo")
fun <S : Inputtable> S.addHoverInfo(vararg drawables: Drawable?, size: Vec2 = Vec2.ZERO, align: Align = AlignDefault, position: Point = Point.Above): S {
    var mx = 0f
    var my = 0f
    val popup = PopupMenu(*drawables, size = size, align = align, polyUI = null, openNow = false, position = position)
    val exe = Clock.Bomb(0.5.seconds) {
        polyUI.focus(popup)
    }
    on(Event.Mouse.Entered) {
        mx = polyUI.mouseX
        my = polyUI.mouseY
        polyUI.addExecutor(exe)
    }
    on(Event.Mouse.Exited) {
        if (popup.focused) polyUI.unfocus()
        polyUI.removeExecutor(exe)
    }
    val stop: (S.(Event) -> Boolean) = {
        if (popup.focused) {
            if (abs(mx - polyUI.mouseX) > 3f || abs(my - polyUI.mouseY) > 3f) {
                polyUI.unfocus()
                polyUI.addExecutor(exe)
            }
        }
        exe.refuse()
        false
    }
    on(Event.Mouse.Moved, stop)
    on(Event.Mouse.Drag, stop)
    on(Event.Mouse.Scrolled, stop)
    return this
}

/**
 * Make this drawable toggleable, meaning that, when clicked:
 * - it will change its palette to the brand foreground color if it is toggled on, and to the component background color if it is toggled off.
 * - it will emit a [Event.Change.State] event. you can use the [onToggle] function to easily listen to this event.
 * @since 1.5.0
 */
fun <S : Drawable> S.toggleable(default: Boolean): S {
    withStates()
    var state = default
    onClick {
        state = !state
        if (hasListenersFor(Event.Change.State::class.java)) {
            val ev = Event.Change.State(state)
            accept(ev)
            if (ev.cancelled) return@onClick false
        }
        palette = if (state) polyUI.colors.brand.fg else polyUI.colors.component.bg
        false
    }
    if (state) setPalette { brand.fg }
    return this
}

/**
 * Use this to add a simple listener for the [Event.Change.State] event.
 * @see [toggleable]
 * @since 1.5.0
 */
fun <S : Inputtable> S.onToggle(func: S.(Boolean) -> Unit): S {
    on(Event.Change.State) {
        func.invoke(this, it.state)
        false
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

/**
 * Fade this drawable in or out, with a given duration.
 *
 * If it is not initialized, it applies the fade instantly.
 * @since 1.5.0
 */
fun <S : Drawable> S.fade(`in`: Boolean, durationNanos: Long = 0.1.seconds): S {
    if (`in`) {
        isEnabled = true
        if (!initialized) {
            alpha = 1f
            return this
        }
        Fade(this, 1f, false, Animations.Default.create(durationNanos)).add()
    } else {
        if (!initialized) {
            alpha = 0f
            isEnabled = false
            return this
        }
        Fade(this, 0f, false, Animations.Default.create(durationNanos)) {
            isEnabled = false
        }.add()
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

fun <S : Component> S.namedId(name: String): S {
    this.name = "$name@${this.name.substringAfterLast('@')}"
    return this
}

fun <S : Component> S.named(name: String): S {
    this.name = name
    return this
}

/**
 * Instruct this component to refuse scrolling even if it needs to be.
 */
fun <S : Scrollable> S.dontScroll(): S {
    this.shouldScroll = false
    return this
}

/**
 * Specify a minimum size for this component. **Note that this will also disable scrolling due to optimizations**.
 * @since 1.4.4
 */
@JvmName("minimumSize")
fun <S : Scrollable> S.minimumSize(size: Vec2): S {
    this.shouldScroll = false
    this.visibleSize = size
    return this
}

fun <S : Inputtable> S.disable(state: Boolean = true): S {
    this.isEnabled = !state
    return this
}

fun <S : Component> S.hide(state: Boolean = true): S {
    this.renders = !state
    return this
}

/**
 * Return the position that this drawable is currently moving to, or its current position if it is not moving.
 *
 * @see Move
 * @see getTargetSize
 * @since 1.6.02
 */
fun <S : Component> S.getTargetPosition(): Vec2 {
    val operations = this.operations ?: return this.at
    operations.fastEach {
        if (it is Move) return it.target
    }
    return this.at
}

/**
 * Return the size that this drawable is currently resizing to, or its current size if it is not resizing.
 *
 * @see Resize
 * @see getTargetPosition
 * @since 1.6.02
 */
fun <S : Component> S.getTargetSize(): Vec2 {
    val operations = this.operations ?: return this.size
    operations.fastEach {
        if (it is Resize) return it.target
    }
    return this.size
}

/**
 * Specify that this component should be ignored during the layout stage.
 *
 * This means that it will **not be placed automatically** by the positioner.
 *
 * Additionally, this will ignore if there is no size set on this component and no way to infer it.**
 * This means that, if you use this incorrectly **it will not be visible and may break things if your renderer doesn't like zero sizes.**
 * @since 1.4.4
 */
fun <S : Component> S.ignoreLayout(state: Boolean = true): S {
    this.layoutIgnored = state
    return this
}

fun <S : Drawable> S.setAlpha(alpha: Float): S {
    this.alpha = alpha
    return this
}

fun <S : Component> S.padded(xPad: Float, yPad: Float): S {
    this.padding = Vec4.of(xPad, yPad, xPad, yPad)
    return this
}

fun <S : Component> S.padded(left: Float, top: Float, right: Float, bottom: Float): S {
    this.padding = Vec4.of(left, top, right, bottom)
    return this
}

fun <S : Component> S.padded(padding: Vec4): S {
    this.padding = padding
    return this
}


/**
 * Add a listener for changes to the given String-type property.
 *
 * In PolyUI, this is for [Text] and [TextInput][org.polyfrost.polyui.component.impl.TextInput] only.
 * @since 1.0.6
 */
@JvmName("onChangeString")
fun <S : Inputtable> S.onChange(func: S.(String) -> Boolean): S {
    on(Event.Change.Text) {
        val res = func.invoke(this, it.text)
        it.cancelled = res
        res
    }
    return this
}

/**
 * Make this TextInput only accept numbers between [min] and [max]. It will then dispatch a [Event.Change.Number] event.
 *
 * If [integral] is true, it will only accept integers.
 *
 * @since 1.5.0
 */
fun <S : TextInput> S.numeric(min: Float = 0f, max: Float = 100f, integral: Boolean = false): S {
    onChange { value: String ->
        if (value.isEmpty()) return@onChange false
        // don't fail when the user types a minus sign
        if (value == "-") return@onChange false

        if (integral && value.contains('.')) return@onChange true
        // silently cancel if they try and type multiple zeroes
        if (value == "-00") return@onChange true
        if (value == "00") return@onChange true
        try {
            val v = value.toFloat()
            // fail when out of range
            if (v < min) {
                text = "${if (integral) min.toInt() else min}"
                shake(); false
            } else if (v > max) {
                text = "${if (integral) max.toInt() else max}"
                shake(); false
            } else {
                accept(Event.Change.Number(if (integral) v.toInt() else v))
            }
        } catch (_: NumberFormatException) {
            // fail if it is not a number
            shake(); true
        }
    }
    return this
}

/**
 * Add a listener for changes to the given Boolean-type property.
 *
 * In PolyUI, this is for [Checkbox][org.polyfrost.polyui.component.impl.Checkbox] and [Switch][org.polyfrost.polyui.component.impl.Switch] only.
 * @since 1.0.6
 */
@JvmName("onChangeState")
fun <S : Inputtable> S.onChange(func: S.(Boolean) -> Boolean): S {
    on(Event.Change.State) {
        val res = func.invoke(this, it.state)
        it.cancelled = res
        res
    }
    return this
}

/**
 * Add a listener for changes to the given Int-type property.
 *
 * In PolyUI, this is for [Radiobutton][org.polyfrost.polyui.component.impl.Radiobutton], and [Dropdown][org.polyfrost.polyui.component.impl.Dropdown] only.
 * @since 1.0.6
 */
@JvmName("onChangeIndex")
fun <S : Inputtable> S.onChange(func: S.(Int) -> Boolean): S {
    on(Event.Change.Number) {
        val res = func.invoke(this, it.amount.toInt())
        it.cancelled = res
        res
    }
    return this
}

/**
 * Add a listener for changes to the given Float-type property.
 *
 * In PolyUI, this is for [Slider][org.polyfrost.polyui.component.impl.Slider] only.
 * @since 1.0.6
 */
@JvmName("onChangeNumber")
fun <S : Inputtable> S.onChange(func: S.(Float) -> Boolean): S {
    on(Event.Change.Number) {
        val res = func.invoke(this, it.amount.toFloat())
        it.cancelled = res
        res
    }
    return this
}

fun <S : Drawable> S.setPalette(palette: Colors.Palette): S {
    this.palette = palette
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

fun <S : Text> S.secondary(): S {
    setPalette { text.secondary }
    return this
}

fun <S : Drawable> S.setDestructivePalette() = setPalette {
    Colors.Palette(text.primary.normal, state.danger.hovered, state.danger.pressed, text.primary.disabled)
}

fun <S : Drawable> S.withStates() = withStatesCached()

fun <S : Drawable> S.withStates(
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

fun <S : Block> S.withBoarder(color: PolyColor, width: Float = 1f): S {
    this.borderColor = color
    this.borderWidth = width
    return this
}

fun <S : Block> S.withBoarder(width: Float = 1f, color: (Colors.() -> PolyColor) = { page.border5 }): S {
    onInit {
        this.borderColor = polyUI.colors.color()
        this.borderWidth = width
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

/**
 * Prioritize this component, meaning it will, in relation to its siblings:
 * - be drawn last (so visually on top)
 * - receive events first
 *
 * **this method is experimental as it may interfere with statically-typed references to children.**
 * @see relegate
 * @since 1.0.0
 */
@ApiStatus.Experimental
fun <S : Component> S.prioritize(): S {
    val children = _parent?.children ?: return this
    if (children.last() === this) return this
    children.remove(this)
    children.add(this)
    return this
}

/**
 * Relegate this component, meaning it will, in relation to its siblings:
 * - be drawn first (so visually on the bottom)
 * - receive events last
 *
 * **this method is experimental as it may interfere with statically-typed references to children.**
 * @see prioritize
 * @since 1.1.71
 */
@ApiStatus.Experimental
fun <S : Component> S.relegate(): S {
    val children = _parent?.children ?: return this
    if (children.first() === this) return this
    children.remove(this)
    children.add(0, this)
    return this
}

/**
 * Returns a count of this component's children, including children of children.
 * @since 1.0.1
 */
fun Component.countChildren(): Int {
    var i = children?.size ?: 0
    children?.fastEach {
        i += it.countChildren()
    }
    return i
}

/**
 * @return `true` if this drawable has a child that intersects the provided rectangle.
 * @since 1.0.4
 */
fun Component.hasChildIn(x: Float, y: Float, width: Float, height: Float): Boolean {
    val children = this.children ?: return false
    children.fastEach {
        if (!it.renders) return@fastEach
        if (it.intersects(x, y, width, height)) return true
    }
    return false
}

fun <S : Inputtable> S.onInit(function: S.(Event.Lifetime.Init) -> Unit): S {
    on(Event.Lifetime.Init, function)
    return this
}

fun <S : Inputtable> S.afterInit(function: S.(Event.Lifetime.PostInit) -> Unit): S {
    on(Event.Lifetime.PostInit, function)
    return this
}

/**
 * Add an event handler to this drawable's parent, and (optionally) it's parent's parent ([depth]).
 *
 * This is used to run functions that may move or resize this drawable,
 * as if they were ran under this [Event.Lifetime.PostInit] they would be overwritten by the positioning logic.
 */
fun <S : Inputtable> S.afterParentInit(depth: Int = 1, handler: S.() -> Unit): S {
    this.on(Event.Lifetime.PostInit) { _ ->
        var it: Component = parent // will die if parent is null
        for (i in 0 until depth) {
            it._parent?.let { parent -> it = parent } ?: break
        }
        (it as? Inputtable)?.on(Event.Lifetime.PostInit) {
            handler(this@afterParentInit)
        } ?: throw IllegalArgumentException("Parent at depth $depth is not an Inputtable")
        false
    }
    return this
}

@EventDSL.Marker
@OptIn(ExperimentalContracts::class)
inline fun <S : Inputtable> S.events(dsl: EventDSL<S>.() -> Unit): S {
    contract {
        callsInPlace(dsl, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    EventDSL(this).apply(dsl)
    return this
}

fun <S : Inputtable> S.onDrag(func: S.(Event.Mouse.Drag) -> Unit): S {
    on(Event.Mouse.Drag, func)
    return this
}

fun <S : Inputtable> S.onDragStart(func: S.(Event.Mouse.Drag.Started) -> Unit): S {
    on(Event.Mouse.Drag.Started, func)
    return this
}

fun <S : Inputtable> S.onDragEnd(func: S.(Event.Mouse.Drag.Ended) -> Unit): S {
    on(Event.Mouse.Drag.Ended, func)
    return this
}

@OverloadResolutionByLambdaReturnType
@JvmName("onClickZ")
fun <S : Inputtable> S.onClick(func: S.(Event.Mouse.Clicked) -> Boolean): S {
    on(Event.Mouse.Clicked, func)
    return this
}

@OverloadResolutionByLambdaReturnType
fun <S : Inputtable> S.onClick(func: S.(Event.Mouse.Clicked) -> Unit): S {
    on(Event.Mouse.Clicked, func)
    return this
}

fun <S : Inputtable> S.onPress(func: S.(Event.Mouse.Pressed) -> Unit): S {
    on(Event.Mouse.Pressed, func)
    return this
}

fun <S : Inputtable> S.onRelease(func: S.(Event.Mouse.Released) -> Unit): S {
    on(Event.Mouse.Released, func)
    return this
}

fun <S : Component> S.fix(): S {
    x = x.toInt().toFloat()
    y = y.toInt().toFloat()
    width = width.toInt().toFloat()
    height = height.toInt().toFloat()
    children?.fastEach { it.fix() }
    return this
}

@JvmName("ensureLargerThan")
fun <S : Component> S.ensureLargerThan(vec: Vec2) {
    if (width < vec.x) width = vec.x
    if (height < vec.y) height = vec.y
}

/**
 * Locate a component by its name.
 *
 * This method is recursive, meaning it will search through all children of this component.
 * @param id the name of the drawable to locate.
 * @return the drawable with the given name, or `null` if it was not found.
 * @since 1.1.72
 */
fun <S : Component> Component.locate(id: String): S? {
    @Suppress("UNCHECKED_CAST")
    if (this.name == id) return this as S
    children?.fastEach {
        val res = it.locate<S>(id)
        if (res != null) return res
    }
    return null
}

/**
 * Returns `true` if this component is a child of the specified [component].
 * @see isRelatedTo
 * @since 1.4.2
 */
fun Component.isChildOf(component: Component?): Boolean {
    if (component == null) return false
    var p: Component? = this._parent
    while (p != null) {
        if (p === component) return true
        p = p._parent
    }
    return false
}

/**
 * Returns `true` if this component is a child of the specified [component], or if the [component] is a child of this.
 * @see isChildOf
 * @since 1.4.2
 */
fun Component.isRelatedTo(component: Component?) = component != null && component.isChildOf(this) || this.isChildOf(component)

/**
 * Bulk add method for all the builtin drawable operations in PolyUI.
 *
 * The default values of this method are all zeroes, meaning that nothing is done.
 *
 * This is the "by" version of this method, meaning that each value is added to the current one of this drawable.
 * See the [animateTo] method for the "to" equivalent, which sets each value to the provided one.
 */
@JvmName("animateBy")
fun Drawable.animateBy(
    at: Vec2 = Vec2.ZERO,
    size: Vec2 = Vec2.ZERO,
    rotation: Double = 0.0,
    scaleX: Float = 0f,
    scaleY: Float = 0f,
    skewX: Double = 0.0,
    skewY: Double = 0.0,
    color: PolyColor? = null,
    animation: Animation? = null,
) {
    if (!at.isZero) Move(this, at, true, animation).add()
    if (!at.isZero) Resize(this, size, true, animation).add()
    if (rotation != 0.0) Rotate(this, rotation, true, animation).add()
    if (skewX != 0.0 || skewY != 0.0) Skew(this, skewX, skewY, true, animation).add()
    if (scaleX != 0f || scaleY != 0f) Scale(this, scaleX, scaleY, true, animation).add()
    if (color != null) Recolor(this, color, animation).add()
}

fun <S : Component> S.shake(): S {
    ShakeOp(this, 0.2.seconds, 2).add()
    return this
}

/**
 * Bulk add method for all the builtin drawable operations in PolyUI.
 *
 * The default values of this method are all the current ones of this drawable, meaning that nothing is done.
 *
 * This is the "to" version of this method, meaning that each value is set to the provided one.
 * See the [animateBy] method for the "by" equivalent, which adds each value to the current one of this drawable.
 */
@JvmName("animateTo")
fun Drawable.animateTo(
    at: Vec2 = this.at,
    size: Vec2 = this.size,
    rotation: Double = this.rotation,
    skewX: Double = this.skewX,
    skewY: Double = this.skewY,
    scaleX: Float = this.scaleX,
    scaleY: Float = this.scaleY,
    color: PolyColor = this.color,
    animation: Animation? = null,
) {
    if (at != this.at) Move(this, at, false, animation).add()
    if (size != this.size) Resize(this, size, false, animation).add()
    if (rotation != this.rotation) Rotate(this, rotation, false, animation).add()
    if (skewX != this.skewX || skewY != this.skewY) Skew(this, skewX, skewY, false, animation).add()
    if (scaleX != this.scaleX || scaleY != this.scaleY) Scale(this, scaleX, scaleY, false, animation).add()
    if (color != this.color) Recolor(this, color, animation).add()
}
