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

package org.polyfrost.polyui.data

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.utils.getResourceStream
import org.polyfrost.polyui.utils.toByteArray
import java.io.InputStream
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask

/**
 * representation of a resource, which can be loaded asynchronously or synchronously.
 *
 * For the definition of a resource, see [getResourceStream].
 * @property loadSync `true` if this resource should be loaded synchronously, `false` otherwise. (*optional operation*)
 * @since 1.1.6
 * @see load
 * @see loadAsync
 */
open class Resource(val resourcePath: String, @Transient @get:JvmName("shouldLoadSync") val loadSync: Boolean = "http" !in resourcePath) {
    @Volatile
    @Transient
    private var initializing = false

    @Transient
    private var callbacks: ArrayList<() -> Unit>? = null

    /**
     * return the stream of this resource. **it is the caller's responsibility to close the stream.**
     */
    open fun stream(): InputStream = getResourceStream(resourcePath)

    /**
     * Return this resource's contents as an array of bytes.
     */
    open fun bytes() = stream().toByteArray()

    /**
     * load the given resource asynchronously, mapping to definitely non-nullable type [T], calling the [consumer] with the result **on the worker thread**.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see load
     */
    fun <T> loadAsync(errorHandler: (Throwable) -> Unit = { throw it }, mapper: (ByteArray) -> T & Any, consumer: (T & Any) -> Unit) {
        if (initializing) return
        initializing = true
        doAsync(supplier = { bytes().run(mapper) }) { it, err ->
            if (err != null) errorHandler(err)
            else consumer(it)
            initializing = false
        }
    }

    /**
     * load the given resource asynchronously into a new byte array, calling the [consumer] with the result **on the worker thread**.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see load
     */
    fun loadAsync(errorHandler: (Throwable) -> Unit = { throw it }, consumer: (ByteArray) -> Unit) {
        if (initializing) return
        initializing = true
        doAsync(supplier = { bytes() }) { it, err ->
            if (err != null) errorHandler(err)
            else consumer(it)
            initializing = false
        }
    }

    /**
     * load the given resource synchronously, mapping to definitely non-nullable type [T]. you may then perform any operations on the result.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see loadAsync
     */
    inline fun <T> load(errorHandler: (Throwable) -> T & Any = { throw it }, mapper: (ByteArray) -> T & Any) = try {
        bytes().run(mapper)
    } catch (e: Throwable) {
        errorHandler(e)
    }

    /**
     * load the given resource synchronously into a new byte array. you may then perform any operations on the result.
     *
     * In case of failure, the [errorHandler] will be called with the exception.
     * @since 1.1.6
     * @see loadAsync
     */
    inline fun load(errorHandler: (Throwable) -> ByteArray = { throw it }) = try {
        bytes()
    } catch (e: Throwable) {
        errorHandler(e)
    }

    private fun <T> doAsync(supplier: () -> T, func: (T, Throwable?) -> Unit) {
        ForkJoinPool.commonPool().submit(object : ForkJoinTask<Void>() {
            override fun getRawResult() = null

            override fun exec() = try {
                func(supplier(), null)
                true
            } catch (e: Throwable) {
                @Suppress("UNCHECKED_CAST")
                func(null as T, e)
                false
            }

            override fun setRawResult(value: Void?) {}
        }
        )
    }

    /**
     * Add a callback that will be executed once this resource is about to be initialized by a rendering implementation.
     *
     * @since 1.7.31
     */
    fun onInit(func: () -> Unit) {
        val callbacks = callbacks ?: ArrayList(2)
        callbacks.add(func)
        this.callbacks = callbacks
    }

    /**
     * Use this method to execute any callbacks that were added before the resource was initialized.
     *
     * **This method needs to be called by a rendering implementation at the appropriate time.**
     * @since 1.7.31
     */
    @ApiStatus.Internal
    fun reportInit() {
        val callbacks = callbacks ?: return
        callbacks.forEach { it() }
        callbacks.clear()
        callbacks.trimToSize()
        this.callbacks = null
    }


    override fun toString() = "Resource(file=$resourcePath)"

    override fun hashCode() = resourcePath.hashCode()

    override fun equals(other: Any?) = (other as? Resource)?.resourcePath == this.resourcePath
}
