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

package org.polyfrost.polyui.renderer.data

import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.getResourceStream
import org.polyfrost.polyui.utils.getResourceStreamNullable
import java.io.InputStream
import java.util.concurrent.CompletableFuture

/**
 * Abstract representation of a resource. This is used to lazily load resources.
 *
 * @since 0.21.1
 */
abstract class Resource(val resourcePath: String) {
    var init = false
        private set(value) {
            field = value
            if (value) runCallbacks()
        }
    private var initting = false
    private var _stream: InputStream? = null
    val completionCallbacks = ArrayList<Resource.() -> Unit>(0)

    val stream: InputStream?
        get() {
            if (!init) {
                _stream = getResourceStreamNullable(resourcePath)
                init = true
            }
            return _stream
        }

    fun get() = stream ?: throw NullPointerException("Resource $resourcePath not found!")

    fun close() {
        _stream?.close()
        initting = false
        init = false
    }

    fun getAsync(exceptionHandler: (Throwable) -> Unit = { it.printStackTrace() }, callback: InputStream.() -> Unit) {
        if (!init) {
            if (!initting) {
                initting = true
                CompletableFuture.supplyAsync {
                    getResourceStream(resourcePath)
                }.thenAcceptAsync {
                    _stream = it
                    init = true
                    callback(it!!)
                }.exceptionally {
                    exceptionHandler(it)
                    initting = false
                    null
                }
            }
        } else {
            callback(_stream!!)
        }
    }

    fun getAsyncNullable(exceptionHandler: (Throwable) -> Unit = { it.printStackTrace() }, callback: InputStream?.() -> Unit) {
        if (!init) {
            if (!initting) {
                initting = true
                CompletableFuture.supplyAsync {
                    getResourceStreamNullable(resourcePath)
                }.thenAcceptAsync {
                    _stream = it
                    init = true
                    callback(it)
                    runCallbacks()
                }.exceptionally {
                    exceptionHandler(it)
                    initting = false
                    null
                }
            }
        } else {
            callback(_stream)
        }
    }

    private fun runCallbacks() {
        completionCallbacks.fastEach {
            it()
        }
        completionCallbacks.clear()
        completionCallbacks.trimToSize()
    }

    override fun toString() = "Resource(file=$resourcePath, init=$init)"

    override fun hashCode() = resourcePath.hashCode()

    override fun equals(other: Any?) = resourcePath == other
}
