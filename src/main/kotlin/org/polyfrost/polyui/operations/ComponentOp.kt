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

package org.polyfrost.polyui.operations

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable

/**
 * # ComponentOp
 *
 * component operations represent simple procedures that are ran before
 * and after every render cycle. They can support animations and verification.
 *
 * @since 1.6.1
 */
fun interface ComponentOp {
    /**
     * Apply the operation, this is called before the component is rendered.
     */
    fun apply()

    /**
     * Unapply the operation. this is called after the component is rendered.
     *
     * return `true` to instantly remove this operation from the component.
     */
    fun unapply() = false

    /**
     * override this function to verify that this operation is valid, which is run before it is applied. (default: always `true`)
     *
     * @return `true` if this operation is deemed to be valid.
     * @since 1.1.1
     */
    fun verify() = true

    /**
     * If this is `true`, when this drawable operation is attempted to be added, if one is already present, instead of replacing it, it will be ignored.
     * @since 1.5.0
     */
    fun exclusive() = false

    /**
     * Class to represent an operation that can be applied on a component that modifies it in some way.
     *
     * It is applied before and after rendering, for example a [Move], [Resize], or [Rotate] operation, or a transition.
     */
    abstract class Animatable<S : Component>(protected val self: S, protected val animation: Animation? = null, var onFinish: (S.() -> Unit)? = null) :
        ComponentOp {

        override fun apply() {
            apply(animation?.update(self.polyUI.delta) ?: 1f)
            if (animation?.isFinished == false && self is Drawable) self.needsRedraw = true
            return
        }

        override fun unapply(): Boolean {
            unapply(animation?.value ?: 1f)
            if (animation?.isFinished != false) {
                onFinish?.invoke(self)
                onFinish = null
                return true
            } else {
                return false
            }
        }

        fun add() {
            self.addOperation(this)
        }

        /**
         * finish this operation immediately.
         *
         * This is marked as experimental as it might not always work as expected.
         * @since 1.0.5
         */
        @ApiStatus.Experimental
        open fun finishNow(): Boolean {
            animation?.finishNow()
            apply(1f)
            return unapply()
        }

        open fun reset() {
            animation?.reset()
        }

        open fun reverse() {
            animation?.reverse()
        }

        /** apply this drawable operation.
         *
         * the renderer is provided in case you want to do some transformations. A state will already be [pushed][org.polyfrost.polyui.renderer.Renderer.push] for you.
         *
         * **please note that this is NOT intended** to be used directly for rendering of objects, and only for transformations.
         */
        protected abstract fun apply(value: Float)

        /**
         * de-apply this operation, if required.
         */
        protected open fun unapply(value: Float) {
            // no-op
        }
    }
}
