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

package org.polyfrost.polyui.input

import org.polyfrost.polyui.utils.IntArraySet
import org.polyfrost.polyui.utils.nullIfEmpty
import java.util.function.Consumer

/**
 * Java-style builder for creating a [PolyBind] instance.
 * @since 1.7.02
 */
open class KeybindHelper<T : KeybindHelper<T>> {
    protected var duration = 0L
        private set
    protected var keys = ArrayList<Keys>(2)
        private set

    @get:JvmName("getMods")
    protected var mods = Modifiers(0)
        private set
    protected var unmappedKeys = IntArraySet(2)
        private set
    protected var mouse = IntArraySet(2)
        private set
    protected var func: ((Boolean) -> Boolean)? = null
        private set
    
    @Suppress("UNCHECKED_CAST", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    @kotlin.internal.InlineOnly
    protected inline fun self(): T = this as T

    open fun build(): PolyBind {
        val func = func ?: throw IllegalStateException("Function must be set")
        return PolyBind(
            unmappedKeys.nullIfEmpty()?.toIntArray(),
            keys.ifEmpty { null }?.toTypedArray(),
            mouse.nullIfEmpty()?.toIntArray(),
            mods, duration, func
        )
    }

    fun keys(vararg keys: Keys): T {
        this.keys.addAll(keys)
        return self()
    }

    fun mods(mods: Byte): T {
        this.mods = Modifiers(mods)
        return self()
    }

    fun mods(vararg mods: KeyModifiers): T {
        var b = 0
        for (mod in mods) {
            b = b or mod.value.toInt()
        }
        this.mods = Modifiers(b.toByte())
        return self()
    }

    fun keys(vararg keys: Int): T {
        for (key in keys) {
            this.unmappedKeys.add(key)
        }
        return self()
    }

    @OverloadResolutionByLambdaReturnType
    @JvmName("doesZ")
    fun does(func: (Boolean) -> Boolean): T {
        this.func = func
        return self()
    }

    @OverloadResolutionByLambdaReturnType
    fun does(func: (Boolean) -> Unit): T {
        this.func = { func(it); true }
        return self()
    }

    fun does(func: Consumer<Boolean>): T {
        this.func = { func.accept(it); true }
        return self()
    }


    fun duration(duration: Long): T {
        this.duration = duration
        return self()
    }


    companion object {
        @JvmStatic
        fun builder() = KeybindHelper()
    }
}
