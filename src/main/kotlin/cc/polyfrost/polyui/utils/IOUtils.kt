package cc.polyfrost.polyui.utils

import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object IOUtils {
    @JvmStatic
    fun getResourceAsStream(fileName: String): InputStream {
        return getResourceAsStreamNullable(fileName)
            ?: throw FileNotFoundException("Resource $fileName not found (check your Properties, and make sure the file is in the resources folder/on classpath)")
    }

    @JvmStatic
    fun getResourceAsStreamNullable(fileName: String): InputStream? {
        return IOUtils::class.java.getResourceAsStream(fileName)
            ?: IOUtils::class.java.getResourceAsStream("/$fileName")
            ?: IOUtils::class.java.getResourceAsStream("/resources/$fileName")
    }

    fun InputStream.toByteBuffer(): ByteBuffer {
        val bytes = this.readBytes()
        this.close()
        return ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes).flip() as ByteBuffer
    }
}