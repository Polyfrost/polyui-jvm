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

package org.polyfrost.polyui.operations

import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.component.Drawable

abstract class DrawableOp(protected val self: Drawable) {
    abstract fun apply()

    abstract fun unapply(): Boolean

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

        final override fun apply() {
            if (isFinished) return
            apply(animation?.update(self.polyUI.delta) ?: 1f)
            return
        }

        @Suppress("unchecked_cast")
        final override fun unapply(): Boolean {
            if (isFinished) return true
            unapply(animation?.value ?: 1f)
            if (animation?.isFinished != false) {
                isFinished = true
                onFinish?.invoke(self as T)
                onFinish = null
                return true
            } else {
                return false
            }
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

        protected var isFinished = false
    }
}
