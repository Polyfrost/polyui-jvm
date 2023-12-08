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
import org.jetbrains.annotations.NotNull
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INPUT_DISABLED
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.animate.Animation
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
    at: Vec2? = null,
    var alignment: Align = AlignDefault,
    size: Vec2? = null,
    visibleSize: Vec2? = null,
    palette: Colors.Palette? = null,
    val focusable: Boolean = false,
    vararg children: Drawable? = arrayOf(),
) : Cloneable {
    @set:Locking
    var size = size ?: Vec2()
        set(value) {
            field = value
            (visibleSize as? Vec2.Sourced)?.source = value
        }

    @set:Locking
    var visibleSize: Vec2 = visibleSize ?: Vec2.Based(base = this.size)

    @set:Locking
    var parent: Drawable? = null
        private set(value) {
            if (value === field) return
            if (field != null) {
                PolyUI.LOGGER.info("transferring ownership of ${this.simpleName} from ${field!!.simpleName} to ${value?.simpleName}")
            }
            field = value
        }

    @set:Locking
    var at = at ?: Vec2.Relative()

    /**
     * This property controls weather this drawable resizes "raw", meaning it will **not** respect the aspect ratio.
     *
     * By default, it is `false`, so when resized, the smallest increase is used for both width and height. This means that it will stay at the same aspect ratio.
     *
     * @since 0.19.0
     */
    var rawResize = false

    @Transient
    private var eventHandlers: HashMap<Event, LinkedList<(Drawable.(Event) -> Boolean)>>? = null

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
            this.children = children.filterNotNull().asLinkedList().also { list ->
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

    @set:Locking
    var framebuffer: Framebuffer? = null

    /**
     * Setting of this value could have undesirable results, and the [PolyColor.Animated.recolor] method should be used instead.
     */
    @set:ApiStatus.Internal
    @set:Locking
    lateinit var color: PolyColor.Animated

    @Transient
    @set:Locking
    var palette: Colors.Palette? = palette
        set(@NotNull value) {
            if (value == null) return
            field = value
            if (!::color.isInitialized) return
            color.recolor(value.normal)
        }

    // InlineOnly makes it so that it doesn't add useless local variables called $i$f$getX or whatever
    @kotlin.internal.InlineOnly
    @set:Locking
    inline var x: Float
        get() = at.x
        set(value) {
            at.x = value
        }

    @kotlin.internal.InlineOnly
    @set:Locking
    inline var y: Float
        get() = at.y
        set(value) {
            at.y = value
        }

    @kotlin.internal.InlineOnly
    @set:Locking
    inline var width: Float
        get() = size.x
        set(value) {
            size.x = value
        }

    @kotlin.internal.InlineOnly
    @set:Locking
    inline var height: Float
        get() = size.y
        set(value) {
            size.y = value
        }

    @kotlin.internal.InlineOnly
    @set:Locking
    inline var visibleWidth: Float
        get() = visibleSize.x
        set(value) {
            visibleSize.x = value
        }

    @kotlin.internal.InlineOnly
    @set:Locking
    inline var visibleHeight: Float
        get() = visibleSize.y
        set(value) {
            visibleSize.y = value
        }

    @Transient
    @set:Locking
    protected var xScroll: Animation? = null

    @Transient
    @set:Locking
    protected var yScroll: Animation? = null

    var needsRedraw = true
        set(value) {
            if (field == value) return
            if (value) {
                parent?.needsRedraw = true
            }
            field = value
        }

    protected open val shouldScroll get() = true

    @Transient
    var acceptsInput = false
        get() = field && enabled

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
     * @since 2.0.0
     */
    @Transient
    var inputState = INPUT_NONE
        set(value) {
            if (field == value) return

            if (value == INPUT_DISABLED) {
                accept(Event.Lifetime.Disabled)
                field = value
                return
            }
            if (field == INPUT_DISABLED) {
                accept(Event.Lifetime.Enabled)
            }

            if (INPUT_NONE in field..<value) {
                accept(Event.Mouse.Entered)
            }
            if (INPUT_NONE in value..<field) {
                accept(Event.Mouse.Exited)
            }
            field = value
        }

    /**
     * weather or not this drawable should be rendered.
     *
     * This is controlled by [clipDrawables] to save resources by not drawing drawables which cannot be seen.
     *
     * if you are calling [render] yourself (which you really shouldn't), you should check this value.
     * @see draw
     * @since 0.21.4
     */
    @Transient
    @set:Locking
    open var renders = true
        get() = field && enabled

    /**
     * If this is `true`, the drawable will be the only drawable that is allowed to be hovered at once.
     *
     * There are essentially two modes for hovering in PolyUI, and by default, components are in the "normal" (`true`) mode, where only one of them can be hovered as once
     * as would be expected, but layouts are in the "group" (`false`) mode, so will still receive the events even if another drawable is hovered, for example scrolling.
     * @since 0.21.3
     */
    open val consumesHover get() = true

    /**
     * Disabled flag for this component. Dispatches the [Event.Lifetime.Disabled] and [Event.Lifetime.Enabled] events.
     *
     * @since 0.21.4
     */
    @kotlin.internal.InlineOnly
    @set:Locking
    inline var enabled
        get() = inputState > INPUT_DISABLED
        set(value) {
            inputState = if (value) INPUT_NONE else INPUT_DISABLED
        }

    @Transient
    protected var operations: LinkedList<DrawableOp>? = null
        private set

    /** current rotation of this drawable (radians). */
    @set:Locking
    var rotation: Double = 0.0

    /** current skew in x dimension of this drawable (radians). */
    var skewX: Double = 0.0

    /** current skew in y dimension of this drawable (radians). */
    var skewY: Double = 0.0

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
    fun draw() {
        if (!renders) return
        needsRedraw = false
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

        var ran = false
        xScroll?.let {
            x = it.update(polyUI.delta)
            ran = true
        }
        yScroll?.let {
            y = it.update(polyUI.delta)
            ran = true
        }
        if (ran) renderer.pushScissor(xScroll?.from ?: x, yScroll?.from ?: y, visibleSize.x, visibleSize.y)

        if (rotation != 0.0) {
            renderer.translate(x + width / 2f, y + height / 2f)
            renderer.rotate(rotation)
            renderer.translate(-(width / 2f), -(height / 2f))
            acx = x
            acy = y
            x = 0f
            y = 0f
        }
        if (alpha != 1f) renderer.globalAlpha(alpha)
        if (skewX != 0.0) renderer.skewX(skewX)
        if (skewY != 0.0) renderer.skewY(skewY)
        if (scaleX != 1f || scaleY != 1f) renderer.scale(scaleX, scaleY)
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
        if (rotation != 0.0) {
            x = acx
            y = acy
        }
    }

    /**
     * Method that is called when the physical size of the total window area changes.
     */
    @Locking
    open fun rescale(scaleX: Float, scaleY: Float) {
        if (rawResize) {
            at.scale(scaleX, scaleY)
            size.scale(scaleX, scaleY)
            visibleSize.scale(scaleX, scaleY)
        } else {
            val scale = cl1(scaleX, scaleY)
            at *= scale
            size *= scale
            visibleSize *= scale
        }
        children?.fastEach { it.rescale(scaleX, scaleY) }
    }

    fun clipDrawables() {
        children?.fastEach {
            it.renders = it.intersects(xScroll?.from ?: x, yScroll?.from ?: y, visibleSize.x, visibleSize.y)
        }
        needsRedraw = true
    }

    /** function that should return true if it is ready to be removed from its parent.
     *
     * This is used for drawables that need to wait for an animation to finish before being removed.
     */
    open fun canBeRemoved(): Boolean = operations.isEmpty() && (children?.allAre { it.canBeRemoved() } ?: true)

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
            if (size.x - visibleSize.x > 0f) {
                xScroll?.let {
                    it.durationNanos = 0.6.seconds
                    it.to += event.amountX
                    it.to = it.to.coerceIn(it.from - (size.x - visibleSize.x), it.from)
                }
                ran = true
            }
            if (size.y - visibleSize.y > 0f) {
                yScroll?.let {
                    it.durationNanos = 0.6.seconds
                    it.to += event.amountY
                    it.to = it.to.coerceIn(it.from - (size.y - visibleSize.y), it.from)
                }
                ran = true
            }
            if (ran) {
                polyUI.eventManager.recalculateMousePos()
                clipDrawables()
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
    open fun setup(polyUI: PolyUI) {
//        require(!initialized) { "${this.simpleName} has already been setup!" }
        this.polyUI = polyUI
        if (palette == null) palette = polyUI.colors.component.bg
        if (!::color.isInitialized) this.color = palette!!.normal.toAnimatable()
        children?.fastEach {
            it.setup(polyUI)
        }
        // asm: don't use accept as we don't want to dispatch to children
        eventHandlers?.remove(Event.Lifetime.Init)?.let {
            it.fastEach { handler ->
                handler.invoke(this, Event.Lifetime.Init)
            }
            // help gc
            it.clear()
        }
        polyUI.positioner.position(this)
        children?.fastEach {
            var scrolling = false
            if (it.shouldScroll) {
                if (it.size.x > it.visibleSize.x && it.xScroll == null) {
                    scrolling = true
                    it.xScroll = Easing.Expo(Easing.Type.Out, 0L, it.at.x, it.at.x)
                }
                if (it.size.y > it.visibleSize.y && it.yScroll == null) {
                    scrolling = true
                    it.yScroll = Easing.Expo(Easing.Type.Out, 0L, it.at.y, it.at.y)
                }
                if (scrolling) {
                    it.acceptsInput = true
                    if (polyUI.settings.debug) PolyUI.LOGGER.info("Enabled scrolling for ${it.simpleName}")
                }
            }
        }

//        if (polyUI.settings.debug) {
//            children?.fastEach {
//                require(!it.size.isZero) { "No size property after init on ${it.simpleName} (child of $simpleName)" }
//            }
//        }

        initialized = true
        eventHandlers?.remove(Event.Lifetime.PostInit)?.let {
            it.fastEach { handler ->
                handler.invoke(this, Event.Lifetime.PostInit)
            }
            // help gc
            it.clear()
        }
        // following all init events being removed, if it is empty, we can set it to null
        if (eventHandlers.isNullOrEmpty()) eventHandlers = null
        clipDrawables()
    }

    /** debug print for this drawable.*/
    open fun debugPrint() {
        // noop
    }

    @ApiStatus.Experimental
    fun reset() {
        this.x = 0f
        this.y = 0f
        this.initialized = false
        children?.fastEach { it.reset() }
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
        return (x < tx + this.visibleSize.x && tx < x + width) && (y < ty + this.visibleSize.y && ty < y + height)
    }

    /**
     * Implement this function to enable cloning of your Drawable.
     *
     * If this function is not implemented, attempts to clone the drawable will not compile due to type erasure, but if this is ignored a [CloneNotSupportedException] will be thrown.
     *
     * @since 0.19.0
     */
    public override fun clone(): Drawable = throw CloneNotSupportedException("Cloning is not supported for ${this.simpleName}!")

    override fun toString() = "$simpleName($at, $size)"

    /** Add a [DrawableOp] to this drawable. */
    @Locking
    fun addOperation(drawableOp: DrawableOp) {
        if (operations == null) operations = LinkedList()
        this.operations?.add(drawableOp)
        needsRedraw = true
    }

    @Locking
    fun addOperation(vararg drawableOps: DrawableOp) {
        if (operations == null) operations = LinkedList()
        operations?.addAll(drawableOps)
        needsRedraw = true
    }

    /**
     * Remove an operation from this drawable.
     */
    @Locking
    fun removeOperation(drawableOp: DrawableOp) {
        this.operations?.remove(drawableOp)
        needsRedraw = true
    }

    @Locking
    fun addChild(child: Drawable) {
        child.accept(Event.Lifetime.Added)
        val children = this.children ?: LinkedList()
        children.add(child)
        needsRedraw = true
    }

    @Locking
    fun addChild(vararg children: Drawable) {
        for (child in children) addChild(child)
    }

    @Locking
    fun removeChild(child: Drawable) {
        removeChild(this.children?.indexOf(child) ?: return)
        needsRedraw = true
    }

    @Locking
    fun removeChild(index: Int) {
        val children = this.children ?: return
        children[index].accept(Event.Lifetime.Removed)
        polyUI.forRemoval(children, index)
        needsRedraw = true
    }

    operator fun get(index: Int) = children?.get(index)

    operator fun get(id: String): Drawable? {
        val children = children ?: return null
        children.fastEach {
            if (it.simpleName == id) return it
        }
        return null
    }

    operator fun get(x: Float, y: Float) = children?.firstOrNull { it.isInside(x, y) }

    fun countChildren(): Int {
        var i = children?.size ?: 0
        children?.fastEach {
            i += it.countChildren()
        }
        return i
    }

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
}
