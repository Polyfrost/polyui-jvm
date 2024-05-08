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

package org.polyfrost.polyui.renderer.data

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.utils.getResourceStream
import org.polyfrost.polyui.utils.toByteArray
import org.polyfrost.polyui.utils.toDirectByteBuffer
import org.polyfrost.polyui.utils.toDirectByteBufferNT
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

/**
 * representation of a resource, which can be loaded asynchronously or synchronously.
 *
 * For the definition of a resource, see [getResourceStream].
 * @property loadSync `true` if this resource should be loaded synchronously, `false` otherwise. (*optional operation*)
 * @since 1.1.6
 * @see load
 * @see loadAsync
 * @see loadDirect
 */
open class Resource(val resourcePath: String, @get:JvmName("shouldLoadSync") val loadSync: Boolean = ":/" !in resourcePath) {
    @Volatile
    private var initting = false

    /**
     * load the given resource asynchronously, mapping to definitely non-nullable type [T], calling the [consumer] with the result **on the main thread**.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see load
     * @see loadAsyncDirect
     */
    fun <T> loadAsync(errorHandler: (Throwable) -> Unit = { throw it }, mapper: (InputStream) -> T & Any, consumer: (T & Any) -> Unit) {
        if (initting) return
        initting = true
        CompletableFuture.supplyAsync { getResourceStream(resourcePath).use(mapper) }.whenComplete { it, err ->
            if (err != null) errorHandler(err)
            else consumer(it)
            initting = false
        }
    }

    /**
     * load the given resource asynchronously into a new byte array, calling the [consumer] with the result **on the main thread**.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see load
     * @see loadAsyncDirect
     */
    fun loadAsync(errorHandler: (Throwable) -> Unit = { throw it }, consumer: (ByteArray) -> Unit) {
        if (initting) return
        initting = true
        CompletableFuture.supplyAsync { getResourceStream(resourcePath).toByteArray() }.whenComplete { it, err ->
            if (err != null) errorHandler(err)
            else consumer(it)
            initting = false
        }
    }

    /**
     * load the given resource asynchronously into a new direct byte buffer, calling the [consumer] with the result **on the main thread**.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see load
     * @see loadAsync
     */
    fun loadAsyncDirect(errorHandler: (Throwable) -> Unit = { throw it }, consumer: (ByteBuffer) -> Unit) {
        if (initting) return
        initting = true
        CompletableFuture.supplyAsync { getResourceStream(resourcePath).toDirectByteBuffer() }.whenComplete { it, err ->
            if (err != null) errorHandler(err)
            else consumer(it)
            initting = false
        }
    }

    /**
     * load the given resource asynchronously into a new direct byte buffer, with null termination, calling the [consumer] with the result **on the main thread**.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see load
     * @see loadAsync
     */
    fun loadAsyncDirectNT(errorHandler: (Throwable) -> Unit = { throw it }, consumer: (ByteBuffer) -> Unit) {
        if (initting) return
        initting = true
        CompletableFuture.supplyAsync { getResourceStream(resourcePath).toDirectByteBufferNT() }.whenComplete { it, err ->
            if (err != null) errorHandler(err)
            else consumer(it)
            initting = false
        }
    }

    /**
     * load the given resource synchronously, mapping to definitely non-nullable type [T]. you may then perform any operations on the result.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see loadAsync
     * @see loadDirect
     */
    inline fun <T> load(errorHandler: (Throwable) -> T & Any = { throw it }, mapper: (InputStream) -> T & Any) = try {
        getResourceStream(resourcePath).use(mapper)
    } catch (e: Throwable) {
        errorHandler(e)
    }

    /**
     * load the given resource synchronously into a new byte array. you may then perform any operations on the result.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see loadAsync
     * @see loadDirect
     */
    inline fun load(errorHandler: (Throwable) -> ByteArray = { throw it }) = try {
        getResourceStream(resourcePath).toByteArray()
    } catch (e: Throwable) {
        errorHandler(e)
    }

    /**
     * load the given resource synchronously into a new direct byte buffer. you may then perform any operations on the result.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see loadAsyncDirect
     * @see load
     */
    inline fun loadDirect(errorHandler: (Throwable) -> ByteBuffer = { throw it }) = try {
        getResourceStream(resourcePath).toDirectByteBuffer()
    } catch (e: Throwable) {
        errorHandler(e)
    }

    /**
     * load the given resource synchronously into a new direct byte buffer, with null termination. you may then perform any operations on the result.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see loadAsyncDirect
     * @see load
     */
    inline fun loadDirectNT(errorHandler: (Throwable) -> ByteBuffer = { throw it }) = try {
        getResourceStream(resourcePath).toDirectByteBufferNT()
    } catch (e: Throwable) {
        errorHandler(e)
    }

    /**
     * return the stream of this resource. **it is the caller's responsibility to close the stream.**
     */
    @Suppress("NOTHING_TO_INLINE")
    @ApiStatus.Obsolete(since = "1.1.6")
    inline fun stream() = getResourceStream(resourcePath)


    override fun toString() = "Resource(file=$resourcePath)"

    override fun hashCode() = resourcePath.hashCode()

    override fun equals(other: Any?) = resourcePath == other
}
