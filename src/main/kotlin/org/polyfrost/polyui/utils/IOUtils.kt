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

@file:JvmName("IOUtils")

package org.polyfrost.polyui.utils

import org.polyfrost.polyui.PolyUI
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * return a stream of the given resource or throws an exception.
 * @param resourcePath the path to the resource. Can either be a valid [URL] (including file URL), or a [resource path][Class.getResourceAsStream].
 * @throws java.io.IOException if an IO error occurs.
 * @throws FileNotFoundException if the URL is invalid and/or the resource is not found
 * @see getResourceStreamNullable
 */
fun getResourceStream(resourcePath: String, caller: Class<*> = PolyUI::class.java) =
    getResourceStreamNullable(resourcePath, caller)
        ?: throw FileNotFoundException(
            "Resource $resourcePath not found " +
                    "(check your Properties, and make sure the file " +
                    "is in the resources folder/on classpath; or the URL is valid)",
        )

/**
 * return a stream of the given resource, or null.
 * @param resourcePath the path to the resource. Can either be a valid [URL], [java.nio.file.Path] (including file URL), or a [resource path][Class.getResourceAsStream].
 * @throws java.io.IOException if an IO error occurs.
 * @see getResourceStream
 */
fun getResourceStreamNullable(resourcePath: String, caller: Class<*> = PolyUI::class.java): InputStream? {
    return if (":/" !in resourcePath) {
        caller.getResourceAsStream(resourcePath)
            ?: caller.getResourceAsStream("/$resourcePath")
    } else try {
        val url = URL(resourcePath)
        if ("http" in url.protocol) {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.useCaches = true
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (PolyUI)")
            connection.readTimeout = 5000
            connection.connectTimeout = 5000
            connection.doOutput = true
            connection.inputStream
        } else {
            url.openStream()
        }
    } catch (e: Exception) {
        PolyUI.LOGGER.error("Failed to get resource $resourcePath", e)
        null
    }
}

@Deprecated("This method is rather wasteful, and usage should be avoided.", ReplaceWith("getResourceStreamNullable?.apply {}"))
fun resourceExists(resourcePath: String, caller: Class<*> = PolyUI::class.java) = getResourceStreamNullable(resourcePath, caller)?.close() != null

fun ByteArray.toDirectByteBuffer(): ByteBuffer {
    return ByteBuffer.allocateDirect(this.size)
        .order(ByteOrder.nativeOrder())
        .put(this).flip() as ByteBuffer
}

/**
 * [toDirectByteBuffer] with a null terminator.
 * @since 1.1.6
 */
fun ByteArray.toDirectByteBufferNT(): ByteBuffer {
    return ByteBuffer.allocateDirect(this.size + 1)
        .order(ByteOrder.nativeOrder())
        .put(this).put(0).flip() as ByteBuffer
}

/**
 * Converts the InputStream into a byte array and close it.
 *
 * @return The byte array of the InputStream.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun InputStream.toByteArray(): ByteArray {
    val bytes = this.readBytes()
    this.close()
    return bytes
}
