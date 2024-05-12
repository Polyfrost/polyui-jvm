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

package org.polyfrost.polyui.component

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INPUT_DISABLED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.animate.Easing
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.operations.*
import org.polyfrost.polyui.renderer.data.Framebuffer
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.*

/**
 * # Drawable
 * The most basic thing in the PolyUI rendering system.
 *
 * Supports recoloring, animations, and the entire event system.
 */
abstract class Drawable(
    vararg children: Drawable? = arrayOf(),
    x: Float = 0f,
    y: Float = 0f,
    val alignment: Align = AlignDefault,
    size: Vec2? = null,
    visibleSize: Vec2? = null,
    palette: Colors.Palette? = null,
    val focusable: Boolean = false,
) : Cloneable {
    constructor(
        vararg children: Drawable? = arrayOf(),
        at: Vec2? = null,
        alignment: Align = AlignDefault,
        size: Vec2? = null,
        visibleSize: Vec2? = null,
        palette: Colors.Palette? = null,
        focusable: Boolean = false,
    ) : this(children = children, at?.x ?: 0f, at?.y ?: 0f, alignment, size, visibleSize, palette, focusable)

    var size: Vec2 = size?.mutable() ?: Vec2()

    /**
     * internal field for [visibleSize].
     * @since 1.1.0
     */
    @ApiStatus.Internal
    var _visibleSize: Vec2? = visibleSize?.mutable()

    /**
     * The visible size of this drawable. This is used for clipping and scrolling.
     *
     * Unless set, this will return **the same object as** [size]. This may have unintended side effects, depending on what you are doing.
     *
     * Note that due to how this is implemented, it is recommended that you use this in the following way:
     * ```
     * val vs = drawable.visibleSize
     * // now use vs.x and vs.y, etc.
     * ```
     *
     * @since 1.1.0
     */
    inline var visibleSize: Vec2
        get() = _visibleSize ?: size
        set(value) {
            _visibleSize = value
        }

    /**
     * Returns `true` if this drawable has a visible size set.
     */
    inline val hasVisibleSize get() = _visibleSize != null

    @ApiStatus.Internal
    var _parent: Drawable? = null
        set(value) {
            if (value === field) return
            if (field != null) {
                if (value != null) PolyUI.LOGGER.info("transferring ownership of $simpleName from ${field?.simpleName} to ${value.simpleName}")
//                else PolyUI.LOGGER.warn("$simpleName has no path to root, deleted?")
            }
            field = value
        }

    @SideEffects(["_parent"])
    inline var parent: Drawable
        get() = _parent ?: error("cannot move outside of component tree")
        set(value) {
            _parent = value
        }

    /**
     * This property controls weather this drawable resizes "raw", meaning it will **not** respect the aspect ratio.
     *
     * By default, it is `false`, so when resized, the smallest increase is used for both width and height. This means that it will stay at the same aspect ratio.
     *
     * @since 0.19.0
     */
    var rawResize = false

    private var eventHandlers: MutableMap<Any, LinkedList<(Drawable.(Event) -> Boolean)>>? = null

    /**
     * This is the name of this drawable, and it will be consistent over reboots of the program, so you can use it to get drawables from a layout by ID, e.g:
     *
     * `val text = myLayout["Text@4cf777e8"] as Text`
     */
    open var simpleName = "${this::class.java.simpleName}@${Integer.toHexString(this.hashCode())}"

    var children: LinkedList<Drawable>?
        private set

    init {
        if (children.isNotEmpty()) {
            this.children = children.filterNotNullTo(LinkedList<Drawable>()).also { list ->
                list.fastEach {
                    it.parent = this
                }
            }
        } else {
            this.children = null
        }
    }

    lateinit var polyUI: PolyUI
        private set

    inline val renderer get() = polyUI.renderer

    /**
     * `true` if this drawable has any operations.
     * @since 1.0.3
     */
    val operating get() = !operations.isNullOrEmpty()

    @Locking
    @set:Synchronized
    var framebuffer: Framebuffer? = null
        private set

    /**
     * internal counter for framebuffer render count
     */
    private var fbc = 0

    /**
     * Setting of this value could have undesirable results, and the [PolyColor.Animated.recolor] method should be used instead.
     */
    @set:ApiStatus.Experimental
    lateinit var color: PolyColor.Animated

    private var _palette: Colors.Palette? = null

    @SideEffects(["color", "palette"])
    var palette: Colors.Palette
        get() = _palette ?: throw UninitializedPropertyAccessException("Palette is not initialized")
        set(value) {
            _palette = value
            if (::color.isInitialized) color.recolor(value.get(inputState))
        }


    init {
        if (palette != null) this._palette = palette
    }

    /**
     * Internal [x] position of this drawable.
     * @since 1.1.0
     */
    @ApiStatus.Internal
    var _x = x

    /**
     * Internal [y] position of this drawable.
     * @since 1.1.0
     */
    @ApiStatus.Internal
    var _y = y

    @SideEffects(["x", "_x", "atValid", "this.children::x"], "value != x")
    var x: Float
        inline get() = _x
        set(value) {
            atValid = true
            if (value == _x) return
            val d = value - _x
            _x = value
            children?.fastEach {
                it.x += d
            }
        }

    @SideEffects(["y", "_y", "atValid", "this.children::y"], "value != y")
    var y: Float
        inline get() = _y
        set(value) {
            atValid = true
            if (value == _y) return
            val d = value - _y
            _y = value
            children?.fastEach {
                it.y += d
            }
        }

    inline var width: Float
        get() = size.x
        set(value) {
            size.x = value
        }

    inline var height: Float
        get() = size.y
        set(value) {
            size.y = value
        }

    /**
     * returns `true` if the size of this drawable is valid (not zero)
     * @since 1.1.0
     */
    inline val sizeValid get() = width > 0f && height > 0f

    /**
     * returns `true` if the position of this drawable is valid (has been set)
     * @since 1.1.0
     */
    var atValid = false
        private set

    /**
     * Return the position of this drawable as if it were a [Vec2].
     * @since 1.1.0
     */
    fun at(index: Int) = when (index) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException("Index: $index")
    }

    /**
     * Set the position of this drawable as if it were a [Vec2].
     * @since 1.1.0
     */
    fun at(index: Int, value: Float) {
        when (index) {
            0 -> x = value
            1 -> y = value
            else -> throw IndexOutOfBoundsException("Index: $index")
        }
    }

    @Locking
    @set:Synchronized
    protected var xScroll: Animation? = null
        private set

    @Locking
    @set:Synchronized
    protected var yScroll: Animation? = null
        private set

    val scrolling get() = xScroll != null || yScroll != null

    @SideEffects(["_parent.needsRedraw"], `when` = "field != value")
    var needsRedraw = true
        set(value) {
            if (value && !field) {
                _parent?.needsRedraw = true
            }
            field = value
        }

    protected open val shouldScroll get() = true

    var acceptsInput = false

    var initialized = false
        protected set

    /**
     * The current input state of this drawable. Note that this value is **not responsible** for dispatching events
     * such as [Event.Mouse.Exited] or [Event.Mouse.Pressed]. It is just here for tracking purposes and for your benefit.
     *
     * This value will be of [INPUT_DISABLED] (-1), [INPUT_NONE] (0), [INPUT_HOVERED] (1), or [INPUT_PRESSED] (2).
     *
     * **Do not** modify this value!
     * @since 1.0.0
     */
    @Dispatches("Lifetime.Disabled", "value == INPUT_DISABLED")
    @Dispatches("Lifetime.Enabled", "value != INPUT_DISABLED")
    @Dispatches("Mouse.Entered", "value > INPUT_NONE")
    @Dispatches("Mouse.Exited", "value == INPUT_NONE")
    var inputState = INPUT_NONE
        set(value) {
            if (field == value) return

            if (value == INPUT_DISABLED) {
                accept(Event.Lifetime.Disabled)
                // asm: drop all children when this is disabled
                if (initialized) polyUI.inputManager.drop(this)
                field = value
                return
            }
            if (field == INPUT_DISABLED) {
                accept(Event.Lifetime.Enabled)
                field = value
                return
            }
            if (field == INPUT_NONE && value > INPUT_NONE) {
                accept(Event.Mouse.Entered)
            }
            if (value == INPUT_NONE) {
                accept(Event.Mouse.Exited)
            }
            field = value
        }

    /**
     * Whether this drawable should be rendered.
     *
     * This is controlled by [clipChildren] to save resources by not drawing drawables which cannot be seen.
     *
     * if you are calling [render] yourself (which you really shouldn't), you should check this value.
     * @see draw
     * @since 0.21.4
     */
    open var renders = true
        set(value) {
            if (field == value) return
            field = value
            needsRedraw = true
        }

    /**
     * Disabled flag for this component. Dispatches the [Event.Lifetime.Disabled] and [Event.Lifetime.Enabled] events.
     *
     * @since 0.21.4
     */
    @SideEffects(["inputState"])
    inline var enabled
        get() = inputState > INPUT_DISABLED
        set(value) {
            inputState = if (value) INPUT_NONE else INPUT_DISABLED
        }

    protected var operations: LinkedList<DrawableOp>? = null
        private set

    /**
     * current rotation of this drawable (radians).
     *
     * note: this method locks due to the fact the object needs to be translated to the center, rotated, and then translated back.
     * It only locks if the value is `0.0`.
     */
    @Locking(`when` = "value == 0.0")
    var rotation: Double = 0.0
        set(value) {
            if (field == value) return
            if (value == 0.0) {
                synchronized(this) {
                    // lock required!
                    field = value
                }
            } else field = value
            needsRedraw = true
        }

    /** current skew in x dimension of this drawable (radians).
     *
     * locking if set to `0.0`. See [rotation].
     */
    @Locking(`when` = "value == 0.0")
    var skewX: Double = 0.0
        set(value) {
            if (field == value) return
            if (value == 0.0) {
                synchronized(this) {
                    field = value
                }
            } else field = value
            needsRedraw = true
        }

    /**
     * current skew in y dimension of this drawable (radians).
     *
     * Locking if set to `0.0`. See [rotation].
     */
    @Locking(`when` = "value == 0.0")
    var skewY: Double = 0.0
        set(value) {
            if (field == value) return
            if (value == 0.0) {
                synchronized(this) {
                    field = value
                }
            } else field = value
            needsRedraw = true
        }

    /** current scale in x dimension of this drawable. */
    var scaleX: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            needsRedraw = true
        }

    /** current scale in y dimension of this drawable. */
    var scaleY: Float = 1f
        set(value) {
            if (field == value) return
            field = value
            needsRedraw = true
        }

    /**
     * The alpha value of this drawable.
     * @since 0.20.0
     */
    var alpha = 1f

    /** **a**t **c**ache **x** for transformations. */
    private var acx = 0f

    /** **a**t **c**ache **y** for transformations. */
    private var acy = 0f

    @Locking
    @Synchronized
    fun draw() {
        if (!renders) return
        require(initialized) { "Drawable $simpleName is not initialized!" }

        val framebuffer = framebuffer
        val binds = framebuffer != null && fbc < 3
        if (binds) {
            if (!needsRedraw) {
                renderer.drawFramebuffer(framebuffer!!, x, y)
                return
            }
            renderer.bindFramebuffer(framebuffer!!)
            fbc++
        }

        needsRedraw = false
        preRender()
        render()
        children?.fastEach {
            it.draw()
        }
        postRender()

        if (fbc > 0) fbc--
        if (binds) {
            renderer.unbindFramebuffer()
            renderer.drawFramebuffer(framebuffer!!, x, y)
        }
    }

    /**
     * pre-render functions, such as applying transforms.
     * In this method, you should set needsRedraw to true if you have something to redraw for the **next frame**.
     *
     * **make sure to call super [Drawable.preRender]!**
     */
    @MustBeInvokedByOverriders
    protected open fun preRender() {
        val renderer = renderer
        renderer.push()
        operations?.fastEach { it.apply() }

        val px = x
        val py = y
        var ran = false
        xScroll?.let {
            x = it.update(polyUI.delta)
            ran = true
        }
        yScroll?.let {
            y = it.update(polyUI.delta)
            ran = true
        }
        if (ran) {
            val vs = visibleSize
            renderer.pushScissor(xScroll?.from ?: x, yScroll?.from ?: y, vs.x, vs.y)
            if (x != px) {
                needsRedraw = true
            }
            if (y != py) {
                needsRedraw = true
            }
        }
        val r = rotation != 0.0
        val skx = skewX != 0.0
        val sky = skewY != 0.0
        val s = scaleX != 1f || scaleY != 1f
        if (r || skx || sky || s) {
            if (renderer.transformsWithPoint()) {
                val mx = x + width / 2f
                val my = x + height / 2f
                if (r) renderer.rotate(rotation, mx, my)
                if (skx) renderer.skewX(skewX, mx, my)
                if (sky) renderer.skewY(skewY, mx, my)
                if (s) renderer.scale(scaleX, scaleY, x, y)
            } else {
                renderer.translate(x + width / 2f, y + height / 2f)
                if (r) renderer.rotate(rotation, 0f, 0f)
                if (skx) renderer.skewX(skewX, 0f, 0f)
                if (sky) renderer.skewY(skewY, 0f, 0f)
                renderer.translate(-(width / 2f), -(height / 2f))
                if (s) renderer.scale(scaleX, scaleY, 0f, 0f)
                acx = x
                acy = y
                x = 0f
                y = 0f
            }
        }
        if (alpha != 1f) renderer.globalAlpha(alpha)
        else if (!enabled) renderer.globalAlpha(0.8f)
    }

    /** draw script for this drawable. */
    protected abstract fun render()

    /**
     * Called after rendering, for functions such as removing transformations.
     *
     * **make sure to call super [Drawable.postRender]!**
     */
    @MustBeInvokedByOverriders
    protected open fun postRender() {
        operations?.fastRemoveIfReversed {
            it.unapply()
        }
        if (xScroll != null || yScroll != null) renderer.popScissor()
        renderer.pop()
        if (acx != 0f) {
            x = acx
            y = acy
            acx = 0f
        }
    }

    /**
     * Method that is called when the physical size of the total window area changes.
     * @param position weather to scale the [at] property of this drawable as well. Note that this is a surface level change (by design), and will not affect children.
     */
    @Locking
    @MustBeInvokedByOverriders
    @Synchronized
    open fun rescale(scaleX: Float, scaleY: Float, position: Boolean = true) {
        val sx: Float
        val sy: Float
        if (rawResize) {
            sx = scaleX
            sy = scaleY
        } else {
            val s = cl1(scaleX, scaleY)
            sx = s
            sy = s
        }

        if (position) {
            _x *= sx
            _y *= sy
            xScroll?.let { it.from *= sx; it.to *= sx }
            yScroll?.let { it.from *= sy; it.to *= sy }
        }
        size.scale(sx, sy)
        _visibleSize?.scale(sx, sy)

        children?.fastEach { it.rescale(scaleX, scaleY) }
        framebuffer?.let {
            renderer.delete(it)
            framebuffer = renderer.createFramebuffer(size.x, size.y)
        }
    }

    fun clipChildren() {
        val children = children ?: return
        val tx = xScroll?.from ?: x
        val ty = yScroll?.from ?: y
        val vs = visibleSize
        val tw = vs.x
        val th = vs.y
        _clipChildren(children, tx, ty, tw, th)
        needsRedraw = true
    }

    private fun _clipChildren(children: LinkedList<Drawable>, tx: Float, ty: Float, tw: Float, th: Float) {
        children.fastEach {
            if (!it.enabled) return@fastEach
            it.renders = it.intersects(tx, ty, tw, th)
            it._clipChildren(it.children ?: return@fastEach, tx, ty, tw, th)
        }
    }

    /** function that should return true if it is ready to be removed from its parent.
     *
     * This is used for drawables that need to wait for an animation to finish before being removed.
     */
    open fun canBeRemoved(): Boolean = operations.isNullOrEmpty() && (children?.allAre { it.canBeRemoved() } != false)

    fun debugDraw() {
        if (!renders) return
        debugRender()
        children?.fastEach {
            if (!it.renders) return@fastEach
            it.debugDraw()
        }
    }

    /**
     * Use this function to calculate the size of this drawable. DO NOT include children in this calculation!
     *
     * This function is only called if no size is supplied to this drawable at initialization.
     * A return value of `null` indicates this drawable is not able to infer its own size, as it is for example, a Block with no reference.
     * A drawable such as Text on the other hand, is able to infer its own size.
     */
    open fun calculateSize(): Vec2? = null

    /** add a debug render overlay for this drawable. This is always rendered regardless of the layout re-rendering if debug mode is on. */
    protected open fun debugRender() {
        val color = if (inputState > INPUT_NONE) polyUI.colors.page.border20 else polyUI.colors.page.border10
        val vs = visibleSize
        renderer.hollowRect(xScroll?.from ?: x, yScroll?.from ?: y, vs.x, vs.y, color, 1f)
    }

    /**
     * called when this drawable receives an event.
     *
     * @return true if the event should be consumed (cancelled so no more handlers are called), false otherwise.
     */
    @MustBeInvokedByOverriders
    open fun accept(event: Event): Boolean {
        if (!enabled) return false
        if (hasVisibleSize && event is Event.Mouse.Scrolled) {
            var ran = false
            xScroll?.let {
                it.durationNanos = 0.6.seconds
                it.to += if (yScroll == null && event.amountX == 0f) event.amountY
                else event.amountX
                it.to = it.to.coerceIn(it.from - (size.x - visibleSize.x), it.from)
                ran = true
            }

            yScroll?.let {
                it.durationNanos = 0.6.seconds
                it.to += event.amountY
                it.to = it.to.coerceIn(it.from - (size.y - visibleSize.y), it.from)
                ran = true
            }

            if (ran) {
                needsRedraw = true
                polyUI.inputManager.recalculate()
                clipChildren()
                return true
            }
        }
        val eh = eventHandlers ?: return false
        val handlers = eh[event::class.java] ?: eh[event] ?: return false
        handlers.fastEach {
            if (it(this, event)) return true
        }
        return false
    }

    /** give this a PolyUI reference.
     *
     * You can also use this method to do some calculations (such as text widths) that are not dependent on other sizes,
     * or operations that require access to PolyUI things, like [PolyUI.settings] or [PolyUI.translator]
     *
     * this method is called once, and only once.
     *
     * @see initialized
     */
    @MustBeInvokedByOverriders
    open fun setup(polyUI: PolyUI): Boolean {
        if (initialized) return false
        this.polyUI = polyUI
        if (_palette == null) palette = polyUI.colors.component.bg
        if (!::color.isInitialized) this.color = palette.normal.toAnimatable()
        children?.fastEach {
            it.setup(polyUI)
        }
        // asm: don't use accept as we don't want to dispatch to children
        eventHandlers?.maybeRemove(Event.Lifetime.Init::class.java, polyUI.settings.aggressiveCleanup)?.fastEach { it(this, Event.Lifetime.Init) }
        polyUI.positioner.position(this)
        initialized = true

        eventHandlers?.maybeRemove(Event.Lifetime.PostInit::class.java, polyUI.settings.aggressiveCleanup)?.fastEach { it(this, Event.Lifetime.PostInit) }

        // following all init events being removed, if it is empty, we can set it to null
        if (eventHandlers.isNullOrEmpty()) eventHandlers = null
        clipChildren()
        if (polyUI.canUseFramebuffers) {
            if (countChildren() > polyUI.settings.minDrawablesForFramebuffer || (this === polyUI.master && polyUI.settings.isMasterFrameBuffer)) {
                framebuffer = renderer.createFramebuffer(size.x, size.y)
                if (polyUI.settings.debug) PolyUI.LOGGER.info("Drawable ${this.simpleName} created with $framebuffer")
            }
        }
        return true
    }

    @Locking(`when` = "this.shouldScroll && this.hasVisibleSize && this.visibleSize > this.size")
    fun tryMakeScrolling() {
        if (shouldScroll && hasVisibleSize) {
            var scrolling = false
            if (size.x > visibleSize.x && xScroll == null) {
                scrolling = true
                xScroll = Easing.Expo(Easing.Type.Out, 0L, x, x)
            }
            if (size.y > visibleSize.y && yScroll == null) {
                scrolling = true
                yScroll = Easing.Expo(Easing.Type.Out, 0L, y, y)
            }
            if (scrolling) {
                acceptsInput = true
                if (polyUI.settings.debug) PolyUI.LOGGER.info("Enabled scrolling for $simpleName")
                clipChildren()
            }
        }
        children?.fastEach { it.tryMakeScrolling() }
    }

    @ApiStatus.Experimental
    @MustBeInvokedByOverriders
    @Locking
    @Synchronized
    open fun reset() {
        if (!initialized) return
        this.x = 0f
        this.y = 0f
        this.atValid = false
        this.initialized = false
        children?.fastEach { it.reset() }
    }

    /**
     * Reposition all the children of this drawable.
     *
     * Note that in most circumstances, [recalculate] is the method that you actually want.
     *
     * **This method is experimental because it may interfere with children that were placed manually with the [at] property.**
     * @see recalculate
     * @since 1.0.2
     */
    @ApiStatus.Experimental
    @Locking(`when` = "this.children != null")
    @Synchronized
    fun repositionChildren() {
        if (children == null) return
        children?.fastEach {
            it.x = 0f
            it.y = 0f
            it.atValid = false
        }
        polyUI.positioner.position(this)
        clipChildren()
    }

    /**
     * Fully recalculate this drawable's size, and any of its children's positions.
     *
     * **This method is experimental because it may interfere with children that were placed manually with the [at] property.**
     * @since 1.0.7
     */
    @ApiStatus.Experimental
    @Locking(`when` = "this.children != null")
    @Synchronized
    fun recalculate() {
        if (children == null) return
        val sz = this.size
        val oldW = sz.x
        val oldH = sz.y
        sz.x = 0f
        sz.y = 0f
        repositionChildren()
        x -= (sz.x - oldW) / 2f
        y -= (sz.y - oldH) / 2f
    }

    /**
     * reset the initial scroll position to the current position.
     *
     * This method should be used if you externally modify the position of this drawable with scrolling enabled.
     * @since 1.0.5
     */
    fun resetScroll() {
        xScroll?.let { it.from = x; it.to = x }
        yScroll?.let { it.from = y; it.to = y }
        children?.fastEach { it.resetScroll() }
    }

    /**
     * @return `true` if the given point is inside this drawable.
     * @see intersects
     * @see isInside(Float, Float, Float, Float)
     */
    fun isInside(x: Float, y: Float): Boolean {
        val tx = this.xScroll?.from ?: this.x
        val ty = this.yScroll?.from ?: this.y
        val vs = this.visibleSize
        val tw = vs.x * scaleX
        val th = vs.y * scaleY
        return x in tx..tx + tw && y in ty..ty + th
    }

    /**
     * @return `true` if the drawable has at least one point inside the given box (x, y, width, height).
     * @since 0.21.4
     * @see isInside
     */
    fun intersects(x: Float, y: Float, width: Float, height: Float): Boolean {
        val tx = this.xScroll?.from ?: this.x
        val ty = this.yScroll?.from ?: this.y
        val vs = this.visibleSize
        val tw = vs.x * scaleX
        val th = vs.y * scaleY
        return (x <= tx + tw && tx <= x + width) && (y <= ty + th && ty <= y + height)
    }

    /**
     * Implement this function to enable cloning of your Drawable.
     * @since 0.19.0
     */
    public override fun clone() = (super.clone() as Drawable)

    override fun toString(): String {
        return if (initialized) {
            if (sizeValid) {
                "$simpleName(${x}x$y, $size)"
            } else "$simpleName(being initialized)"
        } else "$simpleName(not initialized)"
    }

    /**
     * Override this function to add things to the debug display of this drawable.
     * @since 1.1.1
     */
    open fun debugString(): String? = null

    /**
     * Add a [DrawableOp] to this drawable.
     * @return `true` if the operation was added, `false` if it was replaced.
     */
    @Locking(`when` = "drawableOp.verify() == true")
    @Synchronized
    fun addOperation(drawableOp: DrawableOp): Boolean {
        if (!drawableOp.verify()) {
            if (polyUI.settings.debug) PolyUI.LOGGER.warn("Dodged invalid op $drawableOp on ${this.simpleName}")
            return false
        }
        if (operations == null) operations = LinkedList()
        needsRedraw = true
        return operations?.addOrReplace(drawableOp) == null
    }

    @Locking
    fun addOperation(vararg drawableOps: DrawableOp) {
        for (drawableOp in drawableOps) addOperation(drawableOp)
    }

    /**
     * Remove an operation from this drawable.
     */
    @Locking(`when` = "operations != null")
    @Synchronized
    fun removeOperation(drawableOp: DrawableOp) {
        this.operations?.apply {
            if (size == 1) operations = null
            else remove(drawableOp)
        }
        needsRedraw = true
    }

    /**
     * the runtime equivalent of adding children to this drawable during the constructor.
     */
    @Locking
    @Synchronized
    fun addChild(child: Drawable, recalculate: Boolean = true) {
        if (children == null) children = LinkedList()
        val children = this.children ?: throw ConcurrentModificationException("well, this sucks")
        child._parent = this
        children.add(child)
        if (initialized) {
            if (child.setup(polyUI)) {
                val totalSx = polyUI.size.x / polyUI.iSize.x
                val totalSy = polyUI.size.y / polyUI.iSize.y
                child.rescale(totalSx, totalSy, position = true)
                child.tryMakeScrolling()
            }
            if (!child.atValid) {
                if (recalculate) recalculate()
                else {
                    child.x += x
                    child.y += y
                }
            }
            child.accept(Event.Lifetime.Added)
            needsRedraw = true
        }
    }

    @Locking(`when` = "children.size != 0")
    fun addChild(vararg children: Drawable) {
        for (child in children) addChild(child)
    }

    @Locking
    fun removeChild(child: Drawable, recalculate: Boolean = true) {
        val i = children?.indexOf(child) ?: throw NoSuchElementException("no children on $this")
        require(i != -1) { "Drawable $child is not a child of $this" }
        removeChild(i, recalculate)
    }

    @Locking
    @Synchronized
    fun removeChild(index: Int, recalculate: Boolean = true) {
        val children = this.children ?: throw NoSuchElementException("no children on $this")
        val it = children.getOrNull(index) ?: throw IndexOutOfBoundsException("index: $index, length: ${children.size}")
        it._parent = null
        polyUI.inputManager.drop(it)
        if (initialized) {
            it.accept(Event.Lifetime.Removed)
            children.remove(it)
            if (recalculate) recalculate()
            needsRedraw = true
        } else {
            children.remove(it)
        }
    }

    operator fun get(index: Int) = children?.get(index) ?: throw IndexOutOfBoundsException("index: $index, length: ${children?.size ?: 0}")

    operator fun get(id: String): Drawable {
        val children = children ?: throw NoSuchElementException("no children on $this")
        children.fastEach {
            if (it.simpleName == id) return it
        }
        throw NoSuchElementException("no child with id $id")
    }

    /**
     * Replace a child of this drawable with another drawable.
     *
     * This will add a nice, smooth [Fade] animation to the transition.
     * @since 1.0.1
     */
    @Locking
    @Synchronized
    operator fun set(old: Drawable, new: Drawable?) {
        require(initialized) { "$this must be setup at this point!" }
        val children = children ?: throw NoSuchElementException("no children on $this")
        val index = children.indexOf(old)
        require(index != -1) { "Drawable $old is not a child of $this" }
        if (new == null) {
            removeChild(old)
            return
        }
        new._parent = this
        val isNew = new.setup(polyUI)
        Fade(old, 0f, false, Animations.EaseInOutQuad.create(0.3.seconds)) {
            enabled = false
            children.remove(this)
            _parent = null
            polyUI.inputManager.drop(this)
        }.add()
        children.add(index, new)
        new.enabled = true
        if (isNew) {
            val totalSx = polyUI.size.x / polyUI.iSize.x
            val totalSy = polyUI.size.y / polyUI.iSize.y
            new.rescale(totalSx, totalSy, position = false)
        }
        new.alpha = 0f
        new.x = old.x - (old.x - (old.xScroll?.from ?: old.x))
        new.y = old.y - (old.y - (old.yScroll?.from ?: old.y))
        if (isNew) new.tryMakeScrolling()
        Fade(new, 1f, false, Animations.EaseInOutQuad.create(0.3.seconds)).add()
    }

    @Locking
    operator fun set(old: Int, new: Drawable) = set(this[old], new)

    operator fun get(x: Float, y: Float) = children?.first { it.isInside(x, y) } ?: throw NoSuchElementException("no children on $this")

    @OverloadResolutionByLambdaReturnType
    fun <E : Event, S : Drawable> S.on(event: E, handler: S.(E) -> Boolean): S {
        if (!acceptsInput && event !is Event.Lifetime) acceptsInput = true
        val ev = eventHandlers ?: HashMap(8)
        // asm: non-specific events will not override hashCode, so identityHashCode will return the same
        val ls = if (event.hashCode() == System.identityHashCode(event)) ev.getOrPut(event::class.java) { LinkedList() }
        else ev.getOrPut(event) { LinkedList() }
        @Suppress("UNCHECKED_CAST")
        ls.add(handler as Drawable.(Event) -> Boolean)
        eventHandlers = ev
        return this
    }

    @JvmName("addEventhandler")
    @OverloadResolutionByLambdaReturnType
    inline fun <E : Event, S : Drawable> S.on(event: E, crossinline handler: S.(E) -> Unit): S = on(event) { handler(this, it); true }

    // asm: uses java class because bootstrapping reflect for this is not worth the slightly better syntax tbh
    /**
     * returns `true` if this drawable has any [non-specific][Event] event handlers registered for it.
     *
     * pass an event instance to this method for [specific][Event] events.
     * @since 1.1.61
     */
    fun hasListenersFor(event: Class<out Event>) = eventHandlers?.containsKey(event) ?: false

    /**
     * returns `true` if this drawable has any [specific][Event] event handlers registered for it.
     *
     * pass a class instance to this method for [non-specific][Event] events.
     * @since 1.1.71
     */
    fun hasListenersFor(event: Event): Boolean {
        val k: Any = if (System.identityHashCode(event) == event.hashCode()) event::class.java else event
        return eventHandlers?.containsKey(k) ?: false
    }
}
