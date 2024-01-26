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

@file:Suppress("invisible_member", "invisible_reference")

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
    at: Vec2? = null,
    val alignment: Align = AlignDefault,
    size: Vec2? = null,
    visibleSize: Vec2? = null,
    palette: Colors.Palette? = null,
    val focusable: Boolean = false,
) : Cloneable {

    var size = size ?: Vec2()
        set(value) {
            field = value
            (visibleSize as? Vec2.Sourced)?.source = value
        }

    val visibleSize: Vec2 = visibleSize ?: Vec2.Based(base = this.size)

    @Transient
    var parent: Drawable? = null
        private set(value) {
            if (value === field) return
            if (field != null) {
                if (value != null) PolyUI.LOGGER.info("transferring ownership of $simpleName from ${field?.simpleName} to ${value.simpleName}")
//                else PolyUI.LOGGER.warn("$simpleName has no path to root, deleted?")
            }
            field = value
        }

    /**
     * Setting this value is not recommended, as it may break relative positioning to the parent.
     * If you want to move this drawable with an animation, use the [Move] drawable operation.
     *
     * If you want to just move it programmatically, use the [Vec2.x] and [Vec2.y] fields or [x] or [y] (they are equal)
     *
     * The current implementation of this method is the following:
     * - if the new value is a [Vec2.Sourced], and its source is `null`, it will be set to the parent's [at] property. It is assumed that that is what you want.
     * - if the new value is a [Vec2.Relative], but its source is not the parent's [at] property, it will be set to the parent's [at] property, and a warning will be logged.
     * - if the new value is not a [Vec2.Sourced], but the parent's [at] property is, it will be set to a [Vec2.Relative] with the parent's [at] property as its source, and a warning will be logged.
     */
    @set:ApiStatus.Experimental
    var at: Vec2 = Vec2.Relative()
        set(value) {
            if (value === field) return

            val fieldSource = (field as? Vec2.Sourced)?.source

            if (value is Vec2.Sourced) {
                if (!value.sourced) {
                    value.source = parent?.at ?: Vec2.ZERO
                    field = value
                } else if (value is Vec2.Relative && value.source !== parent?.at && fieldSource === parent?.at) {
                    PolyUI.LOGGER.warn("setting $simpleName.at which is relative to parent, but the new value is relative to something else, forcing!")
                    field = value.makeRelative()
                } else {
                    field = value
                }
            } else if (fieldSource === parent?.at) {
                PolyUI.LOGGER.warn("setting $simpleName.at which is relative to parent, but the new value is not relative, forcing!")
                field = value.makeRelative()
            } else {
                field = value
            }
        }

    init {
        if (at != null) {
            if (at !is Vec2.Sourced) this.at = at.makeRelative()
            else this.at = at
        }
    }

    /**
     * This property controls weather this drawable resizes "raw", meaning it will **not** respect the aspect ratio.
     *
     * By default, it is `false`, so when resized, the smallest increase is used for both width and height. This means that it will stay at the same aspect ratio.
     *
     * @since 0.19.0
     */
    var rawResize = false

    @Transient
    private var eventHandlers: MutableMap<Event, LinkedList<(Drawable.(Event) -> Boolean)>>? = null

    /**
     * This is the name of this drawable, and it will be consistent over reboots of the program, so you can use it to get drawables from a layout by ID, e.g:
     *
     * `val text = myLayout["Text@4cf777e8"] as Text`
     */
    @Transient
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

    @Transient
    lateinit var polyUI: PolyUI
        private set

    @kotlin.internal.InlineOnly
    inline val renderer get() = polyUI.renderer

    /**
     * `true` if this drawable has any operations.
     * @since 1.0.3
     */
    val operating get() = !operations.isNullOrEmpty()

//    @set:Locking
//    var framebuffer: Framebuffer? = null

    /**
     * Setting of this value could have undesirable results, and the [PolyColor.Animated.recolor] method should be used instead.
     */
    @set:ApiStatus.Experimental
    lateinit var color: PolyColor.Animated

    @Transient
    @set:JvmName("_setPalette")
    lateinit var palette: Colors.Palette
        protected set

    /**
     * For some reason, you can't have custom setters on `lateinit` properties, so this is a workaround.
     * @since 1.0.1
     */
    fun <S : Drawable> S.setPalette(palette: Colors.Palette): S {
        this.palette = palette
        if (::color.isInitialized) this.color.recolor(palette.get(this.inputState))
        return this
    }

    init {
        if (palette != null) this.palette = palette
    }

    // InlineOnly makes it so that it doesn't add useless local variables called $i$f$getX or whatever
    @kotlin.internal.InlineOnly
    inline var x: Float
        get() = at.x
        set(value) {
            at.x = value
        }

    @kotlin.internal.InlineOnly
    inline var y: Float
        get() = at.y
        set(value) {
            at.y = value
        }

    @kotlin.internal.InlineOnly
    inline var width: Float
        get() = size.x
        set(value) {
            size.x = value
        }

    @kotlin.internal.InlineOnly
    inline var height: Float
        get() = size.y
        set(value) {
            size.y = value
        }

    @kotlin.internal.InlineOnly
    inline var visibleWidth: Float
        get() = visibleSize.x
        set(value) {
            visibleSize.x = value
        }

    @kotlin.internal.InlineOnly
    inline var visibleHeight: Float
        get() = visibleSize.y
        set(value) {
            visibleSize.y = value
        }

    @Transient
    @Locking
    @set:Synchronized
    protected var xScroll: Animation? = null
        private set

    @Transient
    @Locking
    @set:Synchronized
    protected var yScroll: Animation? = null
        private set

    val scrolling get() = xScroll != null || yScroll != null

    @Transient
    var needsRedraw = true
        set(value) {
            if (value && !field) {
                parent?.needsRedraw = true
            }
            field = value
        }

    protected open val shouldScroll get() = true

    @Transient
    var acceptsInput = false

    @Transient
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
    @Transient
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
    @Transient
    open var renders = true
        set(value) {
            field = value
            needsRedraw = true
        }

    /**
     * Disabled flag for this component. Dispatches the [Event.Lifetime.Disabled] and [Event.Lifetime.Enabled] events.
     *
     * @since 0.21.4
     */
    @kotlin.internal.InlineOnly
    inline var enabled
        get() = inputState > INPUT_DISABLED
        set(value) {
            inputState = if (value) INPUT_NONE else INPUT_DISABLED
        }

    @Transient
    protected var operations: LinkedList<DrawableOp>? = null
        private set

    /**
     * current rotation of this drawable (radians).
     *
     * note: this method locks due to the fact the object needs to be translated to the center, rotated, and then translated back.
     * It only locks if the value is `0.0`.
     */
    @Locking
    var rotation: Double = 0.0
        set(value) {
            if (value == 0.0) {
                synchronized(this) {
                    // lock required!
                    field = value
                }
            } else field = value
        }

    /** current skew in x dimension of this drawable (radians).
     *
     * locking if set to `0.0`. See [rotation].
     */
    @Locking
    var skewX: Double = 0.0
        set(value) {
            if (value == 0.0) {
                synchronized(this) {
                    field = value
                }
            } else field = value
        }

    /**
     * current skew in y dimension of this drawable (radians).
     *
     * Locking if set to `0.0`. See [rotation].
     */
    @Locking
    var skewY: Double = 0.0
        set(value) {
            if (value == 0.0) {
                synchronized(this) {
                    field = value
                }
            } else field = value
        }

    /** current scale in x dimension of this drawable. */
    var scaleX: Float = 1f

    /** current scale in y dimension of this drawable. */
    var scaleY: Float = 1f

    /**
     * The alpha value of this drawable.
     * @since 0.20.0
     */
    var alpha = 1f

    /** **a**t **c**ache **x** for transformations. */
    @Transient
    private var acx = 0f

    /** **a**t **c**ache **y** for transformations. */
    @Transient
    private var acy = 0f

    @Locking
    @Synchronized
    fun draw() {
        needsRedraw = false
        if (!renders) return
        require(initialized) { "Drawable $simpleName is not initialized!" }
        // todo impl framebuffers
//        framebuffer?.let { renderer.bindFramebuffer(it) }
        preRender()
        render()
        children?.fastEach {
            it.draw()
        }
        postRender()
//        framebuffer?.let { renderer.unbindFramebuffer(it) }
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
            renderer.pushScissor(xScroll?.from ?: x, yScroll?.from ?: y, visibleSize.x, visibleSize.y)
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
        if (rawResize) {
            if (position) {
                at.scale(scaleX, scaleY)
                xScroll?.let { it.from *= scaleX; it.to *= scaleX }
                yScroll?.let { it.from *= scaleY; it.to *= scaleY }
            }
            size.scale(scaleX, scaleY)
            visibleSize.scale(scaleX, scaleY)
        } else {
            val scale = cl1(scaleX, scaleY)
            if (position) {
                at *= scale
                xScroll?.let { it.from *= scale; it.to *= scale }
                yScroll?.let { it.from *= scale; it.to *= scale }
            }
            size *= scale
            visibleSize *= scale
        }
        children?.fastEach { it.rescale(scaleX, scaleY) }
    }

    fun clipChildren() {
        val tx = xScroll?.from ?: x
        val ty = yScroll?.from ?: y
        children?.fastEach {
            if (!it.enabled) return@fastEach
            it.renders = it.intersects(tx, ty, visibleSize.x, visibleSize.y)
            it.clipChildren()
        }
        needsRedraw = true
    }

    /** function that should return true if it is ready to be removed from its parent.
     *
     * This is used for drawables that need to wait for an animation to finish before being removed.
     */
    open fun canBeRemoved(): Boolean = operations.isNullOrEmpty() && (children?.allAre { it.canBeRemoved() } ?: true)

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
        renderer.hollowRect(xScroll?.from ?: x, yScroll?.from ?: y, visibleWidth, visibleHeight, color, 1f)
    }

    /**
     * called when this drawable receives an event.
     *
     * @return true if the event should be consumed (cancelled so no more handlers are called), false otherwise.
     */
    @MustBeInvokedByOverriders
    open fun accept(event: Event): Boolean {
        if (!enabled) return false
        if (event is Event.Mouse.Scrolled) {
            var ran = false
            xScroll?.let {
                it.durationNanos = 0.6.seconds
                it.to += event.amountX
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
        eventHandlers?.get(event)?.fastEach {
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
        initialized = true
        this.polyUI = polyUI
        if (!::palette.isInitialized) palette = polyUI.colors.component.bg
        if (!::color.isInitialized) this.color = palette.normal.toAnimatable()
        children?.fastEach {
            it.setup(polyUI)
        }
        // asm: don't use accept as we don't want to dispatch to children
        eventHandlers?.maybeRemove(Event.Lifetime.Init, polyUI.settings.aggressiveCleanup)?.fastEach { it(this, Event.Lifetime.Init) }
        polyUI.positioner.position(this)

        eventHandlers?.maybeRemove(Event.Lifetime.PostInit, polyUI.settings.aggressiveCleanup)?.fastEach { it(this, Event.Lifetime.PostInit) }

        // following all init events being removed, if it is empty, we can set it to null
        if (eventHandlers.isNullOrEmpty()) eventHandlers = null
        clipChildren()
        return true
    }

    @Locking
    fun tryMakeScrolling() {
        if (shouldScroll) {
            var scrolling = false
            if (size.x > visibleSize.x && xScroll == null) {
                scrolling = true
                xScroll = Easing.Expo(Easing.Type.Out, 0L, at.x, at.x)
            }
            if (size.y > visibleSize.y && yScroll == null) {
                scrolling = true
                yScroll = Easing.Expo(Easing.Type.Out, 0L, at.y, at.y)
            }
            if (scrolling) {
                acceptsInput = true
                if (polyUI.settings.debug) PolyUI.LOGGER.info("Enabled scrolling for $simpleName")
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
        this.initialized = false
        children?.fastEach { it.reset() }
    }

    /**
     * Reposition all the children of this drawable.
     * @since 1.0.2
     */
    @ApiStatus.Experimental
    @Locking
    @Synchronized
    fun recalculateChildren() {
        val oldX = this.x
        val oldY = this.y
        this.x = 0f
        this.y = 0f
        children?.fastEach {
            it.x = 0f
            it.y = 0f
        }
        polyUI.positioner.position(this)
        this.x = oldX
        this.y = oldY
        clipChildren()
    }

    /**
     * Reposition the children of this drawable, and recalculate the size of this drawable.
     * @since 1.0.7
     */
    @ApiStatus.Experimental
    @Locking
    @Synchronized
    fun recalculate() {
        val oldX = this.x
        val oldY = this.y
        this.x = 0f
        this.y = 0f
        this.size.x = 0f
        this.size.y = 0f
        children?.fastEach {
            it.x = 0f
            it.y = 0f
        }
        polyUI.positioner.position(this)
        this.x = oldX
        this.y = oldY
        clipChildren()
    }

    /**
     * reset the initial scroll position to the current position.
     *
     * This method should be used if you externally modify the position of this drawable with scrolling enabled.
     * @since 1.0.5
     */
    fun resetScroll() {
        xScroll?.let {
            it.from = at.x
            it.to = at.x
        }
        yScroll?.let {
            it.from = at.y
            it.to = at.y
        }
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
        return x in tx..tx + visibleSize.x && y in ty..ty + visibleSize.y
    }

    /**
     * @return `true` if the drawable has at least one point inside the given box (x, y, width, height).
     * @since 0.21.4
     * @see isInside
     */
    fun intersects(x: Float, y: Float, width: Float, height: Float): Boolean {
        val tx = this.xScroll?.from ?: this.x
        val ty = this.yScroll?.from ?: this.y
        val tw = this.visibleSize.x
        val th = this.visibleSize.y
        return (x <= tx + tw && tx <= x + width) && (y <= ty + th && ty <= y + height)
    }

    /**
     * Implement this function to enable cloning of your Drawable.
     * @since 0.19.0
     */
    public override fun clone() = (super.clone() as Drawable)

    override fun toString() = "$simpleName($at, $size)"

    /**
     * Add a [DrawableOp] to this drawable.
     * @return `true` if the operation was added, `false` if it was replaced.
     */
    @Locking
    @Synchronized
    fun addOperation(drawableOp: DrawableOp): Boolean {
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
    @Locking
    @Synchronized
    fun removeOperation(drawableOp: DrawableOp) {
        this.operations?.apply {
            if (size == 1) operations = null
            else remove(drawableOp)
        }
        needsRedraw = true
    }

    @Locking
    @Synchronized
    fun addChild(child: Drawable, reposition: Boolean = true) {
        child.parent = this
        if (children == null) children = LinkedList()
        val children = this.children ?: throw ConcurrentModificationException("well, this sucks")
        children.add(child)
        if (initialized) {
            if (child.setup(polyUI)) {
                child.at = child.at.makeRelative(this.at)
                val totalSx = polyUI.size.x / polyUI.iSize.x
                val totalSy = polyUI.size.y / polyUI.iSize.y
                child.rescale(totalSx, totalSy, position = true)
                child.tryMakeScrolling()
            }
            if (reposition && child.at.hasZero) recalculateChildren()
            child.accept(Event.Lifetime.Added)
            needsRedraw = true
        }
    }

    @Locking
    fun addChild(vararg children: Drawable) {
        for (child in children) addChild(child)
    }

    @Locking
    fun removeChild(child: Drawable) {
        val i = children?.indexOf(child) ?: throw NullPointerException("no children on $this")
        require(i != -1) { "Drawable $child is not a child of $this" }
        removeChild(i)
    }

    @Locking
    @Synchronized
    fun removeChild(index: Int) {
        val children = this.children ?: throw NullPointerException("no children on $this")
        val it = children.getOrNull(index) ?: throw IndexOutOfBoundsException("index: $index, length: ${children.size}")
        it.parent = null
        polyUI.inputManager.drop(it)
        if (initialized) {
            it.accept(Event.Lifetime.Removed)
            children.remove(it)
            needsRedraw = true
        } else {
            children.remove(it)
        }
    }

    operator fun get(index: Int) = children?.get(index) ?: throw IndexOutOfBoundsException("index: $index, length: ${children?.size ?: 0}")

    operator fun get(id: String): Drawable {
        val children = children ?: throw NullPointerException("no children on $this")
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
        val children = children ?: throw NullPointerException("no children on $this")
        val index = children.indexOf(old)
        require(index != -1) { "Drawable $old is not a child of $this" }
        if (new == null) {
            removeChild(old)
            return
        }
        new.at = new.at.makeRelative((old.at as? Vec2.Sourced)?.source).zero()
        val isNew = new.setup(polyUI)
        new.x = old.x - (old.x - (old.xScroll?.from ?: old.x))
        new.y = old.y - (old.y - (old.yScroll?.from ?: old.y))
        Fade(old, 0f, false, Animations.EaseInOutQuad.create(0.3.seconds)) {
            enabled = false
            children.remove(this)
            parent = null
            polyUI.inputManager.drop(this)
        }.add()
        if (polyUI.settings.debug && new.visibleSize != old.visibleSize) PolyUI.LOGGER.warn("replacing drawable $old with $new, but visible sizes are different: ${old.visibleSize} -> ${new.visibleSize}")
        children.add(index, new)
        new.enabled = true
        new.parent = this
        if (isNew) {
            val totalSx = polyUI.size.x / polyUI.iSize.x
            val totalSy = polyUI.size.y / polyUI.iSize.y
            new.rescale(totalSx, totalSy, position = false)
            new.tryMakeScrolling()
        }
        new.alpha = 0f
        Fade(new, 1f, false, Animations.EaseInOutQuad.create(0.3.seconds)).add()
    }

    @Locking
    operator fun set(old: Int, new: Drawable) = set(this[old], new)

    operator fun get(x: Float, y: Float) = children?.first { it.isInside(x, y) } ?: throw NullPointerException("no children on $this")

    @JvmName("addEventhandler")
    @OverloadResolutionByLambdaReturnType
    fun <E : Event, S : Drawable> S.addEventHandler(event: E, handler: S.(E) -> Unit?): S {
        val s: S.(E) -> Boolean = {
            handler(this, it)
            true
        }
        addEventHandler(event, s)
        return this
    }

    @OverloadResolutionByLambdaReturnType
    @Suppress("UNCHECKED_CAST")
    fun <E : Event, S : Drawable> S.addEventHandler(event: E, handler: S.(E) -> Boolean): S {
        if (event !is Event.Lifetime) acceptsInput = true
        val ev = eventHandlers ?: HashMap(8)
        val ls = ev.getOrPut(event) { LinkedList() }
        ls.add(handler as Drawable.(Event) -> Boolean)
        eventHandlers = ev
        return this
    }

    /**
     * Forward the events that this drawable receives to the given drawable.
     *
     * This action cannot be undone.
     * @since 1.0.6
     */
    @ApiStatus.Experimental
    fun <S : Drawable> S.forwardEventsTo(to: Drawable): S {
        this.eventHandlers?.forEach { (event, handlers) ->
            to.addEventHandler(event) {
                var ran = false
                handlers.fastEach {
                    if (it.invoke(this, event)) ran = true
                }
                ran
            }
        }
        return this
    }
}
