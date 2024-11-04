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

import org.polyfrost.polyui.utils.nullIfEmpty

/**
 * Java-style builder for creating a [KeyBinder.Bind] instance.
 * @since 1.7.02
 */
open class KeybindHelper {
    protected var duration = 0L
        private set
    protected var keys = ArrayList<Keys>(2)
        private set

    @get:JvmName("getMods")
    protected var mods = Modifiers(0)
        private set
    protected var unmappedKeys = ArrayList<Int>(2)
        private set
    protected var mouse = ArrayList<Int>(2)
        private set
    protected var func: ((Boolean) -> Boolean)? = null
        private set

    open fun build(): KeyBinder.Bind {
        val func = func ?: throw IllegalStateException("Function must be set")
        return KeyBinder.Bind(
            unmappedKeys.nullIfEmpty()?.toIntArray(),
            keys.nullIfEmpty()?.toTypedArray(),
            mouse.nullIfEmpty()?.toIntArray(),
            mods, duration, func
        )
    }

    fun keys(vararg keys: Keys): KeybindHelper {
        this.keys.addAll(keys)
        return this
    }

    fun mods(mods: Byte): KeybindHelper {
        this.mods = Modifiers(mods)
        return this
    }

    fun mods(vararg mods: KeyModifiers): KeybindHelper {
        var b = 0
        for (mod in mods) {
            b = b or mod.value.toInt()
        }
        this.mods = Modifiers(b.toByte())
        return this
    }

    fun chars(vararg chars: Char): KeybindHelper {
        for(char in chars) {
            this.unmappedKeys.add(char.code)
        }
        return this
    }

    fun keys(vararg keys: Int): KeybindHelper {
        for (key in keys) {
            this.unmappedKeys.add(key)
        }
        return this
    }

    @OverloadResolutionByLambdaReturnType
    @JvmName("doesZ")
    fun does(func: (Boolean) -> Boolean): KeybindHelper {
        this.func = func
        return this
    }

    @OverloadResolutionByLambdaReturnType
    fun does(func: (Boolean) -> Unit): KeybindHelper {
        this.func = { func(it); true }
        return this
    }


    fun duration(duration: Long): KeybindHelper {
        this.duration = duration
        return this
    }


    companion object {
        @JvmStatic
        fun builder() = KeybindHelper()
    }
}
