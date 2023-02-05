@file:JvmName("IOUtils")

package cc.polyfrost.polyui.utils

import cc.polyfrost.polyui.PolyUI
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun getResourceStream(fileName: String): InputStream =
    getResourceStreamNullable(fileName)
        ?: throw FileNotFoundException(
            "Resource $fileName not found " +
                    "(check your Properties, and make sure the file " +
                    "is in the resources folder/on classpath)"
        )


fun getResourceStreamNullable(fileName: String): InputStream? =
    PolyUI::class.java.getResourceAsStream(fileName)
        ?: PolyUI::class.java.getResourceAsStream("/$fileName")
        ?: PolyUI::class.java.getResourceAsStream("/resources/$fileName")

fun InputStream.toByteBuffer(): ByteBuffer {
    val bytes = this.readBytes()
    this.close()
    return ByteBuffer.allocateDirect(bytes.size)
        .order(ByteOrder.nativeOrder())
        .put(bytes)
        .also { it.flip() }
}