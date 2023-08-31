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

package org.polyfrost.polyui.layout

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import org.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import org.polyfrost.polyui.PolyUI.Companion.INIT_SETUP
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.DrawableOp
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.component.impl.Scrollbar
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.property.PropertyManager
import org.polyfrost.polyui.property.impl.BlockProperties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.renderer.data.Framebuffer
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.*

/**
 * # Layout
 * Layout is PolyUI's take on containers. They can contain [components] and other [layouts][children], as children.
 *
 * They can dynamically [add] and [remove] their children and components.
 *
 * They are responsible for all their children's sizes, positions and rendering.
 *
 * @see org.polyfrost.polyui.layout.impl.FlexLayout
 * @see org.polyfrost.polyui.layout.impl.PixelLayout
 * @see org.polyfrost.polyui.layout.impl.SwitchingLayout
 */
abstract class Layout(
    at: Point<Unit>,
    override var size: Size<Unit>? = null,
    @Transient internal val onAdded: (Drawable.(Added) -> kotlin.Unit)? = null,
    @Transient internal val onRemoved: (Drawable.(Removed) -> kotlin.Unit)? = null,
    propertyManager: PropertyManager? = null,
    rawResize: Boolean = false,
    /**
     * If this layout resizes its children (**components and layouts!**) when it is resized.
     * @since 0.19.0
     */
    val resizesChildren: Boolean = true,
    /** If this layout can receive events (separate to its children!). */
    acceptInput: Boolean = false,
    vararg drawables: Drawable,
) : Drawable(at, rawResize, acceptInput) {
    @Transient
    open lateinit var propertyManager: PropertyManager

    init {
        if (propertyManager != null) {
            @Suppress("LeakingThis")
            this.propertyManager = propertyManager
        }
    }

    @Transient
    lateinit var colors: Colors
        internal set

    @Transient
    lateinit var fonts: FontFamily
        internal set

    /** list of components in this layout. */
    val components = drawables.filterIsInstance<Component>() as ArrayList
        get() = if (exists) field else EMPTY_CMPLIST

    /** list of child layouts in this layout */
    val children = drawables.filterIsInstance<Layout>() as ArrayList
        get() = if (exists) field else EMPTY_CHLDLIST

    override var consumesHover: Boolean = false

    /**
     * Weather this layout needs redrawing.
     */
    @Transient
    open var needsRedraw = true
        set(value) {
            if (value && !field) {
                layout?.needsRedraw = true
            }
            field = value
        }

    override fun trueX(): Float {
        children.fastEach { it.trueX = it.trueX() }
        components.fastEach { it.trueX = it.trueX() }
        return super.trueX()
    }

    override fun trueY(): Float {
        children.fastEach { it.trueY = it.trueY() }
        components.fastEach { it.trueY = it.trueY() }
        return super.trueY()
    }

    /** tracker variable for framebuffer disabling/enabling. don't touch this. */
    @Transient
    @ApiStatus.Internal
    protected var fboTracker = 0

    /** removal queue of drawables.
     * @see remove
     */
    @Transient
    protected val removeQueue = arrayListOf<Drawable>()

    /** set this to true if you want this layout to never use a framebuffer. Recommended in situations with chroma colors */
    open var refuseFramebuffer: Boolean = false

    /** framebuffer attached to this layout.
     * @see refuseFramebuffer
     * @see org.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer
     */
    internal var fbo: Framebuffer? = null
        set(value) {
            if (!refuseFramebuffer) field = value
        }

    /** reference to parent */
    @Transient
    final override var layout: Layout? = null
        set(value) {
            require(value !== this) { "$this cannot be its own parent!" }
            field = value
        }

    /**
     * These hooks are ran when the [onInitComplete] function is called.
     * @since 0.19.2
     */
    @Transient
    val initCompleteHooks = ArrayList<Layout.() -> kotlin.Unit>(5)

    /**
     * Flag that is true if the layout can be dragged around.
     * @see draggable
     * @see background
     * @since 0.19.2
     */
    @Transient
    var draggable = false
        private set

    /**
     * The visible area of this layout. If this is set, it is used for [isInside] checks.
     *
     * If this layout is a [scrolling layout][scrolling], this is the size of the scrollable area.
     * @since 0.19.2
     */
    var visibleSize: Size<Unit>? = null

    /** used by [scrolling] */
    @Transient
    internal var ox = 0f

    /** used by [scrolling] */
    @Transient
    internal var oy = 0f

    init {
        if (onAdded != null) {
            addEventHandler(Added) {
                onAdded!!.invoke(this, it)
                true
            }
        }
        if (onRemoved != null) {
            addEventHandler(Removed) {
                onRemoved!!.invoke(this, it)
                true
            }
        }
    }

    /** this is the function that is called every frame. It decides whether the layout needs to be entirely re-rendered.
     * If so, the [render] function is called which will redraw all its' and components, and this function will redraw all its child layouts as well.
     *
     * If this layout is [using a framebuffer][org.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer], it will be [drawn][rasterize] to the framebuffer, and then drawn to the screen.
     *
     * **Note:** Do not call this function yourself.
     */
    open fun reRenderIfNecessary() {
        require(initStage == INIT_COMPLETE) { "${this.simpleName} was attempted to be rendered before it was fully initialized (stage $initStage)" }
        if (!renders) return
        rasterChildren()
        if (rasterize()) {
            renderer.drawFramebuffer(fbo!!, x, y)
        } else {
            val x = x
            val y = y
            renderer.translate(x, y)
            render()
            renderer.translate(-x, -y)
            if (!needsRedraw && fboTracker > 1) {
                needsRedraw = true
                fboTracker = 0
            }
        }
    }

    /**
     * This function will rasterize the layout to its framebuffer, if it has one.
     *
     * @return `true` if this framebuffer drawing is enabled (`fbo != null && fboTracker < 2`), `false` otherwise.
     * @since 0.18.0
     */
    protected open fun rasterize(): Boolean {
        if (fbo != null && fboTracker < 2) {
            if (needsRedraw) {
                fboTracker++
                renderer.bindFramebuffer(fbo)
                render()
                renderer.unbindFramebuffer(fbo)
            }
            return true
        }
        return false
    }

    /**
     * perform the [reRenderIfNecessary] function on this layout's children.
     * @since 0.18.0
     */
    protected open fun renderChildren() {
        children.fastEach {
            it.reRenderIfNecessary()
        }
    }

    /**
     * perform the [rasterize] function on this layout's children.
     * @since 0.18.0
     */
    protected open fun rasterChildren() {
        children.fastEach {
            it.rasterize()
        }
    }

    /** perform the given [function] on all this layout's components, and [optionally][onChildLayouts] on all child layouts. */
    fun onAll(onChildLayouts: Boolean = false, function: Component.() -> kotlin.Unit) {
        components.fastEach(0, function)
        if (onChildLayouts) children.fastEach { it.onAll(true, function) }
    }

    /**
     * perform the given [function] on all this layout's children and itself.
     * @since 0.18.0
     */
    fun onAllLayouts(reversed: Boolean = false, function: Layout.() -> kotlin.Unit) {
        if (reversed) {
            children.fastEachReversed { it.onAllLayouts(true, function) }
        } else {
            children.fastEach { it.onAllLayouts(false, function) }
        }
        function(this)
    }

    /**
     * return a component in this layout, by its simple name. This can be accessed using [debugPrint][org.polyfrost.polyui.PolyUI.debugPrint] or using the [field][org.polyfrost.polyui.component.Drawable.simpleName].
     *
     * **Note that** the [simpleName] is **NOT** consistent between updates to PolyUI, or changes to the content of the layout.
     * @throws IllegalArgumentException if the component does not exist
     * @see getOrNull
     */
    inline operator fun <reified T : Drawable> get(simpleName: String): T = getOrNull(simpleName) ?: throw IllegalArgumentException("No drawable found in ${this.simpleName} with ID $simpleName!")

    /**
     * Returns the component at the specified index.
     *
     * @param index The index of the component to retrieve.
     * @return The component at the specified index.
     * @throws IndexOutOfBoundsException If the index is out of range (index < 0 || index >= size()).
     * @throws ClassCastException If the component type cannot be cast to the specified type.
     * @since 0.19.2
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> getComponent(index: Int): T = components[index] as T

    /**
     * Retrieves a layout from the children at the specified index.
     *
     * @param index The index of the layout to retrieve.
     * @param T The type of layout to retrieve. Must be a subtype of Layout.
     * @return The requested layout at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range
     * @throws ClassCastException if the layout at the specified index is not of type T.
     * @since 0.19.2
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Layout> getLayout(index: Int): T = children[index] as T

    /**
     * return a component in this layout, by its simple name. This can be accessed using [debugPrint][org.polyfrost.polyui.PolyUI.debugPrint] or using the [field][org.polyfrost.polyui.component.Drawable.simpleName].
     *
     * **Note that** the [simpleName] is **NOT** consistent between updates to PolyUI, or changes to the content of the layout.
     * Returns null if not found.
     * @see get
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Drawable?> getOrNull(simpleName: String): T? { // <T : Drawable?> as it removes useless null checks (null cannot be cast to not-null type T)
        components.fastEach { if (it.simpleName == simpleName) return it as T }
        children.fastEach {
            return if (it.simpleName == simpleName) {
                it as T
            } else {
                it.getOrNull(simpleName)
            }
        }
        return null
    }

    /**
     * adds the given components/layouts to this layout.
     *
     * this will add the drawables to the [Layout.components] or [children] list, and invoke its added event function.
     */
    fun add(drawables: Collection<Drawable>) {
        drawables.forEach { add(it) }
    }

    /**
     * adds the given components/layouts to this layout.
     *
     * this will add the drawables to the [Layout.components] or [children] list, and invoke its added event function.
     */
    fun add(vararg components: Drawable) {
        components.forEach { add(it) }
    }

    /**
     * adds the given component/layout to this layout.
     *
     * this will add the drawable to the [Layout.components] or [children] list, and invoke its [Added] if it is present.
     */
    open fun add(drawable: Drawable, index: Int = -1) {
        when (drawable) {
            is Component -> {
                if (components.addOrReplace(drawable, index) != null) PolyUI.LOGGER.warn("${drawable.simpleName} was attempted to be added to layout ${this.simpleName} multiple times!")
                drawable.layout = this
            }

            is Layout -> {
                if (children.addOrReplace(drawable, index) != null) PolyUI.LOGGER.warn("${drawable.simpleName} was attempted to be added to layout ${this.simpleName} multiple times!")
                drawable.layout = this
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        if (initStage > INIT_NOT_STARTED && drawable.initStage == INIT_NOT_STARTED) drawable.setup(renderer, polyUI)
        if (initStage == INIT_COMPLETE) {
            drawable.calculateBounds()
            drawable.onParentInitComplete()
            drawable.accept(Added)
        }
        needsRedraw = true
    }

    /**
     * removes a component from this layout.
     *
     * this will add the component to the removal queue, and invoke its removal event function.
     *
     * This removal queue is used so that component can finish up any animations they're doing before being removed.
     */
    open fun remove(drawable: Drawable) {
        if (polyUI.settings.debug) PolyUI.LOGGER.info("Preparing to removing drawable ${drawable.simpleName} from layout ${this.simpleName}")
        when (drawable) {
            is Component -> {
                val idx = components.indexOf(drawable)
                require(idx != -1) { "Tried to remove component $drawable from $this, but it wasn't found!" }
                removeQueue.add(components[idx])
            }

            is Layout -> {
                val idx = children.indexOf(drawable)
                require(idx != -1) { "Tried to remove layout $drawable from $this, but it wasn't found!" }
                removeQueue.add(children[idx])
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        drawable.accept(Removed)
    }

    /**
     * Removes a component from this layout at the specified index.
     *
     * @param index The index of the component to be removed.
     * @see remove
     * @since 0.19.2
     */
    open fun remove(index: Int) {
        remove(components[index])
    }

    /**
     * Removes a component from this layout immediately.
     *
     * @param index The index of the component to be removed.
     * @see removeNow
     * @since 0.19.2
     */
    open fun removeNow(index: Int) {
        removeNow(components[index])
    }

    /** removes a component immediately, without waiting for it to finish up.
     *
     * This is marked as internal because you should be using [remove] for most cases to remove a component, as it waits for it to finish and play any removal animations.
     */
    @ApiStatus.Internal
    open fun removeNow(drawable: Drawable?) {
        if (drawable == null) return
        if (polyUI.settings.debug) PolyUI.LOGGER.info("Removing drawable ${drawable.simpleName} from layout ${this.simpleName}")
        when (drawable) {
            is Component -> {
                if (!components.remove(drawable)) {
                    PolyUI.LOGGER.warn(
                        "Tried to remove component {} from {}, but it wasn't found!",
                        drawable,
                        this,
                    )
                }
            }

            is Layout -> {
                if (!children.remove(drawable)) {
                    PolyUI.LOGGER.warn(
                        "Tried to remove layout {} from {}, but it wasn't found!",
                        drawable,
                        this,
                    )
                }
                drawable.layout = null
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        needsRedraw = true
    }

    override fun calculateBounds() {
        require(initStage != INIT_NOT_STARTED) { "${this.simpleName} has not been setup, but calculateBounds() was called!" }
        if (layout != null) doDynamicSize()
        components.fastEach {
            it.calculateBounds()
        }
        children.fastEach {
            it.calculateBounds()
        }
        if (this.size == null) {
            this.size = calculateSize()
                ?: throw UnsupportedOperationException("getSize() not implemented for ${this::class.simpleName}!")
        }
        if (initStage != INIT_COMPLETE) {
            initStage = INIT_COMPLETE
            onInitComplete()
        }
        clipDrawables()
    }

    override fun onInitComplete() {
        super.onInitComplete()
        initCompleteHooks.fastEach { it(this) }
        initCompleteHooks.clear()
        initCompleteHooks.trimToSize()
        components.fastEach { it.onParentInitComplete() }
        children.fastEach { it.onParentInitComplete() }
        trueX = trueX()
        trueY = trueY()
        ox = trueX
        oy = trueY
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        // asm: enable temporarily so the children and component fields actually exist
        val wasEnabled = exists
        if (!wasEnabled) {
            exists = true
        }
        if (rawResize) {
            ox *= scaleX
            oy *= scaleY
            visibleSize?.scale(scaleX, scaleY)
        } else {
            val m = cl1(scaleX, scaleY)
            ox *= m
            oy *= m
            visibleSize?.scale(m, m)
        }
        if (resizesChildren) {
            children.fastEach { it.rescale(scaleX, scaleY) }
            components.fastEach { it.rescale(scaleX, scaleY) }
        } else {
            children.fastEach { it.rescale(1f, 1f) }
            components.fastEach { it.rescale(1f, 1f) }
        }
        clipDrawables()
        exists = wasEnabled
    }

    /** render this layout's components, and remove them if they are ready to be removed. */
    override fun render() {
        removeQueue.fastRemoveIfReversed {
            if (it.canBeRemoved()) {
                removeNow(it)
                true
            } else {
                false
            }
        }
        val delta = polyUI.delta
        val debug = polyUI.settings.debug
        needsRedraw = false
        preRender(delta)
        components.fastEach {
            if (!it.renders) return@fastEach
            it.preRender(delta)
            it.render()
            if (debug) it.debugRender()
            it.postRender()
        }
        renderChildren()
        if (debug) debugRender()
        postRender()
    }

    /** count the amount of drawables this contains, including the drawables its children have */
    fun countDrawables(): Int {
        var i = 0
        children.fastEach { i += it.countDrawables() }
        i += components.size
        return i
    }

    override fun debugPrint() {
        println("Layout: $simpleName")
        println("Children: ${children.size}")
        println("Components: ${components.size}")
        println("At: $at")
        println("Size: $size")
        println("Needs redraw: $needsRedraw")
        println("FBO: $fbo")
        println("Layout: $layout")
        println()
        children.fastEach { it.debugPrint() }
    }

    override fun debugRender() {
        // asm: currently disabled as new mode is actually useful and this isn't
//        if (!exists) return
//        val width = visibleSize?.width ?: this.width
//        val height = visibleSize?.height ?: this.height
//        val ofsX = ox - trueX
//        val ofsY = oy - trueY
//        renderer.hollowRect(ofsX, ofsY, width, height, colors.page.border20, 2f)
//        renderer.text(PolyUI.defaultFonts.regular, ofsX + 1f, ofsY + 1f, simpleName, colors.text.primary.normal, 10f)
    }

    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        if (!::propertyManager.isInitialized) propertyManager = polyUI.propertyManager.clone()
        colors = propertyManager.colors
        fonts = propertyManager.fonts
        components.fastEach {
            it.layout = this
            it.setup(renderer, polyUI)
        }
        children.fastEach {
            it.layout = this
            it.setup(renderer, polyUI)
        }
        initStage = INIT_SETUP
    }

    /**
     * This function will enable/disable rendering of drawables inside this layout based on their position and size.
     *
     * If they lie outside the bounds of this layout, they will not be drawn to save resources.
     * @since 0.21.4
     */
    open fun clipDrawables() {
        val x: Float
        val y: Float
        val width: Float
        val height: Float
        if (visibleSize != null) {
            x = ox
            y = oy
            width = visibleSize!!.width
            height = visibleSize!!.height
        } else {
            x = trueX
            y = trueY
            width = this.width
            height = this.height
        }
        children.fastEach {
            it.clipDrawables()
            it.renders = it.intersects(x, y, width, height)
        }
        components.fastEach {
            it.renders = it.intersects(x, y, width, height)
        }
    }

    override fun isInside(x: Float, y: Float): Boolean {
        return if (visibleSize != null) {
            val s = visibleSize!!
            val tx = ox
            val ty = oy
            x in tx..(tx + s.width) && y in ty..(ty + s.height)
        } else {
            super.isInside(x, y)
        }
    }

    override fun isInside(x: Float, y: Float, width: Float, height: Float): Boolean {
        return if (visibleSize != null) {
            val s = visibleSize!!
            val tx = ox
            val ty = oy
            tx in x..x + width && ty in y..y + height && tx + s.width in x..x + width && ty + s.height in y..y + height
        } else {
            super.isInside(x, y, width, height)
        }
    }

    override fun intersects(x: Float, y: Float, width: Float, height: Float): Boolean {
        return if (visibleSize != null) {
            val s = visibleSize!!
            val tx = ox
            val ty = oy
            return (x < tx + s.width && tx < x + width) && (y < ty + s.height && ty < y + height)
        } else {
            super.intersects(x, y, width, height)
        }
    }

    /** @see onColorsChanged */
    fun changeColors(colors: Colors) = onColorsChanged(colors)

    /** @see onColorsChanged */
    fun changeFonts(fonts: FontFamily) = onFontsChanged(fonts)

    override fun onColorsChanged(colors: Colors) {
        val wasEnabled = exists
        if (!wasEnabled) {
            exists = true
        }
        this.colors = colors
        for ((_, p) in propertyManager.properties) {
            p.colors = colors
        }
        components.fastEach { it.onColorsChanged(colors) }
        children.fastEach { it.onColorsChanged(colors) }
        exists = wasEnabled
    }

    override fun onFontsChanged(fonts: FontFamily) {
        val wasEnabled = exists
        if (!wasEnabled) {
            exists = true
        }
        this.fonts = fonts
        for ((_, p) in propertyManager.properties) {
            p.fonts = fonts
        }
        components.fastEach { it.onFontsChanged(fonts) }
        children.fastEach { it.onFontsChanged(fonts) }
        exists = wasEnabled
    }

    final override fun reset() {
        components.fastEach { it.reset() }
        children.fastEach { it.reset() }
    }

    /**
     * add a function that is called every [nanos] nanoseconds.
     * @since 0.17.1
     */
    fun every(nanos: Long, repeats: Int = 0, func: Layout.() -> kotlin.Unit): Layout {
        polyUI.every(nanos, repeats) {
            func(this)
        }
        return this
    }

    override fun toString(): String = "$simpleName(${trueX}x$trueY, ${width}x${height}${if (fbo != null) ", buffered" else ""}${if (fbo != null && needsRedraw) ", needsRedraw" else ""})"

    /**
     * Makes the layout draggable.
     *
     * When the layout is made draggable, it can be moved around by dragging it with the mouse.
     * This method adds an internal block component to the layout that handles the dragging functionality.
     *
     * This function should only be called in the initialization of your layout, and not programmatically.
     *
     * @see background
     * @since 0.19.2
     */
    fun draggable() = background(transparent = true, drags = true)

    /**
     * add scrolling functionality to the layout and optionally [scrollbars][withScrollbars].
     * @param size the scrollable area to set. This is the physical size of the layout on the screen.
     * @since 0.19.2
     */
    fun scrolling(size: Size<Unit>, withScrollbars: Boolean = true): Layout {
        require(visibleSize == null) { "${this.simpleName} is already scrolling!" }
        this.acceptsInput = true
        val anims = MutablePair<Animation?, Animation?>(null, null)
        var ofsX = ox
        var ofsY = oy
        var oox = ox
        var ooy = oy
        this.visibleSize = size
        var horizontalBar: Scrollbar? = null
        var verticalBar: Scrollbar? = null

        if (withScrollbars) {
            horizontalBar = Scrollbar(true)
            verticalBar = Scrollbar(false)
            add(horizontalBar, verticalBar)
            addEventHandler(MouseEntered) {
                horizontalBar.show()
                verticalBar.show()
                false
            }
        }

        addEventHandler(MouseScrolled()) { event: MouseScrolled ->
            if (!canBeRemoved()) return@addEventHandler false // disable scrolling when moving
            if ((visibleSize!!.width < width) && event.amountX != 0) {
                val anim = anims.first
                val rem = anim?.to?.minus(anim.value)?.also {
                    ofsX += anim.value
                } ?: 0f.also {
                    ofsX = x
                }
                anims.first = Animation.Type.EaseOutExpo.create(
                    .5.seconds,
                    0f,
                    scrollCalc(rem - event.amountX.toFloat(), ox, trueX, size.width, width),
                )
                horizontalBar?.cancelHide()
            }
            if ((visibleSize!!.height < height) && event.amountY != 0) {
                val anim = anims.second
                val rem = anim?.to?.minus(anim.value)?.also {
                    ofsY += anim.value
                } ?: 0f.also {
                    ofsY = y
                }
                anims.second = Animation.Type.EaseOutExpo.create(
                    .5.seconds,
                    0f,
                    scrollCalc(rem - event.amountY.toFloat(), oy, trueY, size.height, height),
                )
                verticalBar?.cancelHide()
            }

            needsRedraw = true
            true
        }
        addOperation(object : DrawableOp.Persistent(this) {
            override fun apply(renderer: Renderer) {
                if (ox != oox || oy != ooy) {
                    // external change watcher
                    ox = trueX
                    oy = trueY
                    oox = ox
                    ooy = oy
                    clipDrawables()
                }
                renderer.pushScissorIntersecting(ox - trueX, oy - trueY, size.width, size.height)
                val delta = polyUI.delta
                verticalBar?.tryHide(delta)
                horizontalBar?.tryHide(delta)
                val (anim, anim1) = anims
                if (anim?.isFinished == true) {
                    anims.first = null
                } else {
                    anim?.update(delta)?.also {
                        x = ofsX + anim.value
                        needsRedraw = true
                        clipDrawables()
                        polyUI.eventManager.recalculateMousePos()
                    }
                }
                if (anim1?.isFinished == true) {
                    anims.second = null
                } else {
                    anim1?.update(delta)?.also {
                        y = ofsY + anim1.value
                        needsRedraw = true
                        clipDrawables()
                        polyUI.eventManager.recalculateMousePos()
                    }
                }
            }

            override fun unapply(renderer: Renderer) {
                renderer.popScissor()
            }
        })
        val f: Layout.() -> kotlin.Unit = {
            ofsX = ox
            ofsY = oy
            oox = ox
            ooy = oy
            if (withScrollbars) {
                polyUI.every(0L) {
                    val d = polyUI.delta
                    verticalBar!!.tryHide(d)
                    horizontalBar!!.tryHide(d)
                }
            }
        }
        if (initStage == INIT_NOT_STARTED) {
            initCompleteHooks.add(f)
        } else {
            f(this)
        }
        return this
    }

    private fun scrollCalc(toAdd: Float, origin: Float, pos: Float, size: Float, totalSize: Float): Float {
        return if (toAdd > 0f) {
            kotlin.math.min(origin - pos, toAdd)
        } else {
            cl0(-(totalSize - size - (origin - pos)), toAdd)
        }
    }

    /**
     * Adds a background block to the layout.
     *
     * While there is nothing stopping you adding a background more than once, don't do it. It will cause problems.
     *
     * @param transparent whether the background should be transparent or not. This is only used if [drags] is `true`.
     * @param drags whether this background should handle dragging the layout around.
     *
     * @return `this`
     * @see draggable
     * @throws IllegalArgumentException if the method is called outside the initialization block.
     * @since 0.19.2
     */
    fun background(transparent: Boolean = false, drags: Boolean = false, cornerRadii: FloatArray = 8f.radii()): Layout {
        require(initStage == INIT_NOT_STARTED) { "background() can only be called in initialization block" }
        if (!drags) {
            this.components.add(
                0,
                Block(BlockProperties.background(cornerRadii), origin, fill, acceptInput = false, rawResize = true).also {
                    it.simpleName = "Background" + it.simpleName
                },
            )
        } else {
            draggable = true
            var mx = 0f
            var my = 0f
            var dragging = false
            components.add(
                0,
                Block(
                    properties = if (transparent) BlockProperties { Color.TRANSPARENT_PALETTE } else BlockProperties.background(cornerRadii),
                    origin,
                    fill,
                    rawResize = true,
                    acceptInput = true,
                    events = {
                        MousePressed(0) to {
                            dragging = true
                            mx = this@Layout.x - polyUI.mouseX
                            my = this@Layout.y - polyUI.mouseY
                        }
                        MouseReleased(0) to {
                            dragging = false
                        }
                        MouseMoved to {
                            if (dragging) {
                                if (!polyUI.eventManager.mouseDown) {
                                    dragging = false
                                }
                                this@Layout.x = polyUI.mouseX + mx
                                this@Layout.y = polyUI.mouseY + my
                            }
                        }
                    },
                ).also {
                    it.simpleName = "Drag" + it.simpleName
                },
            )
        }
        return this
    }

    companion object {
        /** wrapper for varargs, when arguments are in the wrong order */
        @JvmStatic
        fun drawables(vararg drawables: Drawable): Array<out Drawable> {
            return drawables
        }

        @JvmField
        val EMPTY_CMPLIST = ArrayList<Component>(0)

        @JvmField
        val EMPTY_CHLDLIST = ArrayList<Layout>(0)
    }
}
