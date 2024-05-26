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
import org.polyfrost.polyui.component.Drawable

abstract class DrawableOp(protected val self: Drawable) {
    abstract fun apply()

    abstract fun unapply(): Boolean

    /**
     * override this function to verify that this operation is valid, which is run before it is applied. (default: always `true`)
     *
     * @return `true` if this operation is deemed to be valid.
     * @since 1.1.1
     */
    open fun verify(): Boolean = true

    fun add() {
        self.addOperation(this)
    }

    fun remove() {
        self.removeOperation(this)
    }

    /**
     * Class to represent an operation that can be applied on a component that modifies it in some way.
     *
     * It is applied before and after rendering, for example a [Move], [Resize], or [Rotate] operation, or a transition.
     */
    abstract class Animatable<T : Drawable>(self: T, protected val animation: Animation? = null, var onFinish: (T.() -> Unit)? = null) :
        DrawableOp(self) {
        protected var isFinished = false

        override fun apply() {
            if (isFinished) return
            apply(animation?.update(self.polyUI.delta) ?: 1f)
            if (animation?.isFinished == false) self.needsRedraw = true
            return
        }

        override fun unapply(): Boolean {
            if (isFinished) return true
            unapply(animation?.value ?: 1f)
            if (animation?.isFinished != false) {
                isFinished = true
                @Suppress("unchecked_cast")
                onFinish?.invoke(self as T)
                onFinish = null
                return true
            } else {
                return false
            }
        }

        /**
         * finish this operation immediately.
         *
         * This is marked as experimental as it might not always work as expected.
         * @since 1.0.5
         */
        @ApiStatus.Experimental
        open fun finishNow() {
            animation?.finishNow()
            apply(1f)
            unapply(1f)
        }

        open fun reset() {
            animation?.reset()
            isFinished = false
        }

        open fun reverse() {
            animation?.reverse()
            isFinished = false
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
