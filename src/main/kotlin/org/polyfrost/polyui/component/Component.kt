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

package org.polyfrost.polyui.component

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.PolyUI.Companion.INIT_COMPLETE
import org.polyfrost.polyui.PolyUI.Companion.INIT_NOT_STARTED
import org.polyfrost.polyui.PolyUI.Companion.INIT_SETUP
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.animate.Animations
import org.polyfrost.polyui.animate.KeyFrames
import org.polyfrost.polyui.color.Color
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.event.*
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.renderer.data.FontFamily
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit

/**
 * A component is a drawable object that can be interacted with. <br>
 *
 * It has a [properties] attached to it, which contains various pieces of
 * information about how this component should look, and its default responses
 * to event.
 */
abstract class Component @JvmOverloads constructor(
    properties: Properties? = null,
    /** position relative to this layout. */
    at: Point<Unit>,
    override var size: Size<Unit>? = null,
    rawResize: Boolean = false,
    acceptInput: Boolean = true,
    events: EventDSL<Component>.() -> kotlin.Unit = {},
) : Drawable(at, rawResize, acceptInput) {
    private var props: Properties? = properties

    /** properties for the component. This is `open` so you can cast it, like so:
     * ```
     * override val properties
     *     get() = super.properties as YourProperties
     * ```
     * @see Properties
     */
    open val properties get() = props!!

    /** the color of this component. */
    @Transient
    lateinit var color: Color.Animated

    /**
     * represents the hue value to return to when a chroma color is animated to something else, and is set back.
     */
    @Transient
    private var hueToReturnTo = 0f

    @Transient
    protected var autoSized = false

    @Transient
    protected var finishColorFunc: (Component.() -> kotlin.Unit)? = null

    /**
     * This is a reference to the parent of this component. It is currently only used by [trueX] and [trueY].
     *
     * Controlled by [ContainingComponent]
     */
    @ApiStatus.Internal
    @Transient
    var parent: Drawable? = null

    @Transient
    override lateinit var layout: Layout

    @Transient
    protected var keyframes: KeyFrames? = null

    @Transient
    override var consumesHover = true

    /**
     * Disabled flag for this component. Dispatches the [Disabled] and [Enabled] events by default, leaving it to the [Properties] implementation
     * to handle the disabled state.
     *
     * @since 0.21.4
     */
    var disabled = false
        set(value) {
            field = value
            if (!value) {
                accept(Enabled)
            } else {
                accept(Disabled)
            }
        }

    /**
     * Note that this method will return the [x] if this method is called before the component is added to a layout.
     * @see [Drawable.trueX]
     */
    override fun trueX(): Float {
        var x = this.x
        if (!::layout.isInitialized) return x
        var parent: Drawable? = this.parent ?: this.layout
        while (parent != null) {
            x += parent.x
            parent = (parent as? Component)?.parent ?: parent.layout
        }
        return x
    }

    /**
     * Note that this method will return the [y] if this method is called before the component is added to a layout.
     * @see [Drawable.trueY]
     */
    override fun trueY(): Float {
        var y = this.y
        if (!::layout.isInitialized) return y
        var parent: Drawable? = this.parent ?: this.layout
        while (parent != null) {
            y += parent.y
            parent = (parent as? Component)?.parent ?: parent.layout
        }
        return y
    }

    init {
        events(events)
    }

    override fun accept(event: Event): Boolean {
        if (disabled) return false
        if (super.accept(event)) return true
        return properties.eventHandlers[event]?.let { it(this, event) } == true
    }

    /**
     * Changes the color of the component to the specified color.
     * Supports animation and callback function on finish.
     *
     * If the color is a [Color.Gradient], this component's color will be changed to a gradient between the current color and the specified color.
     *
     * If the color is a [Color.Chroma], this component's color will be changed to a chroma color, keeping its current hue for consistency.
     * **Note:** This will look wierd if the current color is a gradient, and the brightness, saturation and alpha are NOT animated, so will change instantly.
     *
     * @param toColor The color to change the component to.
     * @param animation The type of animation to use.
     * @param durationNanos The duration of the animation in nanoseconds.
     * @param onFinish The callback function to execute when the color change animation finishes.
     * @since 0.19.1
     */
    open fun recolor(
        toColor: Color,
        animation: Animation.Type? = null,
        durationNanos: Long = 1L.seconds,
        onFinish: (Component.() -> kotlin.Unit)? = null,
    ) {
        var finishFunc = onFinish
        when (toColor) {
            is Color.Gradient -> {
                if (color !is Color.Gradient || (color as Color.Gradient).type != toColor.type) {
                    color = Color.Gradient(color, color.clone(), toColor.type)
                }
                val c = color as Color.Gradient
                c.recolor(0, toColor[0], animation, durationNanos)
                c.recolor(1, toColor[1], animation, durationNanos)
            }

            is Color.Chroma -> {
                if (color !is Color.Chroma) {
                    color = Color.Chroma(toColor.speedNanos, toColor.brightness, toColor.saturation, toColor.alpha, color.hue)
                } else {
                    val c = color as Color.Chroma
                    c.speedNanos = toColor.speedNanos
                    c.brightness = toColor.brightness
                    c.saturation = toColor.saturation
                    c.alpha = toColor.alpha
                }
                color.hue = hueToReturnTo
            }

            else -> {
                if (color is Color.Gradient) {
                    val c = color as Color.Gradient
                    c.recolor(0, toColor, animation, durationNanos)
                    c.recolor(1, toColor, animation, durationNanos)
                    finishFunc = {
                        color = toColor.toAnimatable()
                    }
                } else {
                    if (color is Color.Chroma) {
                        color = (color as Color.Chroma).freeze()
                        hueToReturnTo = color.hue
                    }
                    color.recolor(toColor, animation, durationNanos)
                }
            }
        }
        wantRedraw()
        finishColorFunc = finishFunc
    }

    override fun onColorsChanged(colors: Colors) {
        properties.colors = colors
        recolor(properties.palette.normal)
    }

    override fun onFontsChanged(fonts: FontFamily) {
        properties.fonts = fonts
    }

    override fun calculateBounds() {
        require(initStage != INIT_NOT_STARTED) { "${this.simpleName} has not been setup, but calculateBounds() was called!" }
        if (size == null) {
            size = if (properties.size != null) {
                properties.size!!.clone()
            } else {
                autoSized = true
                calculateSize()
                    ?: throw UnsupportedOperationException("calculateSize() not implemented for ${this.simpleName}!")
            }
        }
        doDynamicSize()
        if (initStage != INIT_COMPLETE) {
            initStage = INIT_COMPLETE
            onInitComplete()
        }
    }

    override fun addOperation(drawableOp: DrawableOp, onFinish: (Drawable.() -> kotlin.Unit)?) {
        if (::layout.isInitialized) {
            wantRedraw()
        }
        operations.add(drawableOp to onFinish)
    }

    /** change the properties attached to this component.
     * @see Properties
     */
    fun setProperties(properties: Properties) {
        if (polyUI.settings.debug) PolyUI.LOGGER.info("{}'s properties set to {}", this.simpleName, properties)
        properties.colors = props!!.colors
        properties.fonts = props!!.fonts
        props = properties
        recolor(properties.palette.normal, Animations.Linear, 150L.milliseconds)
        wantRedraw()
    }

    @MustBeInvokedByOverriders
    override fun setup(renderer: Renderer, polyUI: PolyUI) {
        super.setup(renderer, polyUI)
        if (props == null) {
            props = layout.propertyManager.get(this)
        }
        props!!.init(layout.colors, layout.fonts)
        color = properties.palette.normal.toAnimatable()
        initStage = INIT_SETUP
    }

    @MustBeInvokedByOverriders
    override fun preRender(deltaTimeNanos: Long) {
        super.preRender(deltaTimeNanos)
        if (keyframes != null) {
            if (keyframes!!.update(deltaTimeNanos)) {
                keyframes = null
            } else {
                wantRedraw()
            }
        }

        if (color.update(deltaTimeNanos)) {
            finishColorFunc?.invoke(this)
            finishColorFunc = null
        }
        if (color.updating || color.alwaysUpdates) wantRedraw()
    }

    override fun debugRender() {
        val color = if (mouseOver) properties.colors.page.border20 else properties.colors.page.border10
        renderer.hollowRect(x, y, width, height, color, 1f)
    }

    override fun canBeRemoved(): Boolean = super.canBeRemoved() && !color.updating && keyframes == null

    override fun toString(): String {
        return when (initStage) {
            INIT_COMPLETE -> "$simpleName(${trueX}x$trueY, ${width}x${height}${if (autoSized) " (auto)" else ""}${if (operations.isNotEmpty()) ", operating" else ""})"
            else -> simpleName
        }
    }

    /**
     * Make this component draggable.
     * @param consumesEvent weather beginning/ending dragging should cancel the corresponding mouse event
     */
    fun draggable(consumesEvent: Boolean = false): Component {
        var hovered = false
        var mx = 0f
        var my = 0f
        addEventHandler(MousePressed(0)) {
            hovered = true
            mx = polyUI.eventManager.mouseX - x
            my = polyUI.eventManager.mouseY - y
            consumesEvent
        }
        addEventHandler(MouseReleased(0)) {
            hovered = false
            consumesEvent
        }
        addOperation(object : DrawableOp.Persistent(this) {
            override fun apply(renderer: Renderer) {
                if (hovered) {
                    if (!polyUI.eventManager.mouseDown) {
                        hovered = false
                        return
                    }
                    x = polyUI.eventManager.mouseX - mx
                    y = polyUI.eventManager.mouseY - my
                }
            }
        })
        return this
    }

    fun addKeyframes(k: KeyFrames) {
        keyframes = k
        wantRedraw()
    }

    /**
     * add a function that is called every [nanos] nanoseconds.
     * @since 0.17.1
     */
    fun every(nanos: Long, repeats: Int = 0, func: Component.() -> kotlin.Unit): Component {
        polyUI.every(nanos, repeats) {
            func(this)
        }
        return this
    }

    fun wantRedraw() {
        layout.needsRedraw = true
    }
}
