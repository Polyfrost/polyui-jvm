/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors. All rights reserved.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

@file:JvmName("IOUtils")

package cc.polyfrost.polyui.utils

import cc.polyfrost.polyui.PolyUI
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * return a stream of the given resource or throws an exception.
 * @param resourcePath the path to the resource. Can either be a valid [URL] (including file URL), or a [resource path][Class.getResourceAsStream].
 * @throws java.io.IOException if an IO error occurs.
 * @throws FileNotFoundException if the URL is invalid and/or the resource is not found
 * @see getResourceStreamNullable
 * @see getResources
 */
fun getResourceStream(resourcePath: String): InputStream =
    getResourceStreamNullable(resourcePath)
        ?: throw FileNotFoundException(
            "Resource $resourcePath not found " +
                "(check your Properties, and make sure the file " +
                "is in the resources folder/on classpath; or the URL is valid)"
        )

/** get all resources matching the given path and (optionally) extension. Keep empty to ignore extensions. */
fun getResources(path: String, extension: String = ""): List<Pair<String, InputStream>> {
    val resources = PolyUI::class.java.classLoader.getResources(path)
    val out = ArrayList<Pair<String, InputStream>>()
    while (resources.hasMoreElements()) {
        val resource = resources.nextElement()
        if (extension.isEmpty() || resource.path.endsWith(extension)) {
            out.add(
                Pair(
                    resource.path,
                    resource.openStream()
                )
            )
        }
    }
    return out
}

/**
 * return a stream of the given resource or null.
 * @param resourcePath the path to the resource. Can either be a valid [URL] (including file URL), or a [resource path][Class.getResourceAsStream].
 * @throws java.io.IOException if an IO error occurs.
 * @see getResourceStream
 */
fun getResourceStreamNullable(resourcePath: String): InputStream? {
    return try {
        URL(resourcePath).openStream()
    } catch (e: Exception) {
        PolyUI::class.java.getResourceAsStream(resourcePath)
            ?: PolyUI::class.java.getResourceAsStream("/$resourcePath")
            ?: PolyUI::class.java.getResourceAsStream("/resources/$resourcePath")
    }
}

fun InputStream.toByteBuffer(): ByteBuffer {
    val bytes = this.readBytes()
    this.close()
    return ByteBuffer.allocateDirect(bytes.size)
        .order(ByteOrder.nativeOrder())
        .put(bytes)
        .also { it.flip() }
}
