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
import org.polyfrost.polyui.PolyUI.Companion.INPUT_NONE
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.component.extensions.countChildren
import org.polyfrost.polyui.data.Framebuffer
import org.polyfrost.polyui.operations.Recolor
import org.polyfrost.polyui.renderer.FramebufferController
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.AlignDefault
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.annotations.Locking
import org.polyfrost.polyui.utils.annotations.SideEffects
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.fastRemoveIfReversed

/**
 * # Drawable
 * The most basic thing in the PolyUI rendering system.
 *
 * Supports recoloring, animations, and the entire event system.
 */
abstract class Drawable(
    vararg children: Component? = arrayOf(),
    at: Vec2 = Vec2.ZERO,
    alignment: Align = AlignDefault,
    size: Vec2 = Vec2.ZERO,
    visibleSize: Vec2 = Vec2.ZERO,
    palette: Colors.Palette? = null,
    focusable: Boolean = false,
) : Cloneable, Scrollable(at, size, visibleSize, alignment, focusable) {
    init {
        if (children.isNotEmpty()) {
            this.children = children.filterNotNullTo(ArrayList<Component>(children.size)).also { list ->
                list.fastEach {
                    it.parent = this
                }
            }
        } else {
            this.children = null
        }
    }

    inline val renderer get() = polyUI.renderer

    @Locking
    @set:Synchronized
    var framebuffer: Framebuffer? = null
        private set

    /**
     * internal counter for framebuffer render count
     */
    private var fbc: Short = 0

    /**
     * internal storage for the color.
     */
    @ApiStatus.Internal
    @get:JvmName("getColorOrNull")
    @set:JvmName("setColorInternal")
    var _color: PolyColor? = null

    /**
     * The color of this drawable. can be any of the subtypes of this class depending on the current situation.
     *
     * The only safe methods here generally are getting the current color. casting it is tricky as PolyUI supports many different 'types' of color,
     * from gradients to chroma. For this reason, if you want to set it, use the [Recolor] operation on this drawable as it handles the conversions for you.
     *
     * **setting this value is marked as experimental as it is not generally advised**.
     */
    @set:ApiStatus.Internal
    var color: PolyColor
        get() = _color ?: throw UninitializedPropertyAccessException("Color is not initialized")
        set(value) {
            _color = value
        }

    private var _palette: Colors.Palette? = palette

    @SideEffects("color", "palette")
    var palette: Colors.Palette
        get() = _palette ?: throw UninitializedPropertyAccessException("Palette is not initialized")
        set(value) {
            _palette = value
            (_color as? PolyColor.Mut)?.recolor(value.get(inputState)) ?: run { _color = value.get(inputState) }
        }

    @SideEffects("_parent.needsRedraw", `when` = "field != value")
    @get:JvmName("needsRedraw")
    var needsRedraw = true
        set(value) {
            if (value && !field) {
                (_parent as? Drawable)?.needsRedraw = true
            }
            field = value
        }

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
     *
     * **Note:** This value is clamped to the parent's alpha value.
     * @since 0.20.0
     */
    var alpha = 1f
        get() = field.coerceIn(0f, (_parent as? Drawable)?.alpha ?: 1f)

    /** **a**t **c**ache **x** for transformations. */
    private var acx = 0f

    /** **a**t **c**ache **y** for transformations. */
    private var acy = 0f

    @Locking
    @Synchronized
    fun draw() {
        if (!renders) return
        require(initialized) { "Drawable $name is not initialized!" }

        val renderer = renderer
        val framebuffer = framebuffer
        val binds = framebuffer != null && fbc < 3
        if (binds) {
            renderer as FramebufferController
            if (!needsRedraw) {
                renderer.drawFramebuffer(framebuffer!!, x, y)
                return
            }
            renderer.bindFramebuffer(framebuffer!!)
            fbc++
        }

        needsRedraw = false
        preRender(polyUI.delta)
        render()
        children?.fastEach {
            if (it is Drawable) it.draw()
        }
        postRender()

        if (fbc > 0) fbc--
        if (binds) {
            renderer as FramebufferController
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
    protected open fun preRender(delta: Long) {
        val renderer = renderer
        renderer.push()
        operations?.fastEach { it.apply() }

        if (pushScroll(delta, renderer)) needsRedraw = true
        val r = rotation != 0.0
        val skx = skewX != 0.0
        val sky = skewY != 0.0
        val s = scaleX != 1f || scaleY != 1f
        if (r || skx || sky || s) {
            val mx = x + width / 2f
            val my = y + height / 2f
            if (renderer.transformsWithPoint()) {
                if (r) renderer.rotate(rotation, mx, my)
                if (skx) renderer.skewX(skewX, mx, my)
                if (sky) renderer.skewY(skewY, mx, my)
                if (s) renderer.scale(scaleX, scaleY, x, y)
            } else {
                renderer.translate(mx, my)
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
        popScroll(renderer)
        renderer.pop()
        if (acx != 0f) {
            x = acx
            y = acy
            acx = 0f
        }
    }

    override fun rescale0(scaleX: Float, scaleY: Float, withChildren: Boolean) {
        super.rescale0(scaleX, scaleY, withChildren)
        framebuffer?.let {
            val renderer = renderer as FramebufferController
            renderer.delete(it)
            framebuffer = renderer.createFramebuffer(width, height)
        }
    }

    fun debugDraw() {
        if (!renders) return
        debugRender()
        children?.fastEach {
            if (!it.renders) return@fastEach
            if (it is Drawable) it.debugDraw()
        }
    }

    /** add a debug render overlay for this drawable. This is always rendered regardless of the layout re-rendering if debug mode is on. */
    protected open fun debugRender() {
        val color = if (inputState > INPUT_NONE) polyUI.colors.page.border10 else polyUI.colors.page.border5
        val staticAt = this.screenAt
        val visibleSize = this.visibleSize
        renderer.hollowRect(staticAt.x, staticAt.y, visibleSize.x, visibleSize.y, color, 1f)
    }


    override fun setup(polyUI: PolyUI): Boolean {
        if (_color == null) {
            if (_palette == null) palette = polyUI.colors.component.bg
        }
        if (!super.setup(polyUI)) return false
        if (polyUI.canUseFramebuffers) {
            if (countChildren() > polyUI.settings.minDrawablesForFramebuffer || (this === polyUI.master && polyUI.settings.isMasterFrameBuffer)) {
                val renderer = renderer as FramebufferController
                framebuffer = renderer.createFramebuffer(width, height)
                if (polyUI.settings.debug) PolyUI.LOGGER.info("Drawable ${this.name} created with $framebuffer")
            }
        }
        return true
    }

    /**
     * Implement this function to enable cloning of your Drawable.
     * @since 0.19.0
     */
    public override fun clone() = (super.clone() as Drawable)

    override operator fun get(index: Int) = super.get(index) as? Drawable ?: throw IllegalArgumentException("Object at $index on $this is not a Drawable")
}
