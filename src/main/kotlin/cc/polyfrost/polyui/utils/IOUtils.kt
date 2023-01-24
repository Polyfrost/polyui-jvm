package cc.polyfrost.polyui.utils

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object IOUtils {
    @JvmStatic
    fun getResourceAsStream(fileName: String): InputStream {
        return IOUtils::class.java.getResourceAsStream(fileName) ?: throw Exception("Resource $fileName not found")
    }

    @JvmStatic
    fun getResourceAsStreamNullable(fileName: String): InputStream? {
        return IOUtils::class.java.getResourceAsStream(fileName)
    }

    fun InputStream.toByteBuffer(): ByteBuffer {
        val bytes = this.readBytes()
        this.close()
        return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).flip() as ByteBuffer
    }
}