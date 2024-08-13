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
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.component.extensions.fadeIn
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.operations.ComponentOp
import org.polyfrost.polyui.operations.Fade
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.utils.annotations.Locking
import org.polyfrost.polyui.utils.annotations.SideEffects
import org.polyfrost.polyui.utils.cl1
import org.polyfrost.polyui.utils.fastAny
import org.polyfrost.polyui.utils.fastEach
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * # Component
 *
 * Component is the base class of everything on the screen in PolyUI.
 *
 * It itself contains no actual draw methods.
 *
 * @since 1.6.0
 */
abstract class Component(at: Vec2, size: Vec2, val alignment: Align = AlignDefault) {
    /**
     * This is the name of this component, and you can use it to get components from a layout by ID, e.g:
     *
     * `val text = myLayout["Text@4cf777e8"] as Text`
     */
    @OptIn(ExperimentalStdlibApi::class)
    open var name = "${this::class.java.simpleName}@${this.hashCode().toHexString()}"

    lateinit var polyUI: PolyUI
        protected set

    val initialized get() = ::polyUI.isInitialized

    var children: ArrayList<Component>? = null
        protected set

    @ApiStatus.Internal
    @get:JvmName("getParentOrNull")
    var _parent: Component? = null

    @SideEffects("_parent")
    var parent: Component
        get() = _parent ?: error("cannot move outside of component tree")
        set(value) {
            _parent = value
        }

    private var _x = at.x
    private var _y = at.y

    @SideEffects("x", "_x", "atValid", "this.children::x", `when` = "value != x")
    var x: Float
        get() = _x
        set(value) {
            if (value == _x) return
            val d = value - _x
            _x = value
            children?.fastEach {
                it.x += d
            }
        }

    @SideEffects("y", "_y", "atValid", "this.children::y", `when` = "value != y")
    var y: Float
        get() = _y
        set(value) {
            if (value == _y) return
            val d = value - _y
            _y = value
            children?.fastEach {
                it.y += d
            }
        }

    @get:JvmName("getAt")
    @set:JvmName("setAt")
    var at: Vec2
        get() = Vec2(x, y)
        set(value) {
            x = value.x
            y = value.y
        }

    /**
     * Return the position of this component on the screen. This is used if scrolling or a similar mechanic means that its screen position is not the same as [x] and [y].
     * @since 1.6.0
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getScreenAt")
    open val screenAt: Vec2 get() = at

    /**
     * Set the position of this component programmatically. use this if you don't want to hardcode [x] or [y] (for example in mirroring or flipping).
     * @since 1.1.0
     */
    fun at(index: Int, value: Float) {
        when (index) {
            0 -> x = value
            1 -> y = value
            else -> throw IndexOutOfBoundsException("Index: $index")
        }
    }

    open var width = size.x
    open var height = size.y

    @get:JvmName("getSize")
    @set:JvmName("setSize")
    var size: Vec2
        get() = Vec2(width, height)
        set(value) {
            width = value.x
            height = value.y
        }

    /**
     * returns `true` if the size of this component is valid (not zero)
     * @since 1.1.0
     */
    @get:JvmName("isSizeValid")
    val sizeValid get() = width > 0f && height > 0f

    /**
     * Use this function to calculate the size of this component. DO NOT include children in this calculation!
     *
     * This function is only called if no size is supplied to this component at initialization.
     * A return value of `null` indicates this component is not able to infer its own size, as it is for example, a Block with no reference.
     * A component such as Text on the other hand, is able to infer its own size.
     *
     * This function is only ever called if [sizeValid] is false.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("calculateSize")
    open fun calculateSize(): Vec2 = Vec2.ZERO

    /**
     * Set the size of this component programmatically. use this if you don't want to hardcode [width] or [height] (for example in mirroring or flipping).
     * @since 1.5.0
     */
    fun size(index: Int, value: Float) {
        when (index) {
            0 -> width = value
            1 -> height = value
            else -> throw IndexOutOfBoundsException("Index: $index")
        }
    }

    /**
     * The visible size of this component. This is used for clipping and scrolling.
     *
     * Unless set, this will return **the same as** [size]. This may have unintended side effects, depending on what you are doing.
     * @since 1.1.0
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("getVisibleSize")
    open val visibleSize get() = size

    open fun fixVisibleSize() {
//        val vs = visibleSize
//        if (size == vs) return
//        width = width.coerceAtLeast(vs.x)
//        height = height.coerceAtLeast(vs.y)
    }

    private var _padding: Vec4? = null

    /**
     * Padding for this component. This is used by the positioner to ensure that
     * there is adequate space around each object in the UI.
     *
     * The positioner will use this value if it set, and it will add a level of padding directly to this
     * component. **it is used in conjunction with the [Align.pad] property.**
     *
     * @since 1.4
     */
    var padding: Vec4
        get() = _padding ?: Vec4.ZERO
        set(value) {
            _padding = value
        }

    /**
     * Flags that store various states of this component regarding the [org.polyfrost.polyui.layout.LayoutController] and resizing.
     * Currently, the following is stored here:
     * - bit 0: [rawResize]
     * - bit 1: [createdWithSetSize]
     * - bit 2: [createdWithSetPosition]
     * - bit 3: [layoutIgnored]
     *
     * this means there are still 4 bits that can be used for simple boolean-type properties.
     * this is protected so you can use it yourself, but more bits may be used internally in the future.
     * In order to maintain forward compatability, you should use the highest indexes for your flags.
     * @since 1.4.4
     */
    @ApiStatus.Internal
    var layoutFlags: Byte = ((if (!sizeValid) 0b00000000 else 0b00000010) or (if (x == 0f && y == 0f) 0b00000000 else 0b00000100)).toByte()

    /**
     * This property controls weather this component resizes "raw", meaning it will **not** respect the aspect ratio.
     *
     * By default, it is `false`, so when resized, the smallest increase is used for both width and height. This means that it will stay at the same aspect ratio.
     *
     * @since 0.19.0
     */
    @get:JvmName("isRawResize")
    var rawResize: Boolean
        get() = layoutFlags.toInt() and 0b00000001 == 0b00000001
        set(value) {
            layoutFlags = if (value) layoutFlags or 0b00000001 else layoutFlags and 0b11111110.toByte()
        }

    /**
     * If true, this component will be ignored during the [org.polyfrost.polyui.layout.LayoutController]'s routine of placing components. Use this if
     * you want to specify it manually based on something else, for example.
     *
     * *note: this used to be controlled by the [renders] flag pre 1.4.4*.
     *
     * @since 1.4.4
     */
    @get:JvmName("isLayoutIgnored")
    var layoutIgnored: Boolean
        get() = layoutFlags.toInt() and 0b00001000 == 0b00001000
        set(value) {
            layoutFlags = if (value) layoutFlags or 0b00001000 else layoutFlags and 0b11110111.toByte()
        }

    /**
     * Returns `true` if this component had a position specified at creation time.
     * @since 1.4.4
     */
    @get:JvmName("createdWithSetPosition")
    val createdWithSetPosition get() = layoutFlags.toInt() and 0b00000100 == 0b00000100

    /**
     * Returns `true if this component had a size specified at creation time.
     * @since 1.4.4
     */
    @get:JvmName("createdWithSetSize")
    val createdWithSetSize get() = layoutFlags.toInt() and 0b00000010 == 0b00000010

    /**
     * Returns `true` if this component has been initialized to the point where it has been positioned,
     * e.g. [Event.Lifetime.Init] has been dispatched, and [Event.Lifetime.PostInit] is about to be.
     *
     * This can be used to distinguish (as a [org.polyfrost.polyui.layout.LayoutController]) if this is [setup] or [recalculate].
     * @since 1.5.03
     */
    @get:JvmName("isPositioned")
    val positioned get() = layoutFlags.toInt() and 0b00010000 == 0b00010000


    /**
     * Whether this component should be rendered.
     *
     * This is controlled by [clipChildren] to save resources by not drawing components which cannot be seen.
     *
     * @since 0.21.4
     */
    open var renders = true
        set(value) {
            if (field == value) return
            field = value
        }

    /**
     * Disabled flag for this component. Dispatches the [Event.Lifetime.Disabled] and [Event.Lifetime.Enabled] events.
     *
     * @since 0.21.4
     */
    open var isEnabled = true


    var operations: ArrayList<ComponentOp>? = null
        private set

    /**
     * `true` if this drawable has any operations.
     * @since 1.0.3
     */
    val isOperating get() = !operations.isNullOrEmpty()

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
        children?.fastEach { it.setup(polyUI) }
        position()
        return true
    }


    /**
     * Method that is called when the physical size of the total window area changes.
     */
    @Locking
    fun rescale(scaleX: Float, scaleY: Float) {
        if (rawResize) {
            rescale0(scaleX, scaleY, true)
        } else {
            val s = cl1(scaleX, scaleY)
            rescale0(s, s, true)
        }
    }

    /**
     * raw rescale method that **does not check [rawResize] and uses the given scale factors directly**.
     *
     * ### You should be using [rescale] instead in 99% of cases.
     * @param withChildren if `true`, this method will also rescale its children.
     * *note: it will also rescale its children even if `withChildren` is `false` if the child has no children itself.*
     */
    @ApiStatus.Internal
    @Locking
    @MustBeInvokedByOverriders
    @Synchronized
    open fun rescale0(scaleX: Float, scaleY: Float, withChildren: Boolean) {
        _x *= scaleX
        _y *= scaleY
        width *= scaleX
        height *= scaleY
        children?.fastEach {
            if (withChildren || it.children.isNullOrEmpty()) it.rescale0(scaleX, scaleY, true)
        }
    }

    fun clipChildren() {
        val children = children ?: return
        val static = screenAt
        val vs = visibleSize
        _clipChildren(children, static.x, static.y, vs.x, vs.y)
    }

    private fun _clipChildren(children: ArrayList<out Component>, tx: Float, ty: Float, tw: Float, th: Float) {
        children.fastEach {
            val p = it.intersects(tx, ty, tw, th)
            it.renders = p
            // asm: don't bother checking children if this component is not visible
            if (p) it._clipChildren(it.children ?: return@fastEach, tx, ty, tw, th)
            else it._clipChildrenN(it.children ?: return@fastEach)
        }
    }

    private fun _clipChildrenN(children: ArrayList<out Component>) {
        children.fastEach {
            it.renders = false
            it._clipChildrenN(it.children ?: return@fastEach)
        }
    }

    /**
     * @return `true` if the given point is inside this component.
     * @see intersects
     * @see isInside(Float, Float, Float, Float)
     */
    fun isInside(x: Float, y: Float): Boolean {
        val ta = screenAt
        val tx = ta.x
        val ty = ta.y
        val vs = this.visibleSize
        val tw = vs.x
        val th = vs.y
        return x in tx..tx + tw && y in ty..ty + th
    }

    @JvmName("contains")
    operator fun contains(point: Vec2) = isInside(point.x, point.y)

    operator fun contains(box: Vec4) = intersects(box.x, box.y, box.w, box.h)

    /**
     * @return `true` if the component has at least one point inside the given box (x, y, width, height).
     * @since 0.21.4
     */
    fun intersects(x: Float, y: Float, width: Float, height: Float): Boolean {
        val ta = screenAt
        val tx = ta.x
        val ty = ta.y
        val vs = this.visibleSize
        val tw = vs.x
        val th = vs.y
        return (x < tx + tw && tx < x + width) && (y < ty + th && ty < y + height)
    }

    /**
     * Reposition all the children of this component.
     *
     * Note that in most circumstances, [recalculate] is the method that you actually want.
     *
     * @see recalculate
     * @since 1.0.2
     */
    @Locking(`when` = "this.children != null")
    @Synchronized
    open fun position() {
        polyUI.layoutController.layout(this)
        layoutFlags = layoutFlags or 0b00010000
        clipChildren()
    }

    /**
     * Fully recalculate this component's size, and any of its children's positions.
     *
     * @since 1.0.7
     */
    @Locking(`when` = "this.children != null")
    @Synchronized
    open fun recalculate() {
        if (children == null) return
        val oldW = width
        val oldH = height
        if (!createdWithSetSize) {
            width = 0f
            height = 0f
        }
        position()
        if (!createdWithSetSize) {
            x -= (width - oldW) / 2f
            y -= (height - oldH) / 2f
        }
    }

    open operator fun get(index: Int) = children?.get(index) ?: throw IndexOutOfBoundsException("index: $index, length: ${children?.size ?: 0}")

    operator fun get(id: String): Component {
        val children = children ?: throw NoSuchElementException("no children on $this")
        children.fastEach {
            if (it.name == id) return it
        }
        throw NoSuchElementException("no child with id $id")
    }

    operator fun get(x: Float, y: Float) = children?.first { it.isInside(x, y) } ?: throw NoSuchElementException("no children on $this")

    @Locking
    operator fun set(old: Int, new: Component) = set(this[old], new)


    /**
     * the runtime equivalent of adding children to this component during the constructor.
     */
    @Locking
    @Synchronized
    fun addChild(child: Component, recalculate: Boolean = true) {
        if (children == null) children = ArrayList()
        val children = this.children ?: throw ConcurrentModificationException("well, this sucks")
        child.parent = this
        child.isEnabled = true
        if (children.fastAny { it === child }) throw IllegalStateException("attempted to add the same component twice")
        children.add(child)
        if (initialized) {
            child.setup(polyUI)
            if (!child.createdWithSetPosition) {
                if (recalculate) recalculate()
                else {
                    child.x += x
                    child.y += y
                }
            }
            if (child is Inputtable) child.accept(Event.Lifetime.Added)
        }
    }

    @Locking(`when` = "children.size != 0")
    fun addChild(vararg children: Component) {
        for (child in children) addChild(child)
    }

    @Locking
    fun removeChild(child: Component, recalculate: Boolean = true) {
        val i = children?.indexOf(child) ?: throw NoSuchElementException("no children on $this")
        require(i != -1) { "component $child is not a child of $this" }
        removeChild(i, recalculate)
    }

    @Locking
    @Synchronized
    fun removeChild(index: Int, recalculate: Boolean = true) {
        val children = this.children ?: throw NoSuchElementException("no children on $this")
        val it = children.getOrNull(index) ?: throw IndexOutOfBoundsException("index: $index, length: ${children.size}")
        it._parent = null
        children.remove(it)
        it.isEnabled = false
        if (initialized) {
            if (it is Inputtable) {
                polyUI.inputManager.drop(it)
                it.accept(Event.Lifetime.Removed)
            }
            if (recalculate) recalculate()
            if (this is Scrollable) {
                accept(Event.Mouse.Scrolled)
            }
        }
    }

    /**
     * Remove this component from its parent.
     * @since 1.6.1
     */
    fun remove(recalculate: Boolean = true) {
        val parent = this._parent ?: throw IllegalStateException("No parent to remove from")
        parent.removeChild(parent.children?.indexOf(this) ?: throw ConcurrentModificationException(), recalculate)
    }

    /**
     * Replace a child of this component with another component.
     *
     * This will add a nice, smooth [Fade] animation to the transition.
     * @since 1.0.1
     */
    @Locking
    @Synchronized
    operator fun set(old: Component, new: Component?) {
        require(initialized) { "$this must be setup at this point!" }
        val children = children ?: throw NoSuchElementException("no children on $this")
        val index = children.indexOf(old)
        require(index != -1) { "component $old is not a child of $this" }
        if (new == null) {
            removeChild(old)
            return
        }
        new._parent = this
        new.setup(polyUI)
        val oldStatic = old.screenAt
        new.x = old.x - (old.x - oldStatic.x)
        new.y = old.y - (old.y - oldStatic.y)
        // don't want to open a madly scrolled page
        if (new is Scrollable) new.resetScroll()

        polyUI.inputManager.drop(this as? Inputtable)
        if (old is Drawable) {
            addOperation(Fade(old, 0f, false, Animations.Default.create(0.3.seconds)) {
                isEnabled = false
                children.remove(this)
                _parent = null
            })
        } else {
            children.remove(old)
            old._parent = null
            old.isEnabled = false
        }
        children.add(index, new)
        if (new is Drawable) {
            new.alpha = 0f
            new.fadeIn(0.3.seconds)
        }
        if (this is Scrollable) this.tryMakeScrolling()
    }

    /**
     * Override this function to add things to the debug display of this component.
     * @since 1.1.1
     */
    open fun debugString(): String? = null

    /**
     * Add a [ComponentOp] to this component.
     * @return `true` if the operation was added, `false` if it was replaced.
     */
    @Locking(`when` = "operation.verify() == true")
    fun addOperation(operation: ComponentOp): Boolean {
        if (!operation.verify()) {
//            if (polyUI.settings.debug) PolyUI.LOGGER.warn("Dodged invalid op $drawableOp on ${this.simpleName}")
            return false
        }
        synchronized(this) {
            val operations = this.operations ?: ArrayList<ComponentOp>(3).also { operations = it; it.add(operation); return true }
            val i = operations.indexOf(operation)
            return if (i == -1) {
                if (this is Drawable) this.needsRedraw = true
                operations.add(operation)
                true
            } else if (operation.exclusive()) true
            else {
                if (this is Drawable) this.needsRedraw = true
                operations[i] = operation
                false
            }
        }
    }

    @Locking
    fun addOperation(vararg operations: ComponentOp) {
        for (op in operations) addOperation(op)
    }

    /**
     * Remove an operation from this drawable.
     */
    @Locking(`when` = "operations != null")
    @Synchronized
    fun removeOperation(componentOp: ComponentOp) {
        this.operations?.apply {
            if (size == 1) operations = null
            else remove(componentOp)
        }
    }
}
