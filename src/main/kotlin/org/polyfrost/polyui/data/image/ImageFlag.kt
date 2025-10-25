package org.polyfrost.polyui.data.image

class ImageFlag<T>(val key: FlagKey<T>, val value: T) {
    override fun toString(): String = "ImageFlag(${key.name}=$value)"

    override fun hashCode(): Int = key.hashCode() * 31 + value.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is ImageFlag<*>) return false

        return key == other.key && value == other.value
    }
}
