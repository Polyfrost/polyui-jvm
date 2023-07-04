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

package cc.polyfrost.polyui.layout

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import cc.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import cc.polyfrost.polyui.PolyUI.Companion.INIT_SETUP
import cc.polyfrost.polyui.animate.Animation
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.color.Colors
import cc.polyfrost.polyui.component.Component
import cc.polyfrost.polyui.component.Drawable
import cc.polyfrost.polyui.component.DrawableOp
import cc.polyfrost.polyui.component.impl.Block
import cc.polyfrost.polyui.event.Events
import cc.polyfrost.polyui.property.PropertyManager
import cc.polyfrost.polyui.property.impl.BackgroundBlockProperties
import cc.polyfrost.polyui.property.impl.BlockProperties
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.unit.*
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.utils.*
import org.jetbrains.annotations.ApiStatus

/**
 * # Layout
 * Layout is PolyUI's take on containers. They can contain [components] and other [layouts][children], as children.
 *
 * They can dynamically [add][addComponent] and [remove][removeComponent] their children and components.
 *
 * They are responsible for all their children's sizes, positions and rendering.
 *
 * @see cc.polyfrost.polyui.layout.impl.FlexLayout
 * @see cc.polyfrost.polyui.layout.impl.PixelLayout
 * @see cc.polyfrost.polyui.layout.impl.SwitchingLayout
 */
abstract class Layout(
    at: Point<Unit>,
    override var size: Size<Unit>? = null,
    internal val onAdded: (Drawable.() -> kotlin.Unit)? = null,
    internal val onRemoved: (Drawable.() -> kotlin.Unit)? = null,
    propertyManager: PropertyManager? = null,
    rawResize: Boolean = false,
    /**
     * If this layout resizes its children (**components and layouts!**) when it is resized.
     * @since 0.19.0
     */
    val resizesChildren: Boolean = true,
    /** If this layout can receive events (separate to its children!). */
    acceptInput: Boolean = false,
    vararg drawables: Drawable
) : Drawable(at, rawResize, acceptInput) {
    open lateinit var propertyManager: PropertyManager

    init {
        if (propertyManager != null) {
            @Suppress("LeakingThis")
            this.propertyManager = propertyManager
        }
    }
    lateinit var colors: Colors
        internal set

    /** list of components in this layout. */
    val components = drawables.filterIsInstance<Component>() as ArrayList
        get() = if (enabled) field else EMPTY_CMPLIST

    /** list of child layouts in this layout */
    val children = drawables.filterIsInstance<Layout>() as ArrayList
        get() = if (enabled) field else EMPTY_CHLDLIST

    /**
     * Weather this layout needs redrawing.
     */
    open var needsRedraw = true
        set(value) {
            if (value && !field) {
                layout?.needsRedraw = true
            }
            field = value
        }

    override fun trueX(): Float {
        trueX = super.trueX()
        children.fastEach { it.trueX = it.trueX() }
        components.fastEach { it.trueX = it.trueX() }
        return trueX
    }

    override fun trueY(): Float {
        trueY = super.trueY()
        children.fastEach { it.trueY = it.trueY() }
        components.fastEach { it.trueY = it.trueY() }
        return trueY
    }

    /** tracker variable for framebuffer disabling/enabling. don't touch this. */
    protected var fboTracker = 0

    /** removal queue of drawables.
     * @see removeComponent
     */
    protected val removeQueue = arrayListOf<Drawable>()

    /** set this to true if you want this layout to never use a framebuffer. Recommended in situations with chroma colors */
    open var refuseFramebuffer: Boolean = false

    /** framebuffer attached to this layout.
     * @see refuseFramebuffer
     * @see cc.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer
     */
    internal var fbo: Framebuffer? = null
        set(value) {
            if (!refuseFramebuffer) field = value
        }

    /** reference to parent */
    override var layout: Layout? = null

    /**
     * These hooks are ran when the [onInitComplete] function is called.
     * @since 0.19.2
     */
    val initCompleteHooks = ArrayList<Layout.() -> kotlin.Unit>(5)

    /**
     * This flag simply controls whether the layout exists. If it is false, it will not be rendered, and will report having 0 components.
     *
     * @since 0.19.0
     */
    var enabled = true

    /**
     * Flag that is true if the layout can be dragged around.
     * @see draggable
     * @see background
     * @since 0.19.2
     */
    var draggable = false
        private set

    init {
        if (onAdded != null) addHandler(Events.Added, onAdded)
        if (onRemoved != null) addHandler(Events.Removed, onRemoved)
    }

    /** this is the function that is called every frame. It decides whether the layout needs to be entirely re-rendered.
     * If so, the [render] function is called which will redraw all its' and components, and this function will redraw all its child layouts as well.
     *
     * If this layout is [using a framebuffer][cc.polyfrost.polyui.property.Settings.minDrawablesForFramebuffer], it will be [drawn][rasterize] to the framebuffer, and then drawn to the screen.
     *
     * **Note:** Do not call this function yourself.
     */
    open fun reRenderIfNecessary() {
        if (initStage != INIT_COMPLETE) throw IllegalStateException("${this.simpleName} was attempted to be rendered before it was fully initialized (stage $initStage)")
        if (!enabled) return
        rasterChildren()
        rasterize()
        if (fbo != null && fboTracker < 2) {
            renderer.drawFramebuffer(fbo!!, x, y, width, height)
        } else {
            val x = x
            val y = y
            renderer.pushScissor(x, y, width, height)
            renderer.translate(x, y)
            render()
            renderChildren()
            renderer.translate(-x, -y)
            renderer.popScissor()
            if (!needsRedraw && fboTracker > 1) {
                needsRedraw = true
                fboTracker = 0
            }
        }
    }

    /**
     * This function will rasterize the layout to its framebuffer, if it has one.
     * @since 0.18.0
     */
    protected fun rasterize() {
        if (fbo != null && needsRedraw && fboTracker < 2) {
            fboTracker++
            renderer.bindFramebuffer(fbo)
            render()
            renderChildren()
            renderer.unbindFramebuffer(fbo)
        }
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
        components.fastEach(function)
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
     * return a component in this layout, by its simple name. This can be accessed using [debugPrint][cc.polyfrost.polyui.PolyUI.debugPrint] or using the [field][cc.polyfrost.polyui.component.Drawable.simpleName].
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
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= children.size).
     * @throws ClassCastException if the layout at the specified index is not of type T.
     * @since 0.19.2
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Layout> getLayout(index: Int): T = children[index] as T

    /**
     * return a component in this layout, by its simple name. This can be accessed using [debugPrint][cc.polyfrost.polyui.PolyUI.debugPrint] or using the [field][cc.polyfrost.polyui.component.Drawable.simpleName].
     *
     * **Note that** the [simpleName] is **NOT** consistent between updates to PolyUI, or changes to the content of the layout.
     * Returns null if not found.
     * @see get
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Drawable?> getOrNull(simpleName: String): T? { // <T : Drawable?> as it removes useless null checks (null cannot be cast to not-null type T)
        components.fastEach { if (it.simpleName == simpleName) return it as T }
        children.fastEach {
            return if (it.simpleName == simpleName) { it as T } else {
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
    fun addComponents(components: Collection<Drawable>) {
        components.forEach { addComponent(it) }
    }

    /**
     * adds the given components/layouts to this layout.
     *
     * this will add the drawables to the [Layout.components] or [children] list, and invoke its added event function.
     */
    fun addComponents(vararg components: Drawable) {
        components.forEach { addComponent(it) }
    }

    /**
     * adds the given component/layout to this layout.
     *
     * this will add the drawable to the [Layout.components] or [children] list, and invoke its [Events.Added] if it is present.
     */
    open fun addComponent(drawable: Drawable) {
        when (drawable) {
            is Component -> {
                if (components.addOrReplace(drawable) != null) PolyUI.LOGGER.warn("${drawable.simpleName} was attempted to be added to layout ${this.simpleName} multiple times!")
                if (initStage > INIT_NOT_STARTED) drawable.layout = this
            }

            is Layout -> {
                if (children.addOrReplace(drawable) != null) PolyUI.LOGGER.warn("${drawable.simpleName} was attempted to be added to layout ${this.simpleName} multiple times!")
                if (initStage > INIT_NOT_STARTED) drawable.layout = this
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        if (initStage > INIT_NOT_STARTED) drawable.setup(renderer, polyui)
        if (initStage == INIT_COMPLETE) {
            drawable.calculateBounds()
            drawable.onParentInitComplete()
            drawable.accept(Events.Added)
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
    open fun removeComponent(drawable: Drawable) {
        when (drawable) {
            is Component -> {
                removeQueue.add(components[components.indexOf(drawable)])
            }

            is Layout -> {
                removeQueue.add(children[children.indexOf(drawable)])
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        drawable.accept(Events.Removed)
    }

    /**
     * Removes a component from this layout at the specified index.
     *
     * @param index The index of the component to be removed.
     * @see removeComponent
     * @since 0.19.2
     */
    open fun removeComponent(index: Int) {
        removeComponent(components[index])
    }

    /**
     * Removes a component from this layout immediately.
     *
     * @param index The index of the component to be removed.
     * @see removeComponentNow
     * @since 0.19.2
     */
    open fun removeComponentNow(index: Int) {
        removeComponentNow(components[index])
    }

    /** removes a component immediately, without waiting for it to finish up.
     *
     * This is marked as internal because you should be using [removeComponent] for most cases to remove a component, as it waits for it to finish and play any removal animations.
     */
    @ApiStatus.Internal
    open fun removeComponentNow(drawable: Drawable?) {
        if (drawable == null) return
        removeQueue.remove(drawable)
        when (drawable) {
            is Component -> {
                if (!components.remove(drawable)) {
                    PolyUI.LOGGER.warn(
                        "Tried to remove component {} from {}, but it wasn't found!",
                        drawable,
                        this
                    )
                }
            }

            is Layout -> {
                if (!children.remove(drawable)) {
                    PolyUI.LOGGER.warn(
                        "Tried to remove layout {} from {}, but it wasn't found!",
                        drawable,
                        this
                    )
                }
            }

            else -> {
                throw Exception("Drawable $drawable is not a component or layout!")
            }
        }
        needsRedraw = true
    }

    override fun calculateBounds() {
        if (initStage == INIT_NOT_STARTED) throw IllegalStateException("${this.simpleName} has not been setup, but calculateBounds() was called!")
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
    }

    override fun onInitComplete() {
        super.onInitComplete()
        initCompleteHooks.fastEach { it(this) }
        components.fastEach { it.onParentInitComplete() }
        children.fastEach { it.onParentInitComplete() }
    }

    override fun rescale(scaleX: Float, scaleY: Float) {
        super.rescale(scaleX, scaleY)
        if (resizesChildren) {
            children.fastEach { it.rescale(scaleX, scaleY) }
            components.fastEach { it.rescale(scaleX, scaleY) }
        } else {
            children.fastEach { it.rescale(1f, 1f) }
            components.fastEach { it.rescale(1f, 1f) }
        }
    }

    /** render this layout's components, and remove them if they are ready to be removed. */
    override fun render() {
        removeQueue.fastEach { if (it.canBeRemoved()) removeComponentNow(it) }
        val delta = polyui.delta
        needsRedraw = false
        preRender(delta)
        components.fastEach {
            it.preRender(delta)
            it.render()
            it.postRender()
        }
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
        if (!enabled) return
        renderer.hollowRect(trueX, trueY, width, height, colors.page.border20, 2f)
        renderer.text(Renderer.DefaultFont, trueX + 1f, trueY + 1f, simpleName, colors.text.primary, 10f)
        children.fastEach { it.debugRender() }
        components.fastEach { it.debugRender() }
    }

    override fun setup(renderer: Renderer, polyui: PolyUI) {
        super.setup(renderer, polyui)
        if (!::propertyManager.isInitialized) propertyManager = PropertyManager(polyui)
        colors = propertyManager.colors
        components.fastEach {
            it.layout = this
            it.setup(renderer, polyui)
        }
        children.fastEach {
            it.layout = this
            it.setup(renderer, polyui)
        }
        initStage = INIT_SETUP
    }

    /** @see onColorsChanged */
    fun changeColors(colors: Colors) = onColorsChanged(colors)

    override fun onColorsChanged(colors: Colors) {
        this.colors = colors
        for ((_, p) in propertyManager.properties) {
            p.colors = colors
        }
        components.fastEach { it.onColorsChanged(colors) }
        children.fastEach { it.onColorsChanged(colors) }
    }

    override fun canBeRemoved() = !needsRedraw

    final override fun reset() {
        components.fastEach { it.reset() }
        children.fastEach { it.reset() }
    }

    /**
     * add a function that is called every [nanos] nanoseconds.
     * @since 0.17.1
     */
    fun every(nanos: Long, repeats: Int = 0, func: Layout.() -> kotlin.Unit): Layout {
        polyui.every(nanos, repeats) {
            func(this)
        }
        return this
    }

    override fun toString(): String {
        return "$simpleName(${trueX}x$trueY, ${width}x${height}${if (fbo != null) ", buffered" else ""}${if (fbo != null && needsRedraw) ", needsRedraw" else ""})"
    }

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
     */
    @Deprecated("not currently implemented")
    fun scrolling(size: Size<Unit>, withScrollbars: Boolean = true): Layout {
        this.acceptsInput = true
        val anims = MutablePair<Animation?, Animation?>(null, null)
        var ox = 0f
        var oy = 0f
        var ofsX = ox
        var ofsY = oy

        addEventHandler(Events.MouseScrolled()) { e: Events, _ ->
            e as Events.MouseScrolled // this is annoying...
            if (e.amountX != 0) {
                val anim = anims.first
                val rem = anim?.to?.minus(anim.value)?.also {
                    ofsX += anim.value
                } ?: 0f.also {
                    ofsX = x
                }
                anims.first = Animation.Type.EaseOutExpo.create(
                    .5.seconds,
                    0f,
                    scrollCalc(rem - e.amountX.toFloat(), ox, x, size.width, width)
                )
            }
            if (e.amountY != 0) {
                val anim = anims.second
                val rem = anim?.to?.minus(anim.value)?.also {
                    ofsY += anim.value
                } ?: 0f.also {
                    ofsY = y
                }
                anims.second = Animation.Type.EaseOutExpo.create(
                    .5.seconds,
                    0f,
                    scrollCalc(rem - e.amountY.toFloat(), oy, y, size.height, height)
                )
            }

            needsRedraw = true
            true
        }
        addOperation(object : DrawableOp(this) {
            override fun apply(renderer: Renderer) {
                renderer.pushScissor(ox - trueX, oy - trueY, size.width, size.height)
                val delta = polyui.delta
                val (anim, anim1) = anims
                if (anim?.isFinished == true) {
                    anims.first = null
                } else {
                    anim?.update(delta)?.also {
                        x = ofsX + anim.value
                        needsRedraw = true
                    }
                    polyui.eventManager.recalculateMousePos()
                }
                if (anim1?.isFinished == true) {
                    anims.second = null
                } else {
                    anim1?.update(delta)?.also {
                        y = ofsY + anim1.value
                        needsRedraw = true
                    }
                    polyui.eventManager.recalculateMousePos()
                }
            }

            override fun unapply(renderer: Renderer) {
                renderer.popScissor()
            }

            override val isFinished get() = false
        })
        initCompleteHooks.add {
            ox = trueX
            oy = trueY
            ofsX = ox
            ofsY = oy
        }

        if (withScrollbars) {
            // todo
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
                Block(BackgroundBlockProperties(cornerRadii), origin, fill, acceptInput = false, rawResize = true).also {
                    it.simpleName = "Background" + it.simpleName
                }
            )
        } else {
            draggable = true
            var mx = 0f
            var my = 0f
            var dragging = false
            components.add(
                0,
                Block(
                    properties = if (transparent) BlockProperties(Color.TRANSPARENT) else BackgroundBlockProperties(cornerRadii),
                    origin,
                    fill,
                    true,
                    true,
                    Events.MousePressed(0) to {
                        dragging = true
                        mx = this@Layout.x - polyui.mouseX
                        my = this@Layout.y - polyui.mouseY
                    },
                    Events.MouseReleased(0) to {
                        dragging = false
                    },
                    Events.MouseMoved to {
                        if (dragging) {
                            this@Layout.x = polyui.mouseX + mx
                            this@Layout.y = polyui.mouseY + my
                        }
                    }
                ).also {
                    it.simpleName = "Drag" + it.simpleName
                }
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
